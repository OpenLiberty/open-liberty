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

import java.util.Map;
import java.util.HashMap;

import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFRegistry;
import com.ibm.ws.sib.mfp.jmf.JMFEncapsulationManager;
import com.ibm.ws.sib.mfp.jmf.JMFPart;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFMessage;
import com.ibm.ws.sib.mfp.jmf.JMFType;
import com.ibm.ws.sib.mfp.jmf.JMFPrimitiveType;
import com.ibm.ws.sib.mfp.jmf.JMFEnumType;
import com.ibm.ws.sib.mfp.jmf.JMFDynamicType;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JMFVariantType;
import com.ibm.ws.sib.mfp.jmf.JMFRepeatedType;
import com.ibm.ws.sib.mfp.jmf.JMFTupleType;
import com.ibm.ws.sib.mfp.jmf.JMFNativePart;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaIdException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.mfp.util.HashedArray;

import com.ibm.websphere.ras.TraceComponent;

/**
 * The JSRegistry provides the registry and factory methods needed by the JMF
 * implementation.  It supports registration and retrieval of JSchemas, and
 * JMFEncapsulation Managers.  It supports creation of JSMessageImpls for incoming byte
 * arrays and creation of empty JSMessageImpls to be populated by local JMFI calls.
 *
 * <p>Usage of this class is as a single-instance.  The <b>instance</b> static variable
 * holds the single instance and other classes in the JMF implementation refer to it.  A
 * single-instance class is used rather than a static-only class as a reminder that we
 * might want to move in the direction of supporting multiple JMFRegistries per process,
 * although we do not do so at present.
 */

