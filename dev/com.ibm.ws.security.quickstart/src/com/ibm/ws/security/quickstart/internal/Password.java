/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.quickstart.internal;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * A base class defining behaviour and creation methods for doing
 * password checks. This allows the client to do a password check
 * using plain text, xor, aes or hash (recommended) without needing
 * to know the details.
 */
public abstract class Password {
  protected final SerializableProtectedString pwd;
  private final boolean isEmpty;

  public Password(SerializableProtectedString pwd) {
    this.pwd = pwd;
    isEmpty = new String(pwd.getChars()).trim().isEmpty();
  }

  public abstract boolean checkPassword(String pwd);

  public final boolean isEmpty() {
    return isEmpty;
  }

  public static Password create(SerializableProtectedString pwd) {
    String pwdStr = new String(pwd.getChars());
    if (PasswordUtil.getCryptoAlgorithm(pwdStr) == null) {
      return new PlainTextPassword(pwd);
    } else if (PasswordUtil.isHashed(pwdStr)) {
      return new HashedPassword(pwd);
    } else {
      return new ReversablePassword(pwd);
    }
  }

  /**
   * Performs a simple equals check on the password. The password needs
   * to be stored in plain text. A bad production practice, but in dev
   * this is quite common.
   */
  private static class PlainTextPassword extends Password {
    public PlainTextPassword(SerializableProtectedString pwd) {
      super(pwd);
    }

    public boolean checkPassword(@Sensitive String password) {
      if (pwd != null) {
        return Arrays.equals(pwd.getChars(), password.toCharArray());
      } else {
        return password == null;
      }
    }
  }

  /**
   * Performs a password check for xor or aes encrypted passwords.
   * Essentially decrepts/decodes the password and does an equal check.
   */
  private static class ReversablePassword extends Password {

    public ReversablePassword(SerializableProtectedString pwd) {
      super(pwd);
    }

    public boolean checkPassword(@Sensitive String password) {
      if (pwd != null) {
        return Arrays.equals(decodePassword(pwd).getChars(), password.toCharArray());
      } else {
        return password == null;
      }
    }

    private static SerializableProtectedString decodePassword(SerializableProtectedString pw) {
      if (pw != null) {
          String password = new String(pw.getChars());
          password = PasswordUtil.passwordDecode(password.trim());
          char[] passswordArray = new char[password.length()];
          password.getChars(0, password.length(), passswordArray, 0);
          return new SerializableProtectedString(passswordArray);
      }
      return null;
    }
  }

  /**
   * Performs a password check on a hashed password. Works by
   * recomputing the hash and comparing the hash.
   */
  private static class HashedPassword extends Password {
    private String hashAlgorithm;

    public HashedPassword(SerializableProtectedString pwd) {
      super(pwd);
      String pwdStr = new String(pwd.getChars());
      hashAlgorithm = PasswordUtil.getCryptoAlgorithm(pwdStr);
    }

    public boolean checkPassword(@Sensitive String password) {
      HashMap<String, String> props = new HashMap<String, String>();
      props.put(PasswordUtil.PROPERTY_HASH_ENCODED, new String(pwd.getChars()));
      String inPass = null;
      try {
          inPass = PasswordUtil.encode(password, hashAlgorithm, props);
          return Arrays.equals(pwd.getChars(), inPass.toCharArray());
      } catch (Exception e) {
          //fail to encode password.
          throw new IllegalArgumentException("password encoding failure : " + e.getMessage());
      }
    }
  }
}
