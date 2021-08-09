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

package com.ibm.wsspi.sib.core;

/**
 Distribution relates to the distribution model that a Destination possesses
 or a Session requires.
 <p>
 This class has no security implications.
 */
public class Distribution {
  
  /**
   * ONE type
   */
  public static final Distribution ONE  = new Distribution("ONE",0);
  
  /**
   * ALL type
   */
  public static final Distribution ALL  = new Distribution("ALL",1);
  
  private static final Distribution[] set = {ONE
                                              ,ALL
                                              };


  private final String name;
  private final int value;

  /**
   * Return the name of the Distribution type as a string
   *
   * @return String name of the Distribution type
   */

  public final String toString () {
    return name;
  }
  
  /**
   * Method toInt.
   * @return int
   */
  public final int toInt () {
    return value;
  }
  
  /**
   * Method getDistribution.
   * @param aValue
   * @return Distribution
   */
  public final static Distribution getDistribution(int aValue) {
    return set[aValue];
  }

  /**
   * Method Distribution.
   * @param name
   * @param value
   */
  // Private constructor prevents this class being extended so there is no need
  // to make this class final

  private Distribution (String name, int value) {
    this.name = name;
    this.value = value;
  }


}
