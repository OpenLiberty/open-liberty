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
import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.MatchTarget;
import com.ibm.ws.sib.matchspace.impl.Matching;
import com.ibm.ws.sib.matchspace.MatchingException;
import com.ibm.ws.sib.matchspace.SearchResults;
import com.ibm.ws.sib.matchspace.SimpleTest;
import com.ibm.ws.sib.matchspace.selector.impl.OrdinalPosition;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceComponent;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;

public class SetValChildAccessMatcher extends SimpleMatcher 
{
  // Standard trace boilerplate
  private static final Class cclass = SetValChildAccessMatcher.class;
  private static Trace tc = TraceUtils.getTrace(SetValChildAccessMatcher.class,
    MatchSpaceConstants.MSG_GROUP_LISTS); 
  
  /** Arc to children */
  private ContentMatcher child;

  /** 
   * Make a new EqualityMatcher
   * @param id the identifier to use
   */
  SetValChildAccessMatcher(Identifier id) 
  {
    super(id);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      tc.entry(cclass, "SetValChildAccessMatcher", "identifier: "+ id);
      tc.exit(cclass, "SetValChildAccessMatcher", this);
    }
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.matchspace.impl.SimpleMatcher#handleGet(java.lang.Object, com.ibm.ws.sib.matchspace.MatchSpaceKey, com.ibm.ws.sib.matchspace.EvalCache, java.lang.Object, com.ibm.ws.sib.matchspace.SearchResults)
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
    if (value != null)
    {
      if (child != null)
      {
          child.get(null, msg, cache, value, result);
      }
    }       
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "handleGet");       
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.matchspace.impl.SimpleMatcher#handlePut(com.ibm.ws.sib.matchspace.SimpleTest, com.ibm.ws.sib.matchspace.Conjunction, com.ibm.ws.sib.matchspace.MatchTarget, com.ibm.ws.sib.matchspace.impl.InternTable)
   */
  void handlePut(SimpleTest test, Conjunction selector, MatchTarget object, InternTable subExpr) throws MatchingException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(this,cclass, "handlePut",
               new Object[] {test,selector,object, subExpr} 
      );    
    // This will set up a chain of SetValMatchers culminating in a 
    // DifficultMatcher that holds the MatchTarget.
    child = nextMatcher(selector, child);
    if(child != null)
    {
      child.put(selector, object, subExpr);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "handlePut");     
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
          WrappedNodeResults nextWrappedParentNode = (WrappedNodeResults)iter.next();       
          Object nextParentNode = nextWrappedParentNode.getNode();
          
          String debugString = "";
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            tc.debug(this,cclass, "getValue", "Evaluation context node: " + nextWrappedParentNode); 
          
          ArrayList wrappedChildList = nextWrappedParentNode.getEvalListResult(id.getName());
            
          // If not cached we'll need to go to MFP
          if(wrappedChildList == null)
          {
            // Call MFP to get the results for this node
            ArrayList nodeList = 
              (ArrayList) msg.getIdentifierValue(id,
                                                 false,
                                                 nextParentNode, 
                                                 true); // true means return a nodelist
            wrappedChildList = new ArrayList();
            
            if(nodeList != null)
            {
              Iterator iterNode = nodeList.iterator();
              
              while(iterNode.hasNext())
              {
                Object theNode = (Object)iterNode.next();

                // Create an object to hold the wrapped node
                WrappedNodeResults wrapper = new WrappedNodeResults(theNode);
                wrappedChildList.add(wrapper); 
              }              
            }
            // Add result to cache for next time
            nextWrappedParentNode.addEvalListResult(id.getName(), wrappedChildList);
            
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
            Iterator iterDebug = wrappedChildList.iterator();
            String asString = ", type: List - ";
            while(iterDebug.hasNext())
            {
              WrappedNodeResults wrapper = (WrappedNodeResults)iterDebug.next();
              asString = asString + "<" + wrapper + ">"; 
            }
            tc.debug(this,cclass, "getValue", debugString + "for identifier " + id.getName() + asString);
          }          
          
          if(!wrappedChildList.isEmpty())
          {
            // Build the return value as an XPathEvaluationContext
            if(xpEval == null)
              xpEval = new SetValEvaluationContext();
  
            // Add the wrapped node to the result
            xpEval.addWrappedNodeList(wrappedChildList);                      
          } 
        } // eof while
      } // eof instanceof XPathEvaluationContext
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "getValue", xpEval);
    
    return xpEval;
  } // eof getValue()  

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.matchspace.impl.SimpleMatcher#handleRemove(com.ibm.ws.sib.matchspace.SimpleTest, com.ibm.ws.sib.matchspace.Conjunction, com.ibm.ws.sib.matchspace.MatchTarget, com.ibm.ws.sib.matchspace.impl.InternTable, com.ibm.ws.sib.matchspace.selector.impl.OrdinalPosition)
   */
  void handleRemove(SimpleTest test, 
                    Conjunction selector, 
                    MatchTarget object, 
                    InternTable subExpr, 
                    OrdinalPosition parentId) 
    throws MatchingException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(this,cclass, "handleRemove", new Object[]{test,selector,object,subExpr});    
    child = child.remove(selector, object, subExpr, ordinalPosition);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "handleRemove");           
  }

  /** 
   * Test whether this Matcher is empty of children other than the vacantChild, and
   * therefore can be deleted. We need to override the method in the parent class
   **/
  boolean isEmpty()
  {
    return child == null;
  }  
}
