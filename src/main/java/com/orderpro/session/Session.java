package com.orderpro.session;

import com.orderpro.model.Shop;
import com.orderpro.model.User;

/**
 * Replaces the globals in main.pc: `bool is_authorized`, `struct User *user`,
 * `struct Shop *shop`. All feature slices read/write session state through
 * this object.
 */
public class Session {
  private User user = new User();
  private Shop shop; // null until a provider selects a shop (SelectShop)
  private boolean authorized = false;

  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }

  public Shop getShop() { return shop; }
  public void setShop(Shop shop) { this.shop = shop; }

  public boolean isAuthorized() { return authorized; }
  public void setAuthorized(boolean authorized) { this.authorized = authorized; }

  /** Mirrors SignOut(): frees the shop, zeroes the user, clears the flag. */
  public void clear() {
    shop = null;
    user = new User();
    authorized = false;
  }
}
