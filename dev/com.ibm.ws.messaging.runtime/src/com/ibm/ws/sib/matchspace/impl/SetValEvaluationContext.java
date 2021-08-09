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
package com.ibm.ws.sib.matchspace.impl;

import java.util.ArrayList;
import java.util.Iterator;

/** 
 * This class represents the evaluation context for Set Value Matcher classes.
 * 
 * It comprises a list of wrapped nodes that are maintained in WrappedNodeResults 
 * objects.
 *   
 **/
public class SetValEvaluationContext 
{

  private ArrayList contextNodeList = null;
  
  /**
   * Construct a new empty context
   */
  public SetValEvaluationContext()
  {
    contextNodeList = new ArrayList();
  }
  
  /**
   * Construct a new context with a single wrapped node
   * 
   * @param wrapper
   */
  public SetValEvaluationContext(WrappedNodeResults wrapper)
  {
    contextNodeList = new ArrayList();
    
    contextNodeList.add(wrapper); 
  }
  
  public void addNode(WrappedNodeResults wrapper) 
  {
    contextNodeList.add(wrapper);  
  }  

  public void addUnwrappedNode(Object node) 
  {
      // Create an object to hold the wrapped node
      WrappedNodeResults wrapper = new WrappedNodeResults(node);
      contextNodeList.add(wrapper); 
   }    
  
  public void addWrappedNodeList(ArrayList wrappedNodeList) 
  {
    Iterator iter = wrappedNodeList.iterator();

    while(iter.hasNext())
    {
      WrappedNodeResults wrapper = (WrappedNodeResults)iter.next();
      contextNodeList.add(wrapper); 
    }
  }    
  
  public ArrayList getWrappedNodeList()
  {
    return contextNodeList;
  }

  public String toString() 
  {
    String asString = "";
    StringBuffer sb = null;
    Iterator iter = contextNodeList.iterator();

    while(iter.hasNext())
    {
      WrappedNodeResults wrapper = (WrappedNodeResults)iter.next();
      if(sb == null)
      {
        // Instantiate a StringBuffer
        sb = new StringBuffer("<");
      }
      else
      {
        sb.append(" <");       
      }
      sb.append(wrapper.toString());
      sb.append(">"); 
    }
    // We've finished
    if (sb != null)
    {
      asString = sb.toString();
    }
    return asString;
  }  
  
}
