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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.Literal;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.Operator;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.tools.Evaluator;
import com.ibm.ws.sib.matchspace.utils.FFDC;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;

/** The Evaluator is used to evaluate Selector trees, returning a value */

public class EvaluatorImpl implements Evaluator
{
  private static Trace tc = TraceUtils.getTrace(EvaluatorImpl.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);  
  private static final Class cclass = EvaluatorImpl.class;
  
  public EvaluatorImpl()
  {
  } // static only

  /** Evaluates a selector tree
   *
   * @param sel the selector tree to evaluate
   *
   * @param bind the MatchSpaceKey to use in evaluating identifiers and caching partial
   * results
   *
   * @param permissive if true, evaluation should observe "permissive" mode in which there
   * are implicit casts between strings and numbers and between strings and booleans
   * according to the rules for numeric and boolean literals.  If false, evaluation
   * follows the normal JMS rules (numerics are promoted but there are otherwise no
   * implicit casts).
   *
   * @return the result, which will be a String, a BooleanValue, a NumericValue, or null.
   * Null is used for "missing" Numeric or String values.  BooleanValue.NULL is used for
   * missing Boolean values.
   *
   * @exception BadMessageFormatMatchingException when the method is unable to determine a
   * value because the message (or other object) from which the value must be extracted is
   * corrupted or ill-formed.
   **/

  public Object eval(
    Selector sel,
    MatchSpaceKey msg,
    EvalCache cache,
    Object contextValue,
    boolean permissive)
    throws BadMessageFormatMatchingException
  {

    // This method should never be applied to a selector that has marked itself invalid
    // during parsing.
    if (sel.getType() == Selector.INVALID)
      throw new IllegalArgumentException(); // sel.toString());

    Object ans;
/** TEMPORARY DISABLE CACHING in THE EXTENDED CASE **/
    if (sel.getUniqueId() != 0 && !sel.isExtended())
    {
      ans = cache.getExprValue(sel.getUniqueId());
      if (ans != null)
        return ans;
    }

    ans = evalInternal(sel, msg, cache, contextValue, permissive);
    if (sel.getUniqueId() != 0)
      cache.saveExprValue(sel.getUniqueId(), ans);
    return ans;
  }

  /** Evaluates a selector tree without resolving identifiers (usually applied only to
   * Selector subtrees with numIds == 0).
   *
   * @param sel the selector tree to evaluate
   *
   * @return the result, which will be a String, a BooleanValue, a NumericValue, or null.
   * Null is used for "missing" Numeric or String values.  BooleanValue.NULL is used for
   * missing Boolean values.
   **/

  public Object eval(Selector sel)
  {
    try
    {
      return eval(sel, MatchSpaceKey.DUMMY, EvalCache.DUMMY, null, false);
    }
    catch (BadMessageFormatMatchingException e)
    {
      // No FFDC Code Needed.
      // FFDC driven by wrapper class.
      FFDC.processException(this,
          cclass,
          "com.ibm.ws.sib.matchspace.selector.impl.Evaluator.eval",
          e,
          "1:145:1.28");
//TODO: Pass "sel" in FFDC or merely trace?      
      // This truly does not happen (see MatchSpaceKey.DUMMY)
      throw new IllegalStateException(); // );
    }
  }

  // Working subroutine of eval

  private Object evalInternal(
    Selector sel,
    MatchSpaceKey bind,
    EvalCache cache,
    Object contextValue,
    boolean permissive)
    throws BadMessageFormatMatchingException
  {
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, "evalInternal", new Object[] { sel, bind, cache, contextValue, new Boolean(permissive) });
    
    Object theReturn = null;
    // If sel is a literal, its value is simply the literal's value
    if (sel instanceof Literal)
    {
      if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
        tc.debug(cclass, "Selector is a literal");         
      theReturn = ((Literal) sel).getValue();
      if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(cclass, "evalInternal", theReturn);  
      return theReturn;
    }

