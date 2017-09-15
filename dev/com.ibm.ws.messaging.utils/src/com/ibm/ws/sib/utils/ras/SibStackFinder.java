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
package com.ibm.ws.sib.utils.ras;

import java.security.AccessController;
import java.security.PrivilegedAction;

/* ************************************************************************** */
/**
 *  Find the class of the caller that is NOT part of the com.ibm.ws.sib.utils.ras package
 */
/* ************************************************************************** */
public class SibStackFinder extends SecurityManager
{
  private static SibStackFinder finder = null;
  
  private static final Package OUR_PACKAGE = SibStackFinder.class.getPackage();
  
  /* -------------------------------------------------------------------------- */
  /* getInstance method
  /* -------------------------------------------------------------------------- */
  /**
   * @return the singleton instance of a SibStackFinder
   */
  public static synchronized SibStackFinder getInstance()
  {
    if (finder == null)
    {
      finder = AccessController.doPrivileged(new PrivilegedAction<SibStackFinder>()
      {
        public SibStackFinder run()
        {
          return new SibStackFinder();
        }
      }); 
    }
    return finder;
  }
  
  /* -------------------------------------------------------------------------- */
  /* getCaller method
  /* -------------------------------------------------------------------------- */
  /**
   * @return the class outside of our package that called into our package. 
   */
  public Class<?> getCaller()
  {
    Class<?> stack[] = this.getClassContext();
    
    // Walk the stack looking for a caller that isn't in our package
    // (There's no need to look at index 0 as that's us!)
    for(int i=1; i < stack.length; i++)
    {
      if (!stack[i].getPackage().equals(OUR_PACKAGE))
        return stack[i];
    }
    
    return null;
  }
}
