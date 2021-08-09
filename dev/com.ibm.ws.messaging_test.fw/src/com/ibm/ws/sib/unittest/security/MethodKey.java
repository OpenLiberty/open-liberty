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
package com.ibm.ws.sib.unittest.security;

/**
 * <p>This class identifies a method on a class. It can be used as a key into
 *   a map.
 * </p>
 *
 * <p>SIB build component: sib.unittest.security</p>
 *
 * @author nottinga
 * @version 1.2
 * @since 1.0
 */
public final class MethodKey
{
  /** The name of the method */
  private String _methodName;
  /** The name of the class */
  private String _className;
  /** The hashCode (cached for performance reasons */
  private int _hashCode;

  /* ------------------------------------------------------------------------ */
  /* MethodKey method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * the constructor.
   * 
   * @param className  the name of the class the method is on.
   * @param methodName the name of the method.
   */
  public MethodKey(String className, String methodName)
  {
    _className = className;
    _methodName = methodName.intern();
    
    _hashCode = _methodName.hashCode() * 1003 + _className.hashCode();
  }

  public boolean equals(Object other)
  {
    if (other == this)
      return true;
    if (other == null)
      return false;

    if (other instanceof MethodKey)
    {
      MethodKey otherKey = (MethodKey) other;
      return otherKey._hashCode == _hashCode && otherKey._className == _className && otherKey._methodName == _methodName;
    }

    return false;
  }
  
  public int hashCode()
  {
    return _hashCode;
  }

  /* ------------------------------------------------------------------------ */
  /* getMethodName method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @return the method name.
   */
  public String getMethodName()
  {
    return _methodName;
  }
  
  /* ------------------------------------------------------------------------ */
  /* getClassName method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @return the class name.
   */
  public String getClassName()
  {
    return _className;
  }
}