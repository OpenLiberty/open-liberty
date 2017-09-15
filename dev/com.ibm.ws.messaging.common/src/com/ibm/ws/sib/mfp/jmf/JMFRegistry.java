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
 * The JMFRegistry provides the registry and factory methods needed to support
 * bootstrapping and extension of the JMF interface.  It supports registration and
 * retrieval of JMFSchemas and JMFEncapsulationManagers.  It supports creation of
 * JMFMessages for incoming byte arrays and creation of empty JMFMessages to be populated
 * by local JMFI calls.
 *
 * <p>Usage of this class is as a single-instance.  The <b>instance</b> static variable
 * holds the single instance.
 */

import com.ibm.ws.sib.mfp.jmf.impl.JSRegistry;

public interface JMFRegistry {

  /**
   * The current JMF encoding version
   */
  public final static int JMF_ENCODING_VERSION = 1;

  /**
   * The single instance of this class
   */
  public final static JMFRegistry instance = new JSRegistry();

  /**
   * Register the JMFEncapsulationManager for a particular Model ID.
   *
   * @param mgr the JMFEncapsulationManager being registered
   * @param id the model ID for which registration is occuring
   */
  public void register(JMFEncapsulationManager mgr, int id);

  /**
   * Retrieve the JMFEncapsulationManager for a particular Model ID.
   *
   * @param id the model ID whose JMFEncapsulationManager is desired
   * @return the desired JMFEncapsulationManager or null if it isn't registered
   */
  public JMFEncapsulationManager retrieve(int id);

  /**
   * Register a JMFSchema
   *
   * @param schema the Schema to register
   */
  public void register(JMFSchema schema) throws JMFSchemaIdException;

  /**
   * Register a JMFSchema and all JMFSchemas in the transitive closure of the
   * 'dependent' relation where the dependents are the result of calling
   * JMFDynamic.getExpectedSchema() on all the JMFDynamic nodes of a schema.
   *
   * @param schema the Schema to register, along with its dependents
   */
  public void registerAll(JMFSchema schema) throws JMFSchemaIdException;

  /**
   * Retrieve a JMFSchema by its id.
   *
   * @param id the long identifier for the schema
   * @return the JMFSchema or null if it is not there
   */
  public JMFSchema retrieve(long id);

  /**
   * Retrieve a JMFSchema by the Object recorded in the association property of its
   * top-level JMFType (typically, this is an Ecore model, but, by design we don't
   * make JMF dependent on EMF).
   *
   * @param association the Object recorded in the association property of the
   * top-level JMFType of a JMFSchema.  Retrieval of JMFSchemas with null association
   * properties is not supported via this interface, however.
   * @return the JMFSchema whose top-level JMFType has association as its association
   * property.
   */
  public JMFSchema retrieve(Object association);

  /**
   * Retrieve all registered schemas
   *
   * @return Array of registered schemas
   */
  public JMFSchema[] retrieveAll();

  /**
   * Get a new empty JMFMessage for a given schema using the latest version of the JMF
   * encoding
   *
   * @param schema the JMFSchema to use
   * @return a JMFMessage for the schema
   */
  public JMFMessage newMessage(JMFSchema schema);

  /**
   * Get a new empty JMFNativePart for a given schema using the latest version of the
   * JMF encoding.  Note that newMessage also returns a JMFNativePart because JMFMessage
   * extends JMFNativePart.  But this method should be used when the returned part does
   * not need to be a message.
   *
   * @param schema the JMFSchema to use
   * @return a JMFNativePart for the schema
   */
  public JMFNativePart newNativePart(JMFSchema schema);

  /**
   * Decode a message byte array using the latest version of the JMF encoding.  This
   * method is a convenience for cases where the schema is well-known and the message is
   * known to have been encoded with the latest JMF version.  The more general method
   * should be used for messages received from remote systems where a JMF version id and
   * schema id have been retrieved from the message frame.
   *
   * @param schema the JMFSchema describing the message
   * @param contents the byte array containing the message contents
   * @param offset the starting offset of the message contents within 'contents.'  This
   * excludes any framing material that is not the business of the schema interpreter.
   * @param length the length of the message contents within 'contents' that is being
   * provided.
   * @exception MessageCorruptionException if the message contents byte array is found
   * to be corrupt.
   */
  public JMFMessage decode(JMFSchema schema, byte[] contents, int offset, int length)
    throws JMFMessageCorruptionException;

