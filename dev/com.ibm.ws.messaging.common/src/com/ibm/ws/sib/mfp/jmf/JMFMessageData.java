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

package com.ibm.ws.sib.mfp.jmf;

/**
 * The JMFMessageData interface contains the data access methods that are common to both
 * JMFNativePart and JMFList (which is used to access data within lists).
 */

public interface JMFMessageData {

  /**
   * Get the 'value' of a given field.  What is returned depends on the
   * <b>JMFFieldDef</b> for the field and on how many <b>JMFRepeatedType</b> nodes
   * dominate that <b>JMFFieldDef</b> in the tree representation of the schema.
   *
   * <p>First, the <em>base</em> representation class of a field is determined from its
   * <b>JMFFieldDef</b> as follows.
   *
   * <ul><li>For a <b>JMFPrimitiveType</b>, the base representation class is the class
   * returned by the <b>getJavaClass()</b> method for that <b>JMFPrimitiveType</b>.
   *
   * <li>For a <b>JMFDynamicType</b>, the base representation class is a <b>JMFPart</b>.
   * But, this may be either a <b>JMFNativePart</b> or an <b>JMFEncapsulation</b>,
   * depending on whether the field is JMF-encoded or encapsulated.  Use
   * <b>getNativePart</b> or <b>getEncapsulation</b> rather than <b>getValue</b> to get
   * the field in a caller-specified form that does not depend on its actual encoding.
   *
   * <li>For a <b>JMFVariantType</b>, the base representation class is <b>Integer</b> and
   * represents the 'case' of the variant.
   *
   * <li>For a <b>JMFEnumType</b>, the base representation class is <b>Integer</b> and
   * represents the index of the enumerator assigned to a value of the
   * <b>JMFEnumType</b>.</ul>
   *
   * <p>Next, for each <b>JMFRepeatedType</b> that dominated the <b>JMFFieldDef</b> in the
   * tree representation of the schema, the base object is repeated inside a
   * <b>JMFList</b>.  It is the outermost <b>JMFList</b> that is actually returned.
   *
   * <p>If the Object returned by this method is one that implements <b>JMFMessageData</b>
   * or <b>JMFEncapsulation</b>, mutations performed on the returned object are reflected
   * to this <b>JMFMessageData</b>.  If this behavior is not desired, the dependent
   * object's clone() method should be called and mutations performed on the clone.  The
   * relationship between the dependent object and its containing object is severed if a
   * <b>setValue</b> call replaces the dependent object with a completely new one.
   *
   * @param accessor the accessor associated with the field
   * @return the Object representation of the value at that position as described above.
   * @exception IndexOutOfBoundsException if accessor is not a valid accessor for
   * this message's schema.
   * @exception JMFUninitializedAccessException if the field associated with the accessor
   * does not contain a value.  This can happen either because the message is incompletely
   * initialized or because this field does not occur in the message as initialized, given
   * the variant case settings.  In the case of <b>JMFList</b> returns, the absence of
   * this exception does not mean that it will not be thrown later when individual
   * elements of the <b>JMFList</b> are accessed.
   * @exception JMFMessageCorruptionException if the message was found to be corrupted.
   * The formatting service does not guarantee that all forms of message corruption will
   * be detected but it makes an effort to sanity-check messages received from remote
   * systems.  True message integrity protection is the responsibility of a security
   * layer.
   * @exception JMFModelNotImplementedException if the field is a JMFDynamicType field
   * that is an encapsulation (not JMF encoded) and no JMFEncapsulationManager has been
   * registered that understands how to decode it.  This should not happen in a
   * well-deployed Jetstream, where a small number of message models are registered
   * everywhere and no other ones are used.
   */
  public Object getValue(int accessor)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to ((Boolean) getValue(accessor)).booleanValue() but
   * possibly more efficient for (type-correct) isolated accesses to single fields.
   * Throws <b>ClassCastException</b> in cases where the longer expression would do so.
   */
  public boolean getBoolean(int accessor)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to ((Byte) getValue(accessor)).byteValue() but
   * possibly more efficient for (type-correct) isolated accesses to single fields.
   * Throws <b>ClassCastException</b> in cases where the longer expression would do so.
   */
  public byte getByte(int accessor)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to ((Short) getValue(accessor)).shortValue() but
   * possibly more efficient for (type-correct) isolated accesses to single fields.
   * Throws <b>ClassCastException</b> in cases where the longer expression would do so.
   */
  public short getShort(int accessor)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to ((Character) getValue(accessor)).charValue() but possibly
   * more efficient for (type-correct) isolated accesses to single fields.
   * Throws <b>ClassCastException</b> in cases where the longer expression would do so.
   */
  public char getChar(int accessor)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to ((Integer) getValue(accessor)).intValue() but possibly
   * more efficient for (type-correct) isolated accesses to single fields.
   * Throws <b>ClassCastException</b> in cases where the longer expression would do so.
   */
  public int getInt(int accessor)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to ((Long) getValue(accessor)).longValue() but
   * possibly more efficient for (type-correct) isolated accesses to single fields.
   * Throws <b>ClassCastException</b> in cases where the longer expression would do so.
   */
  public long getLong(int accessor)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to ((Float) getValue(accessor)).floatValue() but
   * possibly more efficient for (type-correct) isolated accesses to single fields.
   * Throws <b>ClassCastException</b> in cases where the longer expression would do so.
   */
  public float getFloat(int accessor)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to ((Double) getValue(accessor)).doubleValue() but
   * possibly more efficient for (type-correct) isolated accesses to single fields.
   * Throws <b>ClassCastException</b> in cases where the longer expression would do so.
   */
  public double getDouble(int accessor)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Determine if a field is actually present in a message.  When directed at a field for
   * which <b>getValue</b> would return a <b>JMFList</b>, this method simply reports
   * whether such a call would return without throwing
   * <b>JMSUninitializedAccessException</b>.  A <b>true</b> return does not necessarily
   * mean that the list is non-empty or that all of its members are present.  For more
   * fine-grained information it is necessary to retrieve the list and interrogate its
   * individual members.
   *
   * @param accessor the accessor of the field
   * @return true if the field denoted by the accessor is present in the message, false
   * otherwise.
   * @exception IndexOutOfBoundsException if accessor is not a valid accessor in this
   * message's schema.
   * @exception JMFSchemaViolationException if the field is one for which <b>getValue</b>
   * would return a <b>JMFList</b>.
   */
  public boolean isPresent(int accessor)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Set the value of a given field. Setting a value has the side-effect of setting all
   * variant cases that must be set if the value is to appear in the message.  This can,
   * in turn, cause other values (associated with previous case settings for those
   * variants) to be removed from the message.
   *
   * <p>By convention, <b>setValue</b> accepts <b>null</b> values in some cases but not
   * others.
   *
   * <ul><li>For primitive fields, a <b>null</b> either is or is not accepted, depending
   * on properties of the type.  A <b>null</b> is not accepted for numbers, booleans,
   * single characters, and date/times.  A <b>null</b> is accepted for Strings, and byte
   * arrays.  When a <b>null</b> is accepted it is distinct from other "empty"
   * cases.  For example, and empty String and a <b>null</b> String are not the same, and
   * the null byte array and the empty byte array are not the same.
   *
   * <li>In all other cases, a <b>null</b> is not accepted.  In particular, in cases where
   * a list would be returned by <b>getValue</b>, the absence of any list members should
   * be indicated by the empty <b>JMFList</b>, not by <b>null</b>.</ul>
   *
   * <p>It is not necessary to call <b>setValue</b> for <b>JMFVariantType</b> fields when
   * their cases can be inferred from the settings of <b>JMFFieldDefs</b> lower down in the
   * tree representation of the schema.  However,
   *
   * <ul><li>Setting a variant's case explicitly is necessary when that case is the empty
   * tuple.
   *
   * <li>Setting a variant's case explicitly is never an error.</ul>
   *
   * @param accessor the accessor associated with the field
   * @param val the value to be set.  The value must have a base representation class and
   * number of enclosing <b>JMFList</b> wrappers appropriate for the <b>JMFFieldDef</b> as
   * described for the return value of <b>getValue</b>.  However, for convenience, any
   * <b>java.util.Collection</b> or an array can be used in lieu of a
   * <b>JMFList</b> as long as the base representation class is the correct one.
   * @exception JMFSchemaViolationException if <b>val</b> has an incorrect base
   * representation class or the wrong number of <b>JMFList</b> wrappers for this
   * <b>JMFFieldDef</b>.
   */
  public void setValue(int accessor, Object val)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to setValue(accessor, new Boolean(val)) but may be more
   * efficient for (type-correct) isolated accesses to single fields.  Throws
   * <b>JMFSchemaViolationException</b> in cases where the longer expression would do so.
   */
  public void setBoolean(int accessor, boolean val)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to setValue(accessor, new Byte(val)) but may be more
   * efficient for (type-correct) isolated accesses to single fields.  Throws
   * <b>JMFSchemaViolationException</b> in cases where the longer expression would do so.
   */
  public void setByte(int accessor, byte val)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to setValue(accessor, new Short(val)) but may be more
   * efficient for (type-correct) isolated accesses to single fields.  Throws
   * <b>JMFSchemaViolationException</b> in cases where the longer expression would do so.
   */
  public void setShort(int accessor, short val)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to setValue(accessor, new Character(val)) but may be more
   * efficient for (type-correct) isolated accesses to single fields.  Throws
   * <b>JMFSchemaViolationException</b> in cases where the longer expression would do so.
   */
  public void setChar(int accessor, char val)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to setValue(accessor, new Integer(val)) but may be more
   * efficient for (type-correct) isolated accesses to single fields.  Throws
   * <b>JMFSchemaViolationException</b> in cases where the longer expression would do so.
   */
  public void setInt(int accessor, int val)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to setValue(accessor, new Long(val)) but may be more
   * efficient for (type-correct) isolated accesses to single fields.  Throws
   * <b>JMFSchemaViolationException</b> in cases where the longer expression would do so.
   */
  public void setLong(int accessor, long val)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to setValue(accessor, new Float(val)) but may be more
   * efficient for (type-correct) isolated accesses to single fields.  Throws
   * <b>JMFSchemaViolationException</b> in cases where the longer expression would do so.
   */
  public void setFloat(int accessor, float val)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Semantically equivalent to setValue(accessor, new Double(val)) but may be more
   * efficient for (type-correct) isolated accesses to single fields.  Throws
   * <b>JMFSchemaViolationException</b> in cases where the longer expression would do so.
   */
  public void setDouble(int accessor, double val)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Retrieve a JMFNativePart representing a dynamic field while specifying the access
   * JMFSchema to be used to navigate it.
   *
   * <p>This method should be used in preference to getValue when the application requires
   * either (1) that the dynamic field be represented as a JMFNativePart regardless of how
   * it was encoded, and/or (2) that a particular access schema be used even if a
   * different (but compatible) schema was used for encoding (assuming a JMFNativePart
   * representation is acceptable).
   *
   * <p>This method is only appropriate when <b>getValue</b> using the same accessor would
   * not return a <b>JMFList</b>.  Otherwise, <b>getValue</b> should be used to retrieve
   * the list and the individual members of the list subjected to <b>getNativePart</b>
   * calls.
   *
   * @param accessor the accessor associated with the field, which must be a
   * <b>JMFDynamicType</b>.
   * @param schema the access schema to be used.  If this is null, it is assumed that the
   * access schema should be the same as the encoding schema.  If the access schema is
   * compatible with the encoding schema, but different, a compatibility layer is inserted
   * so that the access schema can be used to navigate the message part under the JMF
   * compatibility rules.
   * @return a JMFNativePart conforming to the provided access schema.
   * @exception IndexOutOfBoundsException if accessor is not a valid accessor for
   * this message's schema.
   * @exception JMFUninitializedAccessException if the field associated with the accessor
   * does not contain a value.  This can happen either because the message is incompletely
   * initialized or because this field does not occur in the message as initialized, given
   * the variant case settings.
   * @exception JMFSchemaViolationException if the accessor does not correspond to a
   * JMFDynamicType field, if the supplied schema is incompatible with the encoding
   * schema, or if the field is one for which <b>getValue</b> would have returned a list.
   * @exception JMFModelNotImplementedException if the JMFDynamicType field being
   * retrieved is an encapsulation (not JMF encoded) and no JMFEncapsulationManager has
   * been registered that understands how to decode it.  This should not happen in a
   * well-deployed Jetstream, where a small number of message models are registered
   * everywhere and no other ones are used.
   */
  public JMFNativePart getNativePart(int accessor, JMFSchema schema)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Get the model ID associated with a JMFDynamicType field.  This information may be
   * useful in deciding whether to navigate the field in its "natural" representation
   * (getValue) or coerce it to some more specific model (getNativePart or
   * getEncapsulation).  This method does not works when <b>getValue</b> with the same
   * accessor would return a list.  In that case, <b>JMFList</b> for the field should be
   * retrieved with <b>getValue</b> and the individual elements interrogated to determine
   * their model IDs.
   *
   * @return the model ID associated with the JMFDynamicType field
   * @exception IndexOutOfBoundsException if the accessor is not a valid accessor for this
   * message's schema.
   * @exception JMFSchemaViolationException if the accessor does not designate a
   * JMFDynamicType, or if the accessor is one for which <b>getValue</b> would have
   * returned a list.
   * @exception JMFUninitializedAccessException if the JMFDynamicType associated with the
   * accessor does not contain any value.  This can happen either because the message is
   * incompletely initialized or because this field does not occur in the message as
   * initialized, given the variant case settings.
   */
  public int getModelID(int accessor)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Notifies this JMFMessageData segment that its serialized (byte array) form should no
   * longer be regarded as correct and should be reassembled when next needed.  This
   * method is normally called by subordinate JMFParts (whether in JMF format or
   * adhering to the more general JMFEncapsulation contract) when they have been mutated in
   * a fashion that could not be performed in-line in their portion of the byte array
   * because it would have required a length change.
   */
  public void unassemble()
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * estimateUnassembledValueSize
   * Get estimated unassembled size of the 'value' of a given field.
   * What is returned, and how good an estimate it is, depends hugely on the
   * <b>JMFFieldDef</b> for the field.
   * Despite the potential for encountering Exceptions, this method will not
   *
   * @param accessor the accessor associated with the field
   * @return  The estimated size of the value if unassembled.
   */
  public int estimateUnassembledValueSize(int accessor);
}
