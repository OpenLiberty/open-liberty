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
 * A representation of all primitive types.  These correspond to EDataTypes that are not
 * EEnums in Ecore, and to XSDSimpleType without enumeration facets (stripped of other
 * restricting facets and reduced to base types) in XSD.  Each JMFPrimitiveType has a
 * typecode which represents its type in JMF encoding terms.
 *
 * <p>Associated with each typecode is a Java class instances of which are returned by
 * JMFMessageData.getValue() for the field at runtime.
 *
 * <p>For the convenience of mapping to and from the XSD type system, there is an
 * XSDTypeName property that can be used to store that information.  The mapping from XSD
 * types to JMF primitive types is many to one.
 */

public interface JMFPrimitiveType extends JMFFieldDef {

  /**
   * The typeCode for a boolean field
   */
  public static final int BOOLEAN = 1;

  /**
   * The typeCode for a signed field whose range is that of a Java byte
   */
  public static final int BYTE = 2;

  /**
   * The typeCode for a signed field whose range is that of a Java short
   */
  public static final int SHORT = 3;

  /**
   * The typeCode for an unsigned field whose range is that of a Java char
   */
  public static final int CHAR = 4;

  /**
   * The typeCode for a signed field whose range is that of a Java int
   */
  public static final int INT = 5;

  /**
   * The typeCode for a signed field whose range is that of a Java long
   */
  public static final int LONG = 6;

  /**
   * The typeCode for a floating point field whose range is that of a Java float
   */
  public static final int FLOAT = 7;

  /**
   * The typeCode for a floating point field whose range is that of a Java double
   */
  public static final int DOUBLE = 8;

  /**
   * The typeCode for a field whose semantics are that of a Java String, including the
   * ability to distinguish between a null String and an empty String.
   */
  public static final int STRING = 9;

  /**
   * The typeCode for a field whose contents are an arbitrary precision arbitrary scale
   * decimal value
   */
  public static final int DECIMAL = 10;

  /**
   * The typeCode for a field whose contents are a date and/or time and or duration
   * expressed units of time.
   */
  public static final int DATETIME = 11;

  /**
   * The typeCode for a field whose semantics are that of a Java byte array, including
   * the possibility of distinguishing between a null value and the empty array.  Note:
   * such an array may represent a serialized Java Object.  However, JMF does not have
   * explicit support for Object serialization per se.  This is the responsibility of a
   * higher layer.
   */
  public static final int BINARY = 12;

  /**
   * The typeCode for a field representing any one of the more explicit primitive types.
   * Semantically equivalent to XSD's anySimpleType.  The Java Class representation for
   * this type is reported as <b>java.lang.Object.class</b>, though, in fact, the real
   * representation will be one of those accepted for a more explicit primitive type and
   * not an arbitrary Object.
   */
  public static final int ANYSIMPLETYPE = 13;

  /**
   * The typeCode for a field whose contents are an arbitrary precision integer value
   */
  public static final int BIGINTEGER = 14;

  /**
   * The typeCode for a field containing an encoded reference to some object via its
   * identity
   */
  public static final int IDREF = 15;

  /**
   * The typeCode for a field containing zero or more repetitions of ANYSIMPLETYPE;
   * that is, zero or more repetitions of any of the primitive types.  This definition
   * permits SIMPLELISTs to nest, but, initially at least, we don't expect this case
   * to arise.
   */
  public static final int SIMPLELIST = 16;

  /**
   * The typeCodes for fields whose contents are fixed length Java byte arrays.
   */
  public static final int BYTE8 = 17;
  public static final int BYTE12 = 18;
  
  /**
   * The typeCode for QName objects.
   */
  public static final int QNAME = 19;

  /**
   * Set the compact integer "typecode" used to represent the type.  This must correspond
   * to one of the constants provided by this interface for use in interpreting the
   * typecode.
   */
  public void setTypeCode(int typeCode);

  /**
   * Retrieve the XSD Schema built-in type name.  This is only available if it has been
   * set (it is not derived from the typeCode) and it is not propagated with the schema
   */
  public String getXSDTypeName();

  /**
   * Retrieve the Java class used to represent the type
   */
  public Class getJavaClass();
  
  /**
   * Retreive the size of the Java class used to represent the type.  This may be useful
   * when a potentially variable length Java class is used to represent a fixed size 
   * type, for example a Java byte[] is used to represent the fixed 8-byte "byte8" type
   * This will return -1 for variable length types, or types not represented by a 
   * potentially variable length array. 
   */
  public int getJavaClassSize();

  /**
   * Retrieve the compact integer "typecode" used to represent the type.  This interface
   * also provides constants used in interpreting the typecode.
   */
  public int getTypeCode();

  /**
   * Set the XSD type name for the type, also setting the typeCode to the best available
   * JMF type for encoding that XSD type.  This is provided as a convenience.  The XSD
   * type name does not propagate with the schema (only the JMF typecode does).
   */
  public void setXSDTypeName(String name);
}
