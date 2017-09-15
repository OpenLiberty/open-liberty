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
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.Literal;
import com.ibm.ws.sib.matchspace.impl.Matching;
import com.ibm.ws.sib.matchspace.Operator;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.tools.PositionAssigner;
import com.ibm.ws.sib.matchspace.tools.Resolver;
import com.ibm.ws.sib.matchspace.tools.Transformer;

public final class TransformerImpl implements Transformer {
  public TransformerImpl() {} // static-only class


  /** Walks a Selector tree and resolves all the Identifiers
   *
   * @param tree the Selector tree to walk.  The tree may be modified in place by this
   * method.  Strictly speaking, only Identifier nodes can ever be replaced, but, since
   * the tree can consist of only an Identifier, this may result in the tree being
   * replaced rather than modified.
   *
   * @param resolver an implementation of the Resolver interface which will be called with
   * every identifier encountered and also to push and pop Identifier resolution contexts.
   *
   * @return a replacement Selector tree.  This will be the old tree modified in place
   * except for the special case where the top level node was an Identifier and it was
   * replaced by the resolver.
   **/

  public Selector resolve(Selector tree, Resolver resolver, PositionAssigner positionAssigner) {
    if (tree instanceof Identifier) {
      Identifier id = (Identifier) tree;
      // The subtree to resolve is an Identifier.  Resolve the Identifier.
      return resolver.resolve(id, positionAssigner);
    }
    else if (tree.getNumIds() > 0) {
      Operator oper = (Operator) tree;
      // The subtree to resolve is an Operator, under which there are some Identifiers.
      for (int i = 0; i < oper.getOperands().length; i++)
        oper.setOperand(i,resolve(oper.getOperands()[i], resolver, positionAssigner));
      oper.assignType();
    } // else tree is a literal and not modified
    return tree;
  }


  /** Performs DNF transformation on a Selector tree or subtree.  The result will have OR
   * nodes (if any) at the top, with AND nodes (if any) underneath those.  EQ and NE
   * operations between booleans are eliminated. NOT nodes are also eliminated unless
   * their operands are Identifiers or Operators of types LIKE, TOPIC_LIKE or ISNULL.  NE
   * nodes are eliminated (turned into OR nodes over inequalities) if they compare
   * manifestly arithmetic types.  Finally, boolean AND/OR is simplified based on
   * subsexpressions whose value can be statically known.
   *
   * @param tree the Selector tree that is to be transformed
   *
   * @return the transformed Selector tree
   **/

  public Selector DNF(Selector tree) {
    tree = simplifyTree(tree); /* eliminate NOT, Boolean EQ, Boolean and numeric NE.  This
                                  may generate additional AND and OR nodes, which is why
                                  we do it first */
    return DNF0(tree);
  }


  // Working recursive subroutine of DNF


  private static Selector DNF0(Selector tree) {
    if (!(tree instanceof Operator))
      return tree;
    Operator oper = (Operator) tree;
    switch(oper.getOp()) {
    case Selector.AND:
      return processAND(oper.getOperands()[0], oper.getOperands()[1]);
    case Selector.OR:
      return makeOR(DNF0(oper.getOperands()[0]), DNF0(oper.getOperands()[1]));
    default:
      return tree;
    }
  }


  // Subroutine of DNF0 to process AND nodes

  private static Selector processAND(Selector sel0, Selector sel1) {
    sel0 = DNF0(sel0);
    sel1 = DNF0(sel1);
    if (sel0 instanceof Operator) {
      Operator oper = (Operator) sel0;
      if (oper.getOp() == Selector.OR)
        return makeOR(processAND(oper.getOperands()[0], sel1),
                      processAND(oper.getOperands()[1], sel1));
    }
    // Left operand is not OR, so treat it as an atom while exploring the right operand
    return processANDRight(sel0, sel1);
  }


  // Subroutine of DNF0 to process AND nodes whose left child is an atom

  private static Selector processANDRight(Selector sel0, Selector sel1) {
    if (sel1 instanceof Operator) {
      Operator oper = (Operator) sel1;
      if (oper.getOp() == Selector.OR)
        return makeOR(processANDRight(sel0, oper.getOperands()[0]),
                      processANDRight(sel0, oper.getOperands()[1]));
    }
    // Right operand is not OR (neither is left) so treat both as atoms
    return makeAND(sel0, sel1);
  }


