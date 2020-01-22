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

import com.ibm.ws.sib.mfp.jmf.JMFType;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.mfp.util.HashedArray;
import com.ibm.ws.sib.mfp.util.ArrayUtil;

import com.ibm.websphere.ras.TraceComponent;

/**
 * This data structure defines a mapping between an access schema and an encoding schema.
 * It is used to build the JSCompatibleMessageImpl when a bridge between the two schemas
 * is needed in interpreting a specific message.
 *
 * <p>It is possible to create a CompatibilityMap between variant box schemas as well as
 * "top-level" schemas.  To support this, all accessors are calculated relative to
 * supplied schemas rather than using the default getAccessor() method.
 *
 * <p>Note that when a JSchema is turned into its byte array form, only CompatibilityMaps
 * associated with the top-level schema are serialized, hence only these will be
 * propagated.  This is not a problem as long as just one of the following conditions
 * is met.
 * <ul><li>No Jetstream 1 system schema uses variants inside lists.  This is true as of
 * this writing.  Obviously, if there are no variant box schemas in Jetstream 1, then
 * there can be no extensions made to them in a future Jetstream!  Applications may use
 * schemas with variant boxes (indirectly, via Ecore) but we aren't relying on schema
 * propagation for those.
 * <li>(or) Future extensions are made according to the present rules of compatibility so
 * that Jetstream 1 systems can calculate maps automatically without having received them
 * from abroad.
 * <li>(or) Future extensions made according (as yet uninvented) new rules of compatibility
 * refrain from applying such extensions inside lists of variant (should any such have
 * meanwhile been added to any Jetstream 1 schemas).</ul>
 * I think we are safe.
 */
