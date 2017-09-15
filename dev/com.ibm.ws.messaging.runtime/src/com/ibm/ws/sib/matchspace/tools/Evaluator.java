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

import java.util.ArrayList;

import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;

/** The Evaluator is used to evaluate Selector trees, returning a value */

public interface Evaluator {

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

  public Object eval(Selector sel, MatchSpaceKey msg, EvalCache cache, Object contextValue, boolean permissive)
    throws BadMessageFormatMatchingException;


  /** Evaluates a selector tree without resolving identifiers (usually applied only to
   * Selector subtrees with numIds == 0).
   *
   * @param sel the selector tree to evaluate
   *
   * @return the result, which will be a String, a BooleanValue, a NumericValue, or null.
   * Null is used for "missing" Numeric or String values.  BooleanValue.NULL is used for
   * missing Boolean values.
   **/

  public Object eval(Selector sel);

  /**
   * Get a DOM document root from a message. This method will return null unless driven
   * where XPath support is implemented.
   * 
   * @param childNodeList
   * @return
   * @throws BadMessageFormatMatchingException 
   */
  public Object getDocumentRoot(MatchSpaceKey msg) throws BadMessageFormatMatchingException;


  /**
   * Retrieve text associated with a node, for debug.
   * 
   * @param node
   * @return
   */
  public String getNodeText(Object node);

  /**
   * Cast an ArrayList of Nodes to an ArrayList of Numbers.
   * 
   * @param childNodeList
   * @return
   */
  public ArrayList castToNumberList(ArrayList childNodeList);

  /**
   * Cast an ArrayList of Nodes to an ArrayList of Strings.
   * 
   * @param childNodeList
   * @return
   */
  public ArrayList castToStringList(ArrayList childNodeList);

}
