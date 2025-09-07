package de.schosin.decs.examples.roguelike;

import java.util.Arrays;

public class Utils {

    public static char[][] createCharArray(int width, int height, char defaultValue) {
        var array = new char[height][width];

        for (var y = 0; y < height; y++) {
            Arrays.fill(array[y], defaultValue);
        }

        return array;
    }

}
