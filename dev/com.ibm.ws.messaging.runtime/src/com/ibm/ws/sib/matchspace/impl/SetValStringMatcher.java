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

import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.impl.Matching;
import com.ibm.ws.sib.matchspace.MatchingException;
import com.ibm.ws.sib.matchspace.SearchResults;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceComponent;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;

public class SetValStringMatcher extends StringMatcher 
{
  // Standard trace boilerplate
  private static final Class cclass = SetValStringMatcher.class;
  private static Trace tc = TraceUtils.getTrace(SetValStringMatcher.class,
    MatchSpaceConstants.MSG_GROUP_LISTS);   
  
  /** 
   * Make a new SetValStringMatcher
   * @param id the identifier to use
   */
  SetValStringMatcher(Identifier id) 
  {
    super(id);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      tc.entry(cclass, "SetValStringMatcher", "identifier: "+ id);
      tc.exit(cclass, "SetValStringMatcher", this);
    }
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.matchspace.impl.StringMatcher#handleGet(java.lang.Object, com.ibm.ws.sib.matchspace.MatchSpaceKey, com.ibm.ws.sib.matchspace.EvalCache, java.lang.Object, com.ibm.ws.sib.matchspace.SearchResults)
   */
  void handleGet(Object value, 
                   MatchSpaceKey msg, 
                   EvalCache cache,
                   Object contextValue,
                   SearchResults result) 
      throws MatchingException, BadMessageFormatMatchingException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(this,
               cclass, 
               "handleGet", 
               new Object[] {value,msg,cache,contextValue,result});
    // Call the handleGet() method in the parent class but 
    // provide a context and we may need to call it multiple times
    if (value != null && value instanceof SetValEvaluationContext)
    {
      // The "value" we get back is a list of wrapped string values and
      // DOM nodes that we need to process individually
      
      SetValEvaluationContext xpContext = (SetValEvaluationContext)value;
      ArrayList nodeList = xpContext.getWrappedNodeList();
      
      // Iterate over the nodes that were passed into us
      Iterator iter = nodeList.iterator();

      while(iter.hasNext())
      {
        WrappedNodeResults wrappedNode = (WrappedNodeResults)iter.next();
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(this,cclass, "handleGet", "Processing node: " + wrappedNode); 
        
        // Need to be careful here, we propagate the string value of
        // the attribute or element that we have retrieved, but the
        // context for subsequent operations is the parent node.
        ArrayList stringList = wrappedNode.getStringList(id.getName());
        if(stringList != null)
        {
          Iterator iterString = stringList.iterator();
          
          while(iterString.hasNext())
          {
            String strValue = (String)iterString.next();

            // Need to pass in an XPathEvaluationContext
            SetValEvaluationContext xpEval = new SetValEvaluationContext(wrappedNode);
      
            super.handleGet(strValue, msg, cache, xpEval, result);
          } // eof while             
        } // eof if integerList not null               
      } // eof iterating over the nodes
    } // eof value not null 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "handleGet");        
  }
    
  /** Override default implementation of getValue()
   * 
   * @param msg
   * @param contextValue
   * @throws MatchingException
   * @throws BadMessageFormatMatchingException
   */
  Object getValue(
        MatchSpaceKey msg,
        Object contextValue)
        throws MatchingException, BadMessageFormatMatchingException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(
        cclass,
        "getValue",
        "msg: " + msg + "contextValue: " + contextValue);
    
    // The value which we'll return    
    SetValEvaluationContext xpEval = null;    

    // May need to call MFP multiple times, if our context has multiple nodes
    if(contextValue != null)
    {
      // Currently this must be a list of nodes
      if (contextValue instanceof SetValEvaluationContext)
      {
        SetValEvaluationContext evalContext = (SetValEvaluationContext)contextValue;

        // Get the node list
        ArrayList wrappedParentList = evalContext.getWrappedNodeList();
        
        // If the list is empty, then we have yet to get the document root
        if(wrappedParentList.isEmpty())
        {
    	    //Set up a root (document) context for evaluation
          Object docRoot = Matching.getEvaluator().getDocumentRoot(msg);
            
          // Create an object to hold the wrapped node
          WrappedNodeResults wrapper = new WrappedNodeResults(docRoot);
          // Set the root into the evaluation context
          evalContext.addNode(wrapper);
        }
        
        // Iterate over the nodes
        Iterator iter = wrappedParentList.iterator();

        while(iter.hasNext())
        {
          WrappedNodeResults nextWrappedNode = (WrappedNodeResults)iter.next();       
          Object nextParentNode = nextWrappedNode.getNode();
          
          String debugString = "";
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            tc.debug(this,cclass, "getValue", "Evaluation context node: " + nextWrappedNode);  
          
          // If no cached value we'll need to call MFP
          ArrayList stringList = nextWrappedNode.getEvalStringResult(id.getName());
            
          if(stringList == null)
          {
            // Call MFP to get the results for this node
            ArrayList childNodeList = 
              (ArrayList) msg.getIdentifierValue(id,
                                                 false,
                                                 nextParentNode, 
                                                 true); // true means return a nodelist
            
            // Process the retrieved nodes to get a list of Integers
            stringList = Matching.getEvaluator().castToStringList(childNodeList);
            
            // Add result to cache for next time
            nextWrappedNode.addEvalStringResult(id.getName(), stringList);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              debugString = "Result from MFP ";
          }
          else
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              debugString = "Result from Cache ";
          }
          
          // Useful debug
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          {
            String asString = ", type: String - <";
            if(stringList == null)
            {
              asString = asString + "null";
            }
            else
            {            
              Iterator iterDebug = stringList.iterator();
            
              boolean first = true;
              while(iterDebug.hasNext())
              {
                String str = (String)iterDebug.next();
                if(!first)
                  asString = asString + "," + str;
                else
                {
                  asString = asString + str;
                  first = false;
                }
              }
            }
            asString = asString + ">"; 
            tc.debug(this,cclass, "getValue", debugString + "for identifier " + id.getName() + asString);
          } 
          
          // Build the return value as an XPathEvaluationContext
          if(xpEval == null)
            xpEval = new SetValEvaluationContext();
  
          // Add the wrapped node to the result
          xpEval.addNode(nextWrappedNode);
            
        } // eof while
      } // eof instanceof XPathEvaluationContext
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "getValue", xpEval);
    return xpEval;
  } // eof getValue()  
}
