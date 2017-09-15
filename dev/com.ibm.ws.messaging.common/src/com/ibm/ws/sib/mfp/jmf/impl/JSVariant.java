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
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.jmf.JMFMessageData;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFType;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JMFVariantType;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.mfp.util.ArrayUtil;

/**
 * A representation for all variant-like types in a Jetstream schema.  These may
 * correspond (in Ecore) to EClass nodes with the JSVariant annotation or to
 * EStructuralFeatures for which both isMany() and isRequired() are false.  In XSD, these
 * may correspond to XSDModelGroup with compositor==CHOICE or to an XSDParticle with
 * minOccurs=0,maxOccurs=1.  When a JSVariant is used to indicate optional data its first
 * case is the empty tuple and the second case is the type of the optional data.  The
 * optional data design pattern is indicated by the predicate isOptionalData() being true.
 *
 * JSVariant nodes can also represent "variant boxes" which are containers for subschemas
 * within larger schemas where each such subschema's root is itself variant.  A variant
 * box may be created whenever a JSVariant has a JSRepeated ancestor that is younger than its
 * youngest JSVariant ancestor.  This convention is followed stringently for JSType trees
 * that have been installed in JSchemas, but need not be followed (and generally isn't)
 * when JSType trees are being initialized or manipulated by tools prior to be installed
 * in a JSchema.
 */

