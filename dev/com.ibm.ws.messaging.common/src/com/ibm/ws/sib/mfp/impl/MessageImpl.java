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
package com.ibm.ws.sib.mfp.impl;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.List;

import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.ws.sib.mfp.jmf.JMFRegistry;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaIdException;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 *  MessageImpl is the base implementation class for all Jetstream messages,
 *  including those control messages which do not require a Jetstream header
 *  and are not processed by the Message Processor.
 *  <p>
 *  The MessageImpl instance contains the JsMsgObject which is the
 *  internal object which represents a Message of any type.
 *  This class is never used directly - all instantiateable messages are
 *  instances of one of its subclasses.
 *  <br>
 *  MessageImpl also provides writeObject and readObject methods for
 *  efficient serialization of any message specialization.
 */
abstract class MessageImpl implements Serializable {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(MessageImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /*  ******************************************************************************  */
  /* The JsMsgObject represents the real messages in both encoded and abstract form.  */
  /*  ******************************************************************************  */

  /* The JMO is defined as transient so that it will not get automatically    */
  /* serialized. readObject/writeObject methods are provided to set it        */
  /* correctly.                                                               */
  transient JsMsgObject jmo;

  void setJmo(JsMsgObject jmo) {
    this.jmo = jmo;
    jmo.setMessage(this);
  }

  // For debug only ...
  final String debugMsg() {
    return jmo.debugMsg();
  }

  /*
   * Return a string containing the message content for use by other components'
   * debugging.
   *
   * Javadoc description supplied by the AbstractMessage interface
   */
  public final String toVerboseString() {
    updateDataFields(MfpConstants.UDF_VERBOSE_STRING);
    return debugMsg();
  }

  /* **************************************************************************/
  /* Allow update of any cached data                                          */
  /* **************************************************************************/
  // Subclassess must override this method if they cache any message data in
  // transient storage.  when invoked any such data must be written back to JMF.
  // an int reason for why the update is happening will be passed. This int will be
  // one of those defined in MfpConstants with a UDF_ prefix
  void updateDataFields(int why) {
    return;
  }

  // Subclasses must override this method if they cache JsMsgParts in transient
  // storage.  When invoked any such caches should be cleared and reloaded if
  // required.
  void clearPartCaches() {
    return;
  }


  /* **************************************************************************/
  /* Serialization and de-serialization methods                               */
  /* **************************************************************************/

  /**
   * The writeObject method for serialization.
   * <p>
   * writeObject calls the defaultWriteObject to serialize the existing class,
   * however the jmo variable is not serialized as it is transient.
   * The method then calls the JMO to get the message encoded to a set of byte
   * buffers which it writes to the ObjectOutputStream.
   *
   * @param out The ObjectOutputStream for serialization.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeObject");

    /* Call the default writeObject */
    out.defaultWriteObject();

    try {
      /* Get the flattened version of the message */
      List buffers = jmo.encodeFast(null);

      /* Write out the schema defintions needed to decode this message */
      JMFSchema[] schemas = jmo.getEncodingSchemas();
      out.writeInt(schemas.length);
      for (int i = 0; i < schemas.length; i++)
        out.writeObject(schemas[i].toByteArray());

      /* Write out the buffers to the ObjectOutputStream */
      out.writeObject(buffers);
    }
    /* The message is not serializable until all required fields have been set */
    catch (MessageEncodeFailedException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.MessageImpl.writeObject", "msg100", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "serialization failed: " + e);
      NotSerializableException newE  = new NotSerializableException();
      newE.initCause(e);
      throw newE;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeObject");
  }

  /**
   * The readObject method for serialization.
   * <p>
   * readObject calls the defaultReadObject to de-serialize the existing class,
   * however the jmo variable is not recovered as it is transient.
   * The method then reads the List of byte buffers containing the flattened
   * message from the ObjectInputStream and creates the jmo variable from that
   * List.
   *
   * @param out The ObjectInputStream for serialization.
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readObject");

    /* Call the default readObject */
    in.defaultReadObject();

    try {
      /* Recover schemas needed for this message and ensure they are registered */
      int schemaCount = in.readInt();
      for (int i = 0; i < schemaCount; i++) {
        byte[] schemaDef = (byte[])in.readObject();
        JMFSchema schema = JMFRegistry.instance.createJMFSchema(schemaDef);
        JMFRegistry.instance.register(schema);
      }

      /* Recover the flattened version of the message */
      List buffers  = (List)in.readObject();

      /* Create a new unparsed JMO and set the jmo variable */
      JsMsgObject newJmo = new JsMsgObject(null, null, buffers);
      setJmo(newJmo);

      /* Set the transient approxLength & fluffedSize fields if appropriate */
      // This should really be done by a readObject() implementation in JsMessageImpl
      // but that could be confusing as we're used to all the odd bits being done in
      // this one method.
      if (this instanceof JsMessageImpl) {
        // We know this, because we already have an encoded form.
        ((JsMessageImpl)this).setApproximateLength(newJmo.getOriginalLength());
        // We have no idea what this is, so initialize it to -1.
        ((JsMessageImpl)this).setFluffedSize(-1);
      }
    }
    /* If the message was serialized but can't be de-serialized, someone must */
    /* have chewed up the Stream.                                             */
    catch (MessageDecodeFailedException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.MessageImpl.readObject", "msg200", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "deserialization failed: " + e);
      StreamCorruptedException newE  = new StreamCorruptedException();
      newE.initCause(e);
      throw newE;
    }
    /* If we fail to register a required schema, something has gone horribly wrong */
    catch (JMFSchemaIdException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.MessageImpl.readObject", "msg201", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "deserialization failed: " + e);
      InvalidObjectException newE  = new InvalidObjectException(e.getMessage());
      newE.initCause(e);
      throw newE;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readObject");
  }
}
