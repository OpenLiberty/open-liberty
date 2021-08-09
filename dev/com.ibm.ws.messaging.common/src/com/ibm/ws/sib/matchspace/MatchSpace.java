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
package com.ibm.ws.sib.matchspace;

// Import required classes

import java.io.PrintWriter;

//------------------------------------------------------------------------------
// MatchSpace interface
//------------------------------------------------------------------------------
/**
 * MatchSpace defines the interface to be implemented by the primary MatchSpace class.
 **/

public interface MatchSpace
{

  // Constants related to topic parsing and topic matching 
  public static final char NONWILD_MARKER = '\u0001';
  public static final String SUBTOPIC_DOUBLE_SEPARATOR_STRING = "//";
  public static final String SUBTOPIC_DOUBLE_SEPARATOR_STOP_STRING = "//.";
  public static final char SUBTOPIC_MATCHONE_CHAR = '*';
  public static final char SUBTOPIC_SEPARATOR_CHAR = '/';
  public static final char SUBTOPIC_STOP_CHAR = '.';
  public static final char SUBTOPIC_MQSI_MATCHONE_CHAR = '+';
  public static final char SUBTOPIC_MQSI_MATCHMANY_CHAR = '#';
  
  //------------------------------------------------------------------------------
  // Method: MatchSpace.addTarget
  //------------------------------------------------------------------------------
  /** Adds a Conjunction to the space and associates a MatchTarget with it.
   *
   * @param conjunction the Conjunction
   *
   * @param target the MatchTarget
   * 
   * @exception MatchingException thrown for serious errors, including an ill-formed
   * conjunction.  The Conjunction will be well-formed if it was constructed from a syntactically
   * valid expression using a sound parser and Resolver.
   **/

  public void addTarget(
    Conjunction conjunction,
    MatchTarget object)
    throws MatchingException;

  //------------------------------------------------------------------------------
  // Method: MatchSpace.search
  //------------------------------------------------------------------------------
  /** Gets a the result of a matching traversal for a Message.  This method should be used
   * when you want to get the results of a traversal.  This method is designed to be
   * called while NOT holding the MatchSpace lock; it is acquired internally if needed.
   *
   * @param rootValue if non-null, this value is used for the
   *    root Identifier instead of calling the MatchSpaceKey
   * @param msg message to match (based on content).
   * @param cache the EvalCache to use during the match
   * @param result The results of the match are added to this object.
   **/
  
  public void search(
    Object rootValue, 
    MatchSpaceKey msg,
    EvalCache cache,
    SearchResults result)
    throws MatchingException, BadMessageFormatMatchingException;

  //------------------------------------------------------------------------------
  // Method: MatchSpace.removeTarget
  //------------------------------------------------------------------------------
  /**Removes a topic/conjunction/MatchTarget triple from the MatchSpace
   *
   * @param conjunction the conjunction identifying what is to be removed
   * 
   * @param target the MatchTarget to be removed
   *
   * @exception MatchingException on serious error
   **/

  public void removeTarget(
    Conjunction conjunction,
    MatchTarget target)
    throws MatchingException;

  //------------------------------------------------------------------------------
  // Method: MatchSpace.statistics();
  //------------------------------------------------------------------------------
  /** Only used when doing isolated performance testing of the MatchSpace. */

  public void statistics(PrintWriter wtr);


  //------------------------------------------------------------------------------
  // Method: MatchSpace.clear
  //------------------------------------------------------------------------------
  /** Removes all objects from the MatchSpace, resetting it to the 'as new'
   * condition.
   *
   * @param rootId
   * @param enableCache
   */
  public void clear(Identifier rootId, boolean enableCache);


}
