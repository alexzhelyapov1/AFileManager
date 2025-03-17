package com.alexzhelyapov1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Directory {

    private final Path directoryPath;
    private List<Directory> subdirectories;
    private List<File> files;

    public Directory(String directoryPathString) throws IOException {
        this(Paths.get(directoryPathString));
    }

    public Directory(Path directoryPath) throws IOException {
        this.directoryPath = directoryPath;
        this.subdirectories = new ArrayList<>();
        this.files = new ArrayList<>();

        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
            throw new IOException("Path does not exist or is not a directory: " + directoryPath);
        }
    }

    // Добавлен метод для получения списка поддиректорий
    public List<Directory> getSubdirectories() {
        return subdirectories;
    }

    // Добавлен метод для получения списка файлов
    public List<File> getFiles() {
        return files;
    }

    public Path getPath() {
        return directoryPath;
    }

    // Инициирует сканирование директории и поддиректорий
    public void scan() throws IOException, NoSuchAlgorithmException {
        // Очищаем списки перед новым сканированием
        subdirectories.clear();
        files.clear();

        try (Stream<Path> entries = Files.list(directoryPath)) {
            for (Path entry : entries.collect(Collectors.toList())) {
                if (Files.isDirectory(entry)) {
                    // Рекурсивно создаем и сканируем поддиректории
                    Directory subdirectory = new Directory(entry);
                    subdirectory.scan(); // Рекурсивный вызов scan()
                    subdirectories.add(subdirectory);
                } else {
                    // Создаем объекты File
                    files.add(new File(entry.toString()));
                }
            }
        }
    }


    // Метод main для демонстрации
    public static void main(String[] args) {
        try {
            // Создаем временную директорию и несколько файлов/поддиректорий для теста
            Path tempDir = Files.createTempDirectory("test_directory");
            Path file1 = Files.createFile(tempDir.resolve("file1.txt"));
            Files.write(file1, "Content of file1".getBytes());
            Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
            Path file2 = Files.createFile(subDir.resolve("file2.txt"));
            Files.write(file2, "Content of file2".getBytes());


            // Создаем объект Directory
            Directory directory = new Directory(tempDir);
            System.out.println("Directory path: " + directory.getPath());

            //Вызываем скан
            directory.scan();

            // Выводим информацию о файлах и поддиректориях (включая хеши и время изменения)
            System.out.println("Files:");
            for (File file : directory.getFiles()) {
                System.out.println("  Name: " + file.getPath().getFileName());
                System.out.println("  SHA-512 Hash: " + file.getSha512Hash());
                System.out.println("  Last Edit Time: " + file.getLastEditTime());
            }

            System.out.println("Subdirectories:");
            for (Directory subdir : directory.getSubdirectories()) {
                System.out.println("  Name: " + subdir.getPath().getFileName());
                for (File file : subdir.getFiles()) {
                    System.out.println("    File Name: " + file.getPath().getFileName());
                    System.out.println("    File SHA-512 Hash: " + file.getSha512Hash());
                    System.out.println("    File Last Edit Time: " + file.getLastEditTime());
                }
            }


            // Очищаем временную директорию (рекурсивно)
            Files.walk(tempDir)
                    .sorted((a, b) -> -a.compareTo(b)) // Сортируем в обратном порядке для удаления вложенных файлов/директорий первыми
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete: " + path + ", " + e.getMessage());
                        }
                    });
            System.out.println("Cleaned up temporary directory.");


        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();  //  Вывод стека вызовов для отладки
        }
    }
}
