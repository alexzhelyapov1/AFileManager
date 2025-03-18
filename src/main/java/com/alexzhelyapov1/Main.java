package com.alexzhelyapov1;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        System.out.printf("Hello and welcome!");

        try {
            Directory test_directory = new Directory("C:\\Users\\Alex\\Desktop\\MAga");
            test_directory.scan();
            List<File> files = test_directory.getFiles();
            for (File entry : files) {
                System.out.println("File path: " + entry.getPath());
                System.out.println("SHA-512 hash: " + entry.getSha512Hash());
            }
            if (Objects.equals(files.get(1).getSha512Hash(), files.get(2).getSha512Hash())) {
                System.out.println("Equals!!!");
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Main error: " + e.getMessage());
            e.printStackTrace();  //  Вывод стека вызовов для отладки
        }


    }
}