package com.orderpro.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.orderpro.model.Shop;
import com.orderpro.model.User;
import org.junit.jupiter.api.Test;

class SessionTest {

  @Test
  void startsUnauthorizedWithEmptyUserAndNoShop() {
    Session session = new Session();
    assertFalse(session.isAuthorized());
    assertNotNull(session.getUser());
    assertNull(session.getShop());
  }

  @Test
  void clearResetsStateLikeSignOut() {
    Session session = new Session();
    User user = new User();
    user.setUserId(1);
    user.setUsername("tester");
    user.setUserType("provider");
    session.setUser(user);
    session.setAuthorized(true);
    Shop shop = new Shop();
    shop.setShopId(3);
    session.setShop(shop);

    session.clear();

    assertFalse(session.isAuthorized());
    assertNull(session.getShop());
    assertNull(session.getUser().getUsername());
    assertEquals(0, session.getUser().getUserId());
  }

  @Test
  void userTypeHelpers() {
    User user = new User();
    user.setUserType("provider");
    assertTrue(user.isProvider());
    assertFalse(user.isConsumer());
    user.setUserType("consumer");
    assertTrue(user.isConsumer());
    assertFalse(user.isProvider());
  }
}
