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

import java.util.List;

import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.impl.Matching;
import com.ibm.ws.sib.matchspace.Operator;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.SimpleTest;
import com.ibm.ws.sib.matchspace.utils.FFDC;
import com.ibm.ws.sib.matchspace.impl.EvalCacheImpl;

/** This class represents one "simple test" (a test involving exactly one Identifer in
 * exactly one position).  Note that "FOO = FOO" (though vacuous) is NOT simple.  It may
 * involve only one identifier, but the identifier appears in more than one position.
 **/

public class SimpleTestImpl implements SimpleTest
{
  private static final Class cclass = SimpleTestImpl.class;
  
  /** The Identifier on which the SimpleTest is to be performed. */
  public Identifier identifier;

  /** The kind of test that this is.  One of ID, NOTID, EQ, STRINGOTH, NULL,
   * NOTNULL, or NUMERIC
   **/

  public int kind;

  /** The value to compare against for EQ tests, null otherwise */

  public Object value;

  /** The lower bound of a NUMERIC test, null for singular LT or LE and for tests that are
   * not NUMERIC
   **/

  public Number lower; // was NumericValue 

  /** Indicates that the lower bound of a NUMERIC test is inclusive (GE rather than GT).
   * Ignored if lower is null.
   **/

  public boolean lowIncl;

  /** The upper bound of a NUMERIC test, null for singular GT or GE and for tests that are
   * not NUMERIC
   **/

  public Number upper; // was NumericValue 

  /** Indicates that the upper bound of a NUMERIC test is inclusive (LE rather than LT).
   * Ignored if upper is null.
   **/

  public boolean upIncl;

  /** Set of tests to evaluate if test is of STRINGOTH type.  Null otherwise */

  public Selector[] tests;
  
  public SimpleTestImpl()
  {
  	
  }
  /** Construct a SimpleTest from a Selector subtree that has been screened to have the
   * right form: DNF with one identifier.
   *
   * @param sel the Selector that is to be turned into a SimpleTest
   *
   * @exception IllegalArgumentException if the tree is not a valid DNF-transformed tree
   * with one Identifier.  Other RuntimeException types may also "leak" due to the same
   * error, or results may be unpredictable.
   **/

  public SimpleTestImpl(Selector sel)
  {
    if (sel instanceof Identifier)
    {
      identifier = (Identifier) sel;
      kind = ID;
      return;
    }
    if (!(sel instanceof Operator))
      throw new IllegalArgumentException();
    Operator oper = (Operator) sel;
    switch (oper.getOp())
    {
      case Selector.LIKE :
      case Selector.TOPIC_LIKE :
        identifier = (Identifier) oper.getOperands()[0];
        kind = STRINGOTH;
        tests = new Selector[] { sel };
        return;
      case Selector.NOT :
        if (oper.getOperands()[0] instanceof Identifier)
        {
          identifier = (Identifier) oper.getOperands()[0];
          kind = NOTID;
          return;
        }
        Operator subOp = (Operator) oper.getOperands()[0];
        identifier = (Identifier) subOp.getOperands()[0];
        if (subOp.getOp() == Selector.LIKE || subOp.getOp() == Selector.TOPIC_LIKE)
        {
          kind = STRINGOTH;
          tests = new Selector[] { sel };
          return;
        }
        if (subOp.getOp() == Selector.ISNULL)
        {
          kind = NOTNULL;
          return;
        }
        throw new IllegalArgumentException();
      case Selector.ISNULL :
        identifier = (Identifier) oper.getOperands()[0];
        kind = NULL;
        return;
      case Selector.EQ :
      case Selector.NE :
      case Selector.LT :
      case Selector.GT :
      case Selector.LE :
      case Selector.GE :
        oper = simpleComparison(oper);
        identifier = (Identifier) oper.getOperands()[0];
        Object value = Matching.getEvaluator().eval(oper.getOperands()[1]);
        if (oper.getOp() == Selector.EQ)
        {
          kind = EQ;
          this.value = value;
        }
        else if (value instanceof String)
        {
          kind = STRINGOTH;
          tests = new Selector[] { oper };
        }
        else
          recordNumericComparison(oper.getOp(), (Number) value); // was NumericValue
        return;
      default :
        // Can only occur if DNF rule was violated or expression was not type-correct
        throw new IllegalArgumentException();
    }
  }

  // Record a numeric comparison (subroutine of constructor)