    // If sel is an Identifier, we let the MatchSpaceKey evaluate it.  The MatchSpaceKey is
    // given explicit type information unless the evaluation mode is permissive, in which
    // case the value is retrieved regardless and the permissive casts are applied.

    if (sel instanceof Identifier)
    {
      if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
        tc.debug(cclass, "Selector is an Identifier");        
      Identifier id = (Identifier) sel;
      if (permissive && id.getType() != Selector.UNKNOWN)
      {
        // Result may require permissive mode casts.  First, get the value, whatever it
        // is.
        boolean returnList = true;
        if(id.getType() == Selector.BOOLEAN)
          returnList = false;
        Object idVal = 
          bind.getIdentifierValue(id,
                                  true,
                                  contextValue, 
                                  returnList); // true means return a nodelist
        if (idVal == null)
        {
          if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
            tc.exit(cclass, "evalInternal", null);            
          return null;
        }
        // Perform permissive casts
        switch (id.getType())
        {
          case Selector.NUMERIC :
            if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
              tc.debug(cclass, "Selector type is numeric");               
            if (idVal instanceof Number) // was NumericValue
            {
              if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
                tc.exit(cclass, "evalInternal", idVal);                
              return idVal;
            }
            else
            {
              if(idVal instanceof ArrayList)
              {
                theReturn = castToNumberList((ArrayList)idVal);
              }
              else
                theReturn = castToNumber(idVal);
              if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
                tc.exit(cclass, "evalInternal", theReturn);
              return theReturn;
            }
          case Selector.STRING :
            if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
              tc.debug(cclass, "Selector type is string");              
            if (idVal instanceof String)
            {
              if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
                tc.exit(cclass, "evalInternal", idVal);                
              return idVal;
            }
            else
            {
              if(idVal instanceof ArrayList)
              {
                theReturn = castToStringList((ArrayList)idVal);
              }
              else
                theReturn = idVal.toString();
              if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
                tc.exit(cclass, "evalInternal", theReturn);
              return theReturn;
            }            
          case Selector.BOOLEAN :
            if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
              tc.debug(cclass, "Selector type is boolean");              
            if (idVal instanceof Boolean) // was BooleanValue
            {
              if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
                tc.exit(cclass, "evalInternal", idVal);                
              return idVal;
            }
            else
            {
              theReturn = castToBoolean(idVal);
              if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
                tc.exit(cclass, "evalInternal", theReturn);            
              return theReturn;              
            }
          default :
            if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
              tc.debug(cclass, "Selector type is default");              
            // Normally, permissive mode evaluation would not apply to TOPIC and OBJECT
            // Identifiers.  But, in any case, there are no valid casts to those types.
            if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "evalInternal", null);              
            return null;
        }
      }
      else
      {
        // not permissive, or the type is UNKNOWN so no permissive mode casts apply
        boolean returnList = true;
        if(id.getType() == Selector.BOOLEAN)
          returnList = false;
        theReturn = 
          bind.getIdentifierValue(id,
                                  false,
                                  contextValue, 
                                  returnList); // true means return a nodelist
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "evalInternal", theReturn);         
        return theReturn;
      }
    }

    // Otherwise, it's an Operator
    Operator op = (Operator) sel;
    if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
      tc.debug(cclass, "Selector is an operator");  
    // Evaluate the operands to obtain the values needed to evaluate the operator
    Object val0 = eval(op.getOperands()[0], bind, cache, contextValue, permissive); // was false
    Object val1 =
      (op.getOperands().length == 1)
        ? null
        : eval(op.getOperands()[1], bind, cache, contextValue, permissive); // was false

    // Evaluate according to the operator
    switch (op.getOp())
    {
      case Selector.NOT :
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Selector type is NOT");         
        if (val0 instanceof Boolean) 
        {
          theReturn = not((Boolean) val0);
          if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
            tc.exit(cclass, "evalInternal", theReturn);         
          return theReturn;          
        }
        else
          if (permissive && val0 != null)
          {
            theReturn = not(castToBoolean(val0));
            if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "evalInternal", theReturn);         
            return theReturn;            
          }
          else
          {
            if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "evalInternal", null);              
            return null; 
          }
      case Selector.NEG :
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Selector type is NEG");         
        if (!(val0 instanceof Number)) // was NumericValue
          if (permissive && val0 != null)
          {
            val0 = castToNumber(val0);
            if (val0 == null)
            {
              if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
                tc.exit(cclass, "evalInternal", null);               
              return null;
            }
          }
          else
          {
            if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "evalInternal", null);             
            return null;
          }
        // val0 is now definitely a Number
        return neg((Number)val0);
      case Selector.ISNULL :
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Selector type ISNULL");
        theReturn = Boolean.valueOf(val0 == null);
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "evalInternal", theReturn);         
        return theReturn;         
      case Selector.LIKE :
      case Selector.TOPIC_LIKE:
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Selector type is LIKE");        
        if (!(val0 instanceof String))
          if (permissive && val0 != null)
            val0 = val0.toString();
          else
          {
            if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "evalInternal", null);              
            return null;
          }
        // val0 is a String
        theReturn = Boolean.valueOf(((LikeOperatorImpl) op)
            .getInternalPattern().match((String) val0));
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "evalInternal", theReturn);           
        return theReturn;
      case Selector.NE :
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Selector type is NE");          
        if(val0 instanceof ArrayList)
          theReturn = compareList((ArrayList)val0, val1, false, permissive, false);
        else
          theReturn = not(compare(val0, val1, false, permissive));
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "evalInternal", theReturn);           
        return theReturn;        
      case Selector.EQ :
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Selector type is EQ");          
        if(val0 instanceof ArrayList)
          theReturn = compareList((ArrayList)val0, val1, false, permissive, true);
        else        
          theReturn = compare(val0, val1, false, permissive);
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "evalInternal", theReturn); 
        return theReturn;
      case Selector.GT :
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Selector type is GT");          
        if(val0 instanceof ArrayList)
          theReturn = compareList((ArrayList)val0, val1, false, permissive, true);
        else              
          theReturn = compare(val1, val0, true, permissive);
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "evalInternal", theReturn); 
        return theReturn;        
      case Selector.LT :
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Selector type is LT");          
        if(val0 instanceof ArrayList)
          theReturn = compareList((ArrayList)val0, val1, false, permissive, true);
        else              
          theReturn = compare(val0, val1, true, permissive);
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "evalInternal", theReturn); 
        return theReturn;         
      case Selector.GE :
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Selector type is GE");          
        if(val0 instanceof ArrayList)
          theReturn = compareList((ArrayList)val0, val1, false, permissive, false);
        else              
          theReturn = not(compare(val0, val1, true, permissive));
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "evalInternal", theReturn); 
        return theReturn;          
      case Selector.LE :
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Selector type is LE");          
        if(val0 instanceof ArrayList)
          theReturn = compareList((ArrayList)val0, val1, false, permissive, false);
        else              
          theReturn = not(compare(val1, val0, true, permissive));
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "evalInternal", theReturn); 
        return theReturn;          
      case Selector.PLUS :
      case Selector.MINUS :
      case Selector.TIMES :
      case Selector.DIV :
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Selector type is ARITHMETIC");          
        theReturn = promoteAndEvaluate(op.getOp(), val0, val1, permissive);
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "evalInternal", theReturn);         
        return theReturn;
      case Selector.AND :
      case Selector.OR :
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Selector type is ANDOR");          
        if (!(val0 instanceof Boolean)) // was BooleanValue
          if (permissive && val0 != null)
            val0 = castToBoolean(val0);
        if (!(val1 instanceof Boolean)) // was BooleanValue
          if (permissive && val1 != null)
            val1 = castToBoolean(val1);
        if (op.getOp() == Operator.AND)
          theReturn = and((Boolean) val0, (Boolean) val1);
        else
          theReturn = or((Boolean) val0, (Boolean) val1);
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "evalInternal", theReturn);         
        return theReturn;        
      default :
        // Should not occur
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Selector type is DEFAULT");          
        throw new IllegalStateException();
    }
  }

  // Cast an Object which is not null and not an instance of NumericValue to NumericValue,
  // if possible under the permissive rules, null otherwise.  In fact, only a String can
  // be so cast.

  public static Number castToNumber(Object val) // was NumericValue
  {
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, "castToNumber", val);
    
    if (!(val instanceof String))
    {
      if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(cclass, "castToNumber", null);          
      return null;
    }
    
    String stringVal = (String) val;
    if(stringVal.length() == 0)
    {
      if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(cclass, "castToNumber", null);          
      return null;
    }
    // If the value ends in [ l | L | f | F | d | D ] it must be intended to be a
    // Long/Float/Double and we convert it accordingly.  Otherwise, we first try to
    // convert to Integer/Long, then to Double.  If all fail, we return null.
    try
    {
      switch (stringVal.charAt(stringVal.length() - 1))
      {
        case 'l' :
        case 'L' :
          stringVal = stringVal.substring(0, stringVal.length() - 1);
          Long theLong = new Long(stringVal);
          if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "castToNumber", theLong);          
          return theLong; 
        case 'f' :
        case 'F' :
          Float theFloat = new Float(stringVal);
          if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "castToNumber", theFloat);            
          return theFloat; 
        case 'd' :
        case 'D' :
          Double theDouble = new Double(stringVal);
          if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "castToNumber", theDouble);            
          return theDouble; 
        default :
          try
          {
            theLong = new Long(stringVal);
            if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
                tc.exit(cclass, "castToNumber", theLong);               
            return theLong; 
          }
          catch (NumberFormatException e)
          {
            // No FFDC code needed
            theDouble = new Double(stringVal);
            if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
                tc.exit(cclass, "castToNumber", theDouble);            
            return theDouble;             
          }
      }
    }
    catch (NumberFormatException e)
    {
      // No FFDC code needed
      if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(cclass, "castToNumber", null);          
      return null;
    }
  }

  /**
   * Cast an Object which is not null and not an instance of BooleanValue to BooleanValue,
   * if possible under the permissive rules, BooleanValue.NULL otherwise.  In fact, only a
   * String can be so cast.
   * @param val
   * @return
   */
  private static Boolean castToBoolean(Object val) //was BooleanValue
  {
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, "castToBoolean", val);
    Boolean theReturn = null;
    if (val instanceof String)
    {
      String stringVal = (String) val;
      if (stringVal.equalsIgnoreCase("true"))
        theReturn = Boolean.TRUE; // was BooleanValue.TRUE
      else
        if (stringVal.equalsIgnoreCase("false"))
          theReturn = Boolean.FALSE; // was BooleanValue.FALSE
    }
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(cclass, "castToBoolean", theReturn);    
    return theReturn;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.matchspace.tools.Evaluator#castToNumberList(java.util.ArrayList)
   */
  public ArrayList castToNumberList(ArrayList childNodeList) // was NumericValue
  {
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, "castToNumberList", childNodeList);
 
    ArrayList numberList = new ArrayList();
    
    if(childNodeList != null)
    {
      Iterator iterNode = childNodeList.iterator();
      
      while(iterNode.hasNext())
      {
        String strValue = getStringFromNode(iterNode.next());
        
        // Cast the string to a Number
        Number num = EvaluatorImpl.castToNumber(strValue);
        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Add number to list: ", num);  
        numberList.add(num);
      }              
    }
    
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(cclass, "castToNumberList", numberList);    
    return numberList;
  }  

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.matchspace.tools.Evaluator#castToStringList(java.util.ArrayList)
   */
  public ArrayList castToStringList(ArrayList childNodeList)
  {
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, "castToStringList", childNodeList);

    ArrayList stringList = new ArrayList();
    
    if(childNodeList != null)
    {
      Iterator iterNode = childNodeList.iterator();
      
      while(iterNode.hasNext())
      {
        String strValue = getStringFromNode(iterNode.next());

        if (tc.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(cclass, "Add string to list: ", strValue);  
        stringList.add(strValue);
      }              
    }
    
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(cclass, "castToStringList", stringList);    
    return stringList;
  }    
  
  /**
   * Get the String value of a node.
   * 
   * @param inNode
   * @return
   */
  protected String getStringFromNode(Object inNode)
  {
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, "getStringFromNode", inNode);
    
    String strValue = null;

    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(cclass, "getStringFromNode", strValue);    
    return strValue;
  }    

  public String getNodeText(Object inNode)
  {
    return null;
  }      

  /**
   * Get a DOM document root from a message.
   * 
   * @param childNodeList
   * @return
 * @throws BadMessageFormatMatchingException 
   */
  public Object getDocumentRoot(MatchSpaceKey msg) 
    throws BadMessageFormatMatchingException
  {
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, "getDocumentRoot", msg);

    Object docRoot = null;
    
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(cclass, "getDocumentRoot", docRoot);    
    return docRoot;
  }      
  
  /**
   * An implementation of this method is provided in the derived XPath class. In the
   * parent class it merely returns null.
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

      Boolean finalReturn = null;

      if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(cclass, "compareList", finalReturn);       
      return finalReturn;
    }


  /**
   * Evaluate a comparison.  Arguments are the two values to compare, a flag saying
   * whether the test is equality (false) or less-than (true), and a flag saying whether
   * conversion rules are strict (false) or permissive (true).
   *   
   * @param val0
   * @param val1
   * @param lessThan
   * @param permissive
   * @return
   */
  protected static Boolean compare( // was BooleanValue
    Object val0,
    Object val1,
    boolean lessThan,
    boolean permissive)
  {
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, 
               "compare", 
               new Object[] { val0, val1, new Boolean(lessThan), new Boolean(permissive) });
    
    Boolean theReturn = null;
    // Encode the types of the two arguments into sixteen cases based on the classification
    // Number/Boolean/String/Serializable.  All other types (and nulls) result in the NULL
    // answer.
    int ans0 =
      (val0 instanceof Number) // was NumericValue
        ? 0
        : (val0 instanceof Boolean) // was BooleanValue
        ? 1
        : (val0 instanceof String)
        ? 2
        : (val0 instanceof Serializable) 
        ? 3
        : 4;
    int ans1 =
      (val1 instanceof Number) // was NumericValue
        ? 0
        : (val1 instanceof Boolean) // was BooleanValue
        ? 1
        : (val1 instanceof String)
        ? 2
        : (val1 instanceof Serializable) 
        ? 3
        : 4;
    if (ans0 > 3 || ans1 > 3)
    {
      if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(cclass, "compare", null);       
      return null;
    }
    int category = ans0 * 4 + ans1;
    // Categories:        val1
    // val0     Number Boolean String Object
    // Number      0      1       2      3
    // Boolean     4      5       6      7
    // String      8      9      10     11
    // Object     12     13      14     15
    // For the permissive mode (only) do the permissive mode casts according to the
    // category.
    if (permissive)
      switch (category)
      {
        case 0 :
        case 5 :
        case 10 :
        case 15 :
          break; // equal types: no casts needed
        case 2 : // Number/String: attempt to cast val1 to number
          val1 = castToNumber(val1);
          if (val1 == null)
          {
            if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "compare", null); 
            return null;
          }
          category = 0;
          break;
        case 6 : // Boolean/String: attempt to cast val1 to Boolean
          val1 = castToBoolean(val1);
          if (val1 == null)
          {
            if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "compare", null); 
            return null;
          }
          category = 5;
          break;
        case 8 : // String/Number: attempt to cast val0 to number
          val0 = castToNumber(val0);
          if (val0 == null)
          {
            if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "compare", null); 
            return null;
          }
          category = 0;
          break;
        case 9 : // String/Boolean: attempt to cast val0 to Boolean
          val0 = castToBoolean(val0);
          if (val0 == null)
          {
            if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "compare", null); 
            return null;
          }
          category = 4;
          break;
        default : // Other cases have no legal casts, even in permissive mode
          if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
            tc.exit(cclass, "compare", null); 
          return null;
      }
    // Now do the actual evalution
    switch (category)
    {
      case 0 : // both types are Number.  Do the arithimetic in the appropriate precision
        // depending on the Java binary promotion rules.
        int comp = compare((Number) val0, (Number) val1);
        if (lessThan)
          theReturn = Boolean.valueOf(comp < 0);
        else
          theReturn = Boolean.valueOf(comp == 0);
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "compare", theReturn);           
        return theReturn; 
      case 5 :
      case 10:
        if (lessThan)
          if(permissive)
          {
            val1 = castToNumber(val1);
            if (val1 == null)
            {
              if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
                tc.exit(cclass, "compare", null); 
              return null;
            }
            val0 = castToNumber(val0);
            if (val0 == null)
            {
              if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
                tc.exit(cclass, "compare", null); 
              return null;
            }
            int comp2 = compare((Number) val0, (Number) val1); // was ((NumericValue) val0).compareTo(val1)
            theReturn = Boolean.valueOf(comp2 < 0); // was BooleanValue
            if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "compare", theReturn);           
            return theReturn;             
          }
          else
          {
            if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
              tc.exit(cclass, "compare", null); 
            return null;
          }
      case 15:
        // Both types are comparable and non-Null, but only equality is defined
        if (lessThan)
        {
          if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
            tc.exit(cclass, "compare", null); 
          return null;
        }
        else
        {
          theReturn = Boolean.valueOf(val0.equals(val1));
          if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
            tc.exit(cclass, "compare", theReturn);           
          return theReturn; 
        }
      default : // All other cases are disallowed
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "compare", null); 
        return null;
    }
  }

  // Evaluate a binary numeric operator with numeric result

  private static Object promoteAndEvaluate(
    int op,
    Object val0,
    Object val1,
    boolean permissive)
  {
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, 
               "promoteAndEvaluate", 
               new Object[] { new Integer(op), val0, val1, new Boolean(permissive) });
    Object theReturn = null;
    if(val0 instanceof ArrayList)
      theReturn = promoteAndEvaluateList(op, val0, val1, permissive);
    else
      theReturn = promoteAndEvaluateScalar(op, val0, val1, permissive);
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(cclass, "promoteAndEvaluate", theReturn);           
    return theReturn;     
  }

  private static Object promoteAndEvaluateScalar(
      int op,
      Object val0,
      Object val1,
      boolean permissive)
    {
      if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.entry(cclass, 
                 "promoteAndEvaluateScalar", 
                 new Object[] { new Integer(op), val0, val1, new Boolean(permissive) });
      
      Object theReturn = null;
      // The arguments must be numbers
      if (!(val0 instanceof Number)) // was NumericValue
        if (permissive && val0 != null)
          val0 = castToNumber(val0);
        else
        {
          if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
            tc.exit(cclass, "promoteAndEvaluateScalar", null); 
          return null;
        }
      if (!(val1 instanceof Number)) // was NumericValue
        if (permissive && val1 != null)
          val1 = castToNumber(val1);
        else
        {
          if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
            tc.exit(cclass, "promoteAndEvaluateScalar", null); 
          return null;
        }
      if (val0 == null || val1 == null)
      {
        // one or more casts failed
        if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(cclass, "promoteAndEvaluateScalar", null); 
        return null;
      }
      // Do the arithmetic in the appropriate precision according to the Java rules for
      // binary numeric promotion
      switch (op)
      {
        case Selector.PLUS :
          theReturn = plus((Number) val0, (Number) val1);
          break;
        case Selector.MINUS :
          theReturn = minus((Number) val0, (Number) val1);
          break;
        case Selector.TIMES :
          theReturn = times((Number) val0, (Number) val1);
          break;
        case Selector.DIV :
          theReturn = div((Number) val0, (Number) val1);
          break;
        default :
          // Shouldn't happn
          throw new IllegalStateException(); // );
      }
      
      if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(cclass, "promoteAndEvaluateScalar", theReturn);           
      return theReturn;           
    }
  
  
  private static Object promoteAndEvaluateList(
      int op,
      Object val0,
      Object val1,
      boolean permissive)
    {
    if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, 
               "promoteAndEvaluateScalar", 
               new Object[] { new Integer(op), val0, val1, new Boolean(permissive) });        
      // For list processing, we work on the first member of each list
      Object numVal0 = ((ArrayList)val0).get(0);
      Object numVal1 = ((ArrayList)val1).get(0);

      Object theReturn = promoteAndEvaluateScalar(op, numVal0, numVal1, permissive);
      
      if (tc.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(cclass, "promoteAndEvaluateScalar", theReturn);         
      return theReturn;
    }  
  
  // Truth tables
  private static final Boolean[][] andTable =
    {
      new Boolean[] { Boolean.TRUE, Boolean.FALSE, null },
      new Boolean[] { Boolean.FALSE, Boolean.FALSE, Boolean.FALSE },
      new Boolean[] { null, Boolean.FALSE, null }
  };

  private static final Boolean[][] orTable =
    {
      new Boolean[] { Boolean.TRUE, Boolean.TRUE, Boolean.TRUE },
      new Boolean[] { Boolean.TRUE, Boolean.FALSE, null },
      new Boolean[] { Boolean.TRUE, null, null }
  }; 
 
  /** 
   * Get Truth Table index
   */  
  public static int ttIndex(Boolean bVal)
  {
  	int idx = 2;
  	
  	if(bVal != null)
  	{
      if (bVal.booleanValue())
  	  {
  	    idx = 0;
}
  	  else if (!bVal.booleanValue())
  	  {
        idx = 1;
  	  }
  	}
  	return idx;
  }  
  
  /** 
   * Perform logical 'and' between BooleanValue instances 
   */
  public static Boolean and(Boolean a, Boolean b) 
  {
    return andTable[ttIndex(a)][ttIndex(b)];
  }

  /** 
   * Perform logical 'or' between BooleanValue instances 
   */
  public static Boolean or(Boolean a,Boolean b) 
  {
  	return orTable[ttIndex(a)][ttIndex(b)];
  }  
  
  /** 
   * Perform logical 'not' on a Boolean instance 
   */
  public static Boolean not(Boolean bVal) 
  {
//    return notTable[lookupValue];
    if(bVal == null)
      return null;
    else if (bVal.equals(Boolean.TRUE))
      return Boolean.FALSE;
    else if (bVal.equals(Boolean.FALSE))
      return Boolean.TRUE;
    else return null;  
    
  }      

  // Note the ordering is not arbitrary: it is given by the Java promotion rules
  private static final int BYTE = -2, SHORT = -1, INT = 0, LONG = 1, FLOAT = 2, DOUBLE = 3;

  /**
   * Implement Number compare
   */
  public static int compare(Number a, Number b) 
  {
    int aType = getType(a);
    int bType = getType(b);
    
    int compType = (aType >= bType) ? aType : bType;
    switch (compType) 
    {
      case INT :
        int li = a.intValue();
        int ri = b.intValue();
        return (li < ri) ? -1 : (li == ri) ? 0 : 1;
      case LONG :
        long ll = a.longValue();
        long rl = b.longValue();
        return (ll < rl) ? -1 : (ll == rl) ? 0 : 1;
      case FLOAT :
        float lf = a.floatValue();
        float rf = b.floatValue();
        return (lf < rf) ? -1 : (lf == rf) ? 0 : 1;
      case DOUBLE :
        double ld = a.doubleValue();
        double rd = b.doubleValue();
        return (ld < rd) ? -1 : (ld == rd) ? 0 : 1;
      default :
        throw new IllegalStateException();
    }
  }
  
  /**
   * Make a NumericValue from a standard wrapper subclass of Number
   */
  public static int getType(Number val) 
  {
    if (val instanceof Integer) 
    {
      return INT;
    } 
    else if (val instanceof Long) 
    {
      return LONG;
    } 
    else if (val instanceof Short) 
    {
      return SHORT;
    } 
    else if (val instanceof Byte) 
    {
      return BYTE;
    } 
    else if (val instanceof Double) 
    {
      return DOUBLE;
    } 
    else if (val instanceof Float) 
    {
      return FLOAT;
    } else
      throw new IllegalArgumentException();
  }  

  /**
   * Negate the value
   */
  private static Number neg(Number n) 
  {
    switch (getType(n)) 
    {
      case INT :
        return new Integer(-n.intValue());
      case LONG :
        return new Long(-n.longValue());
      case FLOAT :
        return new Float(-n.floatValue());
      case DOUBLE :
        return new Double(-n.doubleValue());
      default :
        throw new IllegalStateException();
    }
  }

  /**
   * Add two values
   */
  private static Number plus(Number a, Number b) 
  {
    int aType = getType(a);
    int bType = getType(b);
    
    int compType = (aType >= bType) ? aType : bType;
    switch (compType)   	
    {
      case INT :
        return new Integer(a.intValue() + b.intValue());
      case LONG :
        return new Long(a.longValue() + b.longValue());
      case FLOAT :
        return new Float(a.floatValue() + b.floatValue());
      case DOUBLE :
        return new Double(a.doubleValue() + b.doubleValue());
      default :
        throw new IllegalStateException();
    }
  }

  /**
   * Multiply two values
   */
  private static  Number times(Number a, Number b) 
  {
    int aType = getType(a);
    int bType = getType(b);
    
    int compType = (aType >= bType) ? aType : bType;
    switch (compType)  
    {
      case INT :
        return new Integer(a.intValue() * b.intValue());
      case LONG :
        return new Long(a.longValue() * b.longValue());
      case FLOAT :
        return new Float(a.floatValue() * b.floatValue());
      case DOUBLE :
        return new Double(a.doubleValue() * b.doubleValue());
      default :
        throw new IllegalStateException();
    }
  }

  /**
   * Subtract two values
   */
  private static Number minus(Number a, Number b) 
  {
    int aType = getType(a);
    int bType = getType(b);
    
    int compType = (aType >= bType) ? aType : bType;
    switch (compType)  
    {
      case INT :
        return new Integer(a.intValue() - b.intValue());
      case LONG :
        return new Long(a.longValue() - b.longValue());
      case FLOAT :
        return new Float(a.floatValue() - b.floatValue());
      case DOUBLE :
        return new Double(a.doubleValue() - b.doubleValue());
      default :
        throw new IllegalStateException();
    }
  }

  /**
   * Divide two values
   */
  private static Number div(Number a, Number b) 
  {
    int aType = getType(a);
    int bType = getType(b);
    
    int compType = (aType >= bType) ? aType : bType;
    switch (compType)  
    {
      case INT :
        return new Integer(a.intValue() / b.intValue());
      case LONG :
        return new Long(a.longValue() / b.longValue());
      case FLOAT :
        return new Float(a.floatValue() / b.floatValue());
      case DOUBLE :
        return new Double(a.doubleValue() / b.doubleValue());
      default :
        throw new IllegalStateException();
    }
  }
  
  public static int hash(Object val) 
  {
  	int hash;
  	long basis;
    if(val instanceof Number)
    {
      if (getType((Number)val) > LONG) 
      {
   	    double doubleVal = ((Number)val).doubleValue();
        basis = (long)doubleVal;
        if (doubleVal != (double)basis)
          basis = Double.doubleToLongBits(doubleVal);
      }
      else
      {
        basis = ((Number)val).longValue();
      }
      hash = (int) (basis ^ (basis >> 32));
    }
    else hash = val.hashCode();
      	
    return hash;
  } 
  
  public static boolean equals(Object a, Object b) 
  {
  	if(a instanceof Number)
    {
      if (b instanceof Number)
      {
        return (compare((Number)a,(Number)b) == 0);
      }
      else 
        return false;
    }
    else 
      return (a.equals(b));
  }     
}
