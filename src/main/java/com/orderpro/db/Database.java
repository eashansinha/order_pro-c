package com.orderpro.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Replaces database.pc: ConnectDatabase(argc, argv) and SqlError(msg).
 *
 * The original Pro*C code connected with EXEC SQL CONNECT :username
 * IDENTIFIED BY :password. The JDBC equivalent needs a URL; it defaults to a
 * local Oracle instance and can be overridden with the ORDER_PRO_JDBC_URL
 * environment variable.
 */
public final class Database {

  private static final String DEFAULT_URL = "jdbc:oracle:thin:@//localhost:1521/FREEPDB1";

  private static Connection connection;

  private Database() {}

  /**
   * Mirrors ConnectDatabase(argc, argv): args[0] = username, args[1] =
   * password (optional). Exits with a usage message on bad arguments, like
   * the C original.
   */
  public static Connection connect(String[] args) {
    if (args.length < 1 || args.length > 2) {
      System.out.printf("Usage: order-pro (username) (password)%n");
      System.out.printf("       order-pro (uid)%n");
      System.exit(1);
    }
    String username = args[0];
    String password = args.length == 2 ? args[1] : "";
    String url = System.getenv().getOrDefault("ORDER_PRO_JDBC_URL", DEFAULT_URL);
    try {
      connection = DriverManager.getConnection(url, username, password);
      connection.setAutoCommit(false);
      System.out.printf("Connected to ORACLE as user: %s%n%n", username);
      return connection;
    } catch (SQLException e) {
      sqlError("ORACLE error--", e);
      return null; // unreachable
    }
  }

  public static Connection getConnection() {
    return connection;
  }

  /**
   * Mirrors SqlError(msg): prints the message and the SQL error text, rolls
   * back, releases the connection, and exits.
   */
  public static void sqlError(String msg, SQLException e) {
    System.out.printf("%n%s%n", msg);
    System.out.println(e.getMessage());
    try {
      if (connection != null) {
        connection.rollback();
        connection.close();
      }
    } catch (SQLException ignored) {
      // best-effort rollback/release, like EXEC SQL ROLLBACK RELEASE
    }
    System.exit(1);
  }
}
