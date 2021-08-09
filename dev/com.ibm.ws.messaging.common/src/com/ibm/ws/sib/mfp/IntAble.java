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
package com.ibm.ws.sib.mfp;

/**
 * This is a 'marker' interface which indicates that it is possible to obtain
 * an integer value corresponding to an instance of the implementing class.
 */
public interface IntAble {

  /**
   * Returns the integer representation of the instance of a type-safe
   * enumeration which extends this interface.
   *
   * @return int      The integer representation of the Inttable.
   */
  public int toInt();

}
