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

import com.ibm.ws.sib.matchspace.impl.Matching;
import com.ibm.ws.sib.matchspace.Selector;

/** The MatchParser class provides parsing support for a superset of the JMS selector
 * syntax, returning a Selector tree.  Except for the superset features, the language
 * accepted is that of the JMS specification.
 *
 * MatchParser language features not in JMS:
 *
 * (1) Identifiers can be quoted with " and may contain any character except unescaped ".
 *
 * (2) Identifiers (unquoted) may contain the field separator character '.'.
 *
 * (3) Set predicates allow an arbitrary expression on the left (not restricted to
 * identifier) and a list of arbitrary expressions on the right (not restricted to string
 * literals).
 *
 * (4) There is support for lists.  This support is accessed through the use of [ ]
 * characters and is described in detail elsewhere.
 *
 * The superset features can be turned off by setting the 'strict' flag, causing the
 * parser to recognize only the JMS syntax.
 **/

public interface MatchParser 
{

  /** Return the Selector tree associated with a primed parser.
   *
   * @return a selector tree.  If the parse was successful, the top node of the tree will
   * be of BOOLEAN type; otherwise, it will be of INVALID type.
   **/
  public Selector getSelector(String Selector);
  
  /**
   * Allow the setting of the Matching instance into the parser
   * 
   * @param matching
   */
  public void setMatching(Matching matching);

}
