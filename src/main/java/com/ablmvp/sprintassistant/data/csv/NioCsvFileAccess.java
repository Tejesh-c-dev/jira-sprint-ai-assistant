package com.ablmvp.sprintassistant.data.csv;

import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NioCsvFileAccess implements CsvFileAccess {
    @Override
    public boolean exists(Path path) {
        return Files.exists(path);
    }

    @Override
    public boolean isRegularFile(Path path) {
        return Files.isRegularFile(path);
    }

    @Override
    public boolean isReadable(Path path) {
        return Files.isReadable(path);
    }

    @Override
    public long size(Path path) throws IOException {
        return Files.size(path);
    }

    @Override
    public Reader newBufferedReader(Path path) throws IOException {
        BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        reader.mark(1);
        int first = reader.read();
        if (first != 0xfeff) {
            reader.reset();
        }
        return reader;
    }

    @Override
    public byte[] readAllBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }
}