  private void recordNumericComparison(int op, Number value) // was NumericValue
  {
    kind = NUMERIC;
    switch (op)
    {
      case Selector.LT :
        upper = value;
        return;
      case Selector.GT :
        lower = value;
        return;
      case Selector.LE :
        upper = value;
        upIncl = true;
        return;
      case Selector.GE :
        lower = value;
        lowIncl = true;
        return;
      default :
        throw new IllegalArgumentException();
    }
  }

  // Convert a Selector tree headed by EQ/NE/LT/GT/LE/GE into a simpler tree which has an
  // Identifier as operand[0] and a constant expression as operand[1].  This only works if
  // the tree has exactly one identifier to begin with.

  private static Operator simpleComparison(Operator expr)
  {
    int op = expr.getOp();
    Selector idExpr = expr.getOperands()[0];
    // May be wrong choice: corrected below.
    Selector constantExpr = expr.getOperands()[1];
    if (idExpr.getNumIds() == 0)
    {
      // Guessed wrong.  Swap.
      Selector temp = constantExpr;
      constantExpr = idExpr;
      idExpr = temp;
      op = invertComparison(op);
    }
    while (!(idExpr instanceof Identifier))
    {
      // The identifier is IN idExpr but not at the top.  Since we started with a DNF
      // conjunct with no boolean comparisons, and since no operators have String result
      // type, the top node of idExpr MUST be an arithmetic operator.
      Operator currOp = (Operator) idExpr;
      if (currOp.getOp() == Operator.NEG)
      {
        op = invertComparison(op);
        idExpr = currOp.getOperands()[0];
        constantExpr = new OperatorImpl(Operator.NEG, constantExpr);
      }
      else
      {
        // Arithmetic binary operators
        Selector residue; // Will hold the constant expression child of currOp
        if (currOp.getOperands()[0].getNumIds() == 0)
        {
          // The id is in the right subchild of currOp, so swap
          switch (currOp.getOp())
          {
            case Operator.PLUS :
            case Operator.TIMES :
              break;
            case Operator.MINUS :
              op = invertComparison(op);
              constantExpr = new OperatorImpl(Operator.NEG, constantExpr);
              break;
            case Operator.DIV :
              op = invertComparison(op);
              constantExpr =
                new OperatorImpl(
                  Operator.DIV,
                  new LiteralImpl(new Integer(1)),
                  constantExpr);
              break;
            default :
              throw new IllegalArgumentException();
          }
          residue = currOp.getOperands()[0];
          idExpr = currOp.getOperands()[1];
        }
        else
        {
          // The id is in the left subchild, no swap is necessary
          residue = currOp.getOperands()[1];
          idExpr = currOp.getOperands()[0];
        }
        // Now that idExpr has been simplified and the comparison inverted if necessary,
        // combine residue with constantExpr to restore correctness of the result.
        int newOp;
        switch (currOp.getOp())
        {
          case Operator.PLUS :
            newOp = Operator.MINUS;
            break;
          case Operator.TIMES :
            newOp = Operator.DIV;
            break;
          case Operator.MINUS :
            newOp = Operator.PLUS;
            break;
          case Operator.DIV :
            newOp = Operator.TIMES;
            break;
          default :
            throw new IllegalArgumentException();
        }
        constantExpr = new OperatorImpl(newOp, constantExpr, residue);
      }
    } // End while loop: idExpr is an identifier
    return new OperatorImpl(op, idExpr, constantExpr);
  }

  // Invert one of the comparison operators to reflect a negation of both sides or taking
  // the reciprocal of both sides.

  private static int invertComparison(int op)
  {
    switch (op)
    {
      case Selector.EQ :
      case Selector.NE :
        return op;
      case Selector.LT :
        return Selector.GT;
      case Selector.GT :
        return Selector.LT;
      case Selector.LE :
        return Selector.GE;
      case Selector.GE :
        return Selector.LE;
      default :
        throw new IllegalArgumentException();
    }
  }

  /** If this SimpleTest is an equality test return its
   * comparand.
   * @return the comparand of an equality test, or null if this isn't an equality test.
   **/

  public Object getValue()
  {
    return value;
  }

  /** Given a SimpleTest on the same identifier as this one, combine its information with
   * the information in this one.
   *
   * @param other the other SimpleTest that is to be combined with this one
   *
   * @return true if the two tests are compatible with each other (their conjunction is
   * capable of being true).  Return false if the two tests contradict each other (nothing
   * is changed in the latter case)
   **/

