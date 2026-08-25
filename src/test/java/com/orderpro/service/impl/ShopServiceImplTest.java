package com.orderpro.service.impl;

import com.orderpro.model.Shop;
import com.orderpro.session.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopServiceImplTest {

  private ByteArrayOutputStream out;
  private PrintStream originalOut;

  @BeforeEach
  void captureStdout() {
    out = new ByteArrayOutputStream();
    originalOut = System.out;
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  void restoreStdout() {
    System.setOut(originalOut);
  }

  private String stdout() {
    return out.toString(StandardCharsets.UTF_8);
  }

  private static Scanner input(String text) {
    return new Scanner(text);
  }

  /** Records executed SQL and parameters; serves canned rows for queries. */
  static class FakeDb {
    final List<String> executedSql = new ArrayList<>();
    final Map<Integer, Object> params = new LinkedHashMap<>();
    boolean committed = false;
    List<Map<String, Object>> rows = new ArrayList<>();

    Connection connection() {
      return (Connection) Proxy.newProxyInstance(
          getClass().getClassLoader(), new Class<?>[] {Connection.class},
          (proxy, method, args) -> {
            switch (method.getName()) {
              case "prepareStatement":
                executedSql.add((String) args[0]);
                return statement();
              case "commit":
                committed = true;
                return null;
              default:
                return defaultValue(method);
            }
          });
    }

    private PreparedStatement statement() {
      return (PreparedStatement) Proxy.newProxyInstance(
          getClass().getClassLoader(), new Class<?>[] {PreparedStatement.class},
          (proxy, method, args) -> {
            String name = method.getName();
            if (name.startsWith("set") && args != null && args.length == 2) {
              params.put((Integer) args[0], args[1]);
              return null;
            }
            switch (name) {
              case "executeUpdate":
                return 1;
              case "executeQuery":
                return resultSet();
              case "close":
                return null;
              default:
                return defaultValue(method);
            }
          });
    }

    private ResultSet resultSet() {
      final int[] index = {-1};
      return (ResultSet) Proxy.newProxyInstance(
          getClass().getClassLoader(), new Class<?>[] {ResultSet.class},
          (proxy, method, args) -> {
            switch (method.getName()) {
              case "next":
                index[0]++;
                return index[0] < rows.size();
              case "getInt":
                return ((Number) rows.get(index[0]).get(args[0])).intValue();
              case "getString":
                return (String) rows.get(index[0]).get(args[0]);
              case "close":
                return null;
              default:
                return defaultValue(method);
            }
          });
    }

    private static Object defaultValue(Method method) {
      Class<?> type = method.getReturnType();
      if (type == boolean.class) return false;
      if (type == int.class) return 0;
      if (type == long.class) return 0L;
      return null;
    }
  }

  @Test
  void registerShopEmptyTitleReturns1011() {
    ShopServiceImpl service = new ShopServiceImpl(null, new Session(), input("\n"));
    assertEquals(-1011, service.registerShop());
    assertTrue(stdout().contains("가게 이름을 입력하세요: "));
    assertTrue(stdout().contains("가게 이름을 입력하세요.\n"));
  }

  @Test
  void registerShopInsertsWithSequenceAndCommits() {
    FakeDb db = new FakeDb();
    Session session = new Session();
    session.getUser().setUserId(7);
    ShopServiceImpl service = new ShopServiceImpl(db.connection(), session, input("치킨집\n"));

    assertEquals(0, service.registerShop());
    assertEquals(1, db.executedSql.size());
    assertTrue(db.executedSql.get(0).contains("INSERT INTO shops"));
    assertTrue(db.executedSql.get(0).contains("shops_seq.nextval"));
    assertEquals(7, db.params.get(1));
    assertEquals("치킨집", db.params.get(2));
    assertTrue(db.committed);
    assertTrue(stdout().contains("\n가게가 등록되었습니다.\n"));
  }

  @Test
  void readShopsFromOwnerPrintsHeaderRowsAndTotal() {
    FakeDb db = new FakeDb();
    db.rows.add(Map.of("shop_id", 1, "title", "A"));
    db.rows.add(Map.of("shop_id", 2, "title", "B"));
    Session session = new Session();
    session.getUser().setUserId(7);
    ShopServiceImpl service = new ShopServiceImpl(db.connection(), session, input(""));

    assertEquals(0, service.readShopsFromOwner());
    String output = stdout();
    assertTrue(output.contains(String.format("%10s %10s\n", "shop_id", "title")));
    assertTrue(output.contains(String.format("%10d %10s\n", 1, "A")));
    assertTrue(output.contains(String.format("%10d %10s\n", 2, "B")));
    assertTrue(output.contains("Total: 2\n"));
    assertEquals(7, db.params.get(1));
  }

  @Test
  void selectShopStoresShopInSession() {
    FakeDb db = new FakeDb();
    db.rows.add(Map.of("shop_id", 3, "owner_id", 7, "title", "피자집"));
    Session session = new Session();
    ShopServiceImpl service = new ShopServiceImpl(db.connection(), session, input("3\n"));

    assertEquals(0, service.selectShop());
    Shop shop = session.getShop();
    assertEquals(3, shop.getShopId());
    assertEquals(7, shop.getOwnerId());
    assertEquals("피자집", shop.getTitle());
    assertEquals(3, db.params.get(1));
    assertTrue(stdout().contains("가게 아이디를 입력하세요: "));
  }

  @Test
  void createShopMenuEmptyTitleReturns2011() {
    ShopServiceImpl service = new ShopServiceImpl(null, new Session(), input("\n"));
    assertEquals(-2011, service.createShopMenu());
    assertTrue(stdout().contains("메뉴 이름을 입력하세요: "));
    assertTrue(stdout().contains("메뉴 이름을 입력하세요.\n"));
  }

  @Test
  void createShopMenuInsertsWithSequenceAndCommits() {
    FakeDb db = new FakeDb();
    Session session = new Session();
    Shop shop = new Shop();
    shop.setShopId(3);
    session.setShop(shop);
    ShopServiceImpl service =
        new ShopServiceImpl(db.connection(), session, input("후라이드\n18000\n바삭한 치킨\n"));

    assertEquals(0, service.createShopMenu());
    assertEquals(1, db.executedSql.size());
    assertTrue(db.executedSql.get(0).contains("INSERT INTO menus"));
    assertTrue(db.executedSql.get(0).contains("menus_seq.nextval"));
    assertEquals(3, db.params.get(1));
    assertEquals("후라이드", db.params.get(2));
    assertEquals(18000L, db.params.get(3));
    assertEquals("바삭한 치킨", db.params.get(4));
    assertTrue(db.committed);
    assertTrue(stdout().contains("\n가게가 등록되었습니다.\n"));
  }

  @Test
  void readMenusFromShopPrintsHeaderRowsAndTotal() {
    FakeDb db = new FakeDb();
    db.rows.add(Map.of("menu_id", 10, "title", "후라이드", "price", 18000, "description", "바삭"));
    Session session = new Session();
    Shop shop = new Shop();
    shop.setShopId(3);
    session.setShop(shop);
    ShopServiceImpl service = new ShopServiceImpl(db.connection(), session, input(""));

    assertEquals(0, service.readMenusFromShop());
    String output = stdout();
    assertTrue(output.contains(
        String.format("%10s %10s %10s %20s\n", "menu_id", "title", "price", "description")));
    assertTrue(output.contains(String.format("%10d %10s %10d %20s\n", 10, "후라이드", 18000L, "바삭")));
    assertTrue(output.contains("Total: 1\n"));
    assertEquals(3, db.params.get(1));
  }
}
