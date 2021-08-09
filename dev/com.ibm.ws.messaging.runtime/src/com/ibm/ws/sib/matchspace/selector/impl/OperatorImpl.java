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

import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.impl.Matching;
import com.ibm.ws.sib.matchspace.Operator;
import com.ibm.ws.sib.matchspace.Selector;

/** This class represents one Operator in a Selector expression */

public class OperatorImpl extends SelectorImpl implements Operator {

  /** Code indicating the specific Operator that is intended.  One of NOT, NEG, ISNULL,
   * LIKE, NE, GT, LT, GE, LE, EQ, AND, OR, PLUS, MINUS, TIMES, or DIV.  These are defined
   * in the Selector class rather than the Operator class so that uniqueness necessary for
   * encoding can be maintained in one place.  If op < Selector.FIRST_BINARY the operator
   * is unary.  Otherwise, it is binary.
   **/

  public int op;


  /** The operands of this Operator (other than the pattern and escape operands of the
   * LIKE operator, which is handled via a subclass).
   **/

  public Selector[] operands;

  public OperatorImpl(){};
  
  /** Make a new unary operator
   *
   * @param op the op code.  Must be one of NOT, NEG, ISNULL, or LIKE (not checked)
   *
   * @param op1 the sole operand
   **/

  public OperatorImpl(int op, Selector op1) {
    this.op = op;
    this.operands = new Selector[] { op1 };
    assignType();
    if (type == INVALID)
      return;
    numIds = op1.getNumIds();
    if (numIds == 0 && op1 instanceof Operator)
      operands[0] = new LiteralImpl(Matching.getEvaluator().eval(op1));
  }


  /** Make a new binary operator
   *
   * @param op the op code.  Must be one of NE, GT, LT, GE, LE, EQ, AND, OR, PLUS, MINUS,
   * TIMES, or DIV (not checked).
   *
   * @param op1 the first operand
   *
   * @param op2 the second operand
   **/

  public OperatorImpl(int op, Selector op1, Selector op2) {
    this.op = op;
    this.operands = new Selector[] { op1, op2 };
    assignType();
    if (type == INVALID)
      return;
    if (op1.getNumIds() == 0 && op1 instanceof Operator)
      operands[0] = new LiteralImpl(Matching.getEvaluator().eval(op1));
    if (op2.getNumIds() == 0 && op2 instanceof Operator)
      operands[1] = new LiteralImpl(Matching.getEvaluator().eval(op2));
    numIds = op1.getNumIds() + op2.getNumIds();
  }


  /** Create an Operator during decoding of a byte[] form (used only by Selector.decode).
   **/

public   OperatorImpl(ObjectInput buf) throws ClassNotFoundException, IOException {
    op = buf.readByte();
    operands = new SelectorImpl[(op >= Selector.FIRST_BINARY) ? 2 : 1];
    for (int i = 0; i < operands.length; i++)
      operands[i] = SelectorImpl.decodeSubtree(buf);
    assignType();
    if (type == INVALID)
      return;
    for (int i = 0; i < operands.length; i++) {
      Selector operand = operands[i];
      if (operand.getNumIds() == 0 && operand instanceof Operator)
        operands[i] = new LiteralImpl(Matching.getEvaluator().eval(operand));
      numIds += operand.getNumIds();
    }
  }


  /** Assign the appropriate type to an operator based on its op code and operands.  Also
   * assigns a type to operands of previously UNKNOWN type if a more precise type can be
   * inferred.  Called as a subroutine of the constructors and also by Transformer.resolve
   * after an operand has a type assigned by the Resolver.
   **/

  public void assignType() {
    switch (op) {
    case Selector.NOT:
      type = operands[0].mayBeBoolean() ? BOOLEAN : INVALID;
      break;
    case Selector.NEG:
      type = operands[0].mayBeNumeric() ? operands[0].getType() : INVALID;
      break;
    case Selector.ISNULL:
      if (operands[0] instanceof Identifier)
        type = BOOLEAN;
      else
        type = INVALID;
      break;
    case Selector.LIKE:
      type = operands[0].mayBeString() ? BOOLEAN : INVALID;
      break;
    case Selector.TOPIC_LIKE:
      // For this operator, the operand must be explicitly set to TOPIC:  no inference
      type = operands[0].getType() == TOPIC ? BOOLEAN : INVALID;
      break;
    case Selector.NE:
    case Selector.EQ:
      int type0 = operands[0].getType();
      int type1 = operands[1].getType();
      if (type0 >= INT && type0 <= DOUBLE)
        type0 = NUMERIC;
      if (type1 >= INT && type1 <= DOUBLE)
        type1 = NUMERIC;
      if (type0 == TOPIC)
        type0 = STRING;
      if (type1 == TOPIC)
        type1 = STRING;
      if (type0 == UNKNOWN)
      {
        type0 = type1;
        operands[0].setType(type1);
      }
      else if (type1 == UNKNOWN)
      {
        type1 = type0;
        operands[1].setType(type0);
      }
      type = (type0 == type1) ? BOOLEAN : INVALID;
      break;
    case Selector.GT:
    case Selector.LT:
    case Selector.GE:
    case Selector.LE:
      type = (operands[0].mayBeNumeric() && operands[1].mayBeNumeric()) ? BOOLEAN : INVALID;
      break;
    case Selector.PLUS:
    case Selector.MINUS:
    case Selector.TIMES:
    case Selector.DIV:
      if (!operands[0].mayBeNumeric() || !operands[1].mayBeNumeric()) {
        type = INVALID;
        break;
      }
      // At this point, both operands have either NUMERIC or a more specific type.  The
      // numeric types are ordered so that the following inequality-based test follows the
      // rules of binary numeric promotion, unless either type is simply NUMERIC, in which
      // case the answer will be NUMERIC.
      type = (operands[0].getType() > operands[1].getType()) ? operands[0].getType() : operands[1].getType();
      break;
    case Selector.AND:
    case Selector.OR:
      type = (operands[0].mayBeBoolean() && operands[1].mayBeBoolean()) ? BOOLEAN : INVALID;
      break;
    default:
      type = INVALID;
      break;
    }
  }


  /** Encode this Operator into a ObjectOutput (used only by Selector.encode). */

  public void encodeSelf(ObjectOutput buf) throws IOException {
    buf.writeByte((byte) op);
    for (int i = 0; i < operands.length; i++)
      operands[i].encodeSelf(buf);
  }


  /** Override intern and unintern to deal with children
   */
  public Selector intern(InternTable table) {
    for (int i = 0; i < operands.length; i++)
      operands[i] = operands[i].intern(table);
    return super.intern(table);
  }

  public void unintern(InternTable table) {
    for (int i = 0; i < operands.length; i++)
      operands[i].unintern(table);
    super.unintern(table);
  }


  // Object Overrides

  public boolean equals(Object o) {
    if (o instanceof Operator && super.equals(o)) {
      Operator other = (Operator) o;
      if (op != other.getOp())
        return false;
      for (int i = 0; i < operands.length; i++)
        if (!operands[i].equals(other.getOperands()[i]))
          return false;
      return true;
    }
    else
      return false;
  }


  public int hashCode() {
    if (hashCodeCached)
      return hashcode;
    int ans = op;
    for (int i = 0; i < operands.length; i++)
      ans = (ans << 9) + operands[i].hashCode();
    hashCodeCached = true;
    hashcode = ans;
    return ans;
  }
  private int hashcode;
  private boolean hashCodeCached;


  public Object clone() {
    Operator result = (Operator) super.clone();
    for (int i = 0; i < operands.length; i++)
      result.setOperand(i,(Selector) operands[i].clone());
    return result;
  }

  public String toString() {
    String stringOp = null;
    int prec = precedence(this);
    String op1 = operands[0].toString();
    if (precedence(operands[0]) > prec)
      op1 = "(" + op1 + ")";
    String op2 = null;
    if (operands.length > 1) {
      op2 = operands[1].toString();
      if (precedence(operands[1]) > prec)
        op2 = "(" + op2 + ")";
    }
    switch (op) {
    case Selector.NOT:
      return "NOT " + op1;
    case Selector.NEG:
      return "-" + op1;
    case Selector.ISNULL:
      return op1 + " IS NULL";
    case Selector.NE:
      stringOp = "<>";
      break;
    case Selector.GT:
      stringOp = ">";
      break;
    case Selector.LT:
      stringOp = "<";
      break;
    case Selector.GE:
      stringOp = ">=";
      break;
    case Selector.LE:
      stringOp = "<=";
      break;
    case Selector.EQ:
      stringOp = "=";
      break;
    case Selector.AND:
      stringOp = " AND ";
      break;
    case Selector.OR:
      stringOp = " OR ";
      break;
    case Selector.PLUS:
      stringOp = "+";
      break;
    case Selector.MINUS:
      stringOp = "-";
      break;
    case Selector.TIMES:
      stringOp = "*";
      break;
    case Selector.DIV:
      stringOp = "/";
      break;
    default:
      throw new IllegalStateException();
    }
    return op1 + stringOp + op2;
  }


  // Subroutine of toString to handle operator precedence'

  private int precedence(Selector sel) {
    if (!(sel instanceof Operator))
      return 0;
    Operator oper = (Operator) sel;
    if (oper.getOp() < Operator.FIRST_BINARY)
      return 0;
    switch (oper.getOp()) {
    case Selector.TIMES:
    case Selector.DIV:
      return 1;
    case Selector.PLUS:
    case Selector.MINUS:
      return 2;
    case Selector.NE:
    case Selector.GT:
    case Selector.LT:
    case Selector.GE:
    case Selector.LE:
    case Selector.EQ:
      return 3;
    case Selector.AND:
      return 4;
    default:
      return 5;
    }
  }
  
  /**
   * Returns the op.
   * @return int
   */
  public int getOp() 
  {
  return op;
  }

  /**
   * Returns the operands.
   * @return Selector[]
   */
  public Selector[] getOperands() 
  {
  return operands;
  }

  /**
   * Sets the i'th operand.
   * @param operands The operands to set
   */
  public void setOperand(int i, Selector operand) 
  {
  this.operands[i] = operand;
  }

}
