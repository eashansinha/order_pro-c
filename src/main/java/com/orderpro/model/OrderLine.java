package com.orderpro.model;

/** Maps the ORDER_LINES table in sql/init.sql. */
public class OrderLine {
  private int orderLineId;
  private int orderId;
  private int menuId;
  private int quantity;

  public int getOrderLineId() { return orderLineId; }
  public void setOrderLineId(int orderLineId) { this.orderLineId = orderLineId; }

  public int getOrderId() { return orderId; }
  public void setOrderId(int orderId) { this.orderId = orderId; }

  public int getMenuId() { return menuId; }
  public void setMenuId(int menuId) { this.menuId = menuId; }

  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
}
