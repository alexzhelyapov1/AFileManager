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
import java.util.function.Function;
import java.util.stream.Collectors;

@FunctionalInterface
interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T t) throws E;
}

public class DuplicateManager {

    private final List<Directory> directories; // Список директорий

    public DuplicateManager(List<Directory> directories) {
        this.directories = directories;
    }

    public Map<String, List<File>> hashDuplicated() throws Exception { // Пробрасываем Exception
        return buildHashTable((ThrowingFunction<File, String, Exception>) File::getSha512Hash);
    }

    public Map<String, List<File>> basenameDuplicated() throws ExecutionException, InterruptedException {
        return buildHashTable(File::getBaseName);
    }

    private <E extends Exception> Map<String, List<File>> buildHashTable(ThrowingFunction<File, String, E> keyExtractor) throws InterruptedException, ExecutionException, E {
        //Используем ConcurrentHashMap для потокобезопасности
        ConcurrentHashMap<String, List<File>> hashTable = new ConcurrentHashMap<>();

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
                        String key = keyExtractor.apply(file);
                        hashTable.computeIfAbsent(key, k -> new ArrayList<>()).add(file);
                        return null; // Возвращаем null, так как результат не нужен
                    };
                    // Отправляем задачу на выполнение в пул потоков
                    futures.add(executorService.submit(task));
                }

                // Рекурсивный вызов для поддиректорий
                if (!directory.getSubdirectories().isEmpty()) {
                    DuplicateManager subManager = new DuplicateManager(directory.getSubdirectories());
                    Map<String, List<File>> subHashTable = subManager.buildHashTable(keyExtractor);

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

    public static void printHashMap(Map<String, List<File>> map) {
        for (Map.Entry<String, List<File>> entry : map.entrySet()) {
            System.out.println("Key: " + entry.getKey());
            for (File file : entry.getValue()) {
                System.out.println("    - Filepath: " + file.getPath());
            }
        }
    }

    public static void main(String[] args) {
        try {
            Directory test_directory = new Directory(Test.CreateTestDirectory());
            test_directory.scan();
            List<Directory> directories = new ArrayList<>();
            directories.add(test_directory);
            DuplicateManager dm = new DuplicateManager(directories);

            printHashMap(dm.hashDuplicated());
            printHashMap(dm.basenameDuplicated());

        } catch (Exception e) {
            System.err.println("Main error: " + e.getMessage());
            e.printStackTrace();  //  Вывод стека вызовов для отладки
        }
    }
}