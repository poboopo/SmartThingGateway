package ru.pobopo.smartthing.gateway.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Slf4j
public class FileRepository<T> {
    private final Class<T> clazz;
    private final ObjectMapper objectMapper;
    private final Path repoFile;

    private final Set<T> data = ConcurrentHashMap.newKeySet();

    public FileRepository(Class<T> clazz, Path repoFile, ObjectMapper objectMapper) {
        this.clazz = clazz;
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

    public void delete(T value) {
        data.remove(value);
    }

    public Optional<T> find(Predicate<T> predicate) {
        return data.stream().filter(predicate).findFirst();
    }

    @SneakyThrows
    public synchronized void commit() {
        // todo add lock
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
            List<T> loaded = objectMapper.readValue(
                    repoFile.toFile(),
                    objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz)
            );
            this.data.clear();
            this.data.addAll(loaded);
        }
    }
}
