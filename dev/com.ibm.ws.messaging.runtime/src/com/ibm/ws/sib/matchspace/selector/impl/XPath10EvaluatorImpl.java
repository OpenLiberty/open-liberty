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

import java.util.ArrayList;
import java.util.Iterator;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;

public class XPath10EvaluatorImpl extends EvaluatorImpl
{
  private static Trace tc = TraceUtils.getTrace(XPath10EvaluatorImpl.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);  
  private static final Class cclass = XPath10EvaluatorImpl.class;
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.matchspace.selector.impl.EvaluatorImpl#getStringFromNode(java.lang.Object)
   */
  protected String getStringFromNode(Object inNode)
  {
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, "getStringFromNode", inNode);
    
    String strValue = null;
    if (inNode instanceof Node)
    {
      Node theNode = (Node)inNode;
      // Check the type of the node
      int nodeType = theNode.getNodeType();
      if(nodeType == Node.ATTRIBUTE_NODE)
      {
        strValue = theNode.getNodeValue();
      }
//TODO: More special casing needed here dependent on node type? text etc?        
      else
      {
        // Note this gets the text associated with the first child element only.
        Node firstChild = theNode.getFirstChild();
        if(firstChild != null)
          strValue = firstChild.getNodeValue();
      }
    }  // eof is inNode instanceof Node
    
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(cclass, "getStringFromNode", strValue);    
    return strValue;
  }    

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.matchspace.selector.impl.EvaluatorImpl#getNodeText(java.lang.Object)
   */
  public String getNodeText(Object inNode)
  {
    String strValue = null;
    
    if (inNode instanceof Node)
    {
      Node theNode = (Node)inNode;
      // Check the type of the node

      String nodeName = theNode.getNodeName();
      if(nodeName == null)
        nodeName = "null";
      StringBuffer wholeString = new StringBuffer(nodeName);
      StringBuffer nodeDetailStr = null;

      NamedNodeMap nodeMap = theNode.getAttributes();
      if(nodeMap != null)
      {
        for(int i=0;i<nodeMap.getLength();i++)
        {
          Node attrNode = nodeMap.item(i);
          if(nodeDetailStr == null)
            nodeDetailStr = new StringBuffer("");
          nodeDetailStr.append(" ");
          nodeDetailStr.append(attrNode.getNodeName());
          nodeDetailStr.append(":");
          nodeDetailStr.append(attrNode.getNodeValue());
          if(i < nodeMap.getLength()-1)
            nodeDetailStr.append(", ");
        }
      }

      if(nodeDetailStr == null)
      {
        // Note this gets the text associated with the first child element only.
        Node firstChild = theNode.getFirstChild();
        if(firstChild != null)
        {
          String firstChildStr = firstChild.getNodeValue();
          if(firstChildStr != null)
            nodeDetailStr = new StringBuffer(firstChildStr);
        }
      }
      
      if(nodeDetailStr == null)
      {
        wholeString.append("(null)");
      }
      else
      {
        wholeString.append("(");
        wholeString.append(nodeDetailStr);
        wholeString.append(")");
      }
        strValue = wholeString.toString(); 
    }  // eof is inNode instanceof Node
    
    return strValue;
  }      

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.matchspace.selector.impl.EvaluatorImpl#getDocumentRoot(com.ibm.ws.sib.matchspace.MatchSpaceKey)
   */
  public Object getDocumentRoot(MatchSpaceKey msg) 
    throws BadMessageFormatMatchingException
  {
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, "getDocumentRoot", msg);

    //Set up a root (document) context for evaluation
    Node docRoot = (Node)msg.getRootContext();
    
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(cclass, "getDocumentRoot", docRoot);    
    return docRoot;
  }      
  
  /**
   * Evaluate a comparison between a list and a second value. Other parameters are a flag saying
   * whether the test is equality (false) or less-than (true), and a flag saying whether
   * conversion rules are strict (false) or permissive (true).
   * 
   * @param firstList
   * @param val1
   * @param lessThan
   * @param permissive
   * @param overallTrue
   * @return
   */
  protected Boolean compareList( // was BooleanValue
      ArrayList firstList,
      Object val1,
      boolean lessThan,
      boolean permissive,
      boolean overallTrue) // If true we are searching for at least one TRUE result, else at least one FALSE.
    {
      if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.entry(cclass, 
                 "compareList", 
                 new Object[] { firstList, 
                                val1, 
                                new Boolean(lessThan), 
                                new Boolean(permissive), 
                                new Boolean(overallTrue) });
    
      ArrayList secondList = null;
      if(val1 instanceof ArrayList)
        secondList = (ArrayList)val1;
      
      Boolean finalReturn = null;
      // We need to compare each item in each list with each other
      if(firstList != null)
      {
        Iterator iterList1 = firstList.iterator();
        
        while(iterList1.hasNext())
        {
          Object firstListVal = (Object)iterList1.next();

          String firstStrVal = getStringFromNode(firstListVal);
          
          // Process the case where the second parameter is also a list 
          if(secondList != null)
          {
            Iterator iterList2 = secondList.iterator();
            
            while(iterList2.hasNext())
            {
              Object secListVal = (Object)iterList2.next();
              String secStrVal = null;
              if (secListVal instanceof Node)
                secStrVal = getStringFromNode((Node)secListVal);              
                         
              Object firstVal = (firstStrVal != null) ? firstStrVal : firstListVal;
              Object secondVal = (secStrVal != null) ? secStrVal : secListVal;
              
              finalReturn = 
                compareListValues(firstVal, 
                                  secondVal, 
                                  lessThan, 
                                  permissive,
                                  overallTrue);              
              
              if(finalReturn != null)
              {
                if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
                  tc.exit(cclass, "compareList", finalReturn);                      
                return finalReturn; // Boolean.TRUE
              }
            }
          } // eof processing where second parameter is a list also
          else
          {
            // In this case the second parameter is a single value
            String secStrVal = null;
            if (val1 instanceof Node)
              secStrVal = getStringFromNode((Node)val1);
            Object firstVal = (firstStrVal != null) ? firstStrVal : firstListVal;
            Object secondVal = (secStrVal != null) ? secStrVal : val1;
            
            finalReturn = 
              compareListValues(firstVal, 
                                secondVal, 
                                lessThan, 
                                permissive,
                                overallTrue);
            if(finalReturn != null)
            {
              if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
                tc.exit(cclass, "compareList", finalReturn);                      
              return finalReturn; // Boolean.TRUE
            }
          } // eof procesing where second param is single value
        }
      }
      if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(cclass, "compareList", finalReturn);       
      return finalReturn;
    }

  /**
   * Evaluate a comparison between 2 values at least one of which will be from a list.
   * If we can determine that there is a matching pair then we return TRUE.
   * 
   * @param firstVal
   * @param secondVal
   * @param lessThan
   * @param permissive
   * @param overallTrue
   * @return
   */
  private Boolean compareListValues( // was BooleanValue
      Object firstVal,
      Object secondVal,
      boolean lessThan,
      boolean permissive,
      boolean overallTrue) // If true we are searching for at least one TRUE result, else at least one FALSE.
    {
      if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.entry(cclass, 
                 "compareListValues", 
                 new Object[] { firstVal, 
                                secondVal, 
                                new Boolean(lessThan), 
                                new Boolean(permissive), 
                                new Boolean(overallTrue) });
      
      Boolean finalReturn = null;
      // Drive the compare() method
      Boolean compReturn = compare(firstVal, secondVal, lessThan, permissive);
      if(compReturn != null)
      {
        if(overallTrue)
        {
          if (compReturn.equals(Boolean.TRUE))
          {
            // We've found a pair that meet the criteria
            finalReturn = Boolean.TRUE;
          }
        }
        else
        {
          if (compReturn.equals(Boolean.FALSE))
          {
            // We've found a pair that meet the criteria
            finalReturn = Boolean.TRUE;
          }
        }
      }            

      if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(cclass, "compareListValues", finalReturn);       
      return finalReturn;
    }  
}
