package com.orderpro.ui;

import java.io.IOException;
import java.util.Scanner;

/**
 * Replaces terminal.c / terminal.h: ClearScreen, MoveCursor, KeyHit,
 * FlushInputBuffer (Linux ANSI/termios implementation).
 */
public final class Terminal {

  private static final char ESC = '\033';
  private static final Scanner SCANNER = new Scanner(System.in);

  private Terminal() {}

  public static Scanner scanner() {
    return SCANNER;
  }

  /** ANSI clear: ESC[H ESC[J */
  public static void clearScreen() {
    System.out.printf("%c[H%c[J", ESC, ESC);
    System.out.flush();
  }

  /** ANSI cursor move (col/row order matches the C original). */
  public static void moveCursor(int row, int col) {
    System.out.printf("%c[%d;%dH", ESC, col, row);
    System.out.flush();
  }

  /**
   * Blocks until a single key is pressed, without requiring Enter, using
   * stty raw mode (the JVM equivalent of the termios trick in terminal.c).
   * Falls back to waiting for a line if raw mode is unavailable.
   */
  public static boolean keyHit() {
    try {
      stty("raw -echo");
      int ch = System.in.read();
      return ch != -1;
    } catch (IOException | InterruptedException e) {
      try {
        if (SCANNER.hasNextLine()) {
          SCANNER.nextLine();
        }
      } catch (Exception ignored) {
        return false;
      }
      return true;
    } finally {
      try {
        stty("sane");
      } catch (IOException | InterruptedException ignored) {
        // best effort
      }
    }
  }

  /** Runs stty with inherited stdio so no pipe file descriptors are leaked. */
  private static void stty(String mode) throws IOException, InterruptedException {
    new ProcessBuilder("/bin/sh", "-c", "stty " + mode + " < /dev/tty")
        .inheritIO()
        .start()
        .waitFor();
  }

  /** Discards the rest of the current input line (like __fpurge(stdin)). */
  public static void flushInputBuffer() {
    try {
      while (System.in.available() > 0) {
        System.in.read();
      }
    } catch (IOException ignored) {
      // best effort
    }
  }
}