  /**
   * Decode a message byte array using a specific JMF version, specific encoding schema,
   * and a specific access schema.  The version and encoding schema are specified as ids,
   * which is the form in which the information is typically received from remote systems.
   * This is the most general form of decode, in that it is prepared to insert a
   * compatibility layer if the schemas are different but compatible and is prepare to use
   * older versions of JMF that are present for backward compatibility.
   *
   * @param access the access JMFSchema to use
   * @param version the version of JMF used to encode the message (typically received in
   * the message frame).
   * @param encoding the id of the encoding JMFSchema used to encode the message (typically
   * received in the message frame).
   * @param contents the byte array containing the message contents
   * @param offset the starting offset of the message contents within 'contents.'  This
   * excludes any framing material that is not the business of the schema interpreter.
   * @param length the length of the message contents within 'contents' that is being
   * provided.
   * @exception SchemaViolationException if the encoding schema is not locally known (due
   * to a failure in schema propagation logic) or if the access schema is not compatible
   * with the encoding schema.
   * @exception MessageCorruptionException if the message contents byte array is found
   * to be corrupt.
   */
  public JMFMessage decode(JMFSchema access, short version, long encoding, byte[] contents, int offset, int length)
    throws JMFSchemaViolationException, JMFMessageCorruptionException;

  /**
   * Check that all schemas are available to decode the given message
   * contents.
   *
   * @param version the version of JMF used to encode the message
   * @param contents the byte array containing the message contents
   * @param offset the starting offset of the message contents within 'contents.'
   * @return an array of schema ids for any unknown schemas
   * @throws JMFMessageCorruptionException if the message contents byte array is found
   * to be corrupt.
   */
  public long[] checkSchemata(short version, byte[] contents, int offset)
    throws JMFMessageCorruptionException;

  /**
   * Create a new JMFPrimitiveType
   */
  public JMFPrimitiveType createJMFPrimitiveType();

  /**
   * Create a new JMFEnumType
   */
  public JMFEnumType createJMFEnumType();

  /**
   * Create a new JMFDynamicType
   */
  public JMFDynamicType createJMFDynamicType();

  /**
   * Create a new JMFVariantType
   */
  public JMFVariantType createJMFVariantType();

  /**
   * Create a new JMFRepeatedType
   */
  public JMFRepeatedType createJMFRepeatedType();

  /**
   * Create a new JMFTupleType
   */
  public JMFTupleType createJMFTupleType();

  /**
   * Create a new JMFSchema from a JMFType tree that was either (1) constructed via JMFI
   * calls, (2) translated from an Ecore model or other notation using one of the
   * translation tools, (3) read from a file using the JSParser tool, (4) instantiated
   * from a class that was generated using the JSchemaTool.
   *
   * @param typeTree the JMFType that is the root of the type definition for the schema
   * @return a JMFSchema representing that type tree
   */
  public JMFSchema createJMFSchema(JMFType typeTree);

  /**
   * Create a new JMFSchema from its transfer encoding (a JMFSchema provides its transfer
   * encoding when you call its toByteArray() method).
   *
   * @param frame a byte array containing the transfer encoding of the JMFSchema and
   * nothing else
   * @exception IllegalStateException if the transfer encoding is not a valid encoding of
   * a JMFSchema.
   */
  public JMFSchema createJMFSchema(byte[] frame);

  /**
   * Create a new JMFSchema from its transfer encoding (a JMFSchema provides its transfer
   * encoding when you call its toByteArray() method).  This version of the method does
   * not assume that the received byte array contains only the JMFSchema; it may contain
   * other material as well.
   *
   * @param frame a byte array containing the transfer encoding of the JMFSchema along
   * with possibly other information.
   * @param offset the offset in frame where the encoding of the JMFSchema begins
   * @param length the length of the JMFSchema's transfer encoding
   * @exception IllegalStateException if the transfer encoding is not a valid encoding of
   * a JMFSchema.
   */
  public JMFSchema createJMFSchema(byte[] frame, int offset, int length);

  /**
   * Return a typeCode that is valid for a JMFPrimitiveType with a given Java class as
   * its (external or internal) representation.  If more than one typeCode uses the same
   * Java class, the one with the lowest typeCode is returned.  If there is no typeCode
   * that employs the given Java class, -1 is returned.
   *
   * @param cls the Java class whose corresponding typeCode is desired
   * @return the typeCode corresponding to <b>cls</b> or -1 if <b>cls</b> is not a valid
   * representation for any JMFPrimitiveType currently defined.
   */
  public int getTypeCode(Class cls);

  /**
   * Copy the contents of one JMFNativePart into another.
   *
   * @param source the source JMFNativePart from which copying is to be done
   * @param target the target (empty) JMFNativePart into which copying is to be done
   * @param deep if true, any JMFPart objects that are encountered during the copy should
   * be transcribed to JMFNativeParts if they are not already JMFNativeParts.  If false,
   * any JMFPart objects encountered are left in their current form and are aliased in the
   * target JMFNativePart rather than being cloned.
   */
  public void copy(JMFNativePart source, JMFNativePart target, boolean deep)
    throws JMFSchemaViolationException,
           JMFModelNotImplementedException,
           JMFUninitializedAccessException,
           JMFMessageCorruptionException;
}
