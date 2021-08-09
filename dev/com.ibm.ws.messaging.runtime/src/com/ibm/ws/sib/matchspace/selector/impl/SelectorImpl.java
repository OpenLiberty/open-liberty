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

import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.utils.FFDC;
/** A Selector tree represents a content selector in a form convenient for checking,
 * transformation, and usage in MatchSpace.  A variety of surface syntaxes can be parsed
 * into Selector trees.
 **/

public abstract class SelectorImpl implements Selector, Cloneable
{
  private static final Class cclass = SelectorImpl.class;
  
  public int type;

  /** The uniqueId of this node, if one has been assigned by execution of the intern
   * method.  This field is for use by MatchSpace or any other "hosting" component of the
   * Evaluator so that the Evaluator doesn't evaluate common subexpressions redundently.
   * If the field is set, it must be set uniquely across all the Selector trees that will
   * be evaluated under a single MatchSpaceKey.  This will be true if the same context was
   * passed to the intern method for all such trees.  If uniqueId == 0 the Evaluator will
   * ignore it.
   **/

  public int uniqueId;

  // A reference count used by intern/remove

  private int refCount;

  /** The number of Identifiers in this Selector subtree.  The Identifier subclass sets
   * this field to 1, the Literal subclass sets it to 0, and the Operator subclass sets it
   * to the sum of the values for its operands.
   **/

  public int numIds;

  /** Returns true if the type is consistent with a boolean context, false otherwise.
   * Sets the type to BOOLEAN if it is UNKNOWN.
   **/

  protected boolean extended;
  
  public boolean mayBeBoolean()
  {
    if (type == UNKNOWN)
      type = BOOLEAN;
    return type == BOOLEAN;
  }

  /** Returns true if the type is consistent with a String context, false otherwise.  Sets
   * the type to STRING if it is UNKNOWN.
   **/

  public boolean mayBeString()
  {
    if (type == UNKNOWN)
      type = STRING;
    return type == STRING;
  }

  /** Returns true if the type is consistent with a numeric context, false otherwise.
   * Sets the type to NUMERIC if it is UNKNOWN.
   **/

  public boolean mayBeNumeric()
  {
    if (type == UNKNOWN)
      type = NUMERIC;
    return type == NUMERIC || (type >= INT && type <= DOUBLE);
  }
  
  // Implement mayBeObject
  public boolean mayBeObject() {
    if (type == UNKNOWN)
      type = OBJECT;
    return type == OBJECT;
  }

  /** Replaces this Selector tree with an equal() Selector tree that is "uniquely
   * identified".  
   * 
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

  public Selector intern(InternTable table)
  {
    // This implementation is inherited by Identifier and Literal which have no children,
    // but is overridden in Operator, which does.
    Selector me = (Selector) table.get(this);
    if (me != null)
    {
      me.incRefCount();
      return me;
    }
    else
    {
      uniqueId = table.getNextUniqueId();
      refCount++;
      table.put(this, this);
      return this;
    }
  }

  /** Removes this Selector from an InternTable
   *
   * @param table the InternTable context from which this Selector should be removed
   **/

  public void unintern(InternTable table)
  {
    // This implementation is inherited by Identifier and Literal which have no children,
    // but is overridden in Operator, which does.
    refCount--;
    if (refCount < 0)
      throw new IllegalStateException();
    if (refCount == 0)
    {
      Object res = table.remove(this);
      if (res == null)
        throw new IllegalStateException();
    }
  }

  /** The decode method turns an ObjectInput into a Selector tree.
   *
   * @param buf the DataInput containing the encoding of the tree
   *
   * @exception IOException when presented with an illformed encoding or other error
   **/

  public static Selector decode(ObjectInput buf) throws IOException
  {
    if (buf.readByte() != VERSION)
      throw new IOException();
    try
    {
      return decodeSubtree(buf);
    }
    catch (Exception e)
    {
      // No FFDC Code Needed.
      // FFDC driven by wrapper class.      
      FFDC.processException(cclass,
          "com.ibm.ws.sib.matchspace.selector.impl.Selector.decode",
          e,
          "1:191:1.19");
//TODO: include buf as parameter               
      throw new IllegalArgumentException();
    }
  }

  // The decodeNode method decodes a subtree (which may be the entire tree, after the
  // version byte has been stripped off).  It is called as a subroutine by decode and by
  // the constructor of Operator (to decode operands).

  static Selector decodeSubtree(ObjectInput buf) throws ClassNotFoundException, IOException
  {
    int type = buf.readByte();
    if (type < IDENTIFIER)
      return new LiteralImpl(buf);
    else
      if (type == IDENTIFIER)
        return new IdentifierImpl(buf);
      else
        if (type == LIKE)
          return new LikeOperatorImpl(buf);
        else
          return new OperatorImpl(buf);
  }

  /** The encode method serializes a Selector tree into DataOutput */

  public void encode(ObjectOutput buf) throws IOException
  {
    buf.writeByte(VERSION);
    encodeSelf(buf);
  }

  // Each of the subclasses Literal, Identifier, and Operator must implement the length
  // encodeSelf method

  // abstract void encodeSelf(DataOutput buf) throws IOException;

  // Overrides

  public boolean equals(Object o)
  
  {
    if (o instanceof Selector)
    {
      Selector s = (Selector) o;
      return type == s.getType() && numIds == s.getNumIds();
    }
    return false;
  }

  public Object clone()
  {
    try
    {
      return super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      // No FFDC Code Needed.
      // FFDC driven by wrapper class.      
      FFDC.processException(this, cclass,
          "com.ibm.ws.sib.matchspace.selector.impl.Selector.clone",
          e,
          "1:255:1.19");      
      // Doesn't happen, since we implement Cloneable
      throw new IllegalStateException();
    }
  }

  /**
   * Returns the numIds.
   * @return int
   */
  public int getNumIds() 
  {
  return numIds;
  }

  /**
   * Returns the refCount.
   * @return int
   */
  public int getRefCount() 
  {
  return refCount;
  }

  /**
   * Returns the type.
   * @return int
   */
  public int getType() 
  {
  return type;
  }

  /**
   * Increments the refCount.
   */
  public void incRefCount() 
  {
  this.refCount++;
  }

  /**
   * Sets the type.
   * @param type The type to set
   */
  public void setType(int type) 
  {
  this.type = type;
  }

  /**
   * Returns the uniqueId.
   * @return int
   */
  public int getUniqueId() {
  return uniqueId;
  }

  /**
   * Sets the uniqueId.
   * @param uniqueId The uniqueId to set
   */
  public void setUniqueId(int uniqueId) 
  {
  this.uniqueId = uniqueId;
  }

  
  /**
   * By default the Selector is not extended
   */
  public boolean isExtended()
  {
    return extended;
  }
  
  public void setExtended()
  {
    extended = true;
  }
}
