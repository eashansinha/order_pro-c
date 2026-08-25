package com.orderpro.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.orderpro.model.User;
import com.orderpro.session.Session;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderServiceImplTest {

  private Connection connection;
  private Session session;
  private ByteArrayOutputStream out;
  private PrintStream originalOut;

  @BeforeEach
  void setUp() throws Exception {
    connection = DriverManager.getConnection(
        "jdbc:h2:mem:orderpro;MODE=Oracle;DB_CLOSE_DELAY=-1", "sa", "");
    connection.setAutoCommit(false);
    try (Statement st = connection.createStatement()) {
      st.execute("DROP ALL OBJECTS");
      st.execute("CREATE TABLE users (user_id NUMBER PRIMARY KEY, username VARCHAR2(20),"
          + " phone_number VARCHAR2(14), address VARCHAR2(80), user_type VARCHAR2(10))");
      st.execute("CREATE TABLE shops (shop_id NUMBER PRIMARY KEY, user_id NUMBER,"
          + " title VARCHAR2(20))");
      st.execute("CREATE TABLE menus (menu_id NUMBER PRIMARY KEY, shop_id NUMBER,"
          + " title VARCHAR2(20), price NUMBER, description VARCHAR2(80))");
      st.execute("CREATE TABLE orders (order_id NUMBER PRIMARY KEY, user_id NUMBER,"
          + " shop_id NUMBER, ordered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
      st.execute("CREATE TABLE order_lines (order_line_id NUMBER PRIMARY KEY,"
          + " order_id NUMBER, menu_id NUMBER, quantity NUMBER)");
      st.execute("CREATE SEQUENCE orders_seq NOCACHE");
      st.execute("CREATE SEQUENCE order_lines_seq NOCACHE");
      st.execute("INSERT INTO users VALUES (1, 'consumer1', '010', 'addr', 'consumer')");
      st.execute("INSERT INTO shops VALUES (1, 2, 'shop1')");
      st.execute("INSERT INTO menus VALUES (1, 1, 'menu1', 1000, 'desc')");
      st.execute("INSERT INTO menus VALUES (2, 1, 'menu2', 2500, 'desc')");
    }
    connection.commit();

    session = new Session();
    User user = new User();
    user.setUserId(1);
    user.setUserType("consumer");
    session.setUser(user);
    session.setAuthorized(true);

    out = new ByteArrayOutputStream();
    originalOut = System.out;
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  void tearDown() throws Exception {
    System.setOut(originalOut);
    connection.close();
  }

  private String stdout() {
    return out.toString(StandardCharsets.UTF_8);
  }

  private OrderServiceImpl service(String input) {
    return new OrderServiceImpl(connection, session, new Scanner(input));
  }

  @Test
  void isValidQuantityRejectsZeroAndNegative() {
    assertFalse(OrderServiceImpl.isValidQuantity(0));
    assertFalse(OrderServiceImpl.isValidQuantity(-1));
    assertTrue(OrderServiceImpl.isValidQuantity(1));
  }

  @Test
  void orderInsertsOrderAndLinesInOneTransaction() throws Exception {
    // shop 1, menu 1 qty 2, menu 2 qty 1, then 0 to finish
    int rc = service("1\n1\n2\n2\n1\n0\n").order();
    assertEquals(0, rc);

    try (Statement st = connection.createStatement()) {
      try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM orders")) {
        rs.next();
        assertEquals(1, rs.getInt(1));
      }
      try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM order_lines")) {
        rs.next();
        assertEquals(2, rs.getInt(1));
      }
    }

    String output = stdout();
    assertTrue(output.contains("가게 목록"));
    assertTrue(output.contains("\n메뉴 목록"));
    assertTrue(output.contains("주문할 가게를 선택하세요: "));
    assertTrue(output.contains("주문 완료 = 0"));
    assertTrue(output.contains("주문할 메뉴를 선택하세요 : "));
    assertTrue(output.contains("수량을 입력하세요: "));
    assertTrue(output.contains("\n주문이 완료되었습니다."));
  }

  @Test
  void orderRejectsNonPositiveQuantity() throws Exception {
    // shop 1, menu 1 qty 0 (rejected), menu 1 qty -5 (rejected), then 0
    int rc = service("1\n1\n0\n1\n-5\n0\n").order();
    assertEquals(0, rc);

    try (Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM order_lines")) {
      rs.next();
      assertEquals(0, rs.getInt(1));
    }

    String output = stdout();
    assertTrue(output.contains("수량은 1이상이어야 합니다.\n"));
  }

  @Test
  void readOrdersReturnsZeroWhenNoOrderSelected() {
    int rc = service("0\n").readOrdersFromConsumer();
    assertEquals(0, rc);

    String output = stdout();
    assertTrue(output.contains("확인할 주문을 선택하세요: "));
    assertTrue(output.contains("Total: 0\n\n"));
    assertFalse(output.contains("menu_title"));
  }

  @Test
  void readOrdersShowsHistoryAndDetail() throws Exception {
    service("1\n1\n2\n0\n").order();
    out.reset();

    int rc = service("1\n").readOrdersFromConsumer();
    assertEquals(0, rc);

    String output = stdout();
    assertTrue(output.contains(String.format("%10s %10s %10s %10s%n",
        "order_id", "shop_title", "amount", "ordered_at")));
    assertTrue(output.contains("Total: 1\n\n"));
    assertTrue(output.contains(String.format("%10s %10s %10s %10s%n",
        "menu_title", "price", "quantity", "subtotal")));
    assertTrue(output.contains("menu1"));
    assertTrue(output.contains("2000")); // 1000 * 2 subtotal
  }
}
