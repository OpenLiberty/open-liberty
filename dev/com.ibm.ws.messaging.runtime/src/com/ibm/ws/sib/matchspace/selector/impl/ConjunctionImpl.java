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
import java.util.List;

import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.SimpleTest;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.Operator;
import com.ibm.ws.sib.matchspace.LikeOperator;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.impl.Matching;

/** This class represents a conjunction of tests after DNF transformation.  The tests are
 * isolated into a set of simple tests and a set of complex tests.  By design, a
 * Conjunction is initialized incrementally.  The constructors create an initial,
 * incompletely initialized Conjunction with zero or one tests.  The <b>and</b> methods
 * add one test at a time.  The <b>organize</b> method computes the final form of the
 * Conjunction for use by MatchSpace.
 **/

public final class ConjunctionImpl implements Conjunction{

  /** Array of simple tests (predicates on a single identifier).  The array will be sorted
   * in ascending order of the identifier.ordinalPosition fields of the tests.  No two
   * tests will have the same ordinalPosition: this requires that the Resolver has
   * assigned ordinalPosition values correctly.  Violations are caught by the
   * <b>organize</b> method
   **/

  public SimpleTest[] simpleTests;


  /** Test to be evaluated after all simple tests have evaluated to true.  If null,
   * there is no residual test.
   **/

  public Selector residual;


  /** Indicator that this Conjunction is always true.  After <b>organize</b> has been
   * called, this is defined as <b>simpleTests.length == 0 && residual == null</b>.  But,
   * the value is also maintained as the Conjunction is being initialized so it can be
   * checked at any time after construction.
   **/

  public boolean alwaysTrue;


  // Temporary holding areas for simpleTests and residual: information is copied into
  // those variables at 'organize' time.

  private List tmpSimpleTests = new ArrayList();
  private List tmpResidual = new ArrayList();


  /** Create an initial Conjunction, to be added to as transformation proceeds
   *
   * @param simple the single SimpleTest with which to prime the Conjunction
   **/

  public ConjunctionImpl(SimpleTest simple) {
    tmpSimpleTests.add(simple);
  }


  /** Create an initial Conjunction, to be added to as transformation proceeds
   *
   * @param resid the single residual test with which to prime the Conjunction
   **/

  public ConjunctionImpl(Selector resid) {
    tmpResidual.add(resid);
  }


  /** Create an initial, vacuous, Conjunction, which, unless added to later, will always
   * be true.
   **/

  public ConjunctionImpl() {
    alwaysTrue = true;
  }


  /** Add a SimpleTest to the Conjunction, searching for contradictions.
   *
   * @param newTest the new SimpleTest to be added
   *
   * @return true if the new test is compatible with the old (a false return means the
   * conjunction will always be false because the test is always false)
   **/

  public boolean and(SimpleTest newTest) {
    for (int i = 0; i < tmpSimpleTests.size(); i++) {
      SimpleTest cand = (SimpleTest) tmpSimpleTests.get(i);
      if (cand.getIdentifier().getName().equals(newTest.getIdentifier().getName()))
      {
    	// Careful, may be operating in XPath selector domain, in which
    	// case we need more stringent tests
    	if(cand.getIdentifier().isExtended())
    	{
          if(cand.getIdentifier().getStep() == newTest.getIdentifier().getStep())
          {
            // Identifiers have same name and same location step
        	return cand.combine(newTest);
          }
        }
    	else
    	  return cand.combine(newTest);
      }
    }
    tmpSimpleTests.add(newTest);
    alwaysTrue = false;
    return true;
  }


  /** Add a residual test to the Conjunction
   *
   * @param newResid the residual test to add
   **/

  public void and(Selector newResid) {
    tmpResidual.add(newResid);
    alwaysTrue = false;
  }


  /** Organize the Conjunction into its final useful form for MatchSpace.
   *
   * @return true if the Conjunction is still capable of being true, false if a
   * contradiction was detected during the organize step.
   *
   * @exception IllegalStateException if the Resolver assigned ordinalPosition information
   * incorrectly so that the simple tests cannot be ordered.
   **/