  // Make an OR node from two terms, or return one of the terms if the other must be
  // false, or return the false Literal if both must be false, or return true if either
  // must be true.

  private static Selector makeOR(Selector sel0, Selector sel1) {
    if (sel0.getNumIds() == 0) {
      Boolean staticResult = (Boolean) // was BooleanValue
        Matching.getEvaluator().eval(sel0);
      if (staticResult != null && staticResult.booleanValue())
        return new LiteralImpl(Boolean.TRUE); // was BooleanValue
      else
        return evalOf(sel1);
    }
    if (sel1.getNumIds() == 0) {
      Boolean staticResult = (Boolean) // was BooleanValue
        Matching.getEvaluator().eval(sel1);
      if (staticResult != null && staticResult.booleanValue())
        return new LiteralImpl(Boolean.TRUE); // was BooleanValue
      else
        return evalOf(sel0);
    }
    return new OperatorImpl(Operator.OR, sel0, sel1);
  }


  // Make an AND node from two terms, or return one of the terms if the other must be
  // true, or return the true Literal if both must be true, or return false if either
  // must be false.

  private static Selector makeAND(Selector sel0, Selector sel1) {
    if (sel0.getNumIds() == 0) {
      Boolean staticResult = (Boolean) // was BooleanValue
        Matching.getEvaluator().eval(sel0);
      if (staticResult == null || !staticResult.booleanValue())
        return new LiteralImpl(Boolean.FALSE); // was BooleanValue
      else
        return evalOf(sel1);
    }
    if (sel1.getNumIds() == 0) {
      Boolean staticResult = (Boolean) // was BooleanValue
        Matching.getEvaluator().eval(sel1);
      if (staticResult == null || !staticResult.booleanValue())
        return new LiteralImpl(Boolean.FALSE); // was BooleanValue
      else
        return evalOf(sel0);
    }
    return new OperatorImpl(Operator.AND, sel0, sel1);
  }


  // Return either the argument or the result of evaluating the argument, the latter only
  // if the argument has no identifiers

  private static Selector evalOf(Selector arg) {
    if (arg.getNumIds() > 0)
      return arg;
    return new LiteralImpl(Matching.getEvaluator().eval(arg));
  }


  // Subroutine of DNF to simplify a tree by eliminating:
  //
  // (1) NOT before anything except identifier, LIKE, TOPIC_LIKE, or ISNULL.
  //
  // (2) Boolean EQ
  //
  // (3) Boolean and Numeric NE
  //
  // We do this in a preliminary pass before processing the remaining (or generated) AND
  // and OR nodes

  private static Selector simplifyTree(Selector tree) {
    if (!(tree instanceof Operator) || tree instanceof ExtensionOperatorImpl)
      return tree;
    Operator oper = (Operator) tree;
    switch (oper.getOp()) {
    case Selector.NOT:
      return simplifyNOT(oper.getOperands()[0]);
    case Selector.EQ:
      return simplifyEQ(oper.getOperands()[0], oper.getOperands()[1]);
    case Selector.NE:
      return simplifyNE(oper.getOperands()[0], oper.getOperands()[1]);
    case Selector.NEG:
    case Selector.ISNULL:
      return new OperatorImpl(oper.getOp(), simplifyTree(oper.getOperands()[0]));
    case Selector.LIKE:
    case Selector.TOPIC_LIKE:
      LikeOperatorImpl loper = (LikeOperatorImpl) oper;
      return new LikeOperatorImpl(loper.getOp(), simplifyTree(loper.getOperands()[0]),
        loper.getInternalPattern(), loper.getPattern(), loper.isEscaped(),
        loper.getEscape());
    default:
      return new OperatorImpl(oper.getOp(), simplifyTree(oper.getOperands()[0]),
                          simplifyTree(oper.getOperands()[1]));
    }
  }


  // Subroutine of simplifyTree to handle NOT nodes

