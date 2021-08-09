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

import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.jmf.JMFException;
import com.ibm.ws.sib.mfp.jmf.JMFList;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFNativePart;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;

/**
 * An implementation of JMFList that represents information within a variant box, when
 * there is more than one level of List indirection around the information.  The
 * <b>JSBoxedListImpl</b> is used when there is only one level of list.
 */

final class JSIndirectBoxedListImpl extends JSBoxedListImpl {
  private static TraceComponent tc = JmfTr.register(JSIndirectBoxedListImpl.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // Cache subsidiary JSBoxedListImpls or JSIndirectBoxedListImpls
  private JSBoxedListImpl[] boxedCache;

  // Constructor used when getting
  JSIndirectBoxedListImpl(
    JMFList subList,
    int subAccessor) {
    super(subList, subAccessor);
    boxedCache = new JSBoxedListImpl[subList.size()];
  }

  // Constructor used when setting
  JSIndirectBoxedListImpl(JSVaryingList subList, int subAccessor, Object val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    this(subList, subAccessor);
    int size = JSListImpl.getSize(val);
    if (size != subList.size())
      throw new JMFSchemaViolationException("List value does not match expected box size");
    Iterator iter = JSListImpl.getIterator(val);
    for (int i = 0; i < size; i++) {
      Object item = iter.next();
      if (item != JMFList.MISSING) {
        JSVaryingList backing;
        if (subList.isPresent(i))
          backing = (JSVaryingList)subList.getValue(i);
        else {
          JSMessageData parent = null;
          if (subList instanceof JSMessageData)
            parent = (JSMessageData) subList;
          backing = new JSVaryingListImpl((JSVariant)subList.getElementType(), item,
            subList.getIndirection() - 1, parent);
          subList.setValue(i, backing);
        }
        boxedCache[i] = create(backing, subAccessor, item);
      }
    }
  }

  // The override of getValue (the most general get method) returns from the cache
  // if possible, otherwise constructs a new JSBoxedListImpl as appropriate.
  public Object getValue(int accessor)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    if (accessor < 0 || accessor >= boxedCache.length)
      throw new IndexOutOfBoundsException();
    JSBoxedListImpl ans = boxedCache[accessor];
    if (ans != null)
      return ans;
    boxedCache[accessor] =
      ans =
        create((JSVaryingList)subList.getValue(accessor), subAccessor);
    return ans;
  }

  // The override of isPresent simply delegates to subList to ask if the corresponding
  // element is present there.  If it is, it is guaranteed that we can wrap the result
  // correctly so getValue() would succeed.
  public boolean isPresent(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    return subList.isPresent(accessor);
  }

  // The override of setValue always makes a new JSBoxedListImpl and puts it in the cache.
  public void setValue(int accessor, Object val)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    // We rely on the fact that subList.getValue(accessor) must execute before any
    // assignment to boxedCache[accessor] so we get the index check for free.
    boxedCache[accessor] =
      create((JSVaryingList)subList.getValue(accessor), subAccessor, val);
  }

  // Other overridden methods.
  public Object get(int accessor) {
    try {
      return getValue(accessor);
    } catch (JMFException ex) {
      FFDCFilter.processException(ex, "get", "134", this);
      return null;
    }
  }

  public int size() {
    return boxedCache.length;
  }

  public Object set(int accessor, Object value) {
    try {
      Object ans = getValue(accessor);
      setValue(accessor, value);
      return ans;
    } catch (JMFException ex) {
      FFDCFilter.processException(ex, "set", "149", this);
      return null;
    }
  }

  public boolean getBoolean(int accessor) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public byte getByte(int accessor) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public short getShort(int accessor) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public char getChar(int accessor) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public int getInt(int accessor) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public long getLong(int accessor) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public float getFloat(int accessor) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public double getDouble(int accessor) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public void setBoolean(int accessor, boolean val) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public void setByte(int accessor, byte val) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public void setShort(int accessor, short val) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public void setChar(int accessor, char val) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public void setInt(int accessor, int val) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public void setLong(int accessor, long val) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public void setFloat(int accessor, float val) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public void setDouble(int accessor, double val) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public JMFNativePart getNativePart(int accessor, JSchema schema) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public int getModelID(int accessor) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  public void unassemble() {
    // Should not be called
    throw new UnsupportedOperationException();
  }
}
