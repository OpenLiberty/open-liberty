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
package com.ibm.ws.sib.admin;

/**
 * <p>This class is a type safe enum of all the usage patterns for
 *   permitted chains.
 * </p>
 *
 * <p>SIB build component: sib.admin</p>
 *
 * @author nottinga
 * @version 1.1
 * @since 1.0
 */
public final class JsPermittedChainUsage
{
  /** The value returned by toInt of the ALL enum */
  public static final int ALL_INT = 0; 
  /** The value returned by toInt of the SSL_ENABLED enum */
  public static final int SSL_ENABLED_INT = 1; 
  /** The value returned by toInt of the LISTED enum */
  public static final int LISTED_INT = 2;
  
  /** The enum used to indicate that ALL chains are permitted */ 
  public static final JsPermittedChainUsage ALL = new JsPermittedChainUsage("ALL", ALL_INT);
  /** The enum used to indicate that only SSL enabled chains are permitted */
  public static final JsPermittedChainUsage SSL_ENABLED = new JsPermittedChainUsage("SSL_ENABLED", SSL_ENABLED_INT);
  /** The enum used to indicate that only chains in the specified list are permitted */
  public static final JsPermittedChainUsage LISTED = new JsPermittedChainUsage("LISTED", LISTED_INT);
  
  /** The set of all the chains, ordered by their int values */
  private final static JsPermittedChainUsage[] _set = {ALL, SSL_ENABLED, LISTED};
  
  /** The name of this enum */
  private String _name;
  /** The int value for this enum */
  private int _id;
  
  /* ------------------------------------------------------------------------ */
  /* JsPermittedChainUsage method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method creates a new instance of the typesafe enum. It is private
   * to ensure only this class can create new instances.
   * 
   * @param name The name of the enum.
   * @param id   The int id of this enum.
   */
  private JsPermittedChainUsage(String name, int id)
  {
    _name = name; 
    _id = id;
  }
  
  /* ------------------------------------------------------------------------ */
  /* fromInt method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method converts an int into an instance of this enum. If the int value
   * is not valid for this enum then null is returned. Currently the int can
   * be in the range 0-2.
   * 
   * @param id The int id of an enum.
   * @return   The matching enum for that id.
   */
  public JsPermittedChainUsage fromInt(int id)
  {
    if (id < 0 || id > 2)
    {
      return null;
    }
    else
    {
      return _set[id];
    }
  }
  
  /* ------------------------------------------------------------------------ */
  /* fromString method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method converts a string into an instance of the enum. If the string
   * is not valid for this enum then null is returned.
   * 
   * @param name The name of one of the enums.
   * @return     The matchine enum for the string.
   */
  public JsPermittedChainUsage fromString(String name)
  {
    JsPermittedChainUsage result = null;
    
    for (int i = 0; i < _set.length && result == null; i++)
    {
      if (name.equals(_set[i].toString())) result = _set[i];
    }
    
    return result;
  }
  
  /* ------------------------------------------------------------------------ */
  /* toInt method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @return the int id for this enum (useful for switch case)
   */
  public int toInt()
  {
    return _id;
  }
  
  /* ------------------------------------------------------------------------ */
  /* toString method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see java.lang.Object#toString()
   */
  public String toString()
  {
    return _name;
  }
}