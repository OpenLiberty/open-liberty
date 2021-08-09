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

import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.Literal;
import com.ibm.ws.sib.matchspace.MatchSpace;
import com.ibm.ws.sib.matchspace.MatchingException;
import com.ibm.ws.sib.matchspace.Operator;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.SimpleTest;
import com.ibm.ws.sib.matchspace.utils.FFDC;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;
import com.ibm.ws.sib.matchspace.tools.Evaluator;
import com.ibm.ws.sib.matchspace.tools.MatchParser;
import com.ibm.ws.sib.matchspace.tools.PositionAssigner;
import com.ibm.ws.sib.matchspace.tools.Resolver;
import com.ibm.ws.sib.matchspace.tools.TopicSyntaxChecker;
import com.ibm.ws.sib.matchspace.tools.Transformer;

//------------------------------------------------------------------------------
// Matching Class
//------------------------------------------------------------------------------
/**
 * The Matching class provides the prime interface to the MatchSpace and its ancillary classes. 
 * It is used to create instances of a Matching implementation, which extends the Matching class 
 * and supports the creation of a concrete MatchSpace and supporting classes.
 **/

public abstract class Matching
{

  private static final Class cclass = Matching.class;
  private static Trace tc = TraceUtils.getTrace(Matching.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);
  private static final String MATCHING_CLASS_NAME =
    "com.ibm.ws.sib.matchspace.impl.MatchingImpl";
  private static Matching instance = null;
  protected static Evaluator eval = null;
  protected static Transformer transformer = null;
  private static Exception createException = null;

  static 
  {    
    createFactoryInstance();
  }
  /**
   * Create the Matching instance.
   */
  private static void createFactoryInstance()
  {
    if (tc.isEntryEnabled())
      tc.entry(cclass, "createFactoryInstance");

    try
    {
      Class cls = Class.forName(MATCHING_CLASS_NAME);
      instance = (Matching) cls.newInstance();
    }
    catch (Exception e)
    {
      // No FFDC Code Needed.
      // FFDC driven by wrapper class.
      FFDC.processException(cclass,
        "com.ibm.ws.sib.matchspace.Matching.createFactoryInstance",
        e,
        "1:94:1.18");
//TODO: trace.error  
//      tc.error(cclass, "UNABLE_TO_CREATE_MATCHING_INSTANCE_CWSIH0007E", e);

      createException = e;
    }
    
    if (tc.isEntryEnabled())
      tc.exit(cclass, "createFactoryInstance");
  }

  /**
   * Obtain a reference to the singleton Matching instance
   *
   * @return The singleton instance of the factory class
   */

  public static Matching getInstance() throws Exception
  {
    if (tc.isEntryEnabled())
      tc.entry(cclass, "getInstance");

    if (instance == null)
      throw createException;

    if (tc.isEntryEnabled())
      tc.exit(cclass, "getInstance", "instance=" + instance);
      
    return instance;
  }

  public abstract MatchSpace createMatchSpace(
    Identifier rootId,
    boolean enableCacheing);

  public abstract EvalCache createEvalCache();

  /** Screen a selector tree for eligibility as a simple test.  Right now the tree is 
   * eligible iff numIds == 1.
   **/

  public static boolean isSimple(Selector sel)
  {
    return sel.getNumIds() == 1;
  }

  /** Prime a MatchParser object with a String form selector so that its QueryExpr method
   * will return the corresponding Selector tree.
   *
   * @param parser an existing MatchParser object to be reused, or null if a new one is to
   * be created.
   *
   * @param selector the String-form selector to be parsed
   *
   * @param selectorDomain determines which messaging API we are parsing within, SIBusmessage,
   * JMS or XPath1.0
   *
   * @return a parser, primed with the supplied selector.  This is the same parser object
   * that was supplied as an argument, if one was supplied.
   **/
  public abstract MatchParser primeMatchParser(
    MatchParser reuse,
    String selector,
    int selectorDomain)
  throws MatchingException;

  public static Evaluator getEvaluator() // single instance
  {
    return eval;
  }

  public static Transformer getTransformer() // single instance  
  {
    return transformer;
  }

  /** 
   * The following methods support the creation of various MatchSpace entities.
   *
   **/
  public abstract PositionAssigner createPositionAssigner();

  public abstract Resolver createMinimalResolver();

  public abstract Conjunction createConjunction();

  public abstract Conjunction createConjunction(Selector sel);

  public abstract Conjunction createConjunction(SimpleTest test);

  public abstract Literal createLiteral(Object obj);

  public abstract Operator createOperator(int op, Selector operand);

  public abstract Operator createOperator(int op, Selector op1, Selector op2);
  
  public abstract Operator createExtensionOperator(int op, Selector op1, Selector op2);
  
  public abstract Operator createLikeOperator(
    Selector ar,
    String pattern,
    boolean escaped,
    char escape);

  public abstract Operator createTopicLikeOperator(Selector ar, String pattern);

  public abstract Identifier createIdentifier(String name);
  
  public abstract TopicSyntaxChecker createXPathTopicSyntaxChecker();
  
  public abstract TopicSyntaxChecker createMQSITopicSyntaxChecker();
  
  public abstract Operator createMQSITopicLikeOperator(Selector ar, String pattern);

}
