package com.alexzhelyapov1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

public class Test {
    private static final Path test_directory_path = Paths.get("C:\\Users\\Public\\Music\\test_directory");

    public static Path CreateTestDirectory() throws IOException {
        if (Files.exists(test_directory_path) && Files.isDirectory(test_directory_path)) {
            return test_directory_path;
        }

        Path tempDir = Files.createDirectory(test_directory_path);

        Path file1 = Files.createFile(tempDir.resolve("file1.txt"));
        Files.write(file1, "Content of file1".getBytes());
        Path file2 = Files.createFile(tempDir.resolve("file2(copy1).txt"));
        Files.write(file2, "Content of file1".getBytes());
        Path file3 = Files.createFile(tempDir.resolve("file3.txt"));
        Files.write(file3, "Content of file3".getBytes());

        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));

        Path file4 = Files.createFile(subDir.resolve("file4.txt"));
        Files.write(file4, "Content of file4".getBytes());

        Path file5 = Files.createFile(subDir.resolve("file5(copy3).txt"));
        Files.write(file5, "Content of file3".getBytes());

        Path file6 = Files.createFile(subDir.resolve("file1.txt"));
        Files.write(file6, "Content of file6".getBytes());

        return tempDir;
    }

    public static void DeleteTestDirectory() throws IOException {
        if (Files.exists(test_directory_path) && Files.isDirectory(test_directory_path)) {
            Files.walk(test_directory_path)
                    .sorted((a, b) -> -a.compareTo(b)) // Сортируем в обратном порядке для удаления вложенных файлов/директорий первыми
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete: " + path + ", " + e.getMessage());
                        }
                    });
            System.out.println("Cleaned up temporary directory.");
        }
    }

    public static void main(String[] args) {
        try {
            CreateTestDirectory();
            DeleteTestDirectory();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();  //  Вывод стека вызовов для отладки
        }
    }
}
