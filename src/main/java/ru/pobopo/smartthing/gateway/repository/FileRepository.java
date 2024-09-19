package ru.pobopo.smartthing.gateway.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Slf4j
public class FileRepository<T> {
    private final ObjectMapper objectMapper;
    private final Path repoFile;

    private final Set<T> data = ConcurrentHashMap.newKeySet();

    public FileRepository(Path repoFile, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.repoFile = repoFile;

        log.info("Using file repo: {}", repoFile);
        loadFromFile();
    }

    public Collection<T> getAll() {
        return Collections.unmodifiableCollection(data);
    }

    public void add(T value) {
        data.add(value);
    }

    public void remove(T value) {
        data.remove(value);
    }

    public Optional<T> find(Predicate<T> predicate) {
        return data.stream().filter(predicate).findFirst();
    }

    @SneakyThrows
    public void commit() {
        objectMapper.writeValue(repoFile.toFile(), data);
    }

    public void rollback() {
        loadFromFile();
    }

    @SneakyThrows
    private void loadFromFile() {
        if (!Files.exists(repoFile)) {
            Files.createDirectories(repoFile.getParent());
            Files.writeString(repoFile, "[]");
        } else {
            try {
                this.data.addAll(objectMapper.readValue(repoFile.toFile(), new TypeReference<>() {}));
            } catch (MismatchedInputException exception) {
                throw new RuntimeException("Failed to load saved devices", exception);
            }
        }
    }
}
