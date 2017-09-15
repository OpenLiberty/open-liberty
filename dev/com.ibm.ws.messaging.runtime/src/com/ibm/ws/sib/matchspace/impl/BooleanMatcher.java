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

// Import required classes.
import com.ibm.ws.sib.matchspace.selector.impl.OrdinalPosition;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;
import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.SimpleTest;
import com.ibm.ws.sib.matchspace.MatchingException;
import com.ibm.ws.sib.matchspace.MatchTarget;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.SearchResults;

//------------------------------------------------------------------------------
// Class BooleanMatcher
//------------------------------------------------------------------------------
/**
 * The BooleanMatcher class handles all comparisons involving boolean
 * Identifiers that are made into simple tests by the transformer.
 *
 */
//---------------------------------------------------------------------------
public class BooleanMatcher extends SimpleMatcher
{
  // Standard trace boilerplate
  private static final Class cclass = BooleanMatcher.class;
  private static Trace tc = TraceUtils.getTrace(BooleanMatcher.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);


  /** Arc to children representing subscriptions requiring this attribute to be TRUE */

  protected ContentMatcher trueChild;


  /** Arc to children representing subscriptions requiring this attribute to be FALSE */

  protected ContentMatcher falseChild;


  //------------------------------------------------------------------------------
  // Constructor BooleanMatcher
  //------------------------------------------------------------------------------

  public BooleanMatcher(Identifier id) {
    super(id);

    if (tc.isEntryEnabled())
      tc.entry(cclass,
               "BooleanMatcher",
                new Object[] {id});    
    if (tc.isEntryEnabled())
      tc.exit(cclass,
              "BooleanMatcher", this);
  }


  //------------------------------------------------------------------------------
  // Method: BooleanMatcher.handlePut
  //------------------------------------------------------------------------------

  void handlePut(SimpleTest test, Conjunction selector, MatchTarget object,
                 InternTable subExpr) throws MatchingException
  {
    if (tc.isEntryEnabled())
	    tc.entry(this,cclass, "handlePut",
               new Object[] {test,selector,object, subExpr}	
	);
	  	
    ContentMatcher next;
    if (test.getKind() == SimpleTest.ID)
      next = trueChild = nextMatcher(selector, trueChild);
    else if (test.getKind() == SimpleTest.NOTID)
      next = falseChild = nextMatcher(selector, falseChild);
    else
      throw new IllegalStateException();
    next.put(selector, object, subExpr);

    if (tc.isEntryEnabled())
	    tc.exit(this,cclass, "handlePut");    
  }


  //------------------------------------------------------------------------------
  // Method: BooleanMatcher.handleGet
  //------------------------------------------------------------------------------

  void handleGet(Object value, 
  		           MatchSpaceKey msg, 
  		           EvalCache cache,
                   Object contextValue,
  		           SearchResults result)
  throws MatchingException, BadMessageFormatMatchingException
  {
    if (tc.isEntryEnabled())
	    tc.entry(this,cclass, "handleGet","value: "+value+ ",msg: "+msg+", context: "+contextValue+", result: "+result);      
	    	
    if (!(value instanceof Boolean)) // was BooleanValue
      return;
    if (value != null && ((Boolean) value).booleanValue()) // was BooleanValue
      if (trueChild != null)
        trueChild.get(null, msg, cache, contextValue, result);
      else;
    else if (falseChild != null)
      falseChild.get(null, msg, cache, contextValue, result);

    if (tc.isEntryEnabled())
	    tc.exit(this,cclass, "handleGet");      
  }


  //------------------------------------------------------------------------------
  // Method: BooleanMatcher.handleRemove
  //------------------------------------------------------------------------------

  void handleRemove(SimpleTest test, Conjunction selector,
                    MatchTarget object, InternTable subExpr, OrdinalPosition parentId) throws MatchingException
  {
    if (tc.isEntryEnabled())
	    tc.entry(this,cclass, "handleRemove","test: "+test+ ",selector: "+selector+", object: "+object);
	    	
    if (test.getKind() == SimpleTest.ID)
      trueChild = trueChild.remove(selector, object, subExpr, ordinalPosition);
    else if (test.getKind() == SimpleTest.NOTID)
      falseChild = falseChild.remove(selector, object, subExpr, ordinalPosition);
    else
      throw new IllegalStateException();

    if (tc.isEntryEnabled())
	    tc.exit(this,cclass, "handleRemove");      
  }


  //------------------------------------------------------------------------------
  // Method: BooleanMatcher.isEmpty
  //------------------------------------------------------------------------------

  boolean isEmpty() {
    return super.isEmpty() && trueChild == null && falseChild == null;
  }
} // BooleanMatcher
