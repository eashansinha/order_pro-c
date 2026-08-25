package com.orderpro.model;

/** Maps struct Shop in main.pc and the SHOPS table in sql/init.sql. */
public class Shop {
  private int shopId;
  private int ownerId;
  private String title; // VARCHAR2(20)

  public int getShopId() { return shopId; }
  public void setShopId(int shopId) { this.shopId = shopId; }

  public int getOwnerId() { return ownerId; }
  public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
}