public final class CompatibilityMap implements HashedArray.Element {
  private static TraceComponent tc = JmfTr.register(CompatibilityMap.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  /**
   * The id of the access schema to which the CompatibilityMap applies (used to retrieve
   * it from a HashedArray of CompatibilityMaps stored in the encoding schema).
   */
  public long accessSchemaId;

  /**
   * The accessors in the encoding schema to use for each accessor defined in the access
   * schema not counting those in the boxed range.
   */
  public int[] indices;

  /**
   * The variant index bias of the encoding schema (subtract this from indices[i] before
   * looking up the result in setCases or getCases).
   */
  public int varBias;

  /**
   * The case index translation to use when setting a variant case.  There are the same
   * number of elements as there are variants in the encoding schema.  For each element,
   * there are the same number of elements as there are cases in the access schema's
   * image of the same variant.
   */
  public int[][] setCases;

  /**
   * The case index translation to use when getting a variant case.  There are the same
   * number of elements as there are variants in the encoding schema.  For each element,
   * there are the same number of elements as there are cases in that variant.
   */
  public int[][] getCases;

  // Cache the encoded form
  private byte[] encodedForm;

  /**
   * Create an CompatibilityMap from a compact byte array encoding of same
   *
   * @param frame the byte array containing the encoded form of this map
   * @param offset the offset into the array where the encoded form begins
   * @param length the number of bytes in the encoded form
   */
  public CompatibilityMap(byte[] frame, int offset, int length) {
    encodedForm = new byte[length];
    System.arraycopy(frame, offset, encodedForm, 0, length);
    accessSchemaId = ArrayUtil.readLong(frame, offset);
    int[] limits = new int[] { offset + 8, offset + length };
    indices = new int[JSType.getCount(frame, limits)];
    for (int i = 0; i < indices.length; i++)
      indices[i] = JSType.getCount(frame, limits);
    varBias = JSType.getCount(frame, limits);
    setCases = new int[JSType.getCount(frame, limits)][];
    for (int i = 0; i < setCases.length; i++) {
      int len = JSType.getCount(frame, limits);
      if (len != -1) { // -1 indicates null entry
        int[] cases = new int[len];
        for (int j = 0; j < len; j++)
          cases[j] = JSType.getCount(frame, limits);
        setCases[i] = cases;
      }
    }
    getCases = new int[JSType.getCount(frame, limits)][];
    for (int i = 0; i < getCases.length; i++) {
      int len = JSType.getCount(frame, limits);
      if (len != -1) { // -1 indicates null entry
        int[] cases = new int[len];
        for (int j = 0; j < len; j++)
          cases[j] = JSType.getCount(frame, limits);
        getCases[i] = cases;
      }
    }
  }

  /**
   * Turn an CompatibilityMap into its encoded form
   *
   * @return the encoded form as a byte array
   */
  public byte[] toEncodedForm() {
    if (encodedForm == null) {
      encodedForm = new byte[encodedSize()];
      ArrayUtil.writeLong(encodedForm, 0, accessSchemaId);
      encode(encodedForm, new int[] { 8, encodedForm.length });
    }
    return encodedForm;
  }

  // Encode subroutine used by toEncodedForm
  private void encode(byte[] frame, int[] limits) {
    JSType.setCount(frame, limits, indices.length);
    for (int i = 0; i < indices.length; i++)
      JSType.setCount(frame, limits, indices[i]);
    JSType.setCount(frame, limits, varBias);
    JSType.setCount(frame, limits, setCases.length);
    for (int i = 0; i < setCases.length; i++) {
      int[] cases = setCases[i];
      if (cases == null)
        JSType.setCount(frame, limits, -1);
      else {
        JSType.setCount(frame, limits, cases.length);
        for (int j = 0; j < cases.length; j++)
          JSType.setCount(frame, limits, cases[j]);
      }
    }
    JSType.setCount(frame, limits, getCases.length);
    for (int i = 0; i < getCases.length; i++) {
      int[] cases = getCases[i];
      if (cases == null)
        JSType.setCount(frame, limits, -1);
      else {
        JSType.setCount(frame, limits, cases.length);
        for (int j = 0; j < cases.length; j++)
          JSType.setCount(frame, limits, cases[j]);
      }
    }
  }

  // Find the number of bytes it takes to encode this CompatibilityMap
  private int encodedSize() {
    int ans =
      16 /* for accessSchemaID, indices.length, varBias, setCases.length,
                          and getCases.length */
      + 2*indices.length; // for the elements in indices
    for (int i = 0; i < setCases.length; i++) {
      int[] cases = setCases[i];
      ans += 2; // for cases.length or null indicator
      if (cases != null)
        ans += 2 * cases.length;
    }
    for (int i = 0; i < getCases.length; i++) {
      int[] cases = getCases[i];
      ans += 2; // for cases.length or null indicator
      if (cases != null)
        ans += 2 * cases.length;
    }
    return ans;
  }

  /**
   * Create a CompatibilityMap from two JSchemas, using the current (Jetstream 1) rules of
   * compatibility.  Note that CompatibilityMaps based on more liberal rules of
   * compatibility are possible, but their generation is beyond the scope of a simple
   * constructor: such maps would be generated by static tools and saved in byte array
   * form.
   *
   * @param access the JSchema for the access schema
   * @param encoding the JSchema for the encoding schema
   * @exception SchemaViolationException if the two schemas can't be adapted
   */
  public CompatibilityMap(JSchema access, JSchema encoding) throws JMFSchemaViolationException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "<init>");

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "access Schema:   " + access.getJMFType());
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "encoding Schema: " + encoding.getJMFType());

    accessSchemaId = access.getID();
    // Set up instance variables.
    indices = new int[access.getFields().length + access.getVariants().length];
    varBias = encoding.getFields().length;
    setCases = new int[encoding.getVariants().length][];
    getCases = new int[setCases.length][];

    // Initialize indices to -1, so that any one that doesn't have an explicit counterpart
    // identified by the recordCompatibilityInfo methods will be marked as invalid.
    for (int i = 0; i < indices.length; i++)
      indices[i] = -1;

    // Fill in the actual information by recursive descent of the underlying Schema trees
    recordCompatibilityInfo(access.getJMFType(), access, encoding.getJMFType(), encoding);

