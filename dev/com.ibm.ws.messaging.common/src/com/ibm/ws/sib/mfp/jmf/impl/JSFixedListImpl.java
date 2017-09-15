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

import com.ibm.ws.sib.mfp.jmf.JMFException;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFMessageData;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * A completion of JSListImpl to provide the behavior of lists of fixed length elements.
 * These include
 *
 * <ol><li>Elements of JSPrimitive type where the JSBaseTypes table provides a fixed
 * length encoding.
 *
 * <li>Elements of JSEnum type, which have an integer encoding.
 *
 * Other elements of JSPrimitive type, elements of JSDynamic type, and all elements of
 * JSVariant type (which are necessarily boxed variants, since they appear in a list) are
 * of varying length and are not handled by this subclass.
 */

public class JSFixedListImpl extends JSListImpl {
  private static TraceComponent tc = JmfTr.register(JSFixedListImpl.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  //////////////////////////////////////////////////////////////////////////////
  // Private static inner class JSFixedListImpl$Empty
  //////////////////////////////////////////////////////////////////////////////

  /**
   * JSFixedList$Empty is a subclass of JSFixedListImpl which can be used
   * to represent empty instances of a JSFixedListImpl.
   * It must NEVER have any content, hence the set methods are overridden to throw Exceptions.
   * If a List is to be properly accessed, it should have been replaced by a
   * 'real' instance by beforehand.
   * A lazyCopy MUST return itself, and NOT a copy of it, so getCopy() is overridden.
   */
  private static class Empty extends JSFixedListImpl {
    private static TraceComponent tcE = JmfTr.register(JSFixedListImpl.Empty.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

    private Empty() throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
      super(null, null);
    }
    // Override of the getCopy method - MUST return itself
    JSMessageData getCopy() {
      return this;
    }
    // Override of the setParent method - we don't want to set any of the relevant fields
    // as this singleton doesn't really belong to anything.
    // It is 'safer' to leave them as null, because then we'll notice pretty fast
    // if the values are being used!
    void setParent(JMFMessageData parent) {
      if (TraceComponent.isAnyTracingEnabled() && tcE.isEntryEnabled()) JmfTr.entry(this, tcE, "setParent", parent);
      if (TraceComponent.isAnyTracingEnabled() && tcE.isEntryEnabled()) JmfTr.exit(this, tcE, "setParent");
    }
    // Override of getValue/setValue
    public Object getValue(int index) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
      JMFUninitializedAccessException ex = new JMFUninitializedAccessException("getValue must not be called against JSFixedListImpl$Empty");
      FFDCFilter.processException(ex, "com.ibm.ws.sib.mfp.jmf.impl.JSFixedListImpl$Empty.getValue", "95", this);
      throw ex;
    }
    public void setValue(int index, Object value) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
      JMFUninitializedAccessException ex = new JMFUninitializedAccessException("setValue must not be called against JSFixedListImpl$Empty");
      FFDCFilter.processException(ex, "com.ibm.ws.sib.mfp.jmf.impl.JSFixedListImpl$Empty.setValue", "99", this);
      throw ex;
    }
    // Override of List methods
    public Object get(int i) {
      ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException();
      FFDCFilter.processException(ex, "com.ibm.ws.sib.mfp.jmf.impl.JSFixedListImpl$Empty.get", "104", this);
      throw ex;
    }
    public Object set(int index, Object value) {
      ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException();
      FFDCFilter.processException(ex, "com.ibm.ws.sib.mfp.jmf.impl.JSFixedListImpl$Empty.set", "108", this);
      throw ex;
    }
  };

  //////////////////////////////////////////////////////////////////////////////
  // JSFixedListImpl
  //////////////////////////////////////////////////////////////////////////////

  /**
   * EMPTY_FIXEDLIST
   * Is a singleton JSFixedListImpl which can be used to represent all empty
   * instances of a JSFixedListImpl.
   * If a List is to be properly accessed, it should have been replaced by a
   * 'real' instance by beforehand.
   */
  static final JSFixedListImpl EMPTY_FIXEDLIST;
  static {
    JSFixedListImpl jsfi = null;
    try {
      jsfi = new Empty();
    }
    catch (JMFException jmfe) {
      FFDCFilter.processException(jmfe, "com.ibm.ws.sib.mfp.jmf.impl.JSListImpl.<clinit>", "122");
      // Actually this can't happen!
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(tc, "Exception:" + jmfe + " initializing EMPTY_FIXEDLIST");
    }

    // Set the final static to whatever we have created (or null if it failed)
    EMPTY_FIXEDLIST = jsfi;
  }

  // Keep the element length
  private int elemLen;

  /**
   * Construct a new empty JSFixedListImpl for a particular JSField
   *
   * @param element the JSField object defining the element type of the array
   * @param values the contents of the JMFList as a Collection or array (the elements
   * of the Collection may be arrays and vice versa down to the base element type, which
   * must be correct).
   */
  public JSFixedListImpl(JSField element, Object values)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    super(element, 0, values, false);
    init();
  }

  /**
   * Construct a new JSFixedListImpl from a portion of a message byte array
   *
   * @param contents the byte array containing the portion being mapped into this list
   * @param offset the offset within the byte array containing the portion being mapped
   * @param element the JSField object defining the element type of the array
   */
  public JSFixedListImpl(byte[] contents, int offset, JSField element) throws JMFMessageCorruptionException {
    super(contents, offset, element, 0);
    init();
  }

  // Copy constructor.  This constructs a new JSFixedListImpl from an existing one.
  private JSFixedListImpl(JSFixedListImpl original) {
    super(original);
    elemLen = original.elemLen;
  }

  // Initialize the element length information
  private void init() {
    if (element instanceof JSPrimitive)
      elemLen = ((JSPrimitive)element).getLength();
    else
      elemLen = 4; /* true of JSEnum; no others should occur here */
  }

  // Implementation of getAbsoluteOffset method
  int getAbsoluteOffset(int index) {
    return offset + elemLen * index;
  }

  // Implementation of isFieldVarying method
  boolean isFieldVarying(int index) {
    return false;
  }

  // The encodeOffsetTable method is a no-op
  int encodeOffsetTable(byte[] frame, int next) {
    return next;
  }

  // Implementation of computeLengthFromCache method
  int computeLengthFromCache() {
    return elemLen * cacheSize;
  }

  // Implementation of the getCopy method
  JSMessageData getCopy() {
    // Create a new instance of ourselves
    JSFixedListImpl copy = new JSFixedListImpl(this);
    // Allow the message data to be lazy-copied
    copy.lazyCopy(this);
    return copy;
  }
}