public final class JSRegistry implements JMFRegistry {
  private static TraceComponent tc = JmfTr.register(JSRegistry.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // Storage for registered schemas
  private HashedArray schemaTable = new HashedArray(8191, 2);

  // Map used to retrieve Schemas by their association Objects
  private Map associations = new HashMap();

  // Storage for JMFEncapsulationManagers.
  private Map mmmgrTable = new HashMap();

  private JSNativePartCopier copier = null;

  // Storage for JSchemaInterpreters.  The current one, implemented as
  // JSchemaInterpreterImpl, is always present.  Additional ones (typically to support
  // compatibility with older verisons) may also be present.
  private JSchemaInterpreter[] interpTab = new JSchemaInterpreter[JMF_ENCODING_VERSION];
  {
    interpTab[JMF_ENCODING_VERSION - 1] = new JSchemaInterpreterImpl();
  }

  /**
   * Register the JMFEncapsulationManager for a particular Model ID.
   *
   * @param mgr the JMFEncapsulationManager being registered
   * @param id the model ID for which registration is occuring
   */
  public void register(JMFEncapsulationManager mgr, int id) {
    if (id > JMFPart.MODEL_ID_JMF)
      mmmgrTable.put(Integer.valueOf(id - 1), mgr);
   else
      throw new IllegalArgumentException("model ID cannot be negative");
  }

  /**
   * Retrieve the JMFEncapsulationManager for a particular Model ID.
   *
   * @param id the model ID whose JMFEncapsulationManager is desired
   * @return the desired JMFEncapsulationManager or null if it isn't registered
   */
  public JMFEncapsulationManager retrieve(int id) {
     return (JMFEncapsulationManager)mmmgrTable.get(Integer.valueOf(id - 1));
  }

  /**
   * Return the schema interpreter associated with an ID
   */
  public JSchemaInterpreter getInterpreter(short id) {
    return interpTab[id - 1];
  }

  /**
   * Registers a single schema
   *
   * @param schema the Schema to register
   */
  public void register(JMFSchema schema) throws JMFSchemaIdException {
    if (!isPresent(schema))
      registerInternal(schema);
  }

  // Subroutine used by register and registerAll
  private void registerInternal(JMFSchema schema) {
    schemaTable.set((HashedArray.Element)schema);
    Object assoc = schema.getJMFType().getAssociation();
    if (assoc != null)
      associations.put(assoc, schema);
  }

  // Determine if a schema is already present and check for schema id collisions.
  // If the new schema has a non-null association, the old schema is updated with it.
  // Note: in the long run, we need some better arbitration for multiple assocations
  // to the same schema.
  private boolean isPresent(JMFSchema schema) throws JMFSchemaIdException {
    long id = schema.getID();
    JMFSchema reg = (JMFSchema)schemaTable.get(id);
    if (reg != null) {
      // We are assuming that collisions on key don't happen
      if (!schema.equals(reg)) {
        // The schema id is a 64bit SHA-1 derived hashcode, so we really don't expect
        // to get random collisions.
        throw new JMFSchemaIdException("Schema id clash: id=" + id);
      }
      Object newAssoc = schema.getJMFType().getAssociation();
      Object oldAssoc = reg.getJMFType().getAssociation();
      if (newAssoc != oldAssoc && newAssoc != null) {
        if (oldAssoc != null)
          associations.remove(oldAssoc);
        reg.getJMFType().updateAssociations(schema.getJMFType());
        associations.put(newAssoc, reg);
      }
      return true;
    }
    else
      return false;
  }

  // Implement registerAll
  public void registerAll(JMFSchema schema) throws JMFSchemaIdException {
    if (isPresent(schema))
      return;
    registerInternal(schema);
    registerDependents(schema.getJMFType());
  }

  // Subroutine to register dependents
  private void registerDependents(JMFType type) throws JMFSchemaIdException {
    if (type instanceof JMFDynamicType) {
      JMFSchema schema = ((JMFDynamicType) type).getExpectedSchema();
      if (schema != null)
        registerAll(schema);
    }
    else if (type instanceof JMFRepeatedType)
      registerDependents(((JMFRepeatedType)type).getItemType());
    else if (type instanceof JMFTupleType) {
      JMFTupleType tup = (JMFTupleType) type;
      for (int i = 0; i < tup.getFieldCount(); i++)
        registerDependents(tup.getField(i));
    }
    else if (type instanceof JMFVariantType) {
      JMFVariantType var = (JMFVariantType) type;
      for (int i = 0; i < var.getCaseCount(); i++)
        registerDependents(var.getCase(i));
    }
    // else there are no dependents and we do nothing
  }

  /**
   * Retrieves a JMFSchema by its id.
   *
   * @param id the long identifier for the schema
   * @return the JMFSchema or null if it is not there
   */
  public JMFSchema retrieve(long id) {
    return (JMFSchema)schemaTable.get(id);
  }

  /**
   * Retrieve all currently registered JMFSchemas
   *
   * @return array of registered schemas
   */
  public JMFSchema[] retrieveAll() {
    // The schemaTable must not have any elements added or removed between the call
    // to size() and the call to toArray(), otherwise it could fall in a crumbling heap!
    synchronized (schemaTable) {
      return (JMFSchema[])schemaTable.toArray(new JMFSchema[schemaTable.size()]);
    }
  }

  /**
   * Get a new empty JMFMessage for a given schema using the latest interpreter
   *
   * @param schema the JSchema to use
   * @return a JMFMessage for the schema
   */
  public JMFMessage newMessage(JMFSchema schema) {
    return interpTab[JMF_ENCODING_VERSION - 1].newMessage((JSchema)schema);
  }

  /**
  * Get a new empty JMFNativePart for a given schema using the latest version of the
  * JMF encoding.  Note that newMessage also returns a JMFNativePart because JMFMessage
  * extends JMFNativePart.  But this method should be used when the returned part does
  * not need to be a message.
  *
  * @param schema the JMFSchema to use
  * @return a JMFNativePart for the schema
  */
  public JMFNativePart newNativePart(JMFSchema schema) {
    // Note: in the present implementation newNativePart just executes newMessage.
    // But, the fact that we use the same implementation for JMFMessage and
    // non-message JMFNativePart is an implementation choice and future evolutions of the
    // implementation may change this.
    return newMessage(schema);
  }

  /**
   * Decode a message byte array using the latest interpreter.  This method is a
   * convenience for cases where the schema is well-known and the interpreter is known to
   * be the latest.  The more general method should be used for messages received from
   * remote systems where an interpreter id and schema id have been retrieved from the
   * message frame.
   *
   * @param schema the JSchema describing the message
   * @param contents the byte array containing the message contents
   * @param offset the starting offset of the message contents within 'contents.'  This
   * excludes any framing material that is not the business of the schema interpreter.
   * @param length the length of the message contents within 'contents' that is being
   * provided.
   * @exception SchemaViolationException if the encoding schema is not locally known (due
   * to a failure in schema propagation logic)
   * @exception MessageCorruptionException if the message contents byte array is found
   * to be corrupt.
   */
  public JMFMessage decode(JMFSchema schema, byte[] contents, int offset, int length)
      throws JMFMessageCorruptionException {
    return interpTab[JMF_ENCODING_VERSION - 1].decode((JSchema)schema, contents, offset, length);
  }

  /**
   * Decode a message byte array using a specific interpreter, specific encoding schema,
   * and a specific access schema.  The interpreter and encoding schema are specified as
   * ids, which is the form in which the information is typically received from remote
   * systems.  This is the most general form of decode, in that it is prepared to insert a
   * compatibility layer if the schemas are different but compatible.
   *
   * @param access the access JMFSchema to use
   * @param interp the id of the interpreter that encoded the message (typically received
   * in the message frame).
   * @param encoding the id of the encoding JSchema used to encode the message (typically
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
  public JMFMessage decode(JMFSchema access, short interp, long encoding,
                           byte[] contents, int offset, int length)
      throws JMFSchemaViolationException, JMFMessageCorruptionException {
    if (access.getID() == encoding)
      return interpTab[interp - 1].decode((JSchema)access, contents, offset, length);
    JSchema encSchema = (JSchema)retrieve(encoding);
    if (encSchema == null) throw new JMFSchemaViolationException("schemaId=null");
    JMFMessage encMsg = interpTab[interp - 1].decode(encSchema, contents, offset, length);
    return new JSCompatibleMessageImpl((JSchema)access, encMsg);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.jmf.JMFRegistry#checkSchemata(short, byte[], int)
   */
  public long[] checkSchemata(short interp, byte[] contents, int offset)
      throws JMFMessageCorruptionException {
    return interpTab[interp - 1].checkSchemata(contents, offset);
  }

  /**
   * For testing purposes (only) reset the schemaTable to the empty state.
   */
  public void clear() {
    schemaTable = new HashedArray(8191, 2);
    associations = new HashMap();
  }

  /**
   * For testing purposes (only) it is useful to be able to selectively remove
   * schemas from the registry.
   */
  public void remove(long id) {
    JMFSchema oldSchema = (JMFSchema)schemaTable.remove(id);
    if (oldSchema != null)
      associations.remove(oldSchema.getJMFType().getAssociation());
  }

  /**
   * For testing purposes (only) it is useful to be able to remove all schemas
   * from the registry that have associations.
   */
  public void removeAssociations() {
    JMFSchema[] schemas = (JMFSchema[])associations.values().toArray(new JMFSchema[0]);
    for (int i = 0; i < schemas.length; i++)
      remove(schemas[i].getID());
  }

  // Implement the creation methods
  public JMFPrimitiveType createJMFPrimitiveType() {
    return new JSPrimitive();
  }

  public JMFEnumType createJMFEnumType() {
    return new JSEnum();
  }

  public JMFDynamicType createJMFDynamicType() {
    return new JSDynamic();
  }

  public JMFVariantType createJMFVariantType() {
    return new JSVariant();
  }

  public JMFRepeatedType createJMFRepeatedType() {
    return new JSRepeated();
  }

  public JMFTupleType createJMFTupleType() {
    return new JSTuple();
  }

  public JMFSchema createJMFSchema(JMFType type) {
    return new JSchema((JSType)type);
  }

  public JMFSchema createJMFSchema(byte[] frame) {
    return new JSchema(frame);
  }

  public JMFSchema createJMFSchema(byte[] frame, int offset, int length) {
    return new JSchema(frame, offset, length);
  }

  // Implement the getTypeCode method
  public int getTypeCode(Class cls) {
    Integer ans = (Integer) JSBaseTypes.classTypes.get(cls);
    if (ans == null)
      return -1;
    else
      return ans.intValue();
  }

  // Implement the copy method
  public void copy(JMFNativePart source, JMFNativePart target, boolean deep)
    throws JMFSchemaViolationException,
           JMFModelNotImplementedException,
           JMFUninitializedAccessException,
           JMFMessageCorruptionException
  {
    if(copier == null)
    {
      copier = new JSNativePartCopier();
    }
    copier.copy(source, target, deep);
  }

  // Implement retrieve by association
  public JMFSchema retrieve(Object association) {
    return (JMFSchema) associations.get(association);
  }
}