  public boolean combine(SimpleTest other)
  {
    switch (kind)
    {
      case ID :
      case NOTID :
      case NULL :
        return kind == other.getKind();
      case NOTNULL :
        absorb(other);
        return true;
      case EQ :
        if (other.getKind() == EQ)
          return value.equals(other.getValue());
        else
          if (other.getKind() == STRINGOTH)
            return selects((String) value, other.getTests());
          else
            return other.getKind() == NOTNULL;
      case STRINGOTH :
        if (other.getKind() == STRINGOTH)
        {
          tests = append(tests, other.getTests());
          return true;
        }
        else
          if (other.getKind() == EQ)
            if (selects((String) other.getValue(), tests))
            {
              absorb(other);
              return true;
            }
            else
              return false;
          else
            return other.getKind() == NOTNULL;
      case NUMERIC :
        if (other.getKind() == NUMERIC)
          return combineNumeric(other);
        else
          return other.getKind() == NOTNULL;
    }
    throw new IllegalArgumentException();
  }

  // Make this SimpleTest the same as another on the same identifier

  protected void absorb(SimpleTest other)
  {
    identifier = other.getIdentifier();
    kind = other.getKind();
    value = other.getValue();
    tests = other.getTests();
    upper = other.getUpper();
    lower = other.getLower();
    upIncl = other.isUpIncl();
    lowIncl = other.isLowIncl();
  }

  // Append two arrays of Selectors

  private Selector[] append(Selector[] one, Selector[] two)
  {
    Selector[] ans = new Selector[one.length + two.length];
    System.arraycopy(one, 0, ans, 0, one.length);
    System.arraycopy(two, 0, ans, one.length, two.length);
    return ans;
  }

  // Determine if an array of tests are all true when the identifier is set to a
  // particular String value.

  private boolean selects(String value, Selector[] tests)
  {
    MatchSpaceKey ec = new OneIdentifierContext(value);
    EvalCache cache = new EvalCacheImpl();
    try
    {
      for (int i = 0; i < tests.length; i++)
      {
        Boolean theEval = (Boolean) Matching.getEvaluator().eval(tests[i], ec, cache, null, false);
        if (theEval == null || !(theEval).booleanValue()) // was if (!((BooleanValue) Matching.getEvaluator().eval(tests[i], ec, cache, false)).booleanValue())
          return false;
      }
      return true;
    }
    catch (BadMessageFormatMatchingException e)
    {
      // No FFDC Code Needed.
      // FFDC driven by wrapper class.
      FFDC.processException(this, cclass,
          "com.ibm.ws.sib.matchspace.selector.impl.SimpleTest.selects",
          e,
          "1:441:1.23");      
      // This truly doesn't happen (see OneIdentifierContext)
      throw new IllegalStateException(); // );
    }

  }

  // Combine information from another NUMERIC SimpleTest into this NUMERIC SimpleTest.
  // Return true if information is consistent, false if contradictory.

  private boolean combineNumeric(SimpleTest other)
  {
    Number newUpper, newLower; // was NumericValue
    boolean newLowIncl, newUpIncl;
    if (upper == null)
    {
      newUpper = other.getUpper();
      newUpIncl = other.isUpIncl();
    }
    else
      if (other.getUpper() == null)
      {
        newUpper = upper;
        newUpIncl = upIncl;
      }
      else
      {
        int upComp = EvaluatorImpl.compare(upper, other.getUpper());
        if (upComp < 0)
        {
          newUpper = upper;
          newUpIncl = upIncl;
        }
        else
          if (upComp == 0)
          {
            newUpper = upper;
            newUpIncl = upIncl & other.isUpIncl();
          }
          else
          {
            newUpper = other.getUpper();
            newUpIncl = other.isUpIncl();
          }
      }
    if (lower == null)
    {
      newLower = other.getLower();
      newLowIncl = other.isLowIncl();
    }
    else
      if (other.getLower() == null)
      {
        newLower = lower;
        newLowIncl = lowIncl;
      }
      else
      {
        int lowComp = EvaluatorImpl.compare(lower,other.getLower());
        if (lowComp > 0)
        {
          newLower = lower;
          newLowIncl = lowIncl;
        }
        else
          if (lowComp == 0)
          {
            newLower = lower;
            newLowIncl = lowIncl & other.isLowIncl();
          }
          else
          {
            newLower = other.getLower();
            newLowIncl = other.isLowIncl();
          }
      }
    if (newLower != null && newUpper != null)
    {
      int vTest = EvaluatorImpl.compare(newLower, newUpper);
      if (vTest > 0)
        return false;
      if (vTest == 0) {
        if (!newLowIncl || !newUpIncl)
          return false;
        // This is now an equality test!
        kind = EQ;
        value = newUpper;
        upper = lower = null;
        upIncl = lowIncl = false;
        return true;
      }
    }

    // newUpper > newLower so this is still a valid range test
    upper = newUpper;
    lower = newLower;
    upIncl = newUpIncl;
    lowIncl = newLowIncl;
    return true;
  }

