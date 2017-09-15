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

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Collection;

import com.ibm.ws.sib.mfp.jmf.JMFList;
import com.ibm.ws.sib.mfp.jmf.JMFException;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.util.ArrayUtil;
import com.ibm.ws.sib.mfp.util.LiteIterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * The base JMFList implementation containing code that is common to both JSFixedListImpl
 * (for elements of fixed length) and JSVaryingListImpl (for elements of varying length).
 * The JSBoxedListImpl does not inherit from this implementation but is an independent
 * implementation that delegates to a JSVaryingListImpl underneath.
 */

public abstract class JSListImpl extends JSMessageData implements JMFList {
  private static TraceComponent tc = JmfTr.register(JSListImpl.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  
  // Starting offset within contents of the active portion.  Skips over the initial eight
  // byte prefix containing the length and size.
  int offset;

  // The length of the active portion not counting either prefix field.  Note that, by
  // convention, what is recorded in the length field includes the 4 bytes of the size
  // field, but that is subtracted out here.
  int length;

  // The type definition for elements of this List.  Note that an element type of
  // JSVariant is a special case.  It occurs (from boxed variants in the schema), but a
  // JSListImpl whose element type is a JSVariant (represented as a JSMessageImpl, since
  // the variant is boxed) is never exposed directly to the application: a JSBoxedListImpl
  // is always interposed.
  JSField element;

  /**
   * Construct a new JSListImpl for a particular JSField with particular contents
   *
   * @param element the JSField object defining the base element type of the List.  This
   * means, actually, the non-List type that one would arrive at after stripping off all
   * "List of List of List..." wrappers.
   * @param indirect zero if this is a list of 'element' type, 1 if it is a list of list
   * of 'element' type, etc.
   * @param values the contents of the JMFList as a Collection or array (the elements of
   * the Collection may be arrays and vice versa down to the base element type, which
   * must be correct).  If values is null, only the element and indirect parameters are
   * processed and the rest of initialization is left to the subclass.
   * @param boxed true if constructing a boxed JSVaryingListImpl, otherwise false
   */
  public JSListImpl(JSField element, int indirect, Object values, boolean boxed)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {

    // The super constructor sets cacheSize
    super(getSize(values));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "<init>", new Object[]{element, Integer.valueOf(indirect), values, Boolean.valueOf(boxed)});

    this.element = element;
    this.indirect = indirect;

    // We don't do this if we're constructing a boxed JSVaryingListImpl
    if (!boxed) {
      if (cacheSize > 0) {
        cache = new Object[cacheSize];
        Iterator iter = getIterator(values);
        int i = 0;
        while (iter.hasNext()) {
          Object val = iter.next();
          if (val != MISSING)
            setValue(i++, val);
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "<init>");
  }

  /**
   * Construct a new JSListImpl from a portion of a message byte array
   *
   * @param contents the byte array containing the portion being mapped into this list
   * @param offset the offset within the byte array containing the portion being mapped
   * into this list
   * @param element the JSField object defining the element type of the array
   * @param indirect zero if this is a list of 'element' type, 1 if it is a list of list
   * of 'element' type, etc.
   */
  public JSListImpl(byte[] contents, int offset, JSField element, int indirect)
      throws JMFMessageCorruptionException {

    // The super constructor sets cacheSize from the value in the buffer
    super(ArrayUtil.readInt(contents, offset + 4));    // 2nd int value in buffer

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "<init>", new Object[]{contents, Integer.valueOf(offset), element, Integer.valueOf(indirect)});

    this.contents = contents;
    this.offset = offset + 8;                          // skip over length & cacheSize
    this.element = element;
    this.indirect = indirect;
    length = ArrayUtil.readInt(contents, offset);      // 1st int value in buffer
    JSListCoder.sanityCheck(length, contents, offset);
    length -= 4; // original length includes cache size
    if (cacheSize > length) {
      // This is a conservative check but will catch flagrant corruption
      JMFMessageCorruptionException jmce =  new JMFMessageCorruptionException(
          "List size " + cacheSize + " at offset " + offset + " greater than length " + length);
      FFDCFilter.processException(jmce, "com.ibm.ws.sib.mfp.jmf.impl.JSListImpl.<init>", "149", this,
          new Object[] { MfpConstants.DM_BUFFER, contents, Integer.valueOf(0), Integer.valueOf(contents.length) });
      throw jmce;
    }
    if (cacheSize > 0) {
      cache = new Object[cacheSize];
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "<init>");
  }

  // Copy constructor.  This constructs a new JSListImpl from an existing one.
  JSListImpl(JSListImpl original) {

    // The super constructor sets cacheSize
    super(original.cacheSize);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "<init>", original);

    offset = original.offset;
    length = original.length;
    element = original.element;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "<init>");
  }

  // Implement the getFieldDef method required by superclass JSMessageData.  In a
  // list, if the message is assembled, then all fields are supposed to be there.  So,
  // this method merely has to return 'element.'
  JSField getFieldDef(int index, boolean mustBePresent) {
    return element;
  }

  // Implement the assembledForField method.  In a list, this method simply checks whether
  // the message is assembled, since, if it is assembled, it must be assembled for all
  // fields.  It never needs to unassemble the message.
  boolean assembledForField(int index) {
    return contents != null;
  }

  // Check that this List conforms to a particular type expectation and throw
  // SchemaViolationException if not
  public void checkType(JSField elem, int indir) throws JMFSchemaViolationException {
    if (!equivFields(element, elem) || indir != indirect)
      throw new JMFSchemaViolationException("Incorrect list element types");
  }

  // Check that two fields are equivalent for the purpose of checkType.  We do not want to
  // use a general-purpose equals() method for this, because the notion of equivalence
  // here is too specialized.
  private boolean equivFields(JSField one, JSField two) {
    if (one instanceof JSDynamic) {
      return two instanceof JSDynamic;
    }
    else if (one instanceof JSEnum) {
      return two instanceof JSEnum;
    }
    else if (one instanceof JSPrimitive) {
      return (two instanceof JSPrimitive)
        && ((JSPrimitive)one).getTypeCode() == ((JSPrimitive)two).getTypeCode();
    }
    else if (one instanceof JSVariant) {
      // we assume without checking that JSVariants in this context must be boxed
      return (two instanceof JSVariant)
        && ((JSVariant)one).getBoxed().getID() == ((JSVariant)two).getBoxed().getID();
    }
    else
      return false;
  }

  /**
   * Implement the List.get() method.  This is semantically identical to
   * JMFMessageData.getValue().
   */
  public Object get(int index) {
    try {
      return getValue(index);
    } catch (JMFUninitializedAccessException ex) {
      // No FFDC code needed
      // This is an expected exception which just means the field is unset, so we return null.
    } catch (JMFException ex) {
      FFDCFilter.processException(ex, "com.ibm.ws.sib.mfp.jmf.impl.JSListImpl.get", "215", this);
    }
    return null;
  }

  /**
   * Implement the List.size() method
   */
  public int size() {
    return cacheSize;
  }

  /**
   * Implement the List.set() method.  This differs from JMFMessageData.setValue in that
   * it is obligated to return the previous value.  In keeping with the java.util.List
   * definition, this method does not throw UninitializedAccessException when setting a
   * formerly unset value.  But, the cost is that a null return from this method can mean
   * either that the field was never set or that it was set to null.
   */
  public Object set(int index, Object value) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "set", new Object[]{Integer.valueOf(index), value});
    try {
      // Need to validate the index and call getInternal here (rather than getValue) because
      // we don't want a SchemaViolationException if the field is initially just unset.
      checkIndex(index);
      Object ans = getInternal(index);
      if (ans == nullIndicator) {
        ans = null;
      }
      setValue(index, value);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "set", value);
      return ans;
    } catch (JMFException ex) {
      FFDCFilter.processException(ex, "com.ibm.ws.sib.mfp.jmf.impl.JSListImpl.set", "245", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "set", null);
      return null;
    }
  }

  // Get the encoded length of this JSListImpl when serialized.
  public int getEncodedLength()
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    if (contents == null) {
      length = computeLengthFromCache();
    }
    return length + 8;
  }

  // Reallocate the entire contents buffer
  int reallocate(int fieldOffset) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "reallocate", new Object[]{Integer.valueOf(offset)});
    byte[] oldContents = contents;
    int oldOffset = offset;
    contents = new byte[length];
    System.arraycopy(oldContents, offset, contents, 0, length);
    offset = 0;
    int result = fieldOffset - oldOffset;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "reallocate", Integer.valueOf(result));
    return result;
  }

  // This method will be called after a new contents buffer has been reallocated.  Pointers
  // to and offsets within the new buffer need to be reset and passed down to any assembled
  // JSMessageData items currently in the cache.
  void reallocated(byte[] newContents, int newOffset) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "reallocated", new Object[]{newContents, Integer.valueOf(newOffset)});
    if (contents != null) {
      contents = newContents;
      offset = newOffset + 8;
      sharedContents = false;

      // Now we must run through the cache any pass the changes down to any depenedent
      // JSMessageData parts we find.
      // d282049: respect implicit state invariant implied by JSMessageData#unassemble
      // that an unassembled List can have no assembled contents
      if (cache != null) {
        for (int i = 0; i < cache.length; i++) {
          try {
            Object entry = cache[i];
            if (entry != null && entry instanceof JSMessageData) {
              ((JSMessageData)entry).reallocated(newContents, getAbsoluteOffset(i));
            }
          } catch (JMFUninitializedAccessException e) {
            // No FFDC code needed - this cannot occur as we know the JSMessageData part is present
          }
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "reallocated");
  }

  // Subclasses provide computeLengthFromCache and encodeOffsetTable methods
  abstract int computeLengthFromCache()
    throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException;
  abstract int encodeOffsetTable(byte[] frame, int next)
    throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException;

  // Encode this JSListImpl into a byte array.  We assume that no mutations have
  // intervened since the last call to getEncodedLength so we can trust the length field.
  public int encode(byte[] frame, int offset)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "encode", new Object[]{frame, Integer.valueOf(offset)});
    ArrayUtil.writeInt(frame, offset, length + 4);
    ArrayUtil.writeInt(frame, offset + 4, cacheSize);
    if (contents != null) {
      System.arraycopy(contents, this.offset, frame, offset + 8, length);
      int result = offset + 8 + length;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "encode", Integer.valueOf(result));
      return result;
    }
    else {
      int next = offset + 8;
      next = encodeOffsetTable(frame, next);
      for (int i = 0; i < cacheSize; i++) {
        Object elem = cache[i];
        if (elem == null) {
          throw new JMFUninitializedAccessException("List element " + i + "is missing");
        }
        if (elem == nullIndicator) {
          elem = null;
        }
        next = element.encodeValue(frame, next, elem, indirect, master);
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "encode", Integer.valueOf(next));
      return next;
    }
  }

  // Get the size of an aggregate that may be either an array or a Collection
  static int getSize(Object agg) throws JMFSchemaViolationException {
    if (agg == null) {
      return 0;
    }
    else if (agg instanceof Collection) {
      return ((Collection)agg).size();
    }
    else if (agg.getClass().isArray()) {
      return Array.getLength(agg);
    }
    else {
      throw new JMFSchemaViolationException(agg.getClass().getName());
    }
  }

  // Get an Iterator over an aggregate that may be either an array or a Collection
  static Iterator getIterator(Object agg) throws JMFSchemaViolationException {
    if (agg instanceof Collection) {
      return ((Collection)agg).iterator();
    }
    else if (agg.getClass().isArray()) {
      return new LiteIterator(agg);
    }
    else {
      throw new JMFSchemaViolationException(agg.getClass().getName());
    }
  }

}
