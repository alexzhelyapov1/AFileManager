package com.alexzhelyapov1;

import javax.tools.Tool;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class DuplicateManager {

    private final List<Directory> directories; // Список директорий

    public DuplicateManager(List<Directory> directories) {
        this.directories = directories;
    }

    public Map<byte[], List<File>> buildHashTable() throws InterruptedException, ExecutionException {
        //Используем ConcurrentHashMap для потокобезопасности
        ConcurrentHashMap<byte[], List<File>> hashTable = new ConcurrentHashMap<>();

        // Создаем пул потоков с фиксированным размером, равным количеству доступных процессоров
        // Это позволяет контролировать количество одновременно выполняемых задач.
        List<Future<?>> futures;
        try (ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {

            // Список для хранения Future объектов
            futures = new ArrayList<>();
            // Обходим все директории
            for (Directory directory : directories) {
                // Обходим все файлы в директории
                for (File file : directory.getFiles()) {
                    // Создаем задачу Callable для вычисления хеша файла
                    Callable<Void> task = () -> {
                        try {
                            byte[] hash = file.getSha512Hash(); // Вычисляем хеш
                            // Потокобезопасное добавление в ConcurrentHashMap
                            hashTable.computeIfAbsent(hash, k -> new ArrayList<>()).add(file);
                        } catch (NoSuchAlgorithmException | IOException e) {
                            System.err.println("Error calculating hash for file: " + file.getPath() + ": " + e.getMessage());
                        }
                        return null; // Возвращаем null, так как результат не нужен
                    };
                    // Отправляем задачу на выполнение в пул потоков
                    futures.add(executorService.submit(task));
                }

                // Рекурсивный вызов для поддиректорий
                if (!directory.getSubdirectories().isEmpty()) {
                    DuplicateManager subManager = new DuplicateManager(directory.getSubdirectories());
                    Map<byte[], List<File>> subHashTable = subManager.buildHashTable();

                    // Объединяем результаты из поддиректорий с текущими результатами
                    subHashTable.forEach((hash, files) ->
                            hashTable.computeIfAbsent(hash, k -> new ArrayList<>()).addAll(files)
                    );
                }
            }
        }
        // Ожидаем завершения всех задач
        for (Future<?> future : futures) {
            future.get(); // Получаем результат (или исключение, если оно было)
        }

        return hashTable;
    }

    public static void main(String[] args) {
        try {
            Directory test_directory = new Directory("C:\\Users\\Alex\\Desktop\\MAga");
            test_directory.scan();
            List<Directory> directories = new ArrayList<>();
            directories.add(test_directory);
            DuplicateManager dm = new DuplicateManager(directories);
            Map<byte[], List<File>> res = dm.buildHashTable();
            for (Map.Entry<byte[], List<File>> entry : res.entrySet()) {
                System.out.println("Hash: " + Tools.bytesToString(entry.getKey()));
                for (File file : entry.getValue()) {
                    System.out.println("    - Filepath: " + file.getPath());
                }
            }

        } catch (IOException | NoSuchAlgorithmException | InterruptedException | ExecutionException e) {
            System.err.println("Main error: " + e.getMessage());
            e.printStackTrace();  //  Вывод стека вызовов для отладки
        }


    }
}