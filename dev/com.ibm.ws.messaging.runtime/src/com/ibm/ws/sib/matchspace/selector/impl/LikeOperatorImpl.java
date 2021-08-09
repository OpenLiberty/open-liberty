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

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;

import com.ibm.ws.sib.matchspace.LikeOperator;
import com.ibm.ws.sib.matchspace.Selector;

/** Subclass of Operator that implements a LIKE expression as a parameterized unary
 * operator
 **/

public final class LikeOperatorImpl extends OperatorImpl implements LikeOperator {
  /** The pattern string.  Used in toString() and serialization but not evaluation */
  public String strPattern;

  /** The escape character if escaped.  Used in toString and serialization but not
   * evaluation. */
  public char escape;

  /** True if this LIKE expression is escaped, false otherwise */
  public boolean escaped = false;
  
  /** The parsed Pattern.  Used in evaluation. */
  public Pattern pattern;


  /** Create a LikeOperator from its constituents
   * 
   * @param op the opcode to use (LIKE or TOPIC_LIKE)
   *
   * @param operand the String expression whose value is to be matched against the pattern
   *
   * @param pattern the pattern to employ, as a Pattern object
   * 
   * @param strPattern the pattern to employ, as a String (for toString and serialization)
   *
   * @param escaped says whether this LikeOperator is escaped (applies only to the
   *   String form).  Always false with TOPIC_LIKE. 
   *
   * @param escape the escape character (ignored if escaped is false)
   **/
  public LikeOperatorImpl(int op, Selector operand, Pattern pattern, String strPattern,
      boolean escaped, char escape) {
    super(op, operand);
    this.pattern = pattern;
    this.strPattern = strPattern;
    this.escaped = escaped;
    this.escape = escape;
  }


  /** Create a LikeOperator from a ObjectInput (used only by Selector.decode) */

  public LikeOperatorImpl(ObjectInput buf) throws ClassNotFoundException, IOException {
    super(buf);   // Get the Identifier operand
    strPattern = buf.readUTF();
    escaped = buf.readBoolean();
    if (escaped)
      escape = buf.readChar();
    this.pattern = (Pattern) Pattern.parsePattern(strPattern, escaped, this.escape);
  }


  /** Encode this LikeOperator into a ObjectOutput (used only by Selector.encode) */

  public void encodeSelf(ObjectOutput buf) throws IOException {
    super.encodeSelf(buf); // encode the opcode and operand
    buf.writeBoolean(escaped);
    if (escaped)
      buf.writeChar(escape);
  }


  // Overrides

  public boolean equals(Object o) {
    if (o instanceof LikeOperator) {
      LikeOperator other = (LikeOperator) o;
      return super.equals(o) && getPattern().equals(other.getPattern()) && escaped == other.isEscaped()
        && escape == other.getEscape();
    }
    else
      return false;
  }

  public int hashCode() {
    return super.hashCode() + pattern.hashCode();
  }


  public String toString() {
    return operands[0] + " LIKE '" + strPattern + "'" + (escaped ? (" ESCAPE '" + escape + "'") : "");
  }

  /**
   * Returns the escape.
   * @return char
   */
  public char getEscape() 
  {
	return escape;
  }

  /**
   * Returns the escaped.
   * @return boolean
   */
  public boolean isEscaped() 
  {
	return escaped;
  }

  /**
   * Returns the pattern.
   * @return String
   */
  public String getPattern() 
  {
	return strPattern;
  }
  
  /** 
   * Returns the parsed pattern (only on LikeOperatorImpl, not LikeOperator)
   * @return the parsed Pattern
   */
  public Pattern getInternalPattern() {
    return pattern;
  }
}
