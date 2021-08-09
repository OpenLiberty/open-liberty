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
package com.ibm.ws.sib.matchspace.selector.impl;

import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.Selector;

public class ExtensionOperatorImpl extends OperatorImpl
{

	/** Make a new extension operator
  *
  * @param op1 the sole operand
  **/

  public ExtensionOperatorImpl(Identifier op1[]) 
  {
    this.operands = op1;
    assignType();
    if (type == INVALID)
      return;
    numIds = op1.length;
  }
  
  public ExtensionOperatorImpl(Selector op1[]) 
  {
    this.operands = op1;
    assignType();
    if (type == INVALID)
      return;
    numIds = op1.length;
  }

  public ExtensionOperatorImpl(int op, Selector op1) 
  {
    super(op,op1);
  }  
  
  public ExtensionOperatorImpl(int op, Selector op1, Selector op2) 
  {
    super(op,op1,op2);
  }  
  
  public void assignType()
  {
	  type = Selector.EXTENSION;
  }
 
  public String toString()
  {
  	String theString = "extensionOP: ";
  	for(int i=0;i<operands.length;i++)
  		theString = theString + "/" + operands[i];
	  return theString; 
  }
  
  /**
   * The Selector is extended if operating in the XPath domain
   */
  public boolean isExtended()
  {
    return true;
  }    
}
