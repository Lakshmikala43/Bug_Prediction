package com.bugpredict.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipArchiverService {

    public void zipDirectory(Path sourceDirPath, Path targetZipPath) throws IOException {
        Files.createDirectories(targetZipPath.getParent());

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(targetZipPath))) {
            Files.walk(sourceDirPath)
                 .filter(path -> !Files.isDirectory(path))
                 .forEach(path -> {
                     ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString().replace('\\', '/'));
                     try {
                         zos.putNextEntry(zipEntry);
                         Files.copy(path, zos);
                         zos.closeEntry();
                     } catch (IOException e) {
                         throw new UncheckedIOException(e);
                     }
                 });
        }
    }
}