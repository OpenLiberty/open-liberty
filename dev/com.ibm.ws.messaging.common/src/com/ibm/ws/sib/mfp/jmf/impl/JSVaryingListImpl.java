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

import com.ibm.ws.sib.mfp.jmf.JMFException;
import com.ibm.ws.sib.mfp.jmf.JMFList;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFMessageData;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.mfp.util.ArrayUtil;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * A completion of JSListImpl to provide the behavior of lists of varying length elements.
 * These include
 *
 * <ol><li>Elements of JSPrimitive type where the JSBaseTypes table provides a varying
 * length encoding.
 *
 * <li>Elements of JSDynamic type, regardless of whether they are JMF-encoded or
 * encapsulated, since the elements will be varying length in either case
 *
 * <li>Elements of JSVariant type.  In this case, the JSVaryingListImpl is used only
 * internally to manage the JSMessageImpls that represent each boxed variant for purposes
 * of access.  A JSBoxedListImpl is what is actually exposed to the application and that
 * implementation will delegate to this one to keep the boxed variants hidden.
 *
 * <li>Elements of JMFList type arising from the fact that a JSField is dominated by more
 * than one JSRepeated, leading to a "list of lists" situation.
 *
 * Other elements of JSPrimitive type and JSEnum type are not handled by this class
 * because they are of fixed length.
 */

public class JSVaryingListImpl extends JSListImpl implements JSVaryingList {
  private static TraceComponent tc = JmfTr.register(JSVaryingListImpl.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  //////////////////////////////////////////////////////////////////////////////
  // Private static inner class JSVaryingListImpl$Empty
  //////////////////////////////////////////////////////////////////////////////

  /**
   * JSVaryingList$Empty is a subclass of JSVaryingListImpl which can be used
   * to represent empty instances of an unboxed JSVaryingList.
   * It must NEVER have any content, hence the set methods are overridden to throw Exceptions.
   * If a List is to be properly accessed, it should have been replaced by a
   * 'real' instance by beforehand.
   * A lazyCopy MUST return itself, and NOT a copy of it, so getCopy() is overridden.
   */
  private static class Empty extends JSVaryingListImpl {
    private static TraceComponent tcE = JmfTr.register(JSVaryingListImpl.Empty.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

    private Empty() throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
      super(null, 0, null);
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
      JMFUninitializedAccessException ex = new JMFUninitializedAccessException("getValue must not be called against JSVaryingListImpl$Empty");
      FFDCFilter.processException(ex, "com.ibm.ws.sib.mfp.jmf.impl.JSVaryingListImpl.$Empty.getValue", "95", this);
      throw ex;
    }
    public void setValue(int index, Object value) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
      JMFUninitializedAccessException ex = new JMFUninitializedAccessException("setValue must not be called against JSVaryingListImpl$Empty");
      FFDCFilter.processException(ex, "com.ibm.ws.sib.mfp.jmf.impl.JSVaryingListImpl$Empty.setValue", "99", this);
      throw ex;
    }
    // Override of List methods
    public Object get(int i) {
      ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException();
      FFDCFilter.processException(ex, "com.ibm.ws.sib.mfp.jmf.impl.JSVaryingListImpl$Empty.get", "104", this);
      throw ex;
    }
    public Object set(int index, Object value) {
      ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException();
      FFDCFilter.processException(ex, "com.ibm.ws.sib.mfp.jmf.impl.JSVaryingListImpl$Empty.set", "108", this);
      throw ex;
    }
  };


  //////////////////////////////////////////////////////////////////////////////
  // JSVaryingListImpl
  //////////////////////////////////////////////////////////////////////////////

  /**
   * EMPTY_UNBOXED_VARYINGLIST
   * Is the singleton JSVaryingListImpl which can be used to represent all empty
   * instances of an unboxed JSVaryingList.
   * If a List is to be properly accessed, it should have been replaced by a
   * 'real' instance by beforehand.
   */
  static final JSVaryingListImpl EMPTY_UNBOXED_VARYINGLIST;
  static {
      JSVaryingListImpl jsvi = null;
    try {
      jsvi = new Empty();
    }
    catch (JMFException jmfe) {
      FFDCFilter.processException(jmfe, "com.ibm.ws.sib.mfp.jmf.impl.JSVaryingListImpl.<clinit>", "144");
      // Actually this can't happen!
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(tc, "Exception:" + jmfe + " initializing EMPTY_UNBOXED_VARYINGLIST");
    }

    // Set the final static to whatever we have created (or null if it failed)
    EMPTY_UNBOXED_VARYINGLIST = jsvi;
  }


  /**
   * Construct a new JSVaryingListImpl for a particular JSField with particular contents
   *
   * @param element the JSField object defining the base element type of the List.  This
   * means, actually, the non-List type that one would arrive at after stripping off all
   * "List of List of List..." wrappers.
   * @param indirect zero if this is a list of 'element' type, 1 if it is a list of list
   * of 'element' type, etc.
   * @param values the contents of the JMFList as a Collection or array (the elements of
   * the Collection may be arrays and vice versa down to the base element type, which
   * must be correct).
   */
  public JSVaryingListImpl(JSField element, int indirect, Object values)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    super(element, indirect, values, false);
  }

  /**
   * Construct a new JSVaryingListImpl from a portion of a message byte array
   *
   * @param contents the byte array containing the portion being mapped into this list
   * @param offset the offset within the byte array containing the portion being mapped
   * into this list
   * @param element the JSField object defining the base element type of the List.  This
   * means, actually, the non-List type that one would arrive at after stripping off all
   * "List of List of List..." wrappers.
   * @param indirect zero if this is a list of 'element' type, 1 if it is a list of list
   * of 'element' type, etc.
   */
  public JSVaryingListImpl(byte[] contents, int offset, JSField element, int indirect)
      throws JMFMessageCorruptionException {
    super(contents, offset, element, indirect);
  }

  /**
   * Construct a new uninitialized JSVaryingListImpl for a boxed JSVariant
   *
   * @param variant the JSVariant object defining the base element type of the List.  This
   * means, actually, the non-List type that one would arrive at after stripping off all
   * "List of List of List..." wrappers.
   * @param shape a Collection or array to use in determining the 'shape' of the
   * result.  In fact, the result will consist entirely of nested JSVaryingListImpl and
   * empty JSMessageImpl objects, where the number of levels of nesting depends on the
   * indirect argument.  The leaf objects of the shape argument (those that are not
   * array or Collection) are ignored.
   * @param indirect zero if this is a list of 'variant' type, 1 if it is a list of list
   * of 'variant' type, etc.
   * @param parent the parent of this JSVaryingListImpl
   */
  public JSVaryingListImpl(JSVariant variant, Object shape, int indirect, JSMessageData parent) throws JMFSchemaViolationException,
                                                                                                       JMFModelNotImplementedException,
                                                                                                       JMFUninitializedAccessException,
                                                                                                       JMFMessageCorruptionException {
    // Have to pass the shape to the super constructor, so it can pass it on to
    // use for setting cacheSize. We don't want JSListImpl's constructor to
    // populate the cache though, so pass in true as the last parameter.
    super(variant, indirect, shape, true);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "<init>", new Object[]{variant, shape, Integer.valueOf(indirect), parent});

    setParent(parent);
    if (cacheSize > 0) {
      cache = new Object[cacheSize];
    }
    if (indirect == 0) {
      JSchema boxSchema = (JSchema) variant.getBoxed();
      for (int i = 0; i < cacheSize; i++)
        setValue(i, new JSMessageImpl(boxSchema));
    } else {
      if (cacheSize > 0) {
        Iterator iter = getIterator(shape);
        for (int i = 0; iter.hasNext(); i++) {
          Object item = iter.next();
          if (item != JMFList.MISSING)
            setValue(i, new JSVaryingListImpl(variant, item, indirect - 1, this));
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "<init>");

  }

  // Copy constructor.  This constructs a new JSVaryingListImpl from an existing one.
  private JSVaryingListImpl(JSVaryingListImpl original) {
    super(original);
  }

  // Implementation of getAbsoluteOffset method
  int getAbsoluteOffset(int index) {
    return offset + ArrayUtil.readInt(contents, offset + index * 4);
  }

  // Implementation of isFieldVarying method
  boolean isFieldVarying(int index) {
    return true;
  }

  // Implementation of encodeOffsetTable method
  int encodeOffsetTable(byte[] frame, int next)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    int offset = 4 * cacheSize;
    for (int i = 0; i < cacheSize; i++) {
      Object elem = cache[i];
      if (elem == null)
        throw new JMFUninitializedAccessException("Value at accessor " + i + " is missing");
      ArrayUtil.writeInt(frame, next, offset);
      if (elem == nullIndicator)
        elem = null;
      offset += element.getEncodedValueLength(elem, indirect, master);
      next += 4;
    }
    return next;
  }

  // Implementation of computeLengthFromCache
  int computeLengthFromCache()
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    int ans = 4 * cacheSize; // length of offset table
    for (int i = 0; i < cacheSize; i++) {
      Object elem = cache[i];
      if (elem == null)
        throw new JMFUninitializedAccessException("Value at accessor " + i + " is missing");
      if (elem == nullIndicator)
        elem = null;
      ans += element.getEncodedValueLength(elem, indirect, master);
    }
    return ans;
  }

  // Implementation of the getCopy method
  JSMessageData getCopy() {
    // Create a new instance of ourselves
    JSVaryingListImpl copy = new JSVaryingListImpl(this);
    // Allow the message data to be lazy-copied
    copy.lazyCopy(this);
    return copy;
  }

  // Implement getIndirection
  public int getIndirection() {
    return indirect;
  }

  // Implement getElementType
  public JSField getElementType() {
    return element;
  }
}
