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

import java.io.ObjectOutput;
import java.io.IOException;

/** A Selector tree represents a content selector in a form convenient for checking,
 * transformation, and usage in MatchSpace.  A variety of surface syntaxes can be parsed
 * into Selector trees.
 **/

public interface Selector {

  /** Version of this Selector 'language.' */
  public static final int VERSION = 1;

  // Byte codes:

  // Literals (< IDENTIFIER)

  /** Byte code for identifier */

  public static final int IDENTIFIER = 0;

  /** Byte code for float literal (also type code) */

  public static final int DOUBLE = -1;


  /** Byte code for double literal (also type code) */

  public static final int FLOAT = -2;


  /** Byte code for int literal (also type code) */

  public static final int LONG = -3;


  /** Byte code for long literal (also type code) */

  public static final int INT = -4;


  /** Byte code for String literal (also type code) */

  public static final int STRING = -5;


  /** Byte code for boolean literal (also type code) */

  public static final int BOOLEAN = -6;
  
  
  /** Byte code for an "object" literal (also type code).  The values of such
   * literals are of type Serializable
   */
  public static final int OBJECT = -7;
  
  /** Byte code for child in set value hierarchy */

  public static final int CHILD = -8;
  
  /** Type code indicating a Selector node of unknown type (NOT a valid Literal byte
   * code, but used in Identifiers and Operators).  */

  public static final int UNKNOWN = 0;


  /** Type code indicating a Selector node of any numeric type (NOT a valid Literal
   * byte code, but used in Identifiers and Operators).  */

  public static final int NUMERIC = 1;


  /** Type code indicating a Selector node of invalid type */

  public static final int INVALID = 2;
  

  /** Type code indicating a Selector node (always an Identifier in practice)
   * of topic type (MQSI-EventBroker topic syntax).  These are comparable with
   * STRING Selectors but require a distinct Matcher and a different LIKE operator.
   */
  
  public static final int TOPIC = 3;
  

  // Unary Operators (> IDENTIFIER && < FIRST_BINARY)

  /** Byte code for logical not */

  public static final int NOT = 1;


  /** Byte code for arithmetic negation */

  public static final int NEG = 2;

  /** Byte code for 'is null' test on identifier */

  public static final int ISNULL = 3;


  /** Byte code for LIKE operator (special parsing) */

  public static final int LIKE = 4;
  
  
  /** Byte code for TOPIC_LIKE operator (special parsing) */
  
  public static final int TOPIC_LIKE = 5;

  /**
   *  Extension operator used for parsing non-JMS selector expressions 
   */
  public static final int EXTENSION = 6;
  
  // Binary Operators (>= FIRST_BINARY)

  static final int FIRST_BINARY = 40;


  /** Byte code for <> comparison */

  public static final int NE = 40;


  /** Byte code for > comparison */

  public static final int GT = 41;


  /** Byte code for &lt; comparison */

  public static final int LT = 42;


  /** Byte code for >= comparison */

  public static final int GE = 43;


  /** Byte code for &lt;= comparison */

  public static final int LE = 44;


  /** Byte code for = comparison */

  public static final int EQ = 45;


  /** Byte code for logical and */

  public static final int AND = 46;


  /** Byte code for logical or */

  public static final int OR = 47;


  /** Byte code for infix + */

  public static final int PLUS = 48;


  /** Byte code for infix - */

  public static final int MINUS = 49;


  /** Byte code for * (times) */

  public static final int TIMES = 50;


  /** Byte code for / (divide by) */

  public static final int DIV = 51;


  /** Returns true if the type is consistent with a boolean context, false otherwise.
   * Sets the type to BOOLEAN if it is UNKNOWN.
   **/

  public boolean mayBeBoolean();

  /** Returns true if the type is consistent with a String context, false otherwise.  Sets
   * the type to STRING if it is UNKNOWN.
   **/

  public boolean mayBeString();

  /** Returns true if the type is consistent with a numeric context, false otherwise.
   * Sets the type to NUMERIC if it is UNKNOWN.
   **/

  public boolean mayBeNumeric();
  
  /** Returns true if the type is consistent with an object context, false otherwise.
   * Sets the type to OBJECT if it is UNKNOWN.  OBJECT is defined as a Serializable that
   * is not a STRING, NUMERIC, or BOOLEAN.
   */
  
  public boolean mayBeObject();


  /** Replaces this Selector tree with an equal() Selector tree that is "uniquely
   * identified".  
   * This means that (1) all of its subtrees are uniquely identified
   * (recursive definition), (2) it has a uniqueId assignment, (3) relative to the
   * supplied InternTable, equal subtrees always have the same uniqueIds, and (4) relative
   * to the supplied InternTable, unequal subtrees always have different uniqueIds.
   *
   * @param table the InternTable that will decide uniqueness, assign uniqueIds and store
   * uniquely identified subtrees.
   *
   * @return a uniquely identified tree equal() to the this tree
   **/

  public Selector intern(InternTable table);

  /** Removes this Selector from an InternTable
   *
   * @param table the InternTable context from which this Selector should be removed
   **/

  public void unintern(InternTable table);

  /** Interface to be implemented by callers of the intern and unintern methods */

  public static interface InternTable {
    public Object get(Object key); // as in Dictionary
    public Object put(Object key, Object element); // as in Dictionary
    public Object remove(Object key); // as in Dictionary
    public int size();  // as in Dictionary
    public int getNextUniqueId();  // return next free id
  }

  /** The encode method serializes a Selector tree into ObjectOutput */

  public void encode(ObjectOutput buf) throws IOException;

  public void encodeSelf(ObjectOutput buf) throws IOException;
  
  // Overrides

  public boolean equals(Object o);


  public Object clone();

  /**
   * Returns the numIds.
   * @return int
   */
  public int getNumIds(); 

  /**
   * Returns the refCount.
   * @return int
   */
  public int getRefCount(); 

  /**
   * Returns the type.
   * @return int
   */
  public int getType(); 
  
 /**
   * Sets the type.
   * @param type The type to set
   */
  public void setType(int type);
  
  /**
   * Increments the refCount.
   */
  public void incRefCount();
  
  /**
   * Returns the uniqueId.
   * @return int
   */
  public int getUniqueId();

  /**
   * Sets the uniqueId.
   * @param uniqueId The uniqueId to set
   */
  public void setUniqueId(int uniqueId);   
  
  public boolean isExtended();
  
  public void setExtended();
}
