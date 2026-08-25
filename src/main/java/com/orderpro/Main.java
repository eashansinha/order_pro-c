package com.orderpro;

import com.orderpro.db.Database;
import com.orderpro.session.Session;

/**
 * Entry point. The full menu loop (ShowMenu + dispatch from main.pc) is
 * wired here during Phase 3 integration.
 */
public class Main {

  public static void main(String[] args) {
    Database.connect(args);
    Session session = new Session();
    // Phase 3: port ShowMenu() and the numeric dispatch loop
    // (0/1/2/101/102/103/201/202) here, wiring AuthService, ShopService,
    // and OrderService implementations.
    System.exit(0);
  }
}
