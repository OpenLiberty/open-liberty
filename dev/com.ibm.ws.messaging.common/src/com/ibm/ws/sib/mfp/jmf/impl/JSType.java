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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.ws.sib.mfp.jmf.JMFType;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 * The JSType class is the base class for all nodes in a Jetstream Schema Type tree.  The
 * JSType tree provides a pure type-structure view of a schema, which can originate as and
 * be manifested as either an XML Schema or Ecore model.
 */

public abstract class JSType implements JMFType {
  private static TraceComponent tc = JmfTr.register(JSType.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // The feature name.
  private String featureName;

  // Link this JSType to its parent in the JSType tree, if any
  JSType parent;

  // The (zero-origin) sibling position that this type occupies in its parent (-1 if
  // parent is null).
  int siblingPosition = -1;

  // The multiChoice count for this type.  All types have a multiChoice count of one,
  // except tuples and variants.
  BigInteger multiChoiceCount = BigInteger.ONE;

  // The "Association" property, which can be used for any reasonable purpose by tools
  // that translate to JMFTypes from other schema notations.  It is intended to capture
  // extra information not relevant to JMF but useful in the domains covered by those
  // other notations, e.g., to support "round-trip" translation, etc.  The EcoreConverter
  // tool will use this to point to corresponding EModelElements in the Ecore
  // representation of the same schema.
  private Object association;

  // The referenceable flag
  private boolean referenceable;

  /**
   * Return the feature name.  The result corresponds to the EStructuralFeature.Name
   * property in Ecore and to the XSDFeature.Name property in XSD.
   */
  public String getFeatureName() {
    return featureName;
  }

  /**
   * Set the feature name of this JSType.  This should be a valid NCName by XML rules
   * (namespace qualification is handled separately). It should correspond to the
   * EStructuralFeature.Name property in Ecore and to the XSDFeature.Name property in XSD.
   * It is permissible for FeatureName to be omitted; this results in an anonymous
   * EStructuralFeature or XSDFeature at translation time.  Those models are usable when
   * not all features are named, but serialization to certain readable formats may fail.
   */
  public void setFeatureName(String name) {
    featureName = name;
  }

  /**
   * Retrieve the parent node in the JSType tree or null if this is the root node.  Note
   * that a JSDynamic node is <em>not</em> the parent of its <b>expectedType</b> (that is
   * considered to be the root of a new tree).
   */
  public JMFType getParent() {
    return parent;
  }

  /**
   * Retrieve the (zero-origin) sibling position that this JSType occupies in its parent
   * or -1 if this is the root.
   */
  public int getSiblingPosition() {
    return siblingPosition;
  }

  // Implement getAssociation
  public Object getAssociation() {
    return association;
  }


  // Implement setAssociation
  public void setAssociation(Object assoc) {
    association = assoc;
  }

  // Implement isReferenceable
  public boolean isReferenceable() {
    return referenceable;
  }

  // Implement setReferenceable
  public void setReferenceable(boolean referenceable) {
    this.referenceable = referenceable;
  }

  // Implement generic updateAssociations.  Subclasses may need to override this method
  // (and probably invoke the super version) if they need to more than simply update
  // the three instance variables.
  public void updateAssociations(JMFType type) {
    // The type we are updating from must exactly match our own type
    if (!getClass().isInstance(type))
      throw new IllegalStateException();
    association = type.getAssociation();
    featureName = type.getFeatureName();
    referenceable = type.isReferenceable();
  }

  /**
   * Get the multiChoice count for this type.
   */
  public BigInteger getMultiChoiceCount() {
    return multiChoiceCount;
  }

  // Set the multiChoice count for this type.  All types have a multiChoice count of one,
  // except tuples and variants, which override this method.
  BigInteger setMultiChoiceCount() {
    // Nothing to do here, as the default multiChoiceCount is one and is already set.
    return multiChoiceCount;
  }

  /**
   * Convert a JSType to the minimalist syntax form accepted by the JSParser
   */
  public String toString() {
    StringBuffer fmt = new StringBuffer();
    Set todo = new HashSet();
    Set done = new HashSet();
    done.add(this);
    format(fmt, done, todo, 0);
    while (!todo.isEmpty()) {
      Set newTodo = new HashSet();
      for (Iterator iter = todo.iterator(); iter.hasNext();) {
        JSType subSchema = (JSType) iter.next();
        if (subSchema != null) {
          fmt.append(",\n");
          done.add(subSchema);
          subSchema.format(fmt, done, newTodo, 0);
        }
      }
      todo = newTodo;
    }
    return fmt.toString();
  }

  // Formats a name (subroutine of toString).
  void formatName(StringBuffer fmt, int indent) {
    indent(fmt, indent);
    String name = getFeatureName();
    if (name != null && name.length() > 0)
      fmt.append(name).append(": ");
  }

  // Indents within a format buffer
  void indent(StringBuffer fmt, int indent) {
    for (int i = 0; i < indent; i++)
      fmt.append(" ");
  }

  // Every JSType specializes this method to accomplish formatting into the JSParser
  // syntax.
  public abstract void format(StringBuffer fmt, Set done, Set toDo, int indent);

  // Byte codes assigned to the six kinds of JSType (used by JSchema methods and subclass
  // constructors and encode methods to convert JSType trees to/from byte array form).
  static final int DYNAMIC = 1;
  static final int PRIMITIVE = 2;
  static final int TUPLE = 3;
  static final int VARIANT = 4;
  static final int ARRAY = 5;
  static final int ENUM = 6;

  // Method to create a JSType from its byte array form
  static JSType createJSType(byte[] frame, int[] limits) {
    byte kind = getByte(frame, limits);
    switch (kind) {
      case PRIMITIVE :
        return new JSPrimitive(frame, limits);
      case DYNAMIC :
        return new JSDynamic(); // nothing more to record
      case ARRAY :
        return new JSRepeated(frame, limits);
      case TUPLE :
        return new JSTuple(frame, limits);
      case VARIANT :
        return new JSVariant(frame, limits);
      case ENUM :
        return new JSEnum(frame, limits);
      default :
        return null;
    }
  }

  // Subroutine to get one byte from a frame given a 'limits' array
  static byte getByte(byte[] frame, int[] limits) {
    if (limits[0] < limits[1])
      return frame[limits[0]++];
    else
      throw new IllegalStateException();
  }

  /**
   * Subroutine to get a two byte count from a frame given a 'limits' array
   */
  public static int getCount(byte[] frame, int[] limits) {
    int ans = getByte(frame, limits) << 8;
    return ans | (getByte(frame, limits) & 0xff);
  }

  // Subroutine to set one byte into a frame given a 'limits' array
  static void setByte(byte[] frame, int[] limits, byte val) {
    if (limits[0] < limits[1])
      frame[limits[0]++] = val;
    else
      throw new IllegalStateException();
  }

  /**
   * Subroutine to set a two byte count into a frame given a 'limits' array
   */
  public static void setCount(byte[] frame, int[] limits, int count) {
    setByte(frame, limits, (byte) (count >>> 8));
    setByte(frame, limits, (byte)count);
  }

  // Every JSType specializes these two methods for use during conversion to byte array form
  public abstract void encodeType(byte[] frame, int[] limits);
  public abstract int encodedTypeLength();
}
