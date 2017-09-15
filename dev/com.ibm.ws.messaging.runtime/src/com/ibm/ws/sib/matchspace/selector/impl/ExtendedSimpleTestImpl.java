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

import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.SimpleTest;


public class ExtendedSimpleTestImpl extends SimpleTestImpl
{
	//Identifier wrappedIdentifiers[] = null;
	
	public ExtendedSimpleTestImpl(Selector sel) 
	{
	  super(sel);
	  
	  // If kind is ID and the selector is not of type CHILD, 
	  // re-map to NOTNULL, which gives the desired behaviour for XPath support
	  
	  // As a result of the remapping, predicates such as [employee] and
	  // [employee>100] can be correctly combined.
//      if (sel instanceof Identifier)
//      {
//        identifier = (Identifier) sel;
//        if (kind == ID && identifier.getType() != Selector.CHILD)
//        {
////          kind = NOTNULL;
//        }
//      }      
    }
  // If kind is ID treat as if we have NOTNULL type, to give the desired 
  // behaviour for XPath support. Then predicates such as 
  // [salary] and [salary>100] can be correctly combined.  
  public boolean combine(SimpleTest other)
  {
    if (kind == ID)
    {
      absorb(other);
      return true;
    }
    else
      return super.combine(other);
  }
}
