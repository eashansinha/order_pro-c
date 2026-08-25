package com.orderpro.model;

/** Maps the ORDERS table in sql/init.sql. */
public class Order {
  private int orderId;
  private int userId;
  private int shopId;
  private String orderedAt; // formatted 'YYYY-MM-DD HH24:MI:SS'

  public int getOrderId() { return orderId; }
  public void setOrderId(int orderId) { this.orderId = orderId; }

  public int getUserId() { return userId; }
  public void setUserId(int userId) { this.userId = userId; }

  public int getShopId() { return shopId; }
  public void setShopId(int shopId) { this.shopId = shopId; }

  public String getOrderedAt() { return orderedAt; }
  public void setOrderedAt(String orderedAt) { this.orderedAt = orderedAt; }
}