  // Degenerate MatchSpaceKey for use by the selects method

  private static final class OneIdentifierContext implements MatchSpaceKey
  {
    String value;
    OneIdentifierContext(String value)
    {
      this.value = value;
    }
    public Object getIdentifierValue(Identifier id, boolean ignoreType, Object contextValue, boolean returnList)
    {
      return value;
    }
    public Object getRootContext() {
      return null;
    }
    public Object getIdentifierValue(Identifier id, boolean ignoreType) throws BadMessageFormatMatchingException {
      return value;
    }
  }

  // Display self

  public String toString()
  {
    return toSelector().toString();
  }

  // Turn SimpleTest back into a Selector tree for evaluation (presumably because we've
  // decided not to combine it with other tests in a Matcher tree).

  public Selector toSelector()
  {
    switch (kind)
    {
      case ID :
        return identifier;
      case NOTID :
        return new OperatorImpl(Operator.NOT, identifier);
      case NULL :
        return new OperatorImpl(Operator.ISNULL, identifier);
      case NOTNULL :
        return new OperatorImpl(
          Operator.NOT,
          new OperatorImpl(Operator.ISNULL, identifier));
      case EQ :
        return new OperatorImpl(Operator.EQ, identifier, new LiteralImpl(value));
      case STRINGOTH :
        Selector ans = null;
        for (int i = 0; i < tests.length; i++)
        {
          if (ans == null)
            ans = tests[i];
          else
            ans = new OperatorImpl(Operator.AND, ans, tests[i]);
        }
        return ans;
      case NUMERIC :
        if (upper == null)
          if (lowIncl)
            return new OperatorImpl(Operator.GE, identifier, new LiteralImpl(lower));
          else
            return new OperatorImpl(Operator.GT, identifier, new LiteralImpl(lower));
        if (lower == null)
          if (upIncl)
            return new OperatorImpl(Operator.LE, identifier, new LiteralImpl(upper));
          else
            return new OperatorImpl(Operator.LT, identifier, new LiteralImpl(upper));
        return new OperatorImpl(
          Operator.AND,
          new OperatorImpl(
            lowIncl ? Operator.GE : Operator.GT,
            identifier,
            new LiteralImpl(lower)),
          new OperatorImpl(
            upIncl ? Operator.LE : Operator.LT,
            identifier,
            new LiteralImpl(upper)));
    }
    throw new IllegalArgumentException();
  }
  
  /** Shed some or all of the subtests in a STRINGOTH test, leaving either nothing
   * or a single LIKE test that can be optimized by MatchSpace.  This method
   * is called only by ConjunctionImpl and is not part of the public interface.
   * @param residual a List to which to append tests that are being shed (this
   *   gets turned into the Conjunction's residual in the end)
   * @return true if the SimpleTest should be retained or false if all of its tests
   *   have been moved to the residual.  For convenient, this method returns true when
   *   called on a test that isn't a STRINGOTH test.
   */
  boolean shedSubtests(List residual) {
    if (kind != STRINGOTH)
      return true;
    Operator keep = null;
    for (int i = 0; i < tests.length; i++) {
      Operator test = (Operator) tests[i];
      if (keep == null && test instanceof LikeOperatorImpl)
        keep = test;
      else
        residual.add(test);
    }
    if (keep == null)
      return false;
    tests = new Selector[] { keep };
    return true;
  }
  
  /**
   * Returns the kind.
   * @return int
   */
  public int getKind() 
  {
	return kind;
  }  

  /**
   * Returns the lower.
   * @return NumericValue
   */
  public Number getLower() // was NumericValue 
  {
	return lower;
  }

  /**
   * Returns the lowIncl.
   * @return boolean
   */
  public boolean isLowIncl() 
  {
	return lowIncl;
  }

  /**
   * Returns the tests.
   * @return Selector[]
   */
  public Selector[] getTests() 
  {
	return tests;
  }

  /**
   * Returns the upIncl.
   * @return boolean
   */
  public boolean isUpIncl() 
  {
	return upIncl;
  }

  /**
   * Returns the upper.
   * @return NumericValue
   */
  public Number getUpper() // was NumericValue
  {
	return upper;
  }

  /**
   * Returns the identifier.
   * @return Identifier
   */
  public Identifier getIdentifier() 
  {
	return identifier;
  }
}
