package com.orderpro.model;

/** Maps struct User in main.pc and the USERS table in sql/init.sql. */
public class User {
  private int userId;
  private String username;     // VARCHAR2(20)
  private String phoneNumber;  // VARCHAR2(14)
  private String address;      // VARCHAR2(80)
  private String userType;     // 'consumer' | 'provider'

  public int getUserId() { return userId; }
  public void setUserId(int userId) { this.userId = userId; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getPhoneNumber() { return phoneNumber; }
  public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }

  public String getUserType() { return userType; }
  public void setUserType(String userType) { this.userType = userType; }

  public boolean isProvider() { return "provider".equals(userType); }
  public boolean isConsumer() { return "consumer".equals(userType); }
}
