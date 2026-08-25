package com.orderpro.service.impl;

import com.orderpro.db.Database;
import com.orderpro.service.OrderService;
import com.orderpro.session.Session;
import com.orderpro.ui.Terminal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Consumer slice: ports Order() and ReadOrdersFromConsumer() from main.pc.
 */
public class OrderServiceImpl implements OrderService {

  static final String SQL_SELECT_SHOPS = "SELECT shop_id, title FROM shops";

  static final String SQL_SELECT_MENUS =
      "SELECT menu_id, title FROM menus WHERE shop_id = ?";

  static final String SQL_INSERT_ORDER =
      "INSERT"
          + "    INTO orders(order_id,"
          + "                user_id,"
          + "                shop_id)"
          + "    VALUES (orders_seq.nextval,"
          + "            ?,"
          + "            ?)";

  static final String SQL_INSERT_ORDER_LINE =
      "INSERT"
          + "    INTO order_lines(order_line_id,"
          + "                     order_id,"
          + "                     menu_id,"
          + "                     quantity)"
          + "    VALUES (order_lines_seq.nextval,"
          + "            orders_seq.currval,"
          + "            ?,"
          + "            ?)";

  static final String SQL_SELECT_ORDER_HISTORY =
      "SELECT o.order_id,"
          + "       s.title,"
          + "       SUM(quantity * price)                        AS amount,"
          + "       TO_CHAR(ordered_at, 'YYYY-MM-DD HH24:MI:SS') AS ordered_at"
          + "    FROM orders o"
          + "             JOIN order_lines ol ON ol.order_id = o.order_id"
          + "             JOIN menus m ON m.menu_id = ol.menu_id"
          + "             JOIN shops s ON s.shop_id = o.shop_id"
          + "             JOIN users u ON u.user_id = o.user_id"
          + "    WHERE o.user_id = ?"
          + "    GROUP BY o.order_id, s.title, ordered_at";

  static final String SQL_SELECT_ORDER_DETAIL =
      "SELECT m.title, m.price, ol.quantity, m.price * ol.quantity AS subtotal"
          + "    FROM orders o"
          + "             JOIN order_lines ol ON ol.order_id = o.order_id"
          + "             JOIN menus m ON m.menu_id = ol.menu_id"
          + "    WHERE o.order_id = ?";

  private final Connection connection;
  private final Session session;
  private final Scanner scanner;

  public OrderServiceImpl(Connection connection, Session session, Scanner scanner) {
    this.connection = connection;
    this.session = session;
    this.scanner = scanner;
  }

  /** Mirrors the `quantity <= 0` guard in the Order() loop. */
  static boolean isValidQuantity(int quantity) {
    return quantity > 0;
  }

  @Override
  public int order() {
    try {
      int rows = 0;
      System.out.println("가게 목록");
      System.out.printf("%10s %10s%n", "shop_id", "title");
      try (PreparedStatement stmt = connection.prepareStatement(SQL_SELECT_SHOPS);
          ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          System.out.printf("%10d %10s%n", rs.getInt("shop_id"), rs.getString("title"));
          rows++;
        }
      }
      System.out.printf("Total: %d%n%n", rows);

      System.out.print("주문할 가게를 선택하세요: ");
      int shopId = scanner.nextInt();

      rows = 0;
      System.out.println("\n메뉴 목록");
      System.out.printf("%10s %10s%n", "menu_id", "title");
      try (PreparedStatement stmt = connection.prepareStatement(SQL_SELECT_MENUS)) {
        stmt.setInt(1, shopId);
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            System.out.printf("%10d %10s%n", rs.getInt("menu_id"), rs.getString("title"));
            rows++;
          }
        }
      }
      System.out.printf("Total: %d%n%n", rows);

      try {
        try (PreparedStatement stmt = connection.prepareStatement(SQL_INSERT_ORDER)) {
          stmt.setInt(1, session.getUser().getUserId());
          stmt.setInt(2, shopId);
          stmt.executeUpdate();
        }

        try (PreparedStatement stmt = connection.prepareStatement(SQL_INSERT_ORDER_LINE)) {
          System.out.println("주문 완료 = 0");
          while (true) {
            System.out.print("주문할 메뉴를 선택하세요 : ");
            int menuId = scanner.nextInt();
            if (menuId == 0) {
              break;
            }

            System.out.print("수량을 입력하세요: ");
            int quantity = scanner.nextInt();
            if (!isValidQuantity(quantity)) {
              System.out.print("수량은 1이상이어야 합니다.\n");
              continue;
            }

            stmt.setInt(1, menuId);
            stmt.setInt(2, quantity);
            stmt.executeUpdate();
          }
        }
        Terminal.flushInputBuffer();

        connection.commit();
      } catch (SQLException e) {
        try {
          connection.rollback();
        } catch (SQLException ignored) {
          // best effort, sqlError below also rolls back
        }
        throw e;
      }
      System.out.println("\n주문이 완료되었습니다.");
      return 0;
    } catch (SQLException e) {
      Database.sqlError("ORACLE error--", e);
      return 0; // unreachable
    }
  }

  @Override
  public int readOrdersFromConsumer() {
    try {
      int rows = 0;
      System.out.printf("%10s %10s %10s %10s%n", "order_id", "shop_title", "amount",
          "ordered_at");
      try (PreparedStatement stmt = connection.prepareStatement(SQL_SELECT_ORDER_HISTORY)) {
        stmt.setInt(1, session.getUser().getUserId());
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            System.out.printf("%10d %10s %10d %10s%n", rs.getInt("order_id"),
                rs.getString("title"), rs.getLong("amount"), rs.getString("ordered_at"));
            rows++;
          }
        }
      }
      System.out.printf("Total: %d%n%n", rows);

      System.out.print("확인할 주문을 선택하세요: ");
      int selectOrder = scanner.nextInt();
      if (selectOrder == 0) {
        return 0;
      }

      System.out.printf("%10s %10s %10s %10s%n", "menu_title", "price", "quantity",
          "subtotal");
      try (PreparedStatement stmt = connection.prepareStatement(SQL_SELECT_ORDER_DETAIL)) {
        stmt.setInt(1, selectOrder);
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            System.out.printf("%10s %10d %10d %10d%n", rs.getString("title"),
                rs.getLong("price"), rs.getInt("quantity"), rs.getLong("subtotal"));
          }
        }
      }
      Terminal.flushInputBuffer();

      return 0;
    } catch (SQLException e) {
      Database.sqlError("ORACLE error--", e);
      return 0; // unreachable
    }
  }
}