  public boolean organize() {
    // First, find any simple tests that can be used to reduce residual components to
    // simple tests or pure truth values, either because the simple test is a NULL test or
    // because it is effectively an equality test.
    if (tmpResidual.size() > 0) {
      List[] equatedIds = findEquatedIdentifiers();
      while (equatedIds != null && tmpResidual.size() > 0) {
        equatedIds = reduceResidual(equatedIds);
        if (equatedIds != null && equatedIds.length == 0)
          // Special indicator for contradiction
          return false;
      }
    }
    // We now have a reduced form Conjunction that is still capable of being true.  We
    // call shedSubtests on each SimpleTest so that STRINGOTH tests can move their
    // "difficult" parts to the residual.  We didn't do this earlier because we wanted to 
    // give the STRINGOTH test an opportunity to be trumped by a EQ test on the 
    // same identifier (or to be contradicted by a NULL test).  A false return from
    // shedSubtests causes the entire SimpleTest to be removed.  
    for (int i = 0; i < tmpSimpleTests.size(); )
      if (((SimpleTestImpl) tmpSimpleTests.get(i)).shedSubtests(tmpResidual))
        i++;
      else
        tmpSimpleTests.remove(i);
    // Sort the simple tests by ordinal position, looking for illegal position
    // assignments.
    for (int i = 0; i < tmpSimpleTests.size()-1; i++)
      for (int j = i+1; j < tmpSimpleTests.size(); j++) {
        SimpleTest iTest = (SimpleTest) tmpSimpleTests.get(i);
        SimpleTest jTest = (SimpleTest) tmpSimpleTests.get(j);
        OrdinalPosition iPos = (OrdinalPosition) iTest.getIdentifier().getOrdinalPosition();
        OrdinalPosition jPos = (OrdinalPosition) jTest.getIdentifier().getOrdinalPosition();
        
        if(jPos.compareTo(iPos) < 0)
        {
          tmpSimpleTests.set(j, iTest);
          tmpSimpleTests.set(i, jTest);
        }
        else if (jTest.getIdentifier().getOrdinalPosition() ==
                 iTest.getIdentifier().getOrdinalPosition())
          throw new IllegalStateException();
      }
    // We can now convert the Conjunction to its final form
    simpleTests = (SimpleTest[]) tmpSimpleTests.toArray(new SimpleTest[0]);
    tmpSimpleTests = null;
    for (int i = 0; i < tmpResidual.size(); i++)
      if (residual == null)
        residual = (Selector) tmpResidual.get(i);
      else
        residual = new OperatorImpl(Operator.AND, residual, (Selector) tmpResidual.get(i));
    tmpResidual = null;
    alwaysTrue = simpleTests.length == 0 && residual == null;
    return true;
  }


  // Finds the Identifiers that appear in tmpSimpleTests equated to a value or to NULL, so
  // that these can be removed from tmpResidual.  Return null if there are none.

  private List[] findEquatedIdentifiers() {
    List[] ans = null;
    for (int i = 0; i < tmpSimpleTests.size(); i++) {
      SimpleTest cand = (SimpleTest) tmpSimpleTests.get(i);
      if (cand.getKind() == SimpleTest.NULL) {
        if (ans == null)
          ans = new List[] { new ArrayList(), new ArrayList() };
        ans[0].add(cand.getIdentifier().getName());
        ans[1].add(null);
      }
      else {
        Object candValue = cand.getValue();
        if (candValue != null) {
          if (ans == null)
            ans = new List[] { new ArrayList(), new ArrayList() };
          ans[0].add(cand.getIdentifier().getName());
          ans[1].add(candValue);
        }
      }
    }
    return ans;
  }


  // Reduce a Conjunction by substituting Literals for Identifiers based on the "symbol
  // table" equatedIds in expressions in the residual.  The process may discover more
  // equatedIds, which is returned as a result.  A null return means that no more
  // equatedIds were found but the Conjunction is still capable of being true.  We use a
  // special empty array return to indicate a Conjunction that must be false and that
  // therefore should be abandoned.

