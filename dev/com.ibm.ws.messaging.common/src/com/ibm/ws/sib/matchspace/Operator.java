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
package com.ibm.ws.sib.matchspace;

/** This interface is to be implemented by classes that represent one Operator in a 
 * Selector expression 
 */

public interface Operator extends Selector {

  /** Assign the appropriate type to an operator based on its op code and operands.  Also
   * assigns a type to operands of previously UNKNOWN type if a more precise type can be
   * inferred.  Called as a subroutine of the constructors and also by Transformer.resolve
   * after an operand has a type assigned by the Resolver.
   **/

  public void assignType();

  public int hashCode();

  /**
   * Returns the op.
   * @return int
   */
  public int getOp(); 

  /**
   * Returns the operands.
   * @return Selector[]
   */
  public Selector[] getOperands();
  
  public void setOperand(int i, Selector operand);  
}
