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

import java.util.Set;

import com.ibm.ws.sib.mfp.jmf.JMFType;
import com.ibm.ws.sib.mfp.jmf.JMFRepeatedType;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 * A representation of all array-like types in a Jetstream schema.  These correspond to
 * EStructuralFeature nodes with isMany()==true in Ecore or to XSDParticle nodes with
 * maxOccurs>1 in XSD.  Note that all JSRepeated nodes created by JSParser will have
 * minOccurs=0 and maxOccurs=unbounded, but finer-grained distinctions can be made when
 * the schema was translated from an XSD or Ecore model.
 */

public final class JSRepeated extends JSType implements JMFRepeatedType {
  private static TraceComponent tc = JmfTr.register(JSRepeated.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // The limits {minOccurs, maxOccurs} of the array.  "Unbounded" is indicated by -1.  If
  // null, corresponds to { 0, -1 }.
  private int[] limits;

  // The type of each item in the array
  private JSType itemType;

  /**
   * Default constructor
   */
  public JSRepeated() {
  }

  /**
   * Gets the lower size limit of the array
   */
  public int getMinOccurs() {
    return (limits == null) ? 0 : limits[0];
  }

  /**
   * Gets the upper size limit of the array (-1 indicates "unbounded")
   */
  public int getMaxOccurs() {
    return (limits == null) ? -1 : limits[1];
  }

  /**
   * Get the array's item type
   */
  public JMFType getItemType() {
    return itemType;
  }

  /** Set the bounds (default is {0, unbounded}).  Use maxOccurs=-1 to indicate
   * "unbounded."
   */
  public void setBounds(int minOccurs, int maxOccurs) {
    if (minOccurs < 0 || maxOccurs < -1)
      throw new IllegalArgumentException("Bounds cannot be negative");
    else if (maxOccurs > 0 && minOccurs > maxOccurs)
      throw new IllegalArgumentException("Minimum bounds less than maximum bounds");
    limits = new int[] { minOccurs, maxOccurs };
  }

  /**
   * Set the item type of the array
   */
  public void setItemType(JMFType elem) {
    if (elem == null)
      throw new NullPointerException("Repeated item cannot be null");
    itemType = (JSType)elem;
    itemType.parent = this;
    itemType.siblingPosition = 0;
  }

  // Convert to printable form (subroutine of toString)
  public void format(StringBuffer fmt, Set done, Set todo, int indent) {
    formatName(fmt, indent);
    fmt.append("*(\n");
    itemType.format(fmt, done, todo, indent + 2);
    fmt.append("\n");
    indent(fmt, indent);
    fmt.append(")*");
  }

  // Constructor from byte array
  JSRepeated(byte[] frame, int[] limits) {
    setItemType(createJSType(frame, limits));
  }

  // Implementation of encodedTypeLength
  public int encodedTypeLength() {
    return 1 + itemType.encodedTypeLength();
  }

  // Implementation of encodeType
  public void encodeType(byte[] frame, int[] limits) {
    setByte(frame, limits, (byte)ARRAY);
    itemType.encodeType(frame, limits);
  }

  // Implementation of updateAssociations
  public void updateAssociations(JMFType type) {
    super.updateAssociations(type);
    itemType.updateAssociations(((JSRepeated)type).getItemType());
  }
}
