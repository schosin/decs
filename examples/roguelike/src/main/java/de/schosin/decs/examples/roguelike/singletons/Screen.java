package de.schosin.decs.examples.roguelike.singletons;

import java.io.IOException;
import java.io.PrintWriter;

import org.jline.keymap.BindingReader;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import de.schosin.decs.examples.roguelike.Utils;

/**
 * Wrapper around {@link Terminal} for rendering the game state and retrieving input from the user.
 */
public final class Screen {

    private final int width;
    private final int height;
    private final char[][] screen;

    private final Terminal terminal;
    private final PrintWriter writer;
    private final BindingReader reader;

    public Screen(int width, int height) throws IOException {
        this.width = width;
        this.height = height;
        this.screen = Utils.createCharArray(width, height, ' ');

        this.terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        this.writer = terminal.writer();
        this.reader = new BindingReader(terminal.reader());
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getCenterX() {
        return width / 2;
    }

    public int getCenterY() {
        return height / 2;
    }

    public void drawText(int x, int y, String text) {
        for (int i = 0, s = text.length(); i < s; i++) {
            drawChar(x + i, y, text.charAt(i));
        }
    }

    public void drawChar(int x, int y, char c) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }

        screen[y][x] = c;
    }

    public String prompt(String message) throws IOException {
        return update(message, null);
    }

    private String update(String message, String error) throws IOException {
        terminal.puts(Capability.clear_screen);

        for (var row : screen) {
            writer.println(row);
        }
        terminal.flush();

        if (error != null) {
            writer.println("Error: " + error);
        }

        writer.printf("%s> %s", System.lineSeparator(), message);
        var character = reader.readCharacter();
        return switch (character) {
            case 'w', 'W' -> "w";
            case 'a', 'A' -> "a";
            case 's', 'S' -> "s";
            case 'd', 'D' -> "d";
            case 'q', 'Q' -> "q";
            case ' ' -> " ";
            default -> update(message, "Invalid, use 'WASDQ' or space: " + character);
        };
    }

}
