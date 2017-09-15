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

import com.ibm.ejs.ras.Traceable;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/* ************************************************************************** */
/**
 * Passsword wraps a string to protect it from trace and ffdc. It is immutable
 *
 * NOTE: Although this class may claim to be serializable, this is merely for
 *       the convenience of admin TaskForm classes. Any attempt to actually
 *       serialize instances of this class will result in exceptions.
 *       If the user of this class manipulates the password using the char array
 *       methods AND the app server is running with correctly configured Java 2
 *       security, the password should be fairly safely protected....
 */
/* ************************************************************************** */
public final class Password implements Traceable, FFDCSelfIntrospectable, Serializable
{
  /** The serial version id for this class */
  private static final long serialVersionUID = 20090820150226L;

  /** Salt to be used when hashing - different for each class loader
   * used so that a dictionary attack will not work. (A dictionary attack is
   * one where you duplicate the code in this class and then hash every word
   * in a dictionary to see if that paswword appears in the trace or ffdc file
  */
  private static final byte[] SALT = (new SIBUuid12()).toByteArray();

  /** A password object that holds null */
  public static final Password NULL_PASSWORD = new Password((char[])null);

  /** A password object that holds the equivalent of the empty string */
  public static final Password EMPTY_PASSWORD = new Password(new char[] {});

  /** The actual password - stored as a character array - ideally passwords
      should NEVER appear as a string so that they do not get interned */
  private final char[] _password;

  /** A traceable string that will be the same for identical passwords (in
      the same app server run */
  private String _traceableString = null;

  /* ------------------------------------------------------------------------ */
  /* Password method                                                          */
  /* ------------------------------------------------------------------------ */
  /**
   * Construct a "Password" from a string. The string will not be revealed to
   * trace or ffdc by this class
   *
   * @param password             The password to be protected
   *
   * @deprecated You should use the constructor that takes a char[] instead
   * @see        #Password(char[])
   */
  /* ------------------------------------------------------------------------ */
  @Deprecated public Password(String password)
  {
    if (password != null)
    {
      _password = password.toCharArray();
    }
    else
    {
      _password = null;
    }
  }

  /* ------------------------------------------------------------------------ */
  /* Password method                                                          */
  /* ------------------------------------------------------------------------ */
  /**
   * Construct a "Password" from an array of characters. The characters will not
   * be trace or ffdc by this class
   *
   * @param password             The password to be protected
   */
  /* ------------------------------------------------------------------------ */
  public Password(char[] password)
  {
    if (password != null)
    {
      if (password.length != 0)
      {
        _password = new char[password.length];
        System.arraycopy(password,0,_password,0,password.length);
      }
      else
      {
        _password = password; // Can safely use the empty array
      }
    }
    else
    {
      _password = null;
    }
  }

  /* ------------------------------------------------------------------------ */
  /* getPassword method                                                       */
  /* ------------------------------------------------------------------------ */
  /**
   * Return the protected password (Note: it is then the job of the caller
   * to prevent its copies reaching trace or ffdc)
   *
   * @return String The protected password
   *
   * @Deprecated ideally you would never convert passwords into strings
   * @see #getPasswordChars
   */
  /* ------------------------------------------------------------------------ */
  @Deprecated public String getPassword()
  {
    if (_password != null)
    {
      return new String(_password);
    }
    else
    {
      return null;
    }
  }

  /* ------------------------------------------------------------------------ */
  /* getPasswordChars method                                                  */
  /* ------------------------------------------------------------------------ */
  /**
   * Return the protected password (Note: it is then the job of the caller
   * to prevent its copies reaching trace, ffdc or converting it to a string
   *
   * @return char[] The protected password
   */
  /* ------------------------------------------------------------------------ */
  public char[] getPasswordChars()
  {
    if (_password != null)
    {
      if (_password.length != 0)
      {
        char[] result = new char[_password.length];
        System.arraycopy(_password,0,result,0,_password.length);
        return result;
      }
      else
      {
        return _password; // Can safely return the empty array - caller can't change it!
      }
    }
    else
    {
      return null;
    }
  }

