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
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.jmf.JMFDynamicType;
import com.ibm.ws.sib.mfp.jmf.JMFEncapsulation;
import com.ibm.ws.sib.mfp.jmf.JMFEncapsulationManager;
import com.ibm.ws.sib.mfp.jmf.JMFMessageData;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFPart;
import com.ibm.ws.sib.mfp.jmf.JMFRegistry;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFType;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.mfp.util.ArrayUtil;

/**
 * A representation of all types that are represented by a MessagePart in JMF.  This
 * includes MessageParts whose schemas are truly unconstrained (corresponding to
 * XSDWildcard) and those whose schemas are constrained but a dynamic encoding is used
 * because the schema is recursive.  The latter case is represented by recording an
 * "expected type" which gives the constraint on what schema must be used in the
 * MessagePart.
 */

public final class JSDynamic extends JSField implements JMFDynamicType {
  private static TraceComponent tc = JmfTr.register(JSDynamic.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // The type that must be used for this Dynamic value at runtime, or null if the runtime
  // type is unconstrained.
  private JSType expectedType;

  // The JSchema corresponding to expectedType.  This field is only filled when when the
  // JSDynamic is part of a JSchema that is fully initialized.  Otherwise, it is optional.
  private JSchema expectedSchema;

  /**
   * Retrieve the type that the runtime value must adhere to.  A return value of null
   * means that the runtime type is unconstrained.
   */
  public JMFType getExpectedType() {
    return expectedType;
  }

  /**
   * Retrieve the subschema corresponding to the expected type (will be null iff expected
   * type is null.  Constructs the subschema if it doesn't already exist.  The context
   * argument guards against duplicate construction of schemas in the event that the
   * definition is recursive.
   */
  public JSchema getExpectedSchema(Map context) {
    if (expectedSchema != null)
      return expectedSchema;
    if (expectedType == null)
      return null;
    expectedSchema = (JSchema)context.get(expectedType);
    if (expectedSchema != null)
      return expectedSchema;
    expectedSchema = new JSchema(expectedType, context);
    return expectedSchema;
  }

  /**
   * Public version of getExpectedSchema just returns the contents of the field
   */
  public JMFSchema getExpectedSchema() {
    return expectedSchema;
  }

  /**
   * Set the type that the runtime value must adhere to.  A value of null means that the
   * runtime type is unconstrained.
   */
  public void setExpectedType(JMFType expect) {
    expectedType = (JSType)expect;
  }

  // Format for printing (subroutine of toString)
  public void format(StringBuffer fmt, Set done, Set todo, int indent) {
    formatName(fmt, indent);
    if (expectedType == null)
      fmt.append("Dynamic");
    else {
      String name = expectedType.getFeatureName();
      fmt.append("<").append(name).append(">");
      if (!done.contains(expectedType))
        todo.add(expectedType);
    }
  }

  // Implementations of the JSCoder interface
  public int getEncodedLength(Object val, int indirect, JMFMessageData msg)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    if (val instanceof JSMessageImpl) {
      // 16 bytes of overhead is 4 byte length plus 4 byte model id plus 8 byte schema id
      return 16 + ((JSMessageImpl)val).getEncodedLength();
    }
    else {
      // 8 bytes of overhead is 4 byte length plus 4 byte model id
      return 8 + ((JMFEncapsulation)val).getEncapsulatedLength(msg);
    }
  }

  public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
    if (val instanceof JMFPart)
      return val;
    else
      throw new JMFSchemaViolationException("Value is not of Dynamic type");
  }

  public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    JMFPart part = (JMFPart)val;
    int len;
    ArrayUtil.writeInt(frame, offset + 4, part.getModelID());
    if (part instanceof JSMessageImpl) {
      ArrayUtil.writeLong(frame, offset + 8, part.getJMFSchema().getID());
      JSMessageImpl msgi = (JSMessageImpl)part;
      len = 16 + msgi.toByteArray(frame, offset + 16);
    }
    else {
      JMFEncapsulation encap = (JMFEncapsulation)part;
      len = encap.getEncapsulatedLength(msg);
      len = 8 + encap.encapsulate(frame, offset + 8, len, msg);
    }
    ArrayUtil.writeInt(frame, offset, len - 4);
    return offset + len;
  }

  public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    int length = ArrayUtil.readInt(frame, offset);
    JSListCoder.sanityCheck(length, frame, offset);
    int model = ArrayUtil.readInt(frame, offset + 4);
    if (model == JMFPart.MODEL_ID_JMF) {
      length -= 12; // original length includes model and schema
      long schemaId = ArrayUtil.readLong(frame, offset + 8);
      JSchema schema = (JSchema)JMFRegistry.instance.retrieve(schemaId);
      if (schema == null)
        throw new JMFSchemaViolationException("No schema: " + schemaId);
      return new JSMessageImpl(schema, frame, offset + 16, length, false);
    }
    else {
      JMFEncapsulationManager mgr = JMFRegistry.instance.retrieve(model);
      if (mgr == null)
        throw new JMFModelNotImplementedException("No encapsulation manager for model: " + model);
      length -= 4;  // original length includes model id
      return mgr.deencapsulate(frame, offset + 8, length, msg);
    }
  }

  public Object copy(Object val, int indirect) throws JMFSchemaViolationException {
    if (val instanceof JSMessageImpl)
       return ((JSMessageImpl)val).getCopy();
    else
       return ((JMFEncapsulation)val).copy();
  }

  // Implement JSCoder.estimatedUnassembledSize(Object.....
  public int estimateUnassembledSize(Object val) {
    return 500;  // We have absoultely no idea what is in this JSDynamic
  }

  // Implement JSCoder.estimatedUnassembledSize(byte[].....
  public int estimateUnassembledSize(byte[] frame, int offset) {
    // We have absoultely no idea what is in this JSDynamic but it must fluff to bigger than its encoded length
    return 500 + ArrayUtil.readInt(frame, offset);
  }

  // Implementation of encodedTypeLength
  public int encodedTypeLength() {
    return 1;
  }

  // Implementation of encodeType: note a JSDynamic does not propagate its expectedType or
  // expectedSchema.  This information, like name information, is not essential to being
  // able to process a message, which is dependent entirely on dynamic knowledge of
  // schemas, never on static knowledge of expected schemas.
  public void encodeType(byte[] frame, int[] limits) {
    setByte(frame, limits, (byte)DYNAMIC);
  }
}
