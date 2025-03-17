package com.alexzhelyapov1;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

public class File {

    private final Path filePath;
    private String sha512Hash; // Храним хеш, чтобы не вычислять каждый раз
    private Instant lastEditTime;

    public File(String filePathString) throws IOException, NoSuchAlgorithmException {
        this(Paths.get(filePathString));
    }

    public File(Path filePath) throws IOException, NoSuchAlgorithmException {
        this.filePath = filePath;
        this.sha512Hash = null;
        this.lastEditTime = null;
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            throw new IOException("File does not exist or is a directory: " + filePath);
        }
        //Получаем время изменения файла сразу же
        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
        this.lastEditTime = attrs.lastModifiedTime().toInstant();
    }

    public Path getPath() {
        return filePath;
    }

    public String getSha512Hash() throws IOException, NoSuchAlgorithmException {
        if (sha512Hash == null) {  // Вычисляем хеш, только если он ещё не был вычислен
            sha512Hash = calculateSha512Hash();
        }
        return sha512Hash;
    }

    public Instant getLastEditTime() {
        return lastEditTime;
    }

    // Приватный метод для вычисления хеша
    private String calculateSha512Hash() throws IOException, NoSuchAlgorithmException {
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            throw new IOException("File does not exist or is a directory: " + filePath);
        }

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        try (InputStream fis = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192]; // Буфер для чтения файла
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        byte[] digest = md.digest();

        // Преобразование байтового массива в шестнадцатеричную строку
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    //Пример использования
    public static void main(String[] args) {
        try {
            // Создание временного файла для теста
            Path tempFile = Files.createTempFile("test", ".txt");
            Files.write(tempFile, "This is a test file.".getBytes());

            File myFile = new File(tempFile.toString());

            System.out.println("File path: " + myFile.getPath());
            System.out.println("SHA-512 hash: " + myFile.getSha512Hash());
            //Выведем повторно. Хеш не должен пересчитываться
            System.out.println("SHA-512 hash: " + myFile.getSha512Hash());

            // Удаление временного файла
            Files.delete(tempFile);

            //Попытка получить хеш несуществующего файла
            File nonExistentFile = new File("nonexistent.txt");
            System.out.println("Path of nonExistentFile " + nonExistentFile.getPath());
            nonExistentFile.getSha512Hash(); // Ожидается IOException
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}