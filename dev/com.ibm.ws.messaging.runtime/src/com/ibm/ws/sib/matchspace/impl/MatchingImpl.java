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

// Import required classes

import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.Literal;
import com.ibm.ws.sib.matchspace.MatchSpace;
import com.ibm.ws.sib.matchspace.impl.Matching;
import com.ibm.ws.sib.matchspace.MatchingException;
import com.ibm.ws.sib.matchspace.Operator;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.SimpleTest;
import com.ibm.ws.sib.matchspace.selector.impl.ConjunctionImpl;
import com.ibm.ws.sib.matchspace.selector.impl.EvaluatorImpl;
import com.ibm.ws.sib.matchspace.selector.impl.ExtensionOperatorImpl;
import com.ibm.ws.sib.matchspace.selector.impl.IdentifierImpl;
import com.ibm.ws.sib.matchspace.selector.impl.LikeOperatorImpl;
import com.ibm.ws.sib.matchspace.selector.impl.LiteralImpl;
import com.ibm.ws.sib.matchspace.selector.impl.MQSITopicSyntaxChecker;
import com.ibm.ws.sib.matchspace.selector.impl.MatchParserImpl;
import com.ibm.ws.sib.matchspace.selector.impl.MinimalResolver;
import com.ibm.ws.sib.matchspace.selector.impl.OperatorImpl;
import com.ibm.ws.sib.matchspace.selector.impl.Pattern;
import com.ibm.ws.sib.matchspace.selector.impl.PositionAssignerImpl;
import com.ibm.ws.sib.matchspace.selector.impl.TopicPattern;
import com.ibm.ws.sib.matchspace.selector.impl.TransformerImpl;
import com.ibm.ws.sib.matchspace.selector.impl.XPathTopicSyntaxChecker;
import com.ibm.ws.sib.matchspace.tools.Evaluator;
import com.ibm.ws.sib.matchspace.tools.MatchParser;
import com.ibm.ws.sib.matchspace.tools.PositionAssigner;
import com.ibm.ws.sib.matchspace.tools.Resolver;
import com.ibm.ws.sib.matchspace.tools.TopicSyntaxChecker;
import com.ibm.ws.sib.matchspace.utils.FFDC;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;

//------------------------------------------------------------------------------
// MatchingImpl Class
//------------------------------------------------------------------------------
/**
 * The MatchingImpl class extends the abstract Matching class and supports the 
 * creation of a concrete MatchSpace and supporting classes.
 **/

public class MatchingImpl extends Matching {
	
  private static final Class cclass = MatchingImpl.class;
  private static Trace tc = TraceUtils.getTrace(MatchingImpl.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);	
	
  public MatchingImpl()
  {
  	eval = null; 
    transformer = new TransformerImpl(); 
    
    Class evaluatorClass = null;
    try 
    {
      evaluatorClass =  Class.forName("com.ibm.ws.sib.matchspace.selector.impl.XPath10EvaluatorImpl");

      eval = (Evaluator)evaluatorClass.newInstance();
        
    } 
    catch (ClassNotFoundException e) 
    {
      // No FFDC Code Needed.
      // This signals that we're running a version of the
      // MSpace that doesn't support XPath processing. We'll trace this         
      if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())      
        tc.debug(cclass, "No XPath10 Evaluator class found", e);
      eval = new EvaluatorImpl(); 
    }
    catch (Exception ex)
    {
      // No FFDC Code Needed.
      if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())      
        tc.debug(cclass, "Error when locating XPath10 Evaluator class", ex);
      eval = new EvaluatorImpl(); 
    }    
  }
 	
  /**
   * Create a concrete instance of a MatchSpace
   */
  public MatchSpace createMatchSpace(Identifier rootId, boolean enableCacheing)
  {
	MatchSpace matchSpace = new MatchSpaceImpl(rootId, enableCacheing);

	return matchSpace; 
  }

  /**
   * Create a concrete instance of an Evaluation cache
   */
  public EvalCache createEvalCache() // throws what
  {
	EvalCache evalCache = new EvalCacheImpl();

	return evalCache; 
  }

  /** Prime a MatchParser object with a String form selector so that its QueryExpr method
   * will return the corresponding Selector tree.
   *
   * @param parser an existing MatchParser object to be reused, or null if a new one is to
   * be created.
   *
   * @param selector the String-form selector to be parsed
   *
   * @param strict true if only the JMS standard syntax is to be accepted, false if the
   * extended syntax for identifiers, set expressions, and lists is to be accepted.
   *
   * @return a parser, primed with the supplied selector.  This is the same parser object
   * that was supplied as an argument, if one was supplied.
   **/

  public MatchParser primeMatchParser(MatchParser parser, 
  		                                String selector, 
  		                                int selectorDomain)
    throws MatchingException
  {
  	MatchParser parserimpl = null;
  	
  	// Drive appropriate parsing dependent on the SelectorDomain
    switch(selectorDomain)
    {
      case 0: // SIMESSAGE
        parserimpl =  MatchParserImpl.prime(parser, selector, false);
        break;
      case 1: // JMS
        parserimpl =  MatchParserImpl.prime(parser, selector, true);
        break;
      case 2: // XPATH10
        // This piece needs to be pluggable. MicroBroker doesn't have this
        // function.
        if(parser == null)
        {
          // Only load the class if we havent done so already
          Class parserClass = null;
          try 
          {
            parserClass =  Class.forName("com.ibm.ws.sib.matchspace.selector.impl.XPath10ParserImpl");

            parserimpl = (MatchParser)parserClass.newInstance();
            
            // Set the matching instance into the parser
            parserimpl.setMatching(this);            
          } 
          catch (ClassNotFoundException e) 
          {
            // No FFDC Code Needed.
            // FFDC driven by wrapper class.
            // This signals that we're running a version of the
            // MSpace that doesn't support XPath processing          
            FFDC.processException(cclass,
              "com.ibm.ws.sib.matchspace.impl.MatchingImpl",
              e,
              "1:191:1.18");
            if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())      
              tc.debug(cclass, "UNABLE_TO_CREATE_MATCHING_INSTANCE_CWSIH0007", e);
            throw new MatchingException(e);
          }
          catch (Exception ex)
          {
            // No FFDC Code Needed.
            // FFDC driven by wrapper class.
            FFDC.processException(cclass,
              "com.ibm.ws.sib.matchspace.impl.MatchingImpl",
              ex,
              "1:203:1.18");
            if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())      
              tc.debug(cclass, "UNABLE_TO_CREATE_MATCHING_INSTANCE_CWSIH0007", ex);
          }
        } // eof paserimpl is null
        else
        {
          parserimpl = parser;
        }
        break;
      default:
        MatchingException mex = new MatchingException();
        // No FFDC Code Needed.
        // FFDC driven by wrapper class.
        // We didn't recognise this selector domain
        FFDC.processException(cclass,
          "com.ibm.ws.sib.matchspace.impl.MatchingImpl",
          mex,
          "1:221:1.18");
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())      
          tc.debug(cclass, "UNABLE_TO_CREATE_MATCHING_INSTANCE_CWSIH0007", mex);
        throw mex;
    }//switch

    return parserimpl;
  }  

  /**
   * Create a concrete instance of a PositionAssigner
   */
  public PositionAssigner createPositionAssigner()
  {
	PositionAssigner pa = new PositionAssignerImpl();

	return pa; 
  }   

  /**
   * Create a concrete instance of a MinimalResolver
   */  
  public Resolver createMinimalResolver()
  {
	Resolver resolver = new MinimalResolver();

	return resolver; 
  }  

  /**
   * Create a concrete instance of a Conjunction
   */    
  public Conjunction createConjunction()
  {
	Conjunction conjunction = new ConjunctionImpl();

	return conjunction; 
  }    
  
  public Conjunction createConjunction(Selector sel)
  {
	Conjunction conjunction = new ConjunctionImpl(sel);

	return conjunction; 
  }      

  public Conjunction createConjunction(SimpleTest test)
  {
	Conjunction conjunction = new ConjunctionImpl(test);

	return conjunction; 
  }        

  /**
   * Create a concrete instance of a Literal
   */      
  public Literal createLiteral(Object obj)
  {
	Literal literal = new LiteralImpl(obj);

	return literal; 
  }          

  /**
   * Create a concrete instance of an Operator
   */    
  public Operator createOperator(int op, Selector operand)
  {
	Operator operator = new OperatorImpl(op, operand);

	return operator; 
  }        
  
  public Operator createOperator(int op, Selector op1, Selector op2)
  {
	Operator operator = new OperatorImpl(op, op1, op2);

	return operator; 
  }       

  public Operator createExtensionOperator(int op, Selector op1, Selector op2)
  {
    
    Operator operator = null;
    if(op1 == null)
    {
      operator = new ExtensionOperatorImpl(op, op2);
    }
    else
    {
      operator = new ExtensionOperatorImpl(op, op1, op2);
    }

    return operator; 
  }        
  
  /**
   * Create a concrete instance of a LikeOperator
   */     
  public Operator createLikeOperator(Selector ar, String pattern, boolean escaped, char escape)
  {
  Object parsed = Pattern.parsePattern(pattern, escaped, escape);
  if (parsed == null)
    return null;
  else if (parsed == Pattern.matchMany)
    return createOperator(Selector.NOT, createOperator(Selector.ISNULL, ar));
  else if (parsed instanceof String)
    return createOperator(Selector.EQ, ar, createLiteral(parsed));
  else
    return new LikeOperatorImpl(Selector.LIKE, ar, (Pattern) parsed, pattern,
      escaped, escape);
  }      
  
  /**
   * Create a concrete instance of an Identifier
   */     
  public Identifier createIdentifier(String name)
  {
	Identifier identifier = new IdentifierImpl(name);

	return identifier; 
  }

  // Implement createTopicLikeOperator
  public Operator createTopicLikeOperator(Selector ar, String pattern)
  {
  Object parsed = TopicPattern.parsePattern(pattern);
  if (parsed == Pattern.matchMany)
    return createOperator(Selector.NOT, createOperator(Selector.ISNULL, ar));
  else if (parsed instanceof String)
    return createOperator(Selector.EQ, ar, createLiteral(parsed));
  else
    return new LikeOperatorImpl(Selector.TOPIC_LIKE, ar, (Pattern) parsed, pattern,
      false, (char) 0);
  }
  
  /**
   * Create a concrete instance of a XPathTopicSyntaxChecker
   */  
  public TopicSyntaxChecker createXPathTopicSyntaxChecker()
  {
	  TopicSyntaxChecker syntaxChecker = new XPathTopicSyntaxChecker();

	return syntaxChecker; 
  }
  
  /**
   * Create a concrete instance of a MQSITopicSyntaxChecker
   */  
  public TopicSyntaxChecker createMQSITopicSyntaxChecker()
  {
	  TopicSyntaxChecker syntaxChecker = new MQSITopicSyntaxChecker();

	return syntaxChecker; 
  }
  
  // Implement createMQSITopicLikeOperator
  public Operator createMQSITopicLikeOperator(Selector ar, String pattern)
  {
  Object parsed = TopicPattern.parseMQSIPattern(pattern);
  if (parsed == Pattern.matchMany)
    return createOperator(Selector.NOT, createOperator(Selector.ISNULL, ar));
  else if (parsed instanceof String)
    return createOperator(Selector.EQ, ar, createLiteral(parsed));
  else
    return new LikeOperatorImpl(Selector.TOPIC_LIKE, ar, (Pattern) parsed, pattern,
      false, (char) 0);
  }
}
