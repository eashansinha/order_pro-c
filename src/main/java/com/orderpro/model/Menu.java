package com.orderpro.model;

/** Maps the MENUS table in sql/init.sql. */
public class Menu {
  private int menuId;
  private int shopId;
  private String title;       // VARCHAR2(20)
  private long price;         // NUMBER
  private String description; // VARCHAR2(100)

  public int getMenuId() { return menuId; }
  public void setMenuId(int menuId) { this.menuId = menuId; }

  public int getShopId() { return shopId; }
  public void setShopId(int shopId) { this.shopId = shopId; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public long getPrice() { return price; }
  public void setPrice(long price) { this.price = price; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
}
