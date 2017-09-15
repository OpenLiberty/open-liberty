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

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.jmf.JMFException;
import com.ibm.ws.sib.mfp.jmf.JMFRegistry;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaIdException;
import com.ibm.ws.sib.mfp.jmf.impl.JSRegistry;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.mfp.schema.JmsBytesBodyAccess;
import com.ibm.ws.sib.mfp.schema.JmsMapBodyAccess;
import com.ibm.ws.sib.mfp.schema.JmsObjectBodyAccess;
import com.ibm.ws.sib.mfp.schema.JmsStreamBodyAccess;
import com.ibm.ws.sib.mfp.schema.JmsTextBodyAccess;
import com.ibm.ws.sib.mfp.schema.JsApiAccess;
import com.ibm.ws.sib.mfp.schema.JsHdrAccess;
import com.ibm.ws.sib.mfp.schema.JsHdr2Access;
import com.ibm.ws.sib.mfp.schema.JsPayloadAccess;
import com.ibm.ws.sib.mfp.schema.SubscriptionAccess;
import com.ibm.ws.sib.mfp.schema.TrmAccess;
import com.ibm.ws.sib.mfp.schema.TrmFirstContactAccess;
import com.ibm.ws.sib.mfp.util.ArrayUtil;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * This class performs two main roles:
 *
 * 1. It maintains and manages a set of tables that map between currently open
 *    connections and the list of schema ids known to exist in the JVM at the
 *    other end of those connections.  This information is first established
 *    during the initial Comms handshake whem two JVMs connect.  It is updated
 *    when new schema definitions are sent or when a connection closes.
 *
 * 2. It determines if the set of schema defiitions needed to decode a message
 *    are present at the destination and if necessary sends any missing schemas
 *    ahead of the message being encoded.
 */

// Note: The JMFRegistry that holds schema definitions is a static one-per-JVM
//       thing.  So the methods in this class are all static methods too - no
//       actual instance of a SchemaManager is needed.

public class SchemaManager {
  private static TraceComponent tc = SibTr.register(SchemaManager.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
  private static final TraceNLS nls = TraceNLS.getTraceNLS(MfpConstants.MSG_BUNDLE);
  
  private static String JMF_ONLY_PROPERTY = "sib.client.JMFOnly";
  private static String JMS_ONLY_PROPERTY = "sib.client.JMSOnly";

  // Ensure all the schemas we deal with are registered in the JMF registry
  static {
    try {
      init();
    } catch (JMFSchemaIdException e) {
      // No FFDC code needed
      String exString = nls.getFormattedMessage("UNEXPECTED_CLASS_INIT_ERROR_CWSIF0721"
          ,new Object[] {e}
          ,"Error in SchemaManager static initialization");
      throw new SIErrorException(exString, e);
    }
  }

  // This method does nothing, but calling it will ensure the class is initialized d380323
  static void ensureInitialized() {
  }

  // Initialization function. Can be called to re-init this class after the
  // state has been cleared (should only be needed for unit tests).
  public static void init() throws JMFSchemaIdException {
    // Register the MFP diagnostic module
    MfpDiagnostics.initialize();


    try {
      JMFRegistry.instance.register(JsHdrAccess.schema);
      JMFRegistry.instance.register(JsHdr2Access.schema);
      JMFRegistry.instance.register(JsApiAccess.schema);
      JMFRegistry.instance.register(JsPayloadAccess.schema);
      JMFRegistry.instance.register(JmsBytesBodyAccess.schema);
      JMFRegistry.instance.register(JmsTextBodyAccess.schema);
      JMFRegistry.instance.register(JmsObjectBodyAccess.schema);
      JMFRegistry.instance.register(JmsStreamBodyAccess.schema);
      JMFRegistry.instance.register(JmsMapBodyAccess.schema);
      JMFRegistry.instance.register(TrmAccess.schema);
      JMFRegistry.instance.register(SubscriptionAccess.schema);
      JMFRegistry.instance.register(ControlAccess.schema);
      JMFRegistry.instance.register(TrmFirstContactAccess.schema);

      
    } catch (JMFSchemaIdException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.SchemaManager.<clinit>", "188");
      throw e;
    }
  }

  // Called when a new connection is being established
  // Note:  Nothing needs to be synchronized here, since only one thread can be opening
  //        the new connection and nothing else can happen on that connection until we
  //        return.
  static void openLink(CommsConnection conn)
      throws SIConnectionLostException, SIConnectionUnavailableException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "openLink", conn);

    // First we send our current list of known schema ids to the destination and get back
    // the destination's list of known schema ids.
    JMFSchema[] schemas = JMFRegistry.instance.retrieveAll();
    byte[] remoteList = conn.mfpHandshakeExchange(makeSchemaIdList(schemas));

