package com.orderpro.service.impl;

import com.orderpro.db.Database;
import com.orderpro.model.Shop;
import com.orderpro.service.ShopService;
import com.orderpro.session.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Provider slice, ported from main.pc: RegisterShop, ReadShopsFromOwner,
 * SelectShop, CreateShopMenu, ReadMenusFromShop.
 */
public class ShopServiceImpl implements ShopService {

  private final Connection connection;
  private final Session session;
  private final Scanner scanner;

  public ShopServiceImpl(Connection connection, Session session, Scanner scanner) {
    this.connection = connection;
    this.session = session;
    this.scanner = scanner;
  }

  /** Mirrors the C fixed-size VARCHAR buffers, which clipped input to the column width. */
  private static String truncate(String value, int maxLength) {
    return value.length() > maxLength ? value.substring(0, maxLength) : value;
  }

  @Override
  public int registerShop() {
    System.out.printf("가게 이름을 입력하세요: ");
    String title = truncate(scanner.nextLine(), 20);
    if (title.isEmpty()) {
      System.out.printf("가게 이름을 입력하세요.\n");
      return -1011;
    }

    try (PreparedStatement stmt = connection.prepareStatement(
        "INSERT INTO shops(shop_id, owner_id, title) "
            + "VALUES (shops_seq.nextval, ?, ?)")) {
      stmt.setInt(1, session.getUser().getUserId());
      stmt.setString(2, title);
      stmt.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      Database.sqlError("ORACLE error--", e);
    }

    System.out.printf("\n가게가 등록되었습니다.\n");
    return 0;
  }

  @Override
  public int readShopsFromOwner() {
    try (PreparedStatement stmt = connection.prepareStatement(
        "SELECT shop_id, title FROM shops WHERE owner_id = ?")) {
      stmt.setInt(1, session.getUser().getUserId());
      try (ResultSet rs = stmt.executeQuery()) {
        System.out.printf("%10s %10s\n", "shop_id", "title");
        int total = 0;
        while (rs.next()) {
          System.out.printf("%10d %10s\n", rs.getInt("shop_id"), rs.getString("title"));
          total++;
        }
        System.out.printf("Total: %d\n", total);
      }
    } catch (SQLException e) {
      Database.sqlError("ORACLE error--", e);
    }
    return 0;
  }

  @Override
  public int selectShop() {
    System.out.printf("가게 아이디를 입력하세요: ");
    int shopId = Integer.parseInt(scanner.nextLine().trim());

    try (PreparedStatement stmt = connection.prepareStatement(
        "SELECT shop_id, owner_id, title FROM shops WHERE shop_id = ?")) {
      stmt.setInt(1, shopId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (!rs.next()) {
          Database.sqlError("ORACLE error--",
              new SQLException("ORA-01403: no data found"));
        }
        Shop shop = session.getShop();
        if (shop == null) {
          shop = new Shop();
          session.setShop(shop);
        }
        shop.setShopId(rs.getInt("shop_id"));
        shop.setOwnerId(rs.getInt("owner_id"));
        shop.setTitle(rs.getString("title"));
      }
    } catch (SQLException e) {
      Database.sqlError("ORACLE error--", e);
    }

    return 0;
  }

  @Override
  public int createShopMenu() {
    System.out.printf("메뉴 이름을 입력하세요: ");
    String title = truncate(scanner.nextLine(), 20);
    if (title.isEmpty()) {
      System.out.printf("메뉴 이름을 입력하세요.\n");
      return -2011;
    }

    System.out.printf("가격을 입력하세요: ");
    long price = Integer.toUnsignedLong(Integer.parseUnsignedInt(scanner.nextLine().trim()));

    System.out.printf("설명을 입력하세요: ");
    String description = truncate(scanner.nextLine(), 100);

    try (PreparedStatement stmt = connection.prepareStatement(
        "INSERT INTO menus(menu_id, shop_id, title, price, description) "
            + "VALUES (menus_seq.nextval, ?, ?, ?, ?)")) {
      stmt.setInt(1, session.getShop().getShopId());
      stmt.setString(2, title);
      stmt.setLong(3, price);
      stmt.setString(4, description);
      stmt.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      Database.sqlError("ORACLE error--", e);
    }

    System.out.printf("\n가게가 등록되었습니다.\n");
    return 0;
  }

  @Override
  public int readMenusFromShop() {
    try (PreparedStatement stmt = connection.prepareStatement(
        "SELECT menu_id, title, price, description FROM menus WHERE shop_id = ?")) {
      stmt.setInt(1, session.getShop().getShopId());
      try (ResultSet rs = stmt.executeQuery()) {
        System.out.printf("%10s %10s %10s %20s\n", "menu_id", "title", "price", "description");
        int total = 0;
        while (rs.next()) {
          System.out.printf("%10d %10s %10d %20s\n",
              rs.getInt("menu_id"),
              rs.getString("title"),
              rs.getLong("price"),
              rs.getString("description"));
          total++;
        }
        System.out.printf("Total: %d\n", total);
      }
    } catch (SQLException e) {
      Database.sqlError("ORACLE error--", e);
    }
    return 0;
  }
}
