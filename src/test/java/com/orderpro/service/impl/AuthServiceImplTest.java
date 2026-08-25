package com.orderpro.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.orderpro.model.Shop;
import com.orderpro.model.User;
import com.orderpro.session.Session;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

class AuthServiceImplTest {

  private AuthServiceImpl service(Session session, String input) {
    return new AuthServiceImpl(null, session, new Scanner(input));
  }

  // --- SignUp validation return codes ---

  @Test
  void signUpEmptyUsernameReturnsMinus201() {
    assertEquals(-201, service(new Session(), "\n").signUp());
  }

  @Test
  void signUpEmptyPasswordReturnsMinus202() {
    assertEquals(-202, service(new Session(), "alice\n\n").signUp());
  }

  @Test
  void signUpPasswordMismatchReturnsMinus202() {
    assertEquals(-202, service(new Session(), "alice\npw1\npw2\n").signUp());
  }

  @Test
  void signUpEmptyPhoneReturnsMinus203() {
    assertEquals(-203, service(new Session(), "alice\npw\npw\n\n").signUp());
  }

  @Test
  void signUpEmptyAddressReturnsMinus204() {
    assertEquals(-204, service(new Session(), "alice\npw\npw\n010-1234\n\n").signUp());
  }

  @Test
  void signUpInvalidUserTypeReturnsMinus205() {
    assertEquals(-205,
        service(new Session(), "alice\npw\npw\n010-1234\nSeoul\nxyz\n").signUp());
  }

  // --- Column-width clipping (mirrors C fixed-size VARCHAR buffers) ---

  @Test
  void truncateClipsToColumnWidth() {
    assertEquals("a".repeat(20), AuthServiceImpl.truncate("a".repeat(25), 20));
    assertEquals("short", AuthServiceImpl.truncate("short", 20));
    assertEquals("", AuthServiceImpl.truncate("", 20));
  }

  // --- SignIn validation return codes ---

  @Test
  void signInEmptyUsernameReturnsMinus211() {
    assertEquals(-211, service(new Session(), "\n").signIn());
  }

  @Test
  void signInEmptyPasswordReturnsMinus212() {
    assertEquals(-212, service(new Session(), "alice\n\n").signIn());
  }

  // --- SignOut ---

  @Test
  void signOutClearsSessionAndReturnsZero() {
    Session session = new Session();
    User user = new User();
    user.setUserId(7);
    user.setUsername("alice");
    session.setUser(user);
    session.setShop(new Shop());
    session.setAuthorized(true);

    assertEquals(0, service(session, "").signOut());
    assertFalse(session.isAuthorized());
    assertNull(session.getShop());
    assertNull(session.getUser().getUsername());
    assertEquals(0, session.getUser().getUserId());
  }

  // --- EditProfile ---

  @Test
  void editProfilePasswordMismatchReturnsMinus1002() {
    assertEquals(-1002, service(new Session(), "pw1\npw2\n\n\n").editProfile());
  }

  @Test
  void editProfileAllEmptyFieldsReturnsZeroWithoutDb() {
    assertEquals(0, service(new Session(), "\n\n\n").editProfile());
  }

  // --- SQL text ---

  @Test
  void insertUserSqlUsesUsersSeqAndBindVariables() {
    assertTrue(AuthServiceImpl.INSERT_USER_SQL.contains("users_seq.nextval"));
    assertEquals(5, AuthServiceImpl.INSERT_USER_SQL.chars().filter(c -> c == '?').count());
  }

  @Test
  void selectUserSqlMatchesOriginalColumns() {
    assertTrue(AuthServiceImpl.SELECT_USER_SQL.startsWith(
        "SELECT user_id, username, phone_number, address, user_type"));
    assertTrue(AuthServiceImpl.SELECT_USER_SQL.contains("username = ? AND password = ?"));
  }

  @Test
  void buildUpdateSqlJoinsOnlyProvidedFields() {
    assertEquals("UPDATE users SET password = ?, phone_number = ? WHERE user_id = ?",
        AuthServiceImpl.buildUpdateSql(List.of("password = ?", "phone_number = ?")));
    assertEquals("UPDATE users SET address = ? WHERE user_id = ?",
        AuthServiceImpl.buildUpdateSql(List.of("address = ?")));
  }
}