    // Add the schema ids returned by the destination to our tables
    Coder cin = new Coder(remoteList);
    conn.setSchemaSet(makeSchemaIdSet(cin));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "openLink");
  }

  // Called when a connection closes.
  static void closeLink(CommsConnection conn) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "closeLink", conn);

    conn.setSchemaSet(null);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "closeLink");
  }

  // Called when schema ids are received during an initial handshake
  static byte[] receiveHandshake(CommsConnection conn, byte[] data) throws JMFException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "receiveHandshake", conn);

    Coder coder = new Coder(data);
    conn.setSchemaSet(makeSchemaIdSet(coder));
    byte[] ids = makeSchemaIdList(JMFRegistry.instance.retrieveAll());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "receiveHandshake");
    return ids;
  }

  // Called when schema definitions are received on a connection
  static void receiveSchemas(CommsConnection conn, byte[] data) throws JMFException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "receiveSchemas", conn);

    final SchemaSet ids;

    try {
      ids = (SchemaSet)conn.getSchemaSet();
      if (ids == null) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "getSchemaSet() returned null for CommsConnection: " + conn);
        throw new IllegalStateException("CommsConnection returned null SchemaSet");
      }
    } catch (SIConnectionDroppedException e) {
      // No FFDC code needed - this is not an unexpected condition when a connection fails during start of a new conversation on the connection
      throw new IllegalStateException("CommsConnection threw exception", e);
    }

    Coder coder = new Coder(data);
    addSchemaDefinitions(ids, coder);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "receiveSchemas");
  }

  // Called when sending a message on a connection to check for and send any missing schemas
  static void sendSchemas(CommsConnection conn, JMFSchema[] schemas)
      throws SIConnectionLostException, SIConnectionUnavailableException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "sendSchemas", conn);

    // Check if the recepient has all the schemas needed to decode the message.
    SchemaSet ids = (SchemaSet)conn.getSchemaSet();
    if (ids == null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "getSchemaSet() returned null for CommsConnection: " + conn);
      throw new IllegalStateException("CommsConnection returned null SchemaSet");
    }

    JMFSchema[] missing = new JMFSchema[schemas.length];
    int count = 0;
    for (int i = 0; i < schemas.length; i++) {
      if (!ids.contains(schemas[i].getLongID()))
        missing[count++] = schemas[i];
    }

    // Send any missing schemas
    if (count > 0) {
      conn.sendMFPSchema(makeSchemaDefinitionList(missing, count));
      // Update the list
      for (int i = 0; i < count; i++)
        ids.add(missing[i].getLongID());
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "sendSchemas");
  }

  //take an encoded list of schema IDs and return the corresponding encoded list of schemas
  static byte[] getEncodedSchemataByEncodedIDs(byte[] encodedSchemaIDs) {
    //double check that there are ids to decode
    if(encodedSchemaIDs != null && encodedSchemaIDs.length >= 8){
      //work out how many there are
      int numSchemas = encodedSchemaIDs.length / 8;
      long[] schemaIDs = new long[numSchemas];
      int offset = 0;
      //decode each one
      for(int i=0; i<numSchemas; i++){
        schemaIDs[i] = ArrayUtil.readLong(encodedSchemaIDs, offset);
        offset += 8;
      }
      //get the encoded schemas
      return getEncodedSchemataBySchemaIDs(schemaIDs);
    }
    else{
      return new byte[0];
    }
  }

  //get an encoded list of schemas from a list of schema IDs
  static byte[] getEncodedSchemataBySchemaIDs(long[] schemaIDs) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getEncodedSchemataBySchemaIDs", schemaIDs);

    JMFSchema[] temp = new JMFSchema[schemaIDs.length];
    int j = 0;

    //get each one from the registry
    for (int i = 0; i < schemaIDs.length; i++) {
      JMFSchema schema = JSRegistry.instance.retrieve(schemaIDs[i]);
      if (schema == null) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Unable to retrieve message schema "+schemaIDs[i]);
        MessageDecodeFailedException e = new MessageDecodeFailedException("No schema registered for schema id "+schemaIDs[i]);
        FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.SchemaManager.getEncodedSchemasBySchemaIDs", "281");
      }
      else{
        temp[j++] = schema;
      }
    }

    JMFSchema[] found = new JMFSchema[j];
    System.arraycopy(temp,0,found,0,j);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getEncodedSchemataBySchemaIDs");

    //encode the list and return it
    return makeSchemaDefinitionList(found, j);
  }

  /*
   * Helper methods
   */

  // Create the encoded list of schema ids
  private static byte[] makeSchemaIdList(JMFSchema[] schemas) {
    Coder coder = new Coder(ArrayUtil.LONG_SIZE * schemas.length, schemas.length);
    for (int i = 0; i < coder.count; i++) {
      ArrayUtil.writeLong(coder.buffer, coder.offset, schemas[i].getID());
      coder.offset += ArrayUtil.LONG_SIZE;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Encoding " + coder.count + " schema ids for handshake");
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.bytes(tc, coder.buffer, Coder.HDR_LENGTH);
    return coder.buffer;
  }

  // Create an initial Set of incoming schema ids
  private static SchemaSet makeSchemaIdSet(Coder coder) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Decoding " + coder.count + "schema ids from handshake");
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.bytes(tc, coder.buffer, Coder.HDR_LENGTH);

    SchemaSet result = new SchemaSet();
    for (int i = 0; i < coder.count; i++) {
      long id = ArrayUtil.readLong(coder.buffer, coder.offset);
      coder.offset += ArrayUtil.LONG_SIZE;
      result.add(Long.valueOf(id));
    }
    return result;
  }

  // Create the encoded list of schema definitions
  private static byte[] makeSchemaDefinitionList(JMFSchema[] schemas, int count) {
    // Calculate the buffer size needed to encode all the schema defintions
    int length = 0;
    for (int i = 0; i < count; i++)
      length += ArrayUtil.INT_SIZE + schemas[i].toByteArray().length;

    // And encode them
    Coder coder = new Coder(length, count);
    for (int i = 0; i < coder.count; i++) {
      byte[] b = schemas[i].toByteArray();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "   Encoded Schema "+ debugSchema(schemas[i]) + " for transmission");
      ArrayUtil.writeInt(coder.buffer, coder.offset, b.length);
      coder.offset += ArrayUtil.INT_SIZE;
      System.arraycopy(b, 0, coder.buffer, coder.offset, b.length);
      coder.offset += b.length;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Encoding " + coder.count + "new schema definitions");
    return coder.buffer;
  }

  // Add extra incoming schema definitions
  private static void addSchemaDefinitions(SchemaSet ids, Coder coder) throws JMFException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Decoding " + coder.count + "new schema definitions");

    // Decode the new schemas and add them to the registry and the list of known
    // schemas for this connection.
    for (int i = 0; i < coder.count; i++) {
      int length = ArrayUtil.readInt(coder.buffer, coder.offset);
      coder.offset += ArrayUtil.INT_SIZE;
      JMFSchema schema = JMFRegistry.instance.createJMFSchema(coder.buffer, coder.offset, length);
      JMFRegistry.instance.register(schema);
      ids.add(schema.getLongID());
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "   Added Schema "+ debugSchema(schema));
      coder.offset += length;
    }
  }

  // For debug only ... Write a schema name(id) details
  private static String debugSchema(JMFSchema schema) {
    if (schema != null)
      return schema.getName() + "(" + Long.toHexString(schema.getID()) + ")";
    else
      return "<null>";
  }

  // This internal class handles the details of encoding and decoding the data
  // formats exchanged with partner systems.
  static class Coder {
    // The encoded buffers have the following format.  Both types (schema id lists and
    // schema defintiion lists start with a fixed header:
    //   int:    JMF version number
    //   int:    Total encoded buffer length (excluding this header)
    //   int:    Count of encoded items (may be zero)
    //
    // This is followed by 'count' items, either schema ids:
    //   long:   Schema id
    // or schema defintions
    //   int:    Schema definiion length
    //   byte[]: JMF encoded schema definition
    static final int HDR_LENGTH = ArrayUtil.BYTE_SIZE + 3 * ArrayUtil.INT_SIZE;

    private int count;
    private byte[] buffer;
    private int offset;

    // Decode an incoming byte[] data
    Coder(byte[] data) {
      // Extract and decode the header
      buffer = data;
      offset = 0;
      offset += ArrayUtil.INT_SIZE;      // Skip JMF version number
      offset += ArrayUtil.INT_SIZE;      // Skip the total length
      count = ArrayUtil.readInt(buffer, offset);
      offset += ArrayUtil.INT_SIZE;
    }

    // Encode an outgoing buffer
    Coder(int length, int count) {
      this.count = count;
      buffer = new byte[length + HDR_LENGTH];
      offset = 0;

      // Build the header
      ArrayUtil.writeInt(buffer, offset, JMFRegistry.JMF_ENCODING_VERSION);
      offset += ArrayUtil.INT_SIZE;
      ArrayUtil.writeInt(buffer, offset, length);
      offset += ArrayUtil.INT_SIZE;
      ArrayUtil.writeInt(buffer, offset, count);
      offset += ArrayUtil.INT_SIZE;
    }
  }
}
