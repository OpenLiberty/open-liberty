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

import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 * This class provides a map of an individual Message layout within a schema (the message
 * variant with a particular multi-choice code).  We build MessageMaps on demand and store
 * them in a <a href="MessageMapTable.html">MessageMapTable</a> which is, in turn, set as
 * the <b>InterpreterCache</b> property of the <b>JMFSchema</b> to which it applies.
 */

public final class MessageMap {
  private static TraceComponent tc = JmfTr.register(MessageMap.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // The multiChoice code for this message
  public BigInteger multiChoice;

  // The number of offset table entries in this message
  public int offsetsNeeded;

  // The variant choice codes for this message (those that don't require interrogating
  // boxed variants; access to codes inside boxed variants is inherently slower).
  public int[] choiceCodes;

  // The field status array for this message
  public Remap[] fields;

  /**
   * Constructor
   *
   * @param schema the JSchema for which this MessageMap is being constructed
   * @param multiChoice the multiChoice code for which this MessageMap is being
   * constructed
   */
  public MessageMap(JSchema schema, BigInteger multiChoice) {
    this.multiChoice = multiChoice;
    JSField[] cfields = schema.getFields();
    choiceCodes = new int[schema.getVariants().length];
    for (int i = 0; i < choiceCodes.length; i++) {
      choiceCodes[i] = -1; // initialize each code to "inapplicable"
    }
    fields = new Remap[cfields.length];
    // Now set the choices to their correct values.  Those that remain -1 really are
    // inapplicable.
    if (choiceCodes.length > 0)
      setChoices(multiChoice, schema);
    // Find the fields that really exist in this message and set up their Remap
    // information.
    int moi = -1; // maximum offset index so far
    int length = 0; // length of last field to be processed
    int incr = 0;
    /* increment of the field from the value that will be at position moi
                        in the offset table at runtime (or from the start of the
                        FlatTuple's data area if moi==-1) */
    for (int i = 0; i < cfields.length; i++) {
      JSField field = cfields[i];
      if (isPresent(field)) {
        if (field.getIndirection() != -1)
          // list
          length = -1;
        else if (field instanceof JSPrimitive)
          length = ((JSPrimitive)field).getLength();
        else if (field instanceof JSEnum)
          length = 4;
        else
          length = -1; // JSDynamic and (boxed) JSVariant always have variable length
        fields[i] = new Remap(moi, incr, length == -1);
        if (length == -1) {
          moi++;
          incr = 0;
        } else
          incr += length;
      }
    }
    // Offsets needed field is related to "maximum offset index" (moi), but if the last
    // field was varying length, then moi will have been incremented once extra.
    if (moi == -1)
      // no offset table
      offsetsNeeded = 0;
    else if (length == -1)
      // last offset was varying length, so moi has already been incremented once extra
      offsetsNeeded = moi;
    else
      // last offset was fixed length, so moi is the index of the last offset used
      offsetsNeeded = moi + 1;
  }

  // The setChoices method recursively initializes the choiceCodes array from the
  // multichoice code.
  private void setChoices(BigInteger multiChoice, JSchema schema) {
    JSType topType = (JSType)schema.getJMFType();
    if (topType instanceof JSVariant)
      setChoices(multiChoice, schema, (JSVariant)topType);
    else if (topType instanceof JSTuple)
      setChoices(
        multiChoice,
        topType.getMultiChoiceCount(),
        schema,
        ((JSTuple)topType).getDominatedVariants());
    else
      // If topType is JSRepeated, JSDynamic, JSEnum, or JSPrimitive there can be no unboxed
      // variants at top level.
      return;
  }

  // Set the choices implied by the multiChoice code or contribution to a single JSVariant
  private void setChoices(BigInteger multiChoice, JSchema schema, JSVariant var) {
    for (int i = 0; i < var.getCaseCount(); i++) {
      BigInteger count = ((JSType)var.getCase(i)).getMultiChoiceCount();
      if (multiChoice.compareTo(count) >= 0)
        multiChoice = multiChoice.subtract(count);
      else {
        // We now have the case of our particular Variant in i.  We set that in
        // choiceCodes and then recursively visit the Variants dominated by ours.
        choiceCodes[var.getIndex()] = i;
        JSVariant[] subVars = var.getDominatedVariants(i);
        setChoices(multiChoice, count, schema, subVars);
        return;
      }
    }
    // We should never get here
    throw new RuntimeException("Bad multiChoice code");
  }

  // Set the choices implied by the multiChoice code or contribution to a set of
  // JSVariants that are either the top-level set or the set dominated by another
  // JSVariant whose choices are being set.
  private void setChoices(BigInteger multiChoice, BigInteger radix, JSchema schema, JSVariant[] vars) {
    for (int j = 0; j < vars.length; j++) {
      radix = radix.divide(vars[j].getMultiChoiceCount());
      BigInteger contrib = multiChoice.divide(radix);
      multiChoice = multiChoice.remainder(radix);
      setChoices(contrib, schema, vars[j]);
    }
  }

  // The isPresent predicate decides whether a given JSType is present in this message
  // based on the hierarchy of dominating variants and their present choice settings.
  // This is invoked only during construction; once a MessageMap is fully constructed you
  // can test whether a field is present by looking for a non-null Remap in the fields
  // array.
  private boolean isPresent(JSType testCase) {
    // Set dom to the immediately dominating variant and testCase to its immediate child
    JSType dom = (JSType)testCase.getParent();
    while (dom != null && !(dom instanceof JSVariant)) {
      testCase = dom;
      dom = (JSType)testCase.getParent();
    }
    // If there's no dominating variant, the testCase must be present
    if (dom == null)
      return true;
    // If the dominating variant has the wrong case, the testCase can't be present
    if (choiceCodes[((JSVariant)dom).getIndex()] != testCase.getSiblingPosition())
      return false;
    // Otherwise, the testCase is present iff the dominating variant is present.
    return isPresent(dom);
  }

  /**
   * The getMultiChoice subroutine is the inverse of setChoices: given a vector of
   * specific choices, it recursively computes the multiChoice code
   */
  static BigInteger getMultiChoice(int[] choices, JSchema schema) throws JMFUninitializedAccessException {
    if (choices == null || choices.length == 0)
      return BigInteger.ZERO;
    JSType topType = (JSType)schema.getJMFType();
    if (topType instanceof JSVariant)
      return getMultiChoice(choices, schema, (JSVariant)topType);
    else if (topType instanceof JSTuple)
      return getMultiChoice(choices, schema, ((JSTuple)topType).getDominatedVariants());
    else
      // If topType is JSRepeated, JSDynamic, JSEnum, or JSPrimitive there can be no unboxed
      // variants at top level.
      return BigInteger.ZERO;
  }

  // Compute the multiChoice code or contribution for an individual variant
  private static BigInteger getMultiChoice(int[] choices, JSchema schema, JSVariant var) throws JMFUninitializedAccessException {
    int choice = choices[var.getIndex()];
    if (choice == -1)
      throw new JMFUninitializedAccessException(schema.getPathName(var));
    BigInteger ans = BigInteger.ZERO;
    // First, add the contribution of the cases less than the present one.
    for (int i = 0; i < choice; i++)
      ans = ans.add(((JSType)var.getCase(i)).getMultiChoiceCount());
    // Now compute the contribution of the actual case.  Get the subvariants dominated by
    // this variant's present case.
    JSVariant[] subVars = var.getDominatedVariants(choice);
    if (subVars == null)
      // There are none: we already have the answer
      return ans;
    return ans.add(getMultiChoice(choices, schema, subVars));
  }

  // Compute the multiChoice code or contribution for a set of variants that are either at
  // top level or are the dominated subvariants of a variant whose multichoice code is
  // being calculate.
  private static BigInteger getMultiChoice(int[] choices, JSchema schema, JSVariant[] vars) throws JMFUninitializedAccessException {
    // Mixed-radix-encode the contribution from all the subvariants
    BigInteger base = BigInteger.ZERO;
    for (int i = 0; i < vars.length; i++)
      base = base.multiply(vars[i].getMultiChoiceCount()).add(getMultiChoice(choices, schema, vars[i]));
    return base;
  }

  /**
   * The embedded Remap class is used by MessageMap so that, for each field actually in
   * the message, the physical location of the field can be efficiently resolved.  There
   * is one Remap for each unboxed non-variant JSField as well as each JSVariant "box".
   * Any field inside a box, and the unboxed JSVariants, are not represented in the Remap
   * array.
   */
  public static class Remap {

    /**
     * The index of the offset table where a particular field's starting offset will be
     * recorded; -1 if the starting offset is 0.
     */
    public int offsetIndex;

    /**
     * A quantity that must be added to the starting offset to get the actual offset
     */
    public int fixedIncr;

    /**
     * Indicates whether length of the field is variable
     */
    public boolean varying;

    /**
     * Constructor
     */
    Remap(int offsetIndex, int fixedIncr, boolean varying) {
      this.offsetIndex = offsetIndex;
      this.fixedIncr = fixedIncr;
      this.varying = varying;
    }
  }
}
