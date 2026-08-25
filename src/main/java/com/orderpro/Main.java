package com.orderpro;

import com.orderpro.db.Database;
import com.orderpro.service.AuthService;
import com.orderpro.service.OrderService;
import com.orderpro.service.ShopService;
import com.orderpro.service.impl.AuthServiceImpl;
import com.orderpro.service.impl.OrderServiceImpl;
import com.orderpro.service.impl.ShopServiceImpl;
import com.orderpro.session.Session;
import com.orderpro.ui.Terminal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Ports main() and ShowMenu() from src/main.pc: the numeric menu dispatch
 * loop (0 = exit; 1/2 = auth; 101/102/103 provider or consumer; 201/202
 * provider menu management, gated on a selected shop).
 */
public class Main {

  private final Session session;
  private final AuthService authService;
  private final ShopService shopService;
  private final OrderService orderService;
  private final Scanner scanner;

  Main(Connection connection, Session session, Scanner scanner) {
    this.session = session;
    this.scanner = scanner;
    this.authService = new AuthServiceImpl(connection, session, scanner);
    this.shopService = new ShopServiceImpl(connection, session, scanner);
    this.orderService = new OrderServiceImpl(connection, session, scanner);
  }

  void showMenu() {
    System.out.printf("0. 종료%n%n");
    if (session.isAuthorized()) {
      System.out.printf("1. 회원정보 수정%n");
      System.out.printf("2. 로그아웃%n");
    } else {
      System.out.printf("1. 로그인%n");
      System.out.printf("2. 회원 가입%n");
    }
    System.out.println();

    if (session.getUser().isProvider()) {
      System.out.printf("101. 가게 등록%n");
      System.out.printf("102. 가게 조회%n");
      System.out.printf("103. 가게 선택%n");
      System.out.println();

      if (session.getShop() != null) {
        System.out.printf("201. 메뉴 등록%n");
        System.out.printf("202. 메뉴 조회%n");
        System.out.printf("203. 메뉴 수정 (TODO)%n");
        System.out.printf("204. 메뉴 삭제 (TODO)%n");
        System.out.println();
      }
    }
    if (session.getUser().isConsumer()) {
      System.out.printf("101. 주문하기%n");
      System.out.printf("102. 주문 조회%n");
      System.out.printf("103. 리뷰 (TODO)%n");
      System.out.println();
    }
  }

  private int readInt() {
    String line = scanner.hasNextLine() ? scanner.nextLine().trim() : "0";
    try {
      return Integer.parseInt(line);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  void run() {
    int selectMenu = -1;
    int returnCode = 0;
    while (selectMenu != 0 && returnCode == 0) {
      Terminal.clearScreen();
      if (session.isAuthorized()) {
        System.out.printf("현재 로그인된 유저: %s%n", session.getUser().getUsername());
        System.out.printf("전화번호: %s%n", session.getUser().getPhoneNumber());
        System.out.printf("주소: %s%n", session.getUser().getAddress());
        System.out.printf("로그인 정보: %s%n", session.getUser().getUserType());
        System.out.println();
      }

      showMenu();

      System.out.print("입력: ");
      selectMenu = readInt();
      Terminal.flushInputBuffer();
      Terminal.clearScreen();

      if (session.isAuthorized()) {
        switch (selectMenu) {
          case 1 -> returnCode = authService.editProfile();
          case 2 -> returnCode = authService.signOut();
          default -> { }
        }
        if (session.getUser().isProvider()) {
          if (session.getShop() == null && selectMenu >= 200 && selectMenu <= 300) {
            continue;
          }
          switch (selectMenu) {
            case 101 -> returnCode = shopService.registerShop();
            case 102 -> {
              returnCode = shopService.readShopsFromOwner();
              Terminal.keyHit();
            }
            case 103 -> returnCode = shopService.selectShop();
            case 201 -> returnCode = shopService.createShopMenu();
            case 202 -> {
              returnCode = shopService.readMenusFromShop();
              Terminal.keyHit();
            }
            default -> { }
          }
        } else if (session.getUser().isConsumer()) {
          switch (selectMenu) {
            case 101 -> {
              returnCode = orderService.order();
              Terminal.keyHit();
            }
            case 102 -> {
              returnCode = orderService.readOrdersFromConsumer();
              Terminal.keyHit();
            }
            case 103 -> { }
            default -> { }
          }
        }
      } else {
        switch (selectMenu) {
          case 1 -> returnCode = authService.signIn();
          case 2 -> returnCode = authService.signUp();
          default -> { }
        }
      }
    }
  }

  public static void main(String[] args) {
    Connection connection = Database.connect(args);
    Main app = new Main(connection, new Session(), Terminal.scanner());
    app.run();
    try {
      connection.rollback();
      connection.close();
    } catch (SQLException ignored) {
      // mirrors EXEC SQL ROLLBACK WORK RELEASE on exit
    }
    System.exit(0);
  }
}