  private static Selector simplifyNOT(Selector tree) {
    if (tree instanceof Literal)
      return new LiteralImpl( EvaluatorImpl.not((Boolean) ((Literal) tree).getValue())); // was BooleanValue
    if (!(tree instanceof Operator))
      return new OperatorImpl(Selector.NOT, tree);
    Operator oper = (Operator) tree;
    switch(oper.getOp()) {
    case Selector.NOT:
      return simplifyTree(oper.getOperands()[0]);
    case Selector.AND:
      return makeOR(simplifyNOT(oper.getOperands()[0]),
                    simplifyNOT(oper.getOperands()[1]));
    case Selector.OR:
      return makeAND(simplifyNOT(oper.getOperands()[0]),
                     simplifyNOT(oper.getOperands()[1]));
    case Selector.EQ:
      return simplifyNE(oper.getOperands()[0], oper.getOperands()[1]);
    case Selector.NE:
      return simplifyEQ(oper.getOperands()[0], oper.getOperands()[1]);
    case Selector.GT:
      return new OperatorImpl(Selector.LE, oper.getOperands()[0], oper.getOperands()[1]);
    case Selector.LT:
      return new OperatorImpl(Selector.GE, oper.getOperands()[0], oper.getOperands()[1]);
    case Selector.GE:
      return new OperatorImpl(Selector.LT, oper.getOperands()[0], oper.getOperands()[1]);
    case Selector.LE:
      return new OperatorImpl(Selector.GT, oper.getOperands()[0], oper.getOperands()[1]);
    default:
      return new OperatorImpl(Selector.NOT, tree);
    }
  }

  // Subroutine of simplifyTree to handle EQ nodes

  private static Selector simplifyEQ(Selector sel0, Selector sel1) {
    if (sel0.getType() != Selector.BOOLEAN)
      return new OperatorImpl(Selector.EQ, sel0, sel1);
    else
      return makeOR(makeAND(simplifyTree(sel0), simplifyTree(sel1)),
                    makeAND(simplifyNOT(sel0), simplifyNOT(sel1)));
  }


  // Subroutine of simplifyTree to handle NE nodes

  private static Selector simplifyNE(Selector sel0, Selector sel1) {
    if (sel0.getType() == Selector.BOOLEAN)
      return makeOR(makeAND(simplifyTree(sel0), simplifyNOT(sel1)),
                    makeAND(simplifyNOT(sel0), simplifyTree(sel1)));
    else if (sel0.getType() == Selector.STRING || sel0.getType() == Selector.UNKNOWN)
      return new OperatorImpl(Selector.NE, sel0, sel1);
    else
      // Numeric: transform into a pair of inequalities
      return makeOR(new OperatorImpl(Selector.LT, sel0, sel1),
                    new OperatorImpl(Selector.GT, sel0, sel1));
  }


  /** Organize a DNF-transformed Selector tree into an array of Conjunctions representing
   * a top-level disjunction.  Each Conjunction is itself organized internally into a set
   * of simple tests and a set of non-simple tests (each of which may be empty). If the
   * Selector tree is found to be statically true regardless of the values of identifiers,
   * null is returned.  If the selector tree is found to be statically false, an empty
   * array is returned.  A variety of minimizations are applied during the transformation.
   *
   * @param tree the DNF-transformed Selector tree to be organized.
   *
   * @return an array of Conjunction objects, which may be empty if the test is always
   * false, or null if the test is always true.
   *
   * @exception IllegalArgumentException if the tree is not a valid DNF-transformed tree.
   * Other RuntimeException types may also "leak" due to the same error, or results may be
   * unpredictable.
   **/

  public Conjunction[] organizeTests(Selector tree) {
    List tests = new ArrayList();
    if (organizeTests(tests, tree))
      return null; // always true
    return (ConjunctionImpl[]) tests.toArray(new ConjunctionImpl[0]);
  }


  // Working subroutine of organizeTests.  A return of true means "always true."  A return
  // of false means "may be false."

