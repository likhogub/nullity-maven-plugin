package ru.likhogub.nullity;

import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class Main {

    public static void main(String[] args) {
        int a = myFunc(new Random().nextBoolean() ? null : new Random().nextInt());
    }

    public static @NotNull Integer myFunc(@NotNull Integer integer) {
        return integer % 2 == 0 ? null : 2 * integer;
    }
}