  private List[] reduceResidual(List[] equatedIds) {
    List[] ans = null;
    for (int i = 0; i < tmpResidual.size(); ) {
      Operator oper = substitute((Operator) tmpResidual.get(i), equatedIds);
      if (oper.getNumIds() > 0 && !Matching.isSimple(oper))
        // Even after substitution, must remain as a residual
        tmpResidual.set(i++, oper);
      else if (oper.getNumIds() == 1) {
        // Eligible as a simple test.  DNF transform the test first to exploit new type
        // information that may have been introduced by the substitution.
        Selector trans = Matching.getTransformer().DNF(oper);
        if (trans instanceof Operator && ((Operator) trans).getOp() == Selector.OR)
          // DNF transformation of the result has revealed a new OR connector.  This can
          // happen iff the process of substitution changed a <> operator of type
          // UNKNOWN into type NUMERIC, which in turn became a disjunction of
          // inequalities.  If this happens we choose to punt since otherwise we would
          // have to redo the entire division of the selector into Conjunctions.  We
          // don't have logic for the case where the transformation reveals a new AND
          // because there is no substitution into a DNF-normalized conjunct that can
          // produce a naked AND.
          tmpResidual.set(i++, oper);
        else {
          // Otherwise, remove the residual and make a new SimpleTest and enter it.  If
          // the new SimpleTest is non-conflicting, proceed, otherwise abandon the
          // Conjunction.
          SimpleTest newTest = new SimpleTestImpl(trans);
          if (!and(newTest))
            return new List[0];
          tmpResidual.remove(i);
          // See if the new test is capable of further reducing the residual
          if (newTest.getKind() == SimpleTest.NULL) {
            if (ans == null)
              ans = new List[] { new ArrayList(), new ArrayList() };
            ans[0].add(newTest.getIdentifier().getName());
            ans[1].add(null);
          }
          else {
            Object newTestValue = newTest.getValue();
            if (newTestValue != null) {
              if (ans == null)
                ans = new List[] { new ArrayList(), new ArrayList() };
              ans[0].add(newTest.getIdentifier().getName());
              ans[1].add(newTestValue);
            }
          }
        }
      }
      else { // oper.numIds == 0
        // Bail if always false
        Boolean theEval = (Boolean) Matching.getEvaluator().eval(oper);
        if (theEval == null || !(theEval).booleanValue())
          return new List[0];
        // Always true, so just forget this residual
        tmpResidual.remove(i);
      }
    }
    return ans;
  }


  // Substitute Literal values for all occurances of Identifiers with known names in a
  // tree headed by an Operator.  A substitution of null for an Identifier anywhere causes
  // a null return (this signifies the presence of an IS NULL test for the identifier
  // which conflicts with any appearance in this tree).  If no substitutions are made, the
  // original tree is returned unmodified.  Otherwise, a new tree is returned which
  // incorporates parts of the old tree only if they are unmodified.  In other words, no
  // mutations are made directly to any tree nodes.

  private static Operator substitute(Operator oper, List[] equatedIds) {
    Selector op1 = oper.getOperands()[0];
    Selector op2 = (oper.getOperands().length == 1) ? null : oper.getOperands()[1];
    if (op1 instanceof Identifier)
      op1 = substitute((Identifier) op1, equatedIds);
    else if (op1 instanceof Operator)
      op1 = substitute((Operator) op1, equatedIds);
    if (op1 == null)
      return null;
    if (op2 != null) {
      if (op2 instanceof Identifier)
        op2 = substitute((Identifier) op2, equatedIds);
      else if (op2 instanceof Operator)
        op2 = substitute((Operator) op2, equatedIds);
      if (op2 == null)
        return null;
    }
    if (op1 == oper.getOperands()[0] && (op2 == null || op2 == oper.getOperands()[1]))
      return oper;
    else if (oper instanceof LikeOperator) {
      LikeOperatorImpl loper = (LikeOperatorImpl) oper;
        return new LikeOperatorImpl(loper.getOp(), op1, loper.getInternalPattern(), loper.getPattern(), loper.isEscaped(), loper.getEscape());
    }
    else
      return (op2 == null) ? new OperatorImpl(oper.getOp(), op1) : new OperatorImpl(oper.getOp(), op1, op2);
  }


  // Substitute a Literal value for an Identifier with known name.  Returns its argument
  // if not matched.

  private static Selector substitute(Identifier id, List[] equatedIds) {
    for (int i = 0; i < equatedIds[0].size(); i++)
      if (id.getName().equals(equatedIds[0].get(i)))
        return new LiteralImpl(equatedIds[1].get(i));
    return id;
  }


  // Display self (only gives a useful answer after organize)

  public String toString() {
    if (simpleTests == null)
      return "unknown";
    if (alwaysTrue)
      return "true";
    StringBuffer ans = new StringBuffer();
    String delim = "";
    for (int i = 0; i < simpleTests.length; i++) {
      ans.append(delim).append(simpleTests[i]);
      delim = " AND " ;
    }
    if (residual != null)
      ans.append(delim).append(residual);
    return ans.toString();
  }
  
  /**
   * Returns the simpleTests.
   * @return SimpleTest[]
   */
  public SimpleTest[] getSimpleTests() 
  {
	return simpleTests;
  }

  /**
   * Returns the residual.
   * @return Selector
   */
  public Selector getResidual() 
  {
	return residual;
  }

}
