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

/** This interface represents one "simple test" (a test involving exactly one Identifer in
 * exactly one position).  Note that "FOO = FOO" (though vacuous) is NOT simple.  It may
 * involve only one identifier, but the identifier appears in more than one position.
 **/

public interface SimpleTest {

  // Kinds of tests

  /** Indicates that the test is simply the value of a boolean identifier */

  public static final int ID = 0;


  /** Indicates that the test is of the form NOT <identifier> */

  public static final int NOTID = 1;


  /** Indicates that the test compares an identifier for equality against a literal */

  public static final int EQ = 2;


  /** Indicates that the test consists of one or more LIKE, NOT LIKE, or NE tests against
   * the same String identifier
   **/

  public static final int STRINGOTH = 3;


  /** Indicates that the test is of the form <identifier> IS NULL */

  public static final int NULL = 4;


  /** Indicates that the test is of the form <identifier> IS NOT NULL */

  public static final int NOTNULL = 5;


  /** Indicates that the test is a numeric comparison (one of LT, LE, GT, or GE).  The
   * comparator NE does not appear as a simple numeric test because it is analyzed into a
   * pair of inequalities earlier in the process, and the EQ test is a different kind of
   * SimpleTest.
   **/

  public static final int NUMERIC = 6;

  /** If this SimpleTest is an EQ test, return its comparand.
   * @return the comparand of an equality test, or null if this isn't an EQ test.
   **/

  public Object getValue();

  /** Given a SimpleTest on the same identifier as this one, combine its information with
   * the information in this one.
   *
   * @param other the other SimpleTest that is to be combined with this one
   *
   * @return true if the two tests are compatible with each other (their conjunction is
   * capable of being true).  Return false if the two tests contradict each other (nothing
   * is changed in the latter case)
   **/

  public boolean combine(SimpleTest other); 

  // Turn SimpleTest back into a Selector tree for evaluation (presumably because we've
  // decided not to combine it with other tests in a Matcher tree).

  public Selector toSelector(); 

  /**
   * Returns the kind.
   * @return int
   */
  public int getKind();

  /**
   * Returns the lower.
   * @return NumericValue
   */
  public Number getLower(); // NumericValue

  /**
   * Returns the lowIncl.
   * @return boolean
   */
  public boolean isLowIncl(); 

  /**
   * Returns the tests (for STRINGOTH)
   * @return Selector[] or null if not STRINGOTH
   */
  public Selector[] getTests(); 


  /**
   * Returns the upIncl.
   * @return boolean
   */
  public boolean isUpIncl(); 

  /**
   * Returns the upper.
   * @return NumericValue
   */
  public Number getUpper(); // was NumericValue

  /**
   * Returns the identifier.
   * @return Identifier
   */
  public Identifier getIdentifier();
}
