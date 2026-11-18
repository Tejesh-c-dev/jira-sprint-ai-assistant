package com.ablmvp.sprintassistant.data.csv;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;

public interface CsvFileAccess {
    boolean exists(Path path);
    boolean isRegularFile(Path path);
    boolean isReadable(Path path);
    long size(Path path) throws IOException;
    Reader newBufferedReader(Path path) throws IOException;
    byte[] readAllBytes(Path path) throws IOException;
}
