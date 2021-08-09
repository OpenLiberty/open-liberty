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
import java.util.HashMap;

import com.ibm.ws.sib.matchspace.impl.Matching;

/** 
 * This class wraps the node object supplied to MFP through the getIdentifierValue()
 * method on the MatchSpaceKey interface. In practice it will wrap a DOM Node, but
 * the class' implementation is kept DOM-neutral so that it may be held as common
 * code.
 * 
 * Its prime purpose is to provide a cache of results that have been retrieved
 * when evaluating XPath subexpressions in the context of the wrapped DOM node.
 *   
 **/
public class WrappedNodeResults 
{
  // The wrapped DOM node
  private Object node = null;

  // These maps provide a cache for the evaluation of an XPath expression
  // where this node is the context. For example, if this node 
  private HashMap evalBoolTable = null;
  private HashMap evalNumberTable = null;
  private HashMap evalStringTable = null;
  private HashMap evalListTable = null;
  

  public WrappedNodeResults(Object node)
  {
    evalBoolTable = new HashMap();
    evalNumberTable = new HashMap();
    evalStringTable = new HashMap();
    evalListTable = new HashMap();
    this.node = node;
  }
  
  public Object getNode()
  {
    return node;
  }
  
  public ArrayList getStringList(String key) 
  {
    ArrayList theReturn = (ArrayList)evalStringTable.get(key);
    return theReturn;
  }
  
  public ArrayList getNumberList(String key) 
  {
    ArrayList theReturn = (ArrayList)evalNumberTable.get(key);
    return theReturn;
  }
  
  public void addEvalStringResult(String key, ArrayList result)
  {
    evalStringTable.put(key, result);
  }

  public void addEvalNumberResult(String key, ArrayList result)
  {
    evalNumberTable.put(key, result);
  }  

  public void addEvalListResult(String key, ArrayList result)
  {
    evalListTable.put(key, result);
  }    

  public void addEvalBoolResult(String key, Boolean result)
  {
    evalBoolTable.put(key, result);
  }  
  
  public ArrayList getEvalStringResult(String key)
  {
    ArrayList result = (ArrayList)evalStringTable.get(key);
    return result;
  }  
  
  public ArrayList getEvalNumberResult(String key)
  {
    ArrayList result = (ArrayList)evalNumberTable.get(key);
    return result;
  }
  
  public Boolean getEvalBoolResult(String key)
  {
    Boolean result = (Boolean)evalBoolTable.get(key);
    return result;
  }      

  public ArrayList getEvalListResult(String key)
  {
    ArrayList result = (ArrayList)evalListTable.get(key);
    return result;
  }
  
  public String toString() 
  {
    String asString = Matching.getEvaluator().getNodeText(node);
    if(asString == null)
    {
      if(node == null)
        asString = "null";
      else
        asString = node.toString();
    }
// The following will additionally dump cache info. Can be a bit 
// voluminous.
//    
//    asString = asString + 
//               ", String: " + evalStringTable.toString() +
//               ", Number: " + evalNumberTable.toString() +
//               ", Bool: " + evalBoolTable.toString() +
//               ", List: " + evalListTable.toString();
    return asString;
  }    
}
