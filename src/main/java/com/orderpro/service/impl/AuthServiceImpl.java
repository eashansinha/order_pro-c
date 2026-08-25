package com.orderpro.service.impl;

import com.orderpro.db.Database;
import com.orderpro.model.User;
import com.orderpro.service.AuthService;
import com.orderpro.session.Session;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Ports SignUp/SignIn/SignOut/EditProfile from main.pc. */
public class AuthServiceImpl implements AuthService {

  static final String INSERT_USER_SQL =
      "INSERT INTO users(user_id, username, password, phone_number, address, user_type) "
          + "VALUES (users_seq.nextval, ?, ?, ?, ?, ?)";

  static final String SELECT_USER_SQL =
      "SELECT user_id, username, phone_number, address, user_type "
          + "FROM users WHERE username = ? AND password = ?";

  private final Connection connection;
  private final Session session;
  private final Scanner scanner;

  public AuthServiceImpl(Connection connection, Session session, Scanner scanner) {
    this.connection = connection;
    this.session = session;
    this.scanner = scanner;
  }

  private String readLine() {
    return scanner.hasNextLine() ? scanner.nextLine() : "";
  }

  @Override
  public int signUp() {
    System.out.print("아이디를 입력하세요: ");
    String username = readLine();
    if (username.isEmpty()) {
      System.out.print("아이디를 입력하세요.\n");
      return -201;
    }

    System.out.print("비밀번호를 입력하세요: ");
    String password = readLine();
    if (password.isEmpty()) {
      System.out.print("비밀번호를 입력하세요.\n");
      return -202;
    }

    System.out.print("비밀번호를 한 번 더 입력하세요: ");
    String passwordConfirm = readLine();
    if (!password.equals(passwordConfirm)) {
      System.out.print("비밀번호가 일치하지 않습니다.\n");
      return -202;
    }

    System.out.print("전화번호를 입력하세요: ");
    String phoneNumber = readLine();
    if (phoneNumber.isEmpty()) {
      System.out.print("전화번호를 입력하세요.\n");
      return -203;
    }

    System.out.print("주소를 입력하세요: ");
    String address = readLine();
    if (address.isEmpty()) {
      System.out.print("주소를 입력하세요.\n");
      return -204;
    }

    System.out.print("회원 유형을 입력하세요 [Consumer / Provider]: ");
    String userTypeInput = readLine();
    System.out.printf("%s\n", userTypeInput);
    String userType;
    if (userTypeInput.isEmpty()
        || userTypeInput.equals("consumer")
        || userTypeInput.startsWith("c")
        || userTypeInput.startsWith("C")) {
      userType = "consumer";
    } else if (userTypeInput.equals("provider")
        || userTypeInput.startsWith("p")
        || userTypeInput.startsWith("P")) {
      userType = "provider";
    } else {
      System.out.print("회원 유형을 입력하세요.\n");
      return -205;
    }
    System.out.printf("%s\n", userType);

    try (PreparedStatement ps = connection.prepareStatement(INSERT_USER_SQL)) {
      ps.setString(1, username);
      ps.setString(2, password);
      ps.setString(3, phoneNumber);
      ps.setString(4, address);
      ps.setString(5, userType);
      ps.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      Database.sqlError("ORACLE error--", e);
    }

    System.out.print("\n회원가입이 완료되었습니다.\n");
    return 0;
  }

  @Override
  public int signIn() {
    System.out.print("아이디를 입력하세요: ");
    String username = readLine();
    if (username.isEmpty()) {
      System.out.print("아이디를 입력하세요.\n");
      return -211;
    }

    System.out.print("비밀번호를 입력하세요: ");
    String password = readLine();
    if (password.isEmpty()) {
      System.out.print("비밀번호를 입력하세요.\n");
      return -212;
    }

    try (PreparedStatement ps = connection.prepareStatement(SELECT_USER_SQL)) {
      ps.setString(1, username);
      ps.setString(2, password);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          Database.sqlError("ORACLE error--",
              new SQLException("ORA-01403: no data found", "02000", 1403));
          return -1; // unreachable: sqlError exits
        }
        User user = session.getUser();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setAddress(rs.getString("address"));
        user.setUserType(rs.getString("user_type"));
      }
    } catch (SQLException e) {
      Database.sqlError("ORACLE error--", e);
    }

    session.setAuthorized(true);
    System.out.printf("\n%s님 환영합니다.\n", session.getUser().getUsername());

    return 0;
  }

  @Override
  public int signOut() {
    session.clear();
    System.out.print("로그아웃 되었습니다.\n\n");
    return 0;
  }

  @Override
  public int editProfile() {
    List<String> setClauses = new ArrayList<>();
    List<String> values = new ArrayList<>();

    System.out.print("비밀번호를 입력하세요: ");
    String password = readLine();
    if (!password.isEmpty()) {
      System.out.print("비밀번호를 한 번 더 입력하세요: ");
      String passwordConfirm = readLine();
      if (!password.equals(passwordConfirm)) {
        System.out.print("비밀번호가 일치하지 않습니다.\n");
        return -1002;
      }
      setClauses.add("password = ?");
      values.add(password);
    }

    System.out.print("전화번호를 입력하세요: ");
    String phoneNumber = readLine();
    if (!phoneNumber.isEmpty()) {
      setClauses.add("phone_number = ?");
      values.add(phoneNumber);
    }

    System.out.print("주소를 입력하세요: ");
    String address = readLine();
    if (!address.isEmpty()) {
      setClauses.add("address = ?");
      values.add(address);
    }

    if (setClauses.isEmpty()) {
      return 0;
    }

    String sql = buildUpdateSql(setClauses);
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      int i = 1;
      for (String value : values) {
        ps.setString(i++, value);
      }
      ps.setInt(i, session.getUser().getUserId());
      ps.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      Database.sqlError("ORACLE error--", e);
    }
    return 0;
  }

  static String buildUpdateSql(List<String> setClauses) {
    return "UPDATE users SET " + String.join(", ", setClauses) + " WHERE user_id = ?";
  }
}
