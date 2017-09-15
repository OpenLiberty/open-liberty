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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import com.ibm.ws.sib.matchspace.Literal;
import com.ibm.ws.sib.matchspace.Selector;

/** This class represents a Literal value in a Selector tree */

public final class LiteralImpl extends SelectorImpl implements Literal {

  /** The value of the literal.  Class will be one of BooleanValue, NumericValue, or
   * String
   **/

  public Object value;


  /** Determine Selector type code of a Literal value according to its class.  Used by the
   * Literal constructor and by the Evaluator
   *
   * @param value the value whose type is requested
   *
   * @return one of the available literal type codes or Selector.INVALID
   **/

  static int objectType(Object value) {
    if (value instanceof String)
      return STRING;
    else if (value instanceof Boolean) // was BooleanValue
      return BOOLEAN;
    else if (value instanceof Number) //was NumericValue
      // NumericValue types are ordered INT..DOUBLE starting at zero.  Selector types are
      // ordered INT..DOUBLE starting at Selector.INT.
      return EvaluatorImpl.getType((Number) value) + Selector.INT; // was ((NumericValue) value).type()
    else if (value instanceof Serializable)
      return OBJECT;
    else
      return INVALID;
  }


  /** Construct a Literal from a value
   *
   * @param value the value to store in the literal
   **/

  public LiteralImpl(Object value) {
    this.value = value;
    type = objectType(value);
  }


  /** Create a Literal during decoding of a byte[] form (used only by Selector.decode).
   **/

  public LiteralImpl(ObjectInput handle) throws ClassNotFoundException, IOException {
    type = handle.readByte();
    switch (type) {
    case FLOAT:
      value = new Float(handle.readFloat()); // was NumericValue
      return;
    case DOUBLE:
      value = new Double(handle.readDouble()); // was NumericValue
      return;
    case INT:
      value = new Integer(handle.readInt()); // was NumericValue
      return;
    case LONG:
      value = new Long(handle.readLong()); // was NumericValue
      return;
    case STRING:
      value = handle.readUTF();
      return;
    case BOOLEAN:
      value = Boolean.valueOf(handle.readBoolean()); // was BooleanValue
      return;
    case OBJECT:
      value = handle.readObject();
    default:
      type = INVALID;
      return;
    }
  }


  /** Encode this Literal into an ObjectOutput (used only by Selector.encode). */

  public void encodeSelf(ObjectOutput buf) throws IOException {
    buf.writeByte((byte) type);
    switch (type) {
    case STRING:
      buf.writeUTF((String) value);
      return;
    case LONG:
      buf.writeLong(((Long) value).longValue()); // was NumericValue
      return;
    case DOUBLE:
      buf.writeDouble(((Double) value).doubleValue()); // was NumericValue
      return;
    case INT:
      buf.writeInt(((Integer) value).intValue()); // was NumericValue
      return;
    case FLOAT:
      buf.writeFloat(((Float) value).floatValue()); // was NumericValue
      return;
    case BOOLEAN:
      buf.writeBoolean(((Boolean) value).booleanValue()); // was BooleanValue
      return;
    case OBJECT:
      buf.writeObject(value);
    default:
      throw new IllegalStateException();
    }
  }


  // Overrides

  public boolean equals(Object o) {
    if (o instanceof Literal && super.equals(o))
      return ((Literal) o).getValue().equals(value);
    else
      return false;
  }

  public int hashCode() {
    return value.hashCode();
  }

  public String toString() {
    if (type == STRING)
      return "'" + value + "'";
    else
      return value.toString();
  }

  /**
   * Returns the value.
   * @return Object
   */
  public Object getValue() 
  {
	return value;
  }

}
