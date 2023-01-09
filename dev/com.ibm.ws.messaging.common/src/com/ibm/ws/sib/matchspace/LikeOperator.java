/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.matchspace;

/** Subinterface of Operator that is to be implemented by a class that provides a LIKE expression as a parameterized unary
 * operator
 **/

public interface LikeOperator extends Operator {

  /**
   * Returns the escape.
   * @return char
   */
  public char getEscape(); 

  /**
   * Returns the escaped.
   * @return boolean
   */
  public boolean isEscaped(); 


  /**
   * Returns the pattern.
   * @return String
   */
  public String getPattern();
 
}
