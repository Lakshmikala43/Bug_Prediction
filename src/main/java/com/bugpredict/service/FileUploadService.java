package com.bugpredict.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FileUploadService {

    private static final java.util.Set<String> SKIPPED_DIR_PREFIXES = java.util.Set.of(
            "node_modules/", ".git/", "target/", "build/", "dist/", ".next/", ".cache/",
            "vendor/", ".idea/", ".vscode/", "obj/", "bin/", "__pycache__/", ".venv/", "venv/"
    );

    public Path extractZipArchive(MultipartFile multipartFile, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(multipartFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName().replace('\\', '/');

                // Instant Speed: Skip dependency trees and cache folders completely
                boolean shouldSkip = false;
                for (String skipPrefix : SKIPPED_DIR_PREFIXES) {
                    if (entryName.contains(skipPrefix) || entryName.startsWith(skipPrefix)) {
                        shouldSkip = true;
                        break;
                    }
                }
                if (shouldSkip) {
                    zis.closeEntry();
                    continue;
                }

                // Prevent Zip Slip vulnerability
                Path resolvedPath = targetDir.resolve(entry.getName()).normalize();
                if (!resolvedPath.startsWith(targetDir)) {
                    throw new SecurityException("Malicious ZIP entry with path traversal: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    if (resolvedPath.getParent() != null) {
                        Files.createDirectories(resolvedPath.getParent());
                    }
                    try (OutputStream os = Files.newOutputStream(resolvedPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        zis.transferTo(os);
                    }
                }
                zis.closeEntry();
            }
        }
        return targetDir;
    }
}
