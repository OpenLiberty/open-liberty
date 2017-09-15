/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.utils;

public class PasswordUtils
{
  /** The word "PASSWORD" in upper case */
  private static final String PASSWORD = "PASSWORD";

  /** The abbreviation "PWD" in upper case */
  private static final String PWD      = "PWD";

  /** The abbreviation "PASSWD" in upper case */
  private static final String PASSWD   = "PASSWD";

  /** The mask string to be used as a replacement for a password */
  private static final String MASK     = "*****";

  /* Suppress construction as we have nothing other than static methods */
  private PasswordUtils()
  {
  }

  /* ------------------------------------------------------------------------ */
  /* containsPassword method                                                  */
  /* ------------------------------------------------------------------------ */
  /**
   * Return true if the input string contains the word password or its
   * abbreviation in any case.
   *
   * @param s                    The string to be tested
   * @return boolean true if the string contains password or pwd, false otherwise
   */
  /* ------------------------------------------------------------------------ */
  public static final boolean containsPassword(String s)
  {
    String u = s.toUpperCase();

    if (u.contains(PASSWORD))
      return true;
    else if (u.contains(PWD))
      return true;
    else if (u.contains(PASSWD))
      return true;
    else
      return false;
  }

  /* ------------------------------------------------------------------------ */
  /* replaceNonNullString method                                              */
  /* ------------------------------------------------------------------------ */
  /**
   * Replaces a non-null string with "*****"
   *
   * @param s                    A string
   * @return String null if s is null, "*****" otherwise
   */
  /* ------------------------------------------------------------------------ */
  public static final String replaceNonNullString(String s)
  {
    if (s == null)
      return null;
    else
      return MASK;
  }

  /* ------------------------------------------------------------------------ */
  /* replacePossiblePassword method                                           */
  /* ------------------------------------------------------------------------ */
  /**
   * Replace a possible password with "*****"
   *
   * @param password             true if s is a password, false otherwise
   * @param s                    A string
   * @return String s if password is false, null if s is null, "*****" if password is true and s is non-null
   */
  /* ------------------------------------------------------------------------ */
  public static final String replacePossiblePassword(boolean password, String s)
  {
    if (password)
      return replaceNonNullString(s);
    else
      return s;
  }

  /* ------------------------------------------------------------------------ */
  /* replaceValueIfKeyIsPassword method                                       */
  /* ------------------------------------------------------------------------ */
  /**
   * Replace a value with "*****" if the key contains the word password
   * and the value is non-null.
   *
   * @param key                  The key
   * @param value                The value
   * @return String value if the key does NOT contain the word password, null if value is null, "*****"
   *                if the key does contain the word password and value is non-null
   */
  /* ------------------------------------------------------------------------ */
  public static final String replaceValueIfKeyIsPassword(String key, String value)
  {
    return replacePossiblePassword(containsPassword(key),value);
  }

  /* ------------------------------------------------------------------------ */
  /* replaceNonNullObbject method                                             */
  /* ------------------------------------------------------------------------ */
  /**
   * Replaces a non-null object with "*****"
   *
   * @param o                    An Object
   * @return String null if s is null, "*****" otherwise
   */
  /* ------------------------------------------------------------------------ */
  public static final String replaceNonNullObject(Object o)
  {
    if (o == null)
      return null;
    else
      return MASK;
  }

  /* ------------------------------------------------------------------------ */
  /* replacePossiblePassword method                                           */
  /* ------------------------------------------------------------------------ */
  /**
   * Replace a possible password with "*****"
   *
   * @param password             true if s is a password, false otherwise
   * @param o                    An object
   * @return o.toString() if password is false, null if s is null, "*****" if password is true and s is non-null
   */
  /* ------------------------------------------------------------------------ */
  public static final String replacePossiblePassword(boolean password, Object o)
  {
    if (password)
      return replaceNonNullObject(o);
    else if (o == null)
      return null;
    else
      return o.toString();
  }

  /* ------------------------------------------------------------------------ */
  /* replaceValueIfKeyIsPassword method                                       */
  /* ------------------------------------------------------------------------ */
  /**
   * Replace a value with "*****" if the key contains the word password
   * and the value is non-null.
   *
   * @param key                  The key
   * @param value                The value
   * @return String value.toString() if the key does NOT contain the word password, null if value is null, "*****"
   *                if the key does contain the word password and value is non-null
   */
  /* ------------------------------------------------------------------------ */
  public static final String replaceValueIfKeyIsPassword(String key, Object value)
  {
    return replacePossiblePassword(containsPassword(key),value);
  }
}
