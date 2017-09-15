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

import java.util.AbstractList;
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.jmf.JMFException;
import com.ibm.ws.sib.mfp.jmf.JMFList;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFNativePart;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;

/**
 * An implementation of JMFList that represents information within a variant box.
 *
 * <p>Every JSBoxedListImpl is paired with a JSVaryingList that contains the actual
 * JMFNativePart representations of the individual variant boxes.  It is this lower
 * "hidden" list that is cached in the primary cache of the JSMessageData that will
 * return a JSBoxedListImpl.
 *
 * <p>It is the JSBoxedListImpl, however, that is seen by all higher layers of the system.
 * Each JSBoxedListImpl keeps a 'subaccessor' which represents its particular 'slice' of
 * the information in the variant box.
 *
 * <p>Although every boxed variant is, by definition, inside a list, it is possible that
 * it is actually inside a list of lists or list of list of lists (etc).  These higher
 * order cases are handled not by this class but by JSIndirectBoxedListImpl.
 */

class JSBoxedListImpl extends AbstractList implements JMFList {
  private static TraceComponent tc = JmfTr.register(JSBoxedListImpl.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

 
  // Store the JMFList to which we will delegate each call.  The element type of the
  // subList is always JSVariant (boxed) and each one is represented by a JMFNativePart
  JMFList subList;

  // Store the accessor that is to be used in every delegated call
  int subAccessor;

  // Constructor used when getting (by this we mean getting the list as a whole; the list
  // can still be mutated after the get).
  JSBoxedListImpl(JMFList subList, int subAccessor) {
    this.subList = subList;
    this.subAccessor = subAccessor;
  }

  // Constructor used when setting
  JSBoxedListImpl(JMFList subList, int subAccessor, Object val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    this(subList, subAccessor);
    int size = JSListImpl.getSize(val);
    if (size != subList.size())
      throw new JMFSchemaViolationException("List value does not match expected box size");
    Iterator iter = JSListImpl.getIterator(val);
    for (int i = 0; i < size; i++) {
      Object item = iter.next();
      if (item != JMFList.MISSING)
         ((JMFNativePart)subList.getValue(i)).setValue(subAccessor, item);
    }
  }

  // Static creation methods that make either a JSBoxedListImpl or a
  // JSIndirectBoxedListImpl according to the indirection of the argument.  One for each
  // kind of constructor.
  static JSBoxedListImpl create(JSVaryingList subList, int subAccessor) {
    if (subList.getIndirection() > 0)
      return new JSIndirectBoxedListImpl(subList, subAccessor);
    else
      return new JSBoxedListImpl(subList, subAccessor);
  }

  static JSBoxedListImpl create(JSVaryingList subList, int subAccessor, Object val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    if (subList.getIndirection() > 0)
      return new JSIndirectBoxedListImpl(subList, subAccessor, val);
    else
      return new JSBoxedListImpl(subList, subAccessor, val);
  }

  // Delegated methods.  We count on the fact that getValue directed at a boxed
  // variant list will never return null because boxed variants are not allowed to be null
  // (if, due to some error, they are not present, UninitializedAccessException will be
  // thrown).
  public Object get(int accessor) {
    try {
      return ((JMFNativePart)subList.getValue(accessor)).getValue(subAccessor);
    } catch (JMFException ex) {
      FFDCFilter.processException(ex, "get", "129", this);
      return null;
    }
  }

  public int size() {
    return subList.size();
  }

  public Object set(int accessor, Object value) {
    try {
      JMFNativePart elem = (JMFNativePart)subList.getValue(accessor);
      Object ans = null;
      if (elem.isPresent(subAccessor))
        ans = elem.getValue(subAccessor);
      elem.setValue(subAccessor, value);
      return ans;
    } catch (JMFException ex) {
      FFDCFilter.processException(ex, "set", "147", this);
      return null;
    }
  }

  public Object getValue(int accessor)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    return ((JMFNativePart)subList.getValue(accessor)).getValue(subAccessor);
  }

  public boolean getBoolean(int accessor)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    return ((JMFNativePart)subList.getValue(accessor)).getBoolean(subAccessor);
  }

  public byte getByte(int accessor)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    return ((JMFNativePart)subList.getValue(accessor)).getByte(subAccessor);
  }

  public short getShort(int accessor)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    return ((JMFNativePart)subList.getValue(accessor)).getShort(subAccessor);
  }

  public char getChar(int accessor)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    return ((JMFNativePart)subList.getValue(accessor)).getChar(subAccessor);
  }

  public int getInt(int accessor)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    return ((JMFNativePart)subList.getValue(accessor)).getInt(subAccessor);
  }

  public long getLong(int accessor)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    return ((JMFNativePart)subList.getValue(accessor)).getLong(subAccessor);
  }

  public float getFloat(int accessor)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    return ((JMFNativePart)subList.getValue(accessor)).getFloat(subAccessor);
  }

  public double getDouble(int accessor)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    return ((JMFNativePart)subList.getValue(accessor)).getDouble(subAccessor);
  }

  public boolean isPresent(int accessor)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    return ((JMFNativePart)subList.getValue(accessor)).isPresent(subAccessor);
  }

  public void setValue(int accessor, Object val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    ((JMFNativePart)subList.getValue(accessor)).setValue(subAccessor, val);
  }

  public void setBoolean(int accessor, boolean val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    ((JMFNativePart)subList.getValue(accessor)).setBoolean(subAccessor, val);
  }

  public void setByte(int accessor, byte val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    ((JMFNativePart)subList.getValue(accessor)).setByte(subAccessor, val);
  }

  public void setShort(int accessor, short val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    ((JMFNativePart)subList.getValue(accessor)).setShort(subAccessor, val);
  }

  public void setChar(int accessor, char val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    ((JMFNativePart)subList.getValue(accessor)).setChar(subAccessor, val);
  }

  public void setInt(int accessor, int val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    ((JMFNativePart)subList.getValue(accessor)).setInt(subAccessor, val);
  }

  public void setLong(int accessor, long val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    ((JMFNativePart)subList.getValue(accessor)).setLong(subAccessor, val);
  }

  public void setFloat(int accessor, float val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    ((JMFNativePart)subList.getValue(accessor)).setFloat(subAccessor, val);
  }

  public void setDouble(int accessor, double val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    ((JMFNativePart)subList.getValue(accessor)).setDouble(subAccessor, val);
  }

  public JMFNativePart getNativePart(int accessor, JMFSchema schema)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
      return ((JMFNativePart)subList.getValue(accessor)).getNativePart(subAccessor, schema);
  }

  public int getModelID(int accessor)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    return ((JMFNativePart)subList.getValue(accessor)).getModelID(subAccessor);
  }

  public void unassemble() {
    // Should not be called
    throw new UnsupportedOperationException();
  }

  // Implement the JMFMessageData.estimateUnassembledValueSize() method.
  // Just return 0 as we don't think we ever use Boxed Lists (& we certainly hope we don't).
  public int estimateUnassembledValueSize(int index) {
    return 0;
  }
}