  /* ------------------------------------------------------------------------ */
  /* toString method                                                          */
  /* ------------------------------------------------------------------------ */
  /**
   * Convert the password to a string, revealing only if it is null or non-null
   *
   * @return String A string representation of the password that can be used in trace etc.
   */
  /* ------------------------------------------------------------------------ */
  @Override public String toString()
  {
    if (_password != null)
    {
      return "*****";
    }
    else
    {
      return "null";
    }
  }

  /* ------------------------------------------------------------------------ */
  /* toTraceString method                                                     */
  /* ------------------------------------------------------------------------ */
  /**
   * Convert the password to a string for tracing purposes. This provides
   * a string that, for the same password, will be the same string, but will
   * be different for different password (well, almost certainly different)
   * and/or different class loaders (of Password). The password cannot
   * be deduced from the trace string
   *
   * @return String A string for the password for trace purposes
   */
  /* ------------------------------------------------------------------------ */
  public String toTraceString()
  {
    if (_traceableString == null)
    {
      if (_password != null)
      {
        try
        {
          MessageDigest digester = MessageDigest.getInstance("SHA-256");
          digester.update(SALT);

          for(char c : _password)
          {
            digester.update( (byte)((c&0xFF00)>>8) );
            digester.update( (byte)((c&0x00FF))    );
          }

          byte[] hash = digester.digest();
          StringBuilder sb = new StringBuilder();

          // Throw away the high nibbles of each byte to increase security and reduce the
          // length of the trace string
          for (byte b:hash)
          {
            int i = b & 0x0F;
            sb.append(Integer.toHexString(i));
          }
          _traceableString = sb.toString();
        }
        catch(NoSuchAlgorithmException nsae)
        {
          // No FFDC Code needed - fall back on the toString implementation
          _traceableString = toString();
        }
      }
      else
      {
        _traceableString = ""; /* not just null :-) */
      }
    }

    return _traceableString;
  }

  /* ------------------------------------------------------------------------ */
  /* introspectSelf method                                                    */
  /* ------------------------------------------------------------------------ */
  /**
   * Provide details on the state of this object to ffdc, hiding the actual
   * contents of the password
   *
   * @return String[] An array of strings to be added to the ffdc log
   */
  /* ------------------------------------------------------------------------ */
  public String[] introspectSelf()
  {
    return new String[] { "_password = "+toString(), "_traceablePassword = "+toTraceString() };
  }

  /* ------------------------------------------------------------------------ */
  /* equals method                                                            */
  /* ------------------------------------------------------------------------ */
  /**
   * Determine if this password is the same as another object
   *
   * @param o                    The other object
   * @return boolean true if the other object is a Password and is the same of this one
   */
  /* ------------------------------------------------------------------------ */
  @Override public boolean equals(Object o)
  {
    if (o == null) return false;
    if (o == this) return true;

    if (o instanceof Password)
    {
      Password other = (Password)o;
      return Arrays.equals(_password,other._password);
    }
    else
    {
      return false;
    }
  }

  /* ------------------------------------------------------------------------ */
  /* hashCode method                                                          */
  /* ------------------------------------------------------------------------ */
  /**
   * return a hash code for this Password
   *
   * @return int The hash code of this password
   */
  /* ------------------------------------------------------------------------ */
  @Override public int hashCode()
  {
    return Arrays.hashCode(_password);
  }

  /* ------------------------------------------------------------------------ */
  /* writeObject method                                                       */
  /* ------------------------------------------------------------------------ */
  /**
   * Although this class may claim to serializable, we prevent actual use of
   * serialization by making this method throw exceptions all the time....
   *
   * @param s                    The stream in which to write ourselves
   * @exception IOException is thrown if the stream throws an exception (or if the encoding fails)
   */
  /* ------------------------------------------------------------------------ */
  private void writeObject(ObjectOutputStream s) throws IOException
  {
    throw new IOException();
  }

  /* ------------------------------------------------------------------------ */
  /* readObject method                                                        */
  /* ------------------------------------------------------------------------ */
  /**
   * Although this class may claim to serializable, we prevent actual use of
   * serialization by making this method throw exceptions all the time....
   *
   * @param s                    The stream from which to read ourselves
   * @exception IOException is thrown if the stream throws an exception
   */
  /* ------------------------------------------------------------------------ */
  private void readObject(ObjectInputStream s) throws IOException
  {
    throw new IOException();
  }
}