//  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "Map:", printableMap());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "<init>");
  }

  // Handle violation of the compatibility rules by throwing an informative exception
  private static void violation(JMFType from, JMFType to) throws JMFSchemaViolationException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(tc, "Violation:"
                                             + "  from = " + from.getFeatureName() + " : " + from
                                             + ", to = " + to.getFeatureName() + " : " + to
                                             );
    throw new JMFSchemaViolationException(from.getFeatureName() + " not compatible with " + to.getFeatureName());
  }

  // Subroutine to fill in compatibility information for one pair of types.
  private void recordCompatibilityInfo(JMFType access, JSchema accSchema, JMFType encoding,
      JSchema encSchema) throws JMFSchemaViolationException
  {
    if (access instanceof JSPrimitive)
      if (encoding instanceof JSPrimitive)
        recordCompatibilityInfo((JSPrimitive)access, accSchema, (JSPrimitive)encoding,
          encSchema);
      else if (encoding instanceof JSVariant)
        checkForDeletingVariant(access, (JSVariant)encoding, false, accSchema, encSchema);
      else {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "Compatibility violation (type mismatch) for JSPrimitive");
        violation(access, encoding);
       }
    else if (access instanceof JSEnum)
      if (encoding instanceof JSEnum)
        recordCompatibilityInfo((JSEnum)access, accSchema, (JSEnum)encoding, encSchema);
      else if (encoding instanceof JSVariant)
        checkForDeletingVariant(access, (JSVariant)encoding, false, accSchema, encSchema);
      else {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "Compatibility violation (type mismatch) for JSEnum");
        violation(access, encoding);
      }
    else if (access instanceof JSDynamic)
      if (encoding instanceof JSDynamic)
        recordOnePair((JSField)access, accSchema, (JSField)encoding, encSchema);
      else if (encoding instanceof JSVariant)
        checkForDeletingVariant(access, (JSVariant)encoding, false, accSchema, encSchema);
      else {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "Compatibility violation (type mismatch) for JSDynamic");
        violation(access, encoding);
      }
    else if (access instanceof JSRepeated)
      if (encoding instanceof JSRepeated)
        recordCompatibilityInfo(((JSRepeated)access).getItemType(), accSchema,
          ((JSRepeated)encoding).getItemType(), encSchema);
      else if (encoding instanceof JSVariant)
        checkForDeletingVariant(access, (JSVariant)encoding, false, accSchema, encSchema);
      else {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "Compatibility violation (type mismatch) for JSRepeated");
        violation(access, encoding);
      }
    else if (access instanceof JSTuple)
      if (encoding instanceof JSTuple)
        recordCompatibilityInfo((JSTuple)access, accSchema, (JSTuple)encoding, encSchema);
      else if (encoding instanceof JSVariant)
        checkForDeletingVariant(access, (JSVariant)encoding, false, accSchema, encSchema);
      else {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "Compatibility violation (type mismatch) for JSTuple");
        violation(access, encoding);
      }
    else if (access instanceof JSVariant) {
      JSVariant accVar = (JSVariant)access;
      if (encoding instanceof JSVariant) {
        JSVariant encVar = (JSVariant)encoding;
        recordCompatibilityInfo(accVar, accSchema, encVar, encSchema);
      } else
        checkForDeletingVariant(encoding, accVar, true, accSchema, encSchema);
    }
  }

  // Subroutine to record the compatibility information for one pair of primitive types,
  // or signal a violation if they aren't compatible.
  private void recordCompatibilityInfo(JSPrimitive access, JSchema accSchema,
      JSPrimitive encoding, JSchema encSchema) throws JMFSchemaViolationException
  {
    if (access.getTypeCode() == encoding.getTypeCode())
      recordOnePair(access, accSchema, encoding, encSchema);
    else {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "Compatibility violation (primitive type mismatch):"
                                             + " access " + access
                                             + ", access TypeCode " + access.getTypeCode()
                                             + ", encoding " + encoding
                                             + ", encoding TypeCode " + encoding.getTypeCode()
                                             );
      violation(access, encoding);
    }
  }

  // Subroutine to record the compatibility information for one pair of enumeration types,
  // or signal a violation if they aren't compatible.
  private void recordCompatibilityInfo(JSEnum access, JSchema accSchema, JSEnum encoding,
      JSchema encSchema) throws JMFSchemaViolationException
  {
    if (access.getEnumeratorCount() == encoding.getEnumeratorCount())
      recordOnePair(access, accSchema, encoding, encSchema);
    else {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "Compatibility violation (enumeration type mismatch):"
                                             + " access " + access
                                             + ", access EnumeratorCount " + access.getEnumeratorCount()
                                             + ", encoding " + encoding
                                             + ", encoding EnumeratorCount " + encoding.getEnumeratorCount()
                                             );
      violation(access, encoding);
    }
  }

  // Subroutine to record a pair of indices known to be a compatibility pair.  Note that
  // we omit recording indices that fall outside the range of indices (that is, within the
  // boxed range of the access schema) because this compatibility map doesn't need them.
  // We do, however, go all the way through the analysis for them in order to detect
  // violations.  If no violations are found building the main map, then none will be found
  // later when maps are built for variant box schemas.
  private void recordOnePair(JSField access, JSchema accSchema, JSField encoding,
      JSchema encSchema)
  {
    int acc = access.getAccessor(accSchema);
    if (acc >= indices.length)
      return;
    indices[acc] = encoding.getAccessor(encSchema);
  }

  // Subroutine to test whether a type is the empty tuple
  private boolean isEmpty(JMFType toTest) {
    return (toTest instanceof JSTuple && ((JSTuple)toTest).getFieldCount() == 0);
  }

  // Subroutine to check for the 'deleting variant' convention when one type is a variant
  // and the other is a deletable type.  If the check passes, we record the compatibility
  // info stripping off the deleting variant.
  private void checkForDeletingVariant(JMFType nonVar, JSVariant var, boolean varIsAccess,
      JSchema accSchema, JSchema encSchema) throws JMFSchemaViolationException
  {
    // Deleting variant always has exactly two cases
    if (var.getCaseCount() != 2) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "Compatibility violation (deleting variant):"
                                             + " var CaseCount = " + var.getCaseCount()
                                             + ", varIsAccess " + varIsAccess
                                             );
      if (varIsAccess) {
        violation(var, nonVar);
      }
      else {
        violation(nonVar, var);
      }
    }
    if (isEmpty(var.getCase(1))) {
      // Second case is empty tuple, so we proceed to compare the first case to the
      // candidate deletable type.
      JMFType compare = var.getCase(0);
      if (varIsAccess)
        recordCompatibilityInfo(compare, accSchema, nonVar, encSchema);
      else
        recordCompatibilityInfo(nonVar, accSchema, compare, encSchema);
    }
    else {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "Compatibility violation (deleting variant):"
                                             + " var Case(1) not empty"
                                             );
      if (varIsAccess) {
        violation(var, nonVar);
      }
      else {
        violation(nonVar, var);
      }
    }
  }

  private void recordCompatibilityInfo(JSVariant access, JSchema accSchema,
      JSVariant encoding, JSchema encSchema) throws JMFSchemaViolationException
  {
    // Either both variants are boxed or neither should be; if both are, record the mapping
    // of their box accessors.
    int accBox = access.getBoxAccessor(accSchema);
    int encBox = encoding.getBoxAccessor(encSchema);
    if (accBox != -1 && encBox != -1) {
      if (accBox < indices.length)
        indices[accBox] = encBox;
      // Do nothing if out of range; nested variant boxes can give rise to this case.
      // But only non-nested (top level) ones should be recorded.
    }
    else if (accBox != -1 || encBox != -1) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "Compatibility violation (variants):"
                                             + " box mismatch "
                                             + accBox + " != " + encBox
                                             );
      violation(access, encoding);
    }
    else
      // both are not boxed so we record their normal accessors.  There was no point in
      // doing this for boxed variants, which were guaranteed to have normal accessors
      // that are too high to record.  Note, however, that recordOnePair may still do
      // nothing because this variant is nested in some other variant box.
      recordOnePair(access, accSchema, encoding, encSchema);
    // Now determine whether the nominal rules of compatibility are met
    int accessCount = access.getCaseCount();
    int encodingCount = encoding.getCaseCount();
    if (accessCount != encodingCount)
      if (!isEmpty(access.getCase(0)) || !isEmpty(encoding.getCase(0))) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "Compatibility violation (variants):"
                                               + " mismatch case count plus first not empty"
                                               );
        violation(access, encoding);
      }
    int encodingIndex = encoding.getAccessor(encSchema) - varBias;
    // Initialize arrays to hold the case remaps for both the getCase and setCase
    // direction.  Note that the outer level of BOTH arrays is indexed by encodingIndex
    // but the inner level is dimensioned by accessCount for setting and by encodingCount
    // for getting.  This may look irregular but is correct!  We can bypass the allocation
    // if we see that nothing will be saved in the end.
    int[] ourSets = null;
    int[] ourGets = null;
    if (encodingIndex < getCases.length) {
      getCases[encodingIndex] = ourGets = new int[encodingCount];
      setCases[encodingIndex] = ourSets = new int[accessCount];
    }
    // Adapt the individual cases and fill in the two case arrays
    for (int i = 0; i < ((accessCount > encodingCount) ? encodingCount : accessCount); i++) {
      if (ourSets != null) {
        ourSets[i] = i;
        ourGets[i] = i;
      }
      recordCompatibilityInfo(access.getCase(i), accSchema, encoding.getCase(i), encSchema);
    }
    // Remaining indices in the longer of ourSets and ourGets are (correctly) zero
  }

  private void recordCompatibilityInfo(JSTuple access, JSchema accSchema, JSTuple encoding,
      JSchema encSchema) throws JMFSchemaViolationException
  {
    int accessCount = access.getFieldCount();
    int encodeCount = encoding.getFieldCount();
    int count = (accessCount < encodeCount) ? accessCount : encodeCount;

    // Check that extra fields, if any, are ignorable

    if (accessCount > count)
      checkExtraFields(access, count, accessCount);
    else if (encodeCount > count)
      checkExtraFields(encoding, count, encodeCount);

    // Process each field

    for (int i = 0; i < count; i++)
      recordCompatibilityInfo(access.getField(i), accSchema, encoding.getField(i),
        encSchema);
  }

  // Check the extra columns in a tuple to make sure they are all defaultable
  private static void checkExtraFields(JSTuple tuple, int startCol, int count) throws JMFSchemaViolationException {
    for (int i = startCol; i < count; i++) {
      JMFType field = tuple.getField(i);
      if (field instanceof JSVariant) {
        JMFType firstCase = ((JSVariant)field).getCase(0);
        if (firstCase instanceof JSTuple)
          if (((JSTuple)firstCase).getFieldCount() == 0)
            continue;
      }
      throw new JMFSchemaViolationException(field.getFeatureName() + " not defaulting variant");
    }
  }

  /**
   * Satisfy the HashedArray.Element interface so that CompatibilityMaps can be stored
   * and retrieved efficiently by their accessSchemaId
   */
  public long getIndex() {
    return accessSchemaId;
  }


  /**
   * Private method to produce a string representation of the indices Map
   * for use in debugging.
   * Commented out so as not to waste space in production.
   *
   * @return String representation of the indices map
   */
//private String printableMap() {
//
//  StringBuffer str = new StringBuffer(indices.length * 20);
//  String i1 = "indices[";
//  String i2 = "] = ";
//  String linesep = System.lineSeparator();
//  str.append(linesep);
//
//  for (int i = 0; i < indices.length; i++) {
//    str.append(i1 + i + i2 + indices[i] + linesep);
//  }
//
//  return new String(str);
//}

}
