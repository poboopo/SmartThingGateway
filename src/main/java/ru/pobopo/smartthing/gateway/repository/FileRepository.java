package ru.pobopo.smartthing.gateway.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.pobopo.smartthing.annotation.FileRepoId;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class FileRepository<T> {
    private final Class<T> clazz;
    private final ObjectMapper objectMapper;
    private final String repoDirectory;
    private final Method getIdMethod;

    public FileRepository(Class<T> clazz, Path repoDirectory, ObjectMapper objectMapper) throws IOException {
        this.clazz = clazz;
        this.objectMapper = objectMapper;
        this.repoDirectory = repoDirectory.toString();

        Optional<Field> idField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(FileRepoId.class) && f.getType().equals(UUID.class)).findAny();
        if (idField.isEmpty()) {
            throw new IllegalArgumentException("Class " + clazz + " is missing id field of UUID type! (FileRepoId annotated field not found)");
        }

        String fieldName = idField.get().getName();
        String getterName = String.format("get%s%s", fieldName.substring(0, 1).toUpperCase(), fieldName.substring(1));

        Optional<Method> method = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.getName().equals(getterName) && m.getReturnType().equals(UUID.class))
                .findFirst();
        if (method.isEmpty()) {
            throw new IllegalArgumentException("Can't find getter for id field in class " + clazz + " (searched name = " + getterName + ")");
        }
        getIdMethod = method.get();

        log.debug("Using file repository directory {} (for {})", repoDirectory, clazz);
        if (!Files.exists(repoDirectory)) {
            Files.createDirectories(repoDirectory);
        }
    }

    public Collection<T> getAll() {
        return readAllFiles();
    }

    @SneakyThrows
    public Optional<T> findById(UUID id) {
        Path file = objectPath(id);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(file.toFile(), clazz));
    }

    @SneakyThrows
    public void add(T value) {
        Path file = objectPath(getId(value));
        if (Files.exists(file)) {
            throw new IllegalStateException("Passed id already in use!");
        }
        objectMapper.writeValue(file.toFile(), value);
    }

    @SneakyThrows
    public void update(T value) {
        Path file = objectPath(getId(value));
        if (!Files.exists(file)) {
            throw new IllegalStateException("Can't find given object file");
        }
        objectMapper.writeValue(file.toFile(), value);
    }

    @SneakyThrows
    public void delete(UUID id) {
        Path file = objectPath(id);
        if (!Files.exists(file)) {
            throw new IllegalStateException("Can't find given object file");
        }
        Files.delete(file);
    }

    private Path objectPath(UUID id) {
        return Path.of(repoDirectory, id.toString() + ".json");
    }

    private UUID getId(T value) {
        try {
            Object id = getIdMethod.invoke(value);
            if (id == null) {
                throw new IllegalStateException("Id field can't be null!");
            }
            if (id instanceof UUID) {
                return (UUID) id;
            }
            throw new IllegalStateException("Id getter returned wrong value type (expected UUID)");
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to call {} method", getIdMethod.getName(), e);
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private List<T> readAllFiles() {
        try (Stream<Path> stream = Files.list(Path.of(repoDirectory))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(file -> {
                        try {
                            return objectMapper.readValue(file.toFile(), clazz);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        }
    }
}
