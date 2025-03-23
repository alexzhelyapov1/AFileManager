package com.alexzhelyapov1;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import javax.tools.Tool;
import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@FunctionalInterface
interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T t) throws E;
}

public class DuplicateManager {
    public static final String FIND_BY_HASH = "By hash";
    public static final String FIND_BY_NAME = "By name";

    private final ObservableList<Directory> directories; // Список директорий

    public DuplicateManager(ObservableList<Directory> directories) {
        this.directories = directories;

//        this.directories.addListener((ListChangeListener<Directory>) change -> {
//            while (change.next()) {
//                if (change.wasAdded()) {
//                    // Обработка добавления новых папок
//                    for (Directory addedDirectory : change.getAddedSubList()) {
//                        // Запуск сканирования в фоновом потоке
//                        scanDirectoryInBackground(addedDirectory);
//                    }
//                }
//                if (change.wasRemoved()) {
//                    // Обработка удаления (если нужно)
//                    System.out.println("Removed: " + change.getRemoved());
//                }
//            }
//        });
    }

    public Map<String, Set<File>> duplicated(String mode) throws Exception { // Пробрасываем Exception
        if (mode == FIND_BY_NAME) {
            return basenameDuplicated();
        } else if (mode == FIND_BY_HASH) {
            return hashDuplicated();
        }
        System.out.println("Error! Wrong mode for duplicate manager.");
        return new ConcurrentHashMap<>();

    }

    public Map<String, Set<File>> hashDuplicated() throws Exception { // Пробрасываем Exception
        return buildHashTable((ThrowingFunction<File, String, Exception>) File::getSha512Hash);
    }

    public Map<String, Set<File>> basenameDuplicated() throws ExecutionException, InterruptedException {
        return buildHashTable(File::getBaseName);
    }

    private <E extends Exception> Map<String, Set<File>> buildHashTable(ThrowingFunction<File, String, E> keyExtractor) throws InterruptedException, ExecutionException, E {
        ConcurrentHashMap<String, Set<File>> hashTable = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();

        try (ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            buildHashTableRecursive(new ArrayList<>(directories), keyExtractor, hashTable, executorService, futures);
        }
        // Ожидаем завершения всех задач *после* закрытия executorService
        for (Future<?> future : futures) {
            future.get(); // Получаем результат (или исключение)
        }

        return hashTable;
    }

    private <E extends Exception> void buildHashTableRecursive(List<Directory> currentDirectories,
                                                               ThrowingFunction<File, String, E> keyExtractor,
                                                               ConcurrentHashMap<String, Set<File>> hashTable,
                                                               ExecutorService executorService, List<Future<?>> futures) throws E {

        for (Directory directory : currentDirectories) {
            for (File file : directory.getFiles()) {
                Callable<Void> task = () -> {
                    String key = keyExtractor.apply(file);
                    hashTable.computeIfAbsent(key, k -> new HashSet<>()).add(file);
                    return null;
                };
                futures.add(executorService.submit(task));
            }

            // Рекурсивный вызов для поддиректорий, но *без* создания нового DuplicateManager
            if (!directory.getSubdirectories().isEmpty()) {
                buildHashTableRecursive(directory.getSubdirectories(), keyExtractor, hashTable, executorService, futures);
            }
        }
    }

    public static void printHashMap(Map<String, Set<File>> map) {
        for (Map.Entry<String, Set<File>> entry : map.entrySet()) {
            System.out.println("Key: " + entry.getKey());
            for (File file : entry.getValue()) {
                System.out.println("    - Filepath: " + file.getPath());
            }
        }
    }

    public static void printDuplicatesHashMap(Map<String, Set<File>> map) {
        for (Map.Entry<String, Set<File>> entry : map.entrySet()) {
            if (entry.getValue().size() > 1) {
                System.out.println("Key: " + entry.getKey());
                for (File file : entry.getValue()) {
                    System.out.println("    - Filepath: " + file.getPath());
                }
            }
        }
    }

//    public static void main(String[] args) {
//        try {
//            Directory test_directory = new Directory(Test.CreateTestDirectory());
//            test_directory.scan();
//            List<Directory> directories = new ArrayList<>();
//            directories.add(test_directory);
//            DuplicateManager dm = new DuplicateManager(directories);
//
//            printHashMap(dm.hashDuplicated());
//            printHashMap(dm.basenameDuplicated());
//
//        } catch (Exception e) {
//            System.err.println("Main error: " + e.getMessage());
//            e.printStackTrace();  //  Вывод стека вызовов для отладки
//        }
//    }
}