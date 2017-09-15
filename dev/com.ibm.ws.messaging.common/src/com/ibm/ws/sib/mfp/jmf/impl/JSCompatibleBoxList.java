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

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.jmf.JMFException;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFNativePart;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;

/**  A delegator to JSVaryingListImpl for the following special case: the JSVaryingListImpl
 * contains JSMessageImpls and implements a list of variant boxes (perhaps indirectly).
 * This delegator ensures that any JSMessageImpl exposed by a get call has a
 * JSCompatibleMessageImpl wrapped around it.
 */
class JSCompatibleBoxList extends AbstractList implements JSVaryingList {
  // The box list wrapped by this delegator
  private JSVaryingListImpl boxList;

  // The element type.
  private JSVariant element;

  // Create a new JSCompatibleBoxList
  JSCompatibleBoxList(JSVariant element, JSVaryingListImpl boxList) {
    this.element = element;
    this.boxList = boxList;
  }

  // Only get, getValue, isPresent, size(), and unassemble are supported on variant
  // box lists.  All mutating operations are illegal and so are the more specific get
  // operations.
  public boolean getBoolean(int accessor) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }
  public byte getByte(int accessor) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }
  public char getChar(int accessor) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }
  public short getShort(int accessor) throws JMFSchemaViolationException {
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
  public void setChar(int accessor, char val) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }
  public void setShort(int accessor, short val) throws JMFSchemaViolationException {
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
  public void setValue(int accessor, Object val) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }
  public int getModelID(int accessor) throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }
  public JMFNativePart getNativePart(int accessor, JMFSchema schema)
      throws JMFSchemaViolationException {
    throw new JMFSchemaViolationException("Operation not supported for boxed variants");
  }

  // Implement get via getValue
  public Object get(int index) {
    try {
      return getValue(index);
    } catch (JMFException e) {
      FFDCFilter.processException(e, "get", "125", this);
      return null;
    }
  }

  // Implement getValue.  We first delegate the get, then wrap the result.
  public Object getValue(int index) throws JMFSchemaViolationException,
      JMFModelNotImplementedException, JMFMessageCorruptionException,
      JMFUninitializedAccessException {
    Object ans = boxList.getValue(index);
    if (ans instanceof JSMessageImpl)
      return new JSCompatibleMessageImpl((JSchema) element.getBoxed(), (JSMessageImpl) ans);
    else
      return new JSCompatibleBoxList(element, (JSVaryingListImpl) ans);
  }

  // Delegate size
  public int size() {
    return boxList.size();
  }

  // Delegate isPresent
  public boolean isPresent(int accessor) {
    return boxList.isPresent(accessor);
  }

  // Delegate unassemble
  public void unassemble()
    throws
      JMFSchemaViolationException,
      JMFModelNotImplementedException,
      JMFMessageCorruptionException,
      JMFUninitializedAccessException {
    boxList.unassemble();
  }

  // Delegate getIndirection
  public int getIndirection() {
    return boxList.getIndirection();
  }

  // Implement getElementType
  public JSField getElementType() {
    return element;
  }

  // Implement the JMFMessageData.estimateUnassembledValueSize() method.
  // Just return 0 as we don't think we ever use Boxed Lists (& we certainly hope we don't).
  public int estimateUnassembledValueSize(int index) {
    return 0;
  }
}
