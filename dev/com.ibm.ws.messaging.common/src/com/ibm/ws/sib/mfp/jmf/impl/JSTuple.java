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

package com.ibm.ws.sib.mfp.jmf.impl;

import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import com.ibm.ws.sib.mfp.jmf.JMFType;
import com.ibm.ws.sib.mfp.jmf.JMFTupleType;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 * A representation of all tuple-like types in a Jetstream schema.  These correspond to
 * EClasses in Ecore or to XSDComplexType or XSDModelGroup (all or sequence) in XSD.  The
 * 'all' model group is distinguished from 'sequence' only by isSemistructured predicate,
 * which might also indicate mixed content, attribute wildcards or simply the
 * "semi-structured" annotation.  Note that all JSTuple nodes created via JSParser will
 * have isSemistructured set to false.  Those created from Ecore models will generally
 * have it false as well, but the "semi-structured" annotation can be used to indicate
 * otherwise.
 */
public final class JSTuple extends JSType implements JMFTupleType {
  private static TraceComponent tc = JmfTr.register(JSTuple.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // Records the fields of the tuple (it is permitted for there to be none, in which case
  // fields may either be null or the empty array).  However, if isSemistructured is true
  // there will be exactly two fields (one to represent the element content and declared
  // attribute content in strict schema order, and one a JSDynamic carrying the additional
  // information required to reconstruct instance order, text node content and/or wildcard
  // attributes).  Nothing in the com.ibm.jetstream.formats.schema package understands the
  // contents of the special JSDynamic field, which is a "JMF encoding detail."
  private JSType[] fields;

  // Record the dominated variants
  private JSVariant[] dominated;

  /**
   * Default constructor for public use
   */
  public JSTuple() {
  }

  /**
   * Get the number of fields in the tuple
   */
  public int getFieldCount() {
    return (fields == null) ? 0 : fields.length;
  }

  /**
   * Get the field at a particular position (the initial position is position 0)
   */
  public JMFType getField(int position) {
    if (position >= getFieldCount())
      throw new IllegalArgumentException("Invalid tuple field");
    return fields[position];
  }

  /**
   * Add a field to this tuple (a tuple need not have any fields).
   */
  public void addField(JMFType field) {
    if (field == null)
      throw new NullPointerException("Tuple field cannot be null");
    JSType newField = (JSType)field;
    if (fields == null)
      fields = new JSType[1];
    else {
      JSType[] oldFields = fields;
      fields = new JSType[oldFields.length + 1];
      System.arraycopy(oldFields, 0, fields, 0, oldFields.length);
    }
    newField.parent = this;
    newField.siblingPosition = fields.length - 1;
    fields[newField.siblingPosition] = newField;
  }

  // Set the multiChoiceCount for this tuple
  BigInteger setMultiChoiceCount() {
    if (fields != null)
      for (int i = 0; i < fields.length; i++)
        multiChoiceCount = multiChoiceCount.multiply(fields[i].setMultiChoiceCount());
    return multiChoiceCount;
  }

  /**
   * Identify any variants that are descendents of this tuple, either directly, or via
   * other tuples.  Used in MessageMap construction.
   */
  public JSVariant[] getDominatedVariants() {
    if (dominated == null) {
      List dom = new ArrayList();
      getDominatedVariants(dom);
      dominated = (JSVariant[])dom.toArray(new JSVariant[0]);
    }
    return dominated;
  }

  private void getDominatedVariants(List dom) {
    if (fields != null)
      for (int i = 0; i < fields.length; i++) {
        JSType field = fields[i];
        if (field instanceof JSVariant)
          dom.add(field);
        else if (field instanceof JSTuple)
           ((JSTuple)field).getDominatedVariants(dom);
      }
  }

  // Format as string (subroutine of toString)
  public void format(StringBuffer fmt, Set done, Set todo, int indent) {
    formatName(fmt, indent);
    fmt.append("[");
    if (fields != null) {
      fmt.append("\n");
      for (int i = 0; i < fields.length; i++) {
        if (i > 0)
          fmt.append(",\n");
        fields[i].format(fmt, done, todo, indent + 2);
      }
      fmt.append("\n");
      indent(fmt, indent);
    }
    fmt.append("]");
  }

  // Constructor from byte array form
  JSTuple(byte[] frame, int[] limits) {
    int count = JSType.getCount(frame, limits);
    fields = new JSType[count];
    for (int i = 0; i < count; i++) {
      fields[i] = JSType.createJSType(frame, limits);
      fields[i].parent = this;
      fields[i].siblingPosition = i;
    }
  }

  // Implementation of encodedTypeLength
  public int encodedTypeLength() {
    int ans = 3; // kind code for TUPLE plus the length
    if (fields != null)
      for (int i = 0; i < fields.length; i++)
        ans += fields[i].encodedTypeLength();
    return ans;
  }

  // Implementation of encodeType
  public void encodeType(byte[] frame, int[] limits) {
    JSType.setByte(frame, limits, (byte)JSType.TUPLE);
    JSType.setCount(frame, limits, getFieldCount());
    if (fields != null)
      for (int i = 0; i < fields.length; i++)
        fields[i].encodeType(frame, limits);
  }

  // Implementation of updateAssociations
  public void updateAssociations(JMFType type) {
    super.updateAssociations(type);
    if (getFieldCount() != ((JSTuple)type).getFieldCount())
      throw new IllegalStateException();
    if (fields != null)
      for (int i = 0; i < fields.length; i++)
         fields[i].updateAssociations(((JSTuple)type).getField(i));
  }
}
