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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.jmf.JMFMessageData;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFPrimitiveType;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;

/**
 * A representation of all primitive types.  These correspond to EDataTypes that are not
 * EEnums in Ecore, and to XSDSimpleType without enumeration facets (stripped of other
 * restricting facets and reduced to base types) in XSD.  Each JSPrimitive records
 *
 * <ul>
 *
 * <li>the XSD Schema type name
 *
 * <li>the Java class whose instances are used to represent the type programmatically
 *
 * <li>an integer "type code" that stands for the type in Jetstream schema propagations
 * and other contexts where a compact representation of the type is needed
 *
 * <li>the length occupied by values of the type when serialized to JMF or an indication
 * that such lengths vary from instance to instance
 *
 * <li>methods to serialize, deserialize, and determine the serialization lengths of
 * values of the type in JMF.
 *
 * </ul>
 *
 * This class is designed to be instantiated for each leaf in a JSchema.  The separate
 * JSBaseTypes class contains the definitions of all the primitive types in Jetstream's
 * repertoire.  Information in the JSBaseTypes class is not unnecessarily repeated in this
 * class.
 */

public final class JSPrimitive extends JSField implements JMFPrimitiveType {
  private static TraceComponent tc = JmfTr.register(JSPrimitive.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // The base type information for this JSPrimitive.
  private JSBaseTypes baseType;

  // The typeCode for this JSPrimitive
  private int typeCode;

  // The XSD type name (if set)
  private String xsdTypeName;

  /**
   * Basic constructor
   */
  public JSPrimitive() {
  }

  /**
   * Retrieve the XSD Schema built-in type name
   */
  public String getXSDTypeName() {
    return xsdTypeName;
  }

  /**
   * Set the XSD type name, and, as a side-effect, set the type code most appropriate for
   * use with that type
   */
  public void setXSDTypeName(String name) {
    this.xsdTypeName = name;
    Integer code = (Integer)JSBaseTypes.xsdTypeNames.get(name);
    if (code == null)
      throw new IllegalArgumentException("XSDTypeName=" + name);
    setTypeCode(code.intValue());
  }

  /**
   * Retrieve the Java class used to represent the type
   */
  public Class getJavaClass() {
    return baseType.javaClass;
  }

  /**
   * Retrieve the size of a variable length representation of a fixed length type.
   */
  public int getJavaClassSize() {
    return baseType.javaClassSize;
  }

  /**
   * Retrieve the compact integer "typecode" used to represent the type.
   */
  public int getTypeCode() {
    return typeCode;
  }

  /**
   * Retrieve the encoding length, based on the type class.  A value of -1 means variable
   * length.
   */
  public int getLength() {
    return baseType.length;
  }

  // Implementation of JSCoder interface (delegates via JSBaseTypes table)
  public int getEncodedLength(Object val, int indirect, JMFMessageData msg)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    return baseType.coder.getEncodedLength(val, 0, msg);
  }

  public Object validate(Object val, int indirect)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    return baseType.coder.validate(val, 0);
  }

  public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    return baseType.coder.encode(frame, offset, val, 0, msg);
  }

  public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    return baseType.coder.decode(frame, offset, 0, msg);
  }

  public Object copy(Object val, int indirect)
      throws JMFSchemaViolationException {
    return baseType.coder.copy(val, 0);
  }

  public int estimateUnassembledSize(Object val) {
    return baseType.coder.estimateUnassembledSize(val);
  }

  public int estimateUnassembledSize(byte[] frame, int offset) {
    return baseType.coder.estimateUnassembledSize(frame, offset);
  }

  /**
   * Set the typeCode for the type.  Doing this automatically sets all the other type
   * information implied by the type.
   */
  public void setTypeCode(int typeCode) {
    if (typeCode <= 0 || typeCode >= JSBaseTypes.baseTypes.length)
      throw new IllegalArgumentException("TypeCode=" + typeCode);
    this.typeCode = typeCode;
    baseType = JSBaseTypes.baseTypes[typeCode];
  }

  // Format for printing (subroutine of toString)
  public void format(StringBuffer fmt, Set done, Set todo, int indent) {
    formatName(fmt, indent);
    fmt.append(typeNames[typeCode]);
  }

  private String[] typeNames =
    {
      null,
      "boolean",
      "byte",
      "short",
      "unsignedShort",
      "int",
      "long",
      "float",
      "double",
      "string",
      "decimal",
      "dateTime",
      "hexBinary",
      "anySimpleType",
      "integer",
      "IDREF",
      "NMTOKENS",
      "byte8",
      "byte12",
      "QName",
    };

  // Constructor from byte array form
  JSPrimitive(byte[] frame, int[] limits) {
    typeCode = getByte(frame, limits);
    baseType = JSBaseTypes.baseTypes[typeCode];
  }

  // Implementation of encodedTypeLength
  public int encodedTypeLength() {
    return 2;
  }

  // Implementation of encodeType
  public void encodeType(byte[] frame, int[] limits) {
    setByte(frame, limits, (byte)PRIMITIVE);
    setByte(frame, limits, (byte)typeCode);
  }
}