  private static boolean organizeTests(List tests, Selector tree) {
    ConjunctionImpl conjunction;
    if (tree instanceof Operator) {
      Operator oper = (Operator) tree;
      if (oper.getOp() == Selector.OR) {
        if (organizeTests(tests, oper.getOperands()[0]) ||
            organizeTests(tests, oper.getOperands()[1]))
          return true; // always true
        return false; // may be false (disjuncts have been processed)
      }
      else if (oper.getOp() == Selector.AND)
        conjunction = organizeConjunction(oper);
      else
        conjunction = simpleConjunct(tree);
    }
    else
      conjunction = simpleConjunct(tree);
    if (conjunction == null)
      // A conjunction that can never be true (indicated by null return) should not be
      // added.
      return false;
    if (conjunction.alwaysTrue)
      // A vacuous conjunction, causing the disjunction to always be true.  Also
      // should not be added, and causes overall result for the Selector to be null.
      return true;
    // Do final processing on a Conjunction, and add it iff it is still capable of being
    // true.
    if (conjunction.organize())
      tests.add(conjunction);
    return false;
  }


  // Make a Selector tree that does not contain any AND or OR nodes into a Conjunction
  // (with at most one test, simple or residual).  If the tree has no identifiers, it is
  // evaluated and we either return null (if the result is false) or the vacuous
  // Conjunction (if the result is true).

  private static ConjunctionImpl simpleConjunct(Selector tree) {
    if (tree.getNumIds() == 0)
    {
      Boolean theEval = (Boolean) Matching.getEvaluator().eval(tree);
      if (theEval != null && theEval.booleanValue()) // was ((BooleanValue) Matching.getEvaluator().eval(tree)).booleanValue()    	
        return new ConjunctionImpl();
      else
        return null;
    }
    else if (Matching.isSimple(tree))
    {
        if(tree.isExtended())
          return new ConjunctionImpl(new ExtendedSimpleTestImpl(tree));
        else
   	      return new ConjunctionImpl(new SimpleTestImpl(tree));
    }
    else
      return new ConjunctionImpl(tree);
  }

  // Organize a selector subtree headed by an AND node and with no OR nodes in it into a
  // Conjunction.  If the Conjunction is always false (a condition that can arise due to a
  // single test being always false), null is returned.  Tests that are always true are
  // not included in the result; if no test would thereby be included, the Conjunction is
  // marked "vacuous" (always true).  This will cause the selector as a whole always to be
  // true.

  private static ConjunctionImpl organizeConjunction(Operator andOper) {
    Selector left = andOper.getOperands()[0];
    ConjunctionImpl conjunction;
    if (left instanceof Operator) {
      Operator oper = (Operator) left;
      if (oper.getOp() == Selector.AND)
        conjunction = organizeConjunction(oper);
      else
        conjunction = simpleConjunct(left);
    }
    else
      conjunction = simpleConjunct(left);
    if (conjunction == null)
      return null;
    return augmentConjunction(conjunction, andOper.getOperands()[1]) ? conjunction : null;
  }


  // Augment a conjunction with an additional Selector, which may be an AND or something
  // simpler.  May modify its argument.  Returns true if the Conjunction can still be true
  // or false if the Conjunction will always be false.

  private static boolean augmentConjunction(ConjunctionImpl target, Selector toAdd) {
    if (toAdd instanceof Operator) {
      Operator oper = (Operator) toAdd;
      if (oper.getOp() == Selector.AND)
        return augmentConjunction(target, oper.getOperands()[0]) &&
          augmentConjunction(target, oper.getOperands()[1]);
    }
    // toAdd is not an AND node, so we process and combine it in the appropriate way
    if (toAdd.getNumIds() == 0)
    {
      // Should not be added to Conjunction, but it could render the Conjuction false if
      // it is false
      Boolean theEval = (Boolean) Matching.getEvaluator().eval(toAdd);
      if (theEval == null)
      	return false;
      else
        return theEval.booleanValue(); // was ((BooleanValue) Matching.getEvaluator().eval(toAdd)).booleanValue()
    }
    else if (Matching.isSimple(toAdd))
    {
      if(toAdd.isExtended())
        return target.and(new ExtendedSimpleTestImpl(toAdd));
      else
        return target.and(new SimpleTestImpl(toAdd));
    }
    else 
    {
      target.and(toAdd);
      return true;
    }
  }
}
