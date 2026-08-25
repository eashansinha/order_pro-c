package com.orderpro.service;

/**
 * Auth slice contract, ported from main.pc.
 * Each method returns 0 on success or the original negative return code:
 * SignUp: -201..-205, SignIn: -211/-212, EditProfile: -1002.
 */
public interface AuthService {

  /** Ports SignUp() — inserts into users via users_seq.nextval. */
  int signUp();

  /** Ports SignIn() — loads the user into the Session and sets authorized. */
  int signIn();

  /** Ports SignOut() — clears the Session. */
  int signOut();

  /** Ports EditProfile() — dynamic UPDATE of password/phone_number/address. */
  int editProfile();
}
