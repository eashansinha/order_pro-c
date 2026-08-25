package com.orderpro.service;

/**
 * Provider slice contract, ported from main.pc.
 * Each method returns 0 on success or the original negative return code:
 * RegisterShop: -1011, CreateShopMenu: -2011.
 */
public interface ShopService {

  /** Ports RegisterShop() — inserts into shops via shops_seq.nextval. */
  int registerShop();

  /** Ports ReadShopsFromOwner() — lists shops owned by the session user. */
  int readShopsFromOwner();

  /** Ports SelectShop() — loads the chosen shop into the Session. */
  int selectShop();

  /** Ports CreateShopMenu() — inserts into menus via menus_seq.nextval. */
  int createShopMenu();

  /** Ports ReadMenusFromShop() — lists menus of the selected shop. */
  int readMenusFromShop();
}
