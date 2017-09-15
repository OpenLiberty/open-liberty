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
 
package com.ibm.ws.sib.matchspace.tools;

import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.Selector;

  /** A Transformer walks a Selector tree and resolves all the Identifiers.
   *
   * Tree the Selector tree to walk.  The tree may be modified in place by this
   * method.  Strictly speaking, only Identifier nodes can ever be replaced, but, since
   * the tree can consist of only an Identifier, this may result in the tree being
   * replaced rather than modified.
   *
   * Resolver an implementation of the Resolver interface which will be called with
   * every identifier encountered and also to push and pop Identifier resolution contexts.
   *
   * A replacement Selector tree.  This will be the old tree modified in place
   * except for the special case where the top level node was an Identifier and it was
   * replaced by the resolver.
   **/
  
public interface Transformer 
{

  /** Performs DNF transformation on a Selector tree or subtree.  The result will have OR
   * nodes (if any) at the top, with AND nodes (if any) underneath those.  EQ and NE
   * operations between booleans are eliminated. NOT nodes are also eliminated unless
   * their operands are Identifiers or Operators of types LIKE, ISEMPTY, or ISNULL.  NE
   * nodes are eliminated (turned into OR nodes over inequalities) if they compare
   * manifestly arithmetic types.  Finally, boolean AND/OR is simplified based on
   * subsexpressions whose value can be statically known.
   *
   * @param tree the Selector tree that is to be transformed
   *
   * @return the transformed Selector tree
   **/

  public Selector DNF(Selector tree);
  
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

  public Conjunction[] organizeTests(Selector tree);  

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

  public Selector resolve(Selector tree, Resolver resolver, PositionAssigner positionAssigner);
  
}