public final class JSVariant extends JSField implements JMFVariantType {
  private static TraceComponent tc = JmfTr.register(JSVariant.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // The cases of the variant.
  private JSType[] cases;

  // Pointer to the subschema within this "variant box" iff this JSVariant represents a
  // box, null otherwise.  Note: the top level JSType of the subschema is always a
  // JSVariant and its cases are the same as this JSVariant's cases (the two case arrays
  // are, in fact, aliases).
  private JSchema boxed;

  // Pointer to the original (boxed) JSVariant iff this JSVariant is the top type of
  // variant box, null otherwise.
  private JSVariant boxedBy;

  // The index assigned to this variant.  The index is only meaningful in a context where
  // the variant is unboxed.  Therefore, if boxed != null, index will not be meaningful,
  // but ((JSVariant) boxed.getJSTypeTree()).index will be meaningful.
  private int index;

  // The unboxed variants dominated by each case of this variant.  This is a cache whose
  // members are computed the first time it's needed.
  private JSVariant[][] dominated;

  // The boxAccessor for this variant, if any
  Accessor boxAccessor;

  /**
   * Public default constructor
   */
  public JSVariant() {
  }

  /**
   * Returns true if the variant represents the "optional data" design pattern
   * (corresponding to an XSDParticle with minOccurs=0,maxOccurs=1 or to an Ecore
   * EStructuralFeature with both isMany() and isRequired() false).
   */
  public boolean isOptionalData() {
    if (cases == null || cases.length != 2)
      return false;
    return (cases[0] instanceof JSTuple) && ((JSTuple)cases[0]).getFieldCount() == 0;
  }

  /**
   * Returns the number of cases in the variant.  Meaningless if this is a variant
   * box.
   */
  public int getCaseCount() {
    return (cases == null) ? 0 : cases.length;
  }

  /**
   * Returns the case of this variant at a given position.  The initial case is assigned
   * position 0
   */
  public JMFType getCase(int position) {
    if (position >= getCaseCount())
      throw new IllegalArgumentException("Invalid variant position");
    return cases[position];
  }

  /**
   *  Add a case to the variant.  Note that every variant must have at least one case.
   */
  public void addCase(JMFType theCase) {
    if (theCase == null)
      throw new NullPointerException("Variant case cannot be null");
    JSType newCase = (JSType)theCase;
    if (cases == null)
      cases = new JSType[1];
    else {
      JSType[] oldCases = cases;
      cases = new JSType[oldCases.length + 1];
      System.arraycopy(oldCases, 0, cases, 0, oldCases.length);
    }
    newCase.parent = this;
    newCase.siblingPosition = cases.length - 1;
    cases[newCase.siblingPosition] = newCase;
  }


  // Set the multiChoice count for this variant.  This is 1 if the variant is boxed;
  // otherwise, it's the sum of the multiChoice counts for the cases.
  BigInteger setMultiChoiceCount() {
    if (boxed == null) {
      multiChoiceCount = BigInteger.ZERO;
      for (int i = 0; i < cases.length; i++)
        multiChoiceCount = multiChoiceCount.add(cases[i].setMultiChoiceCount());
    }
    return multiChoiceCount;
  }

  /**
   * Get the unboxed variants dominated by a case of this variant
   */
  public JSVariant[] getDominatedVariants(int i) {
    if (dominated == null)
      dominated = new JSVariant[cases.length][];
    if (dominated[i] == null) {
      JSType acase = cases[i];
      if (acase instanceof JSVariant)
        dominated[i] = new JSVariant[] {(JSVariant)acase };
      else if (acase instanceof JSTuple)
        dominated[i] = ((JSTuple)acase).getDominatedVariants();
      else
        // Other cases don't dominate any unboxed variants (JSRepeated always boxes any
        // variants under it)
        dominated[i] = new JSVariant[0];
    }
    return dominated[i];
  }

  /**
   * Retrieve the index assigned to this JSVariant iff it is unboxed.  Note that the
   * variant's "index" is related to its accessor but is not the same thing: its accessor
   * is its index plus the length of the 'fields' array in the encompassing schema.  Its
   * index is directly interpretable as an index into the 'variants' array of the schema.
   */
  public int getIndex() {
    return index;
  }

  /**
   * Return contents of variant box or null if this isn't a variant box
   */
  public JMFSchema getBoxed() {
    return boxed;
  }

  /**
   * Return the JSVariant that boxes this JSVariant iff this JSVariant is the root of a
   * variant box, null otherwise.  That is, suppose you have JSVariant <b>var1</b> and
   * <b>boxSchema=var1.getBoxed()</b> such that <b>boxSchema!=null</b>.  Then,
   * <b>boxSchema.getJSTypeTree()</b> will always be a JSVariant and <b>((JSVariant)
   * boxSchema.getJSTypeTree()).getBoxedBy()==var1</b>.
   */
  public JSVariant getBoxedBy() {
    return boxedBy;
  }

  // Box this variant, returning the box.  The method is idempotent: if the variant is
  // already boxed, the method does the same thing as getBoxed().  The context argument is
  // used to guard against recursive creation of JSchemas in the event that the box
  // includes a cyclic reference to a JSchema already under construction.
  JSchema box(Map context) {
    if (boxed != null)
      return boxed; // only do it once
    JSVariant subTop = new JSVariant();
    subTop.cases = cases;
    subTop.boxedBy = this;
    boxed = (JSchema)context.get(subTop);
    if (boxed == null) {
      boxed = new JSchema(subTop, context);
      for (int i = 0; i < cases.length; i++)
        cases[i].parent = subTop;
      context.put(subTop, boxed);
    }
    return boxed;
  }

  // Set the index for this JSVariant, assuming it is unboxed
  void setIndex(int index) {
    this.index = index;
  }

  // Implement the simple form of getBoxedAccessor
  public int getBoxAccessor() {
    if (boxAccessor == null)
      return -1;  // not yet part of a schema
    return boxAccessor.accessor;
  }

  // Implement the general form of getBoxAccessor
  public int getBoxAccessor(JMFSchema schema) {
    for (Accessor acc = boxAccessor; acc != null; acc = acc.next)
      if (schema == acc.schema)
        return acc.accessor;
    return -1;
  }

  // Method to set the boxAccessor during schema initialization
  void setBoxAccessor(int accessor, JMFSchema schema) {
    boxAccessor = new Accessor(accessor, schema, boxAccessor);
  }

  // Format for human consumption (subroutine of toString)
  public void format(StringBuffer fmt, Set done, Set todo, int indent) {
    formatName(fmt, indent);
    fmt.append("{\n");
    for (int i = 0; i < cases.length; i++) {
      if (i > 0)
        fmt.append(" |\n");
      cases[i].format(fmt, done, todo, indent + 2);
    }
    fmt.append("\n");
    indent(fmt, indent);
    fmt.append("}");
  }

  // Constructor from byte array form
  JSVariant(byte[] frame, int[] limits) {
    int count = JSType.getCount(frame, limits);
    cases = new JSType[count];
    for (int i = 0; i < count; i++) {
      cases[i] = JSType.createJSType(frame, limits);
      cases[i].parent = this;
      cases[i].siblingPosition = i;
    }
  }

  // Implementations of the JSCoder interface.  These are used ONLY when the variant is
  // boxed (otherwise, variant case values are available from the multichoice code).
  public int getEncodedLength(Object val, int indirect, JMFMessageData msg)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    return 4 + ((JSMessageImpl)val).getEncodedLength();
  }

  public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
    if (val instanceof JSMessageImpl) {
      if (boxed.getID() != ((JSMessageImpl)val).getJMFSchema().getID())
        throw new JMFSchemaViolationException("Incorrect schema for boxed variant");
      return val;
    } else
      throw new JMFSchemaViolationException(val == null ? "null" : val.getClass().getName());
  }

  public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    JSMessageImpl box = (JSMessageImpl)val;
    int len = box.getEncodedLength();
    ArrayUtil.writeInt(frame, offset, len);
    box.toByteArray(frame, offset + 4, len);
    return offset + 4 + len;
  }

  public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg)
      throws JMFMessageCorruptionException {
    int length = ArrayUtil.readInt(frame, offset);
    JSListCoder.sanityCheck(length, frame, offset);
    return new JSMessageImpl(boxed, frame, offset + 4, length, false);
  }

  public Object copy(Object val, int indirect) throws JMFSchemaViolationException {
    return ((JSMessageImpl)val).getCopy();
  }

  // We do not have any Boxed Variants, so the answer may be academic, however
  // a HeapDump shows a JSVariant as taking 88 bytes so we'll use that value.
  public int estimateUnassembledSize(Object val) {
    return 88;
  }
  public int estimateUnassembledSize(byte[] frame, int offset) {
    return 88;
  }

  // Implementation of encodedTypeLength
  public int encodedTypeLength() {
    int ans = 3; // 3 includes kind code for VARIANT plus the length
    for (int i = 0; i < cases.length; i++)
      ans += cases[i].encodedTypeLength();
    return ans;
  }

  // Implementation of encodeType
  public void encodeType(byte[] frame, int[] limits) {
    JSType.setByte(frame, limits, (byte)JSType.VARIANT);
    JSType.setCount(frame, limits, getCaseCount());
    if (cases != null)
      for (int i = 0; i < cases.length; i++)
        cases[i].encodeType(frame, limits);
  }

  // Implementation of updateAssociations
  public void updateAssociations(JMFType type) {
    super.updateAssociations(type);
    if (getCaseCount() != ((JSVariant)type).getCaseCount())
      throw new IllegalStateException();
    if (cases != null)
      for (int i = 0; i < cases.length; i++)
         cases[i].updateAssociations(((JSVariant)type).getCase(i));
  }

  // We mostly only consider JSType nodes to be equal when they are identical, but we make
  // an exception for JSVariant so that recursion checking is tight when variants are
  // boxed.  Two JSVariants are equal when their cases are equal, and their hashCode is
  // therefore their cases hashCode.  This is probably a kluge: really, all JSType nodes
  // should have equals and hashCode methods based on a semantic notion of equality.  The
  // problem is that at this point we can't be precise about what semantic equality
  // exactly means.  One definition would be structural equality (ignoring names,
  // crosslinks, etc.).  But, what we have now is the most conservative notion of equality
  // we can get away with and it is probably the best course until we understand more
  // precisely what we need.
  public boolean equals(Object o) {
    if (o instanceof JSVariant)
      return cases == ((JSVariant)o).cases;
    else
      return false;
  }
  public int hashCode() {
    if (cases == null)
      return super.hashCode();
    else
      return cases.hashCode();
  }
}
