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

import java.io.UnsupportedEncodingException;
import java.util.List;

import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.mfp.*;
import com.ibm.ws.sib.mfp.schema.JsHdrAccess;
import com.ibm.ws.sib.mfp.schema.JsPayloadAccess;
import com.ibm.ws.sib.mfp.util.ArrayUtil;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 *  This class extends the abstract com.ibm.ws.sib.mfp.JsMessageFactory
 *  class and provides the concrete implementations of the methods for
 *  creating JsMessages.
 *  <p>
 *  The class must be public so that the abstract class static
 *  initialization can create an instance of it at runtime.
 *
 */
public final class JsMessageFactoryImpl extends JsMessageFactory {

  private static TraceComponent tc = SibTr.register(JsMessageFactoryImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
  private static final TraceNLS nls = TraceNLS.getTraceNLS(MfpConstants.MSG_BUNDLE);

  /* If this is final, and set at clinit by calling getJmsFactory, we appear  */
  /* to get a deadly embrace of static initializers and an NPE. Therefore, we */
  /* have to leave it until needed.                                           */
  private static JsJmsMessageFactoryImpl jmsFactory;

  // Ensure we register all the JMF schemas needed to process these messages
  static {
    SchemaManager.ensureInitialized();                                          // d380323
  }

  /* Get the factory instance we need for making inbound messages             */
  private static JsJmsMessageFactoryImpl getJmsFactory() {
    if (jmsFactory == null) {
      try {
        jmsFactory = (JsJmsMessageFactoryImpl)JsJmsMessageFactory.getInstance();
      } catch (Exception e) {
        /* A Console error message will have been written for the underlying problem */
        FFDCFilter.processException(e, "<clinit>", "116");
      }
    }
    return jmsFactory;
  }

  /**
   *  Create a JsMessage to represent an inbound message.
   *  (To be called by the Communications component.)
   *
   *  @param rawMessage  The inbound byte array containging a complete message
   *  @param offset      The offset in the byte array at which the message begins
   *  @param length      The length of the message within the byte array
   *
   *  @return The new JsMessage
   *
   *  @exception MessageDecodeFailedException Thrown if the inbound message could not be decoded
   */
  public JsMessage createInboundJsMessage(byte rawMessage[], int offset, int length)
                                         throws MessageDecodeFailedException {
    return createInboundJsMessage(rawMessage, offset, length, null);
  }

  /**
   *  Create a JsMessage to represent an inbound message.
   *  (To be called by the Communications component.)
   *
   *  @param rawMessage  The inbound byte array containging a complete message
   *  @param offset      The offset within the byte array at which the message begins
   *  @param length      The length of the message within the byte array
   *  @param conn        The CommsConnection object, if any, which is associated with this message
   *
   *  @return The new JsMessage
   *
   *  @exception MessageDecodeFailedException Thrown if the inbound message could not be decoded
   */
  public JsMessage createInboundJsMessage(byte rawMessage[], int offset, int length, Object conn)
                                         throws MessageDecodeFailedException  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createInboundJsMessage", new Object[]{rawMessage, offset, length});

    CommsConnection commsConnection = null;

    //try to cast the connection object to a CommsConnection
    if(conn != null){
      if(conn instanceof CommsConnection){
        commsConnection = (CommsConnection) conn;
      }
      else{
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Invalid comms connection");
        ClassCastException e = new ClassCastException();
        FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMessageFactoryImpl.createInboundJsMessage", "168", this,
          new Object[] { MfpConstants.DM_BUFFER, rawMessage, offset, length, conn });
        //FFDC but do not actually throw the exception as we might still be able to decode
        //without the CommsConnection
      }
    }

    // Trace the first 256 bytes of the buffer just in case we get passed rubbish
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Start of buffer: ", SibTr.formatBytes(rawMessage, 0, 256));

    //create a new jmo using the given data
    JsMsgObject jmo = new JsMsgObject(JsHdrAccess.schema,
                                      JsPayloadAccess.schema,
                                      rawMessage,
                                      offset,
                                      length,
                                      commsConnection);

    // Create a message of the appropriate specialisation
    JsMessage message = createSpecialisedMessage(jmo);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createInboundJsMessage", message);
    return message;

  }

  /**
   *  Create a JsMessage to represent an inbound message.
   *  (To be called by the Communications component.)
   *
   *  @param  slices         The List of DataSlices representing the inbound message.
   *
   *  @return The new JsMessage
   *
   *  @exception MessageDecodeFailedException Thrown if the inbound message could not be decoded
   */
  public JsMessage createInboundJsMessage(List<DataSlice> slices) throws MessageDecodeFailedException {
    return createInboundJsMessage(slices, null);
  }

  /**
   *  Create a JsMessage to represent an inbound message.
   *  (To be called by the Communications component.)
   *
   *  @param  slices         The List of DataSlices representing the inbound message.
   *  @param commsConnection The CommsConnection object, if any, which is associated with this message
   *
   *  @return The new JsMessage
   *
   *  @exception MessageDecodeFailedException Thrown if the inbound message could not be decoded
   */
  public JsMessage createInboundJsMessage(List<DataSlice> slices, Object conn) throws MessageDecodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createInboundJsMessage", slices);

    CommsConnection commsConnection = null;

    //try to cast the connection object to a CommsConnection
    if(conn != null){
      if(conn instanceof CommsConnection){
        commsConnection = (CommsConnection) conn;
      }
      else{
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Invalid comms connection");
        ClassCastException e = new ClassCastException();
        FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMessageFactoryImpl.createInboundJsMessage", "254", this,
          new Object[] { MfpConstants.DM_SLICES, slices, conn });
        //FFDC but do not actually throw the exception as we might still be able to decode
        //without the CommsConnection
      }
    }

    // Trace the first slice or chunk just in case we get passed rubbish
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
      if (slices.size() > 1) {
        SibTr.debug(tc, "Slice0: ", SibTr.formatSlice(slices.get(0)));
      }
      else {
        SibTr.debug(tc, "Start of only slice: ", SibTr.formatBytes(slices.get(0).getBytes(), slices.get(0).getOffset(), 256));
      }
    }

    //create a new jmo using the given data
    JsMsgObject jmo = new JsMsgObject(JsHdrAccess.schema,
                                      JsPayloadAccess.schema,
                                      slices,
                                      commsConnection);

    // Create a message of the appropriate specialisation
    JsMessage message = createSpecialisedMessage(jmo);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createInboundJsMessage", message);
    return message;

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.JsMessageFactory#createInboundMQClientMessage(java.util.List, java.lang.String, java.lang.String, com.ibm.websphere.sib.Reliability, com.ibm.websphere.sib.Reliability, boolean)
   
  public JsMessage createInboundMQClientMessage(List<WsByteBuffer> bufferList
                                               ,String busName
                                               ,String virtualQMgrName
                                               ,Reliability nonPersistentReliability
                                               ,Reliability persistentReliability
                                               ,boolean stickyRfh
                                               )
                                               throws IOException, MessageDecodeFailedException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createInboundMQClientMessage", new Object[]
                                                                                     {bufferList
                                                                                     ,busName
                                                                                     ,virtualQMgrName
                                                                                     ,nonPersistentReliability
                                                                                     ,persistentReliability
                                                                                     ,stickyRfh
                                                                                     });

    JsMessage message = MQJsMessageFactoryImpl.createInboundMQClientMessage (bufferList,
      busName, virtualQMgrName, nonPersistentReliability, persistentReliability, stickyRfh);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createInboundMQClientMessage", message);
    return message;
  }*/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.JsMessageFactory#createInboundMQLinkMessage(java.util.List, java.lang.String, java.lang.String, com.ibm.websphere.sib.Reliability, com.ibm.websphere.sib.Reliability)
   
  public JsMessage createInboundMQLinkMessage(List<WsByteBuffer> bufferList
                                             ,String busName
                                             ,String virtualQMgrName
                                             ,String foreignBusName
                                             ,Reliability nonPersistentReliability
                                             ,Reliability persistentReliability
                                             )
                                             throws IOException, MessageDecodeFailedException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createInboundMQLinkMessage", new Object[]
                                                                                     {bufferList
                                                                                     ,busName
                                                                                     ,virtualQMgrName
                                                                                     ,foreignBusName
                                                                                     ,nonPersistentReliability
                                                                                     ,persistentReliability
                                                                                     });

    JsMessage message = MQJsMessageFactoryImpl.createInboundMQLinkMessage (bufferList,
      busName, virtualQMgrName, foreignBusName, nonPersistentReliability, persistentReliability);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createInboundMQLinkMessage", message);
    return message;
  }

  /**
   * @see JsMessageFactory#createInboundMQMsg2(MQMsg2, Reliability, Reliability, boolean, String)
   
  public JsMessage createInboundMQMsg2(MQMsg2 mqMsg2
                                      ,String busName
                                      ,String virtualQMgrName
                                      ,String foreignBusName
                                      ,Reliability nonPersistentReliability
                                      ,Reliability persistentReliability
                                      ,boolean trustUserIdInMsg
                                      ,String untrustedUserId
                                      )
                                      throws IOException, MessageDecodeFailedException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createInboundMQMsg2", new Object[]
                                                                                     {mqMsg2
                                                                                     ,busName
                                                                                     ,virtualQMgrName
                                                                                     ,foreignBusName
                                                                                     ,nonPersistentReliability
                                                                                     ,persistentReliability
                                                                                     ,trustUserIdInMsg
                                                                                     ,untrustedUserId
                                                                                     });

    JsMessage message = MQJsMessageFactoryImpl.createInboundMQMsg2(mqMsg2,
                                                       busName,
                                                       virtualQMgrName,
                                                       foreignBusName,
                                                       nonPersistentReliability,
                                                       persistentReliability,
                                                       trustUserIdInMsg,
                                                       untrustedUserId);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createInboundMQMsg2", message);
    return message;
  }*/

  /*
   * (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.JsMessageFactory#createInboundWebMessage(String)
   */
  public JsMessage createInboundWebMessage(String data) throws MessageDecodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createInboundWebMessage", data);
    JsMessage message = WebJsMessageFactoryImpl.createInboundWebMessage(data);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createInboundWebMessage", message);
    return message;
  }


  /**
   *  Restore a JsMessage of any specializaton from a 'flattened' copy.
   *  Note that the content of the List of DataSlices is affected by the processing
   *  so the List can not subsequently be reused. (The DataSlices themselves are not
   *  affected.)
   *  Added for Line Item SIB0112b.
   *
   *  @param  slices The List of DataSlices representing the flattened message.
   *  @param  store  The MesasgeStore from which the message is being recovered, may be null.
   *
   *  @return The new JsMessage of appropriate specialization
   *
   *  @exception MessageRestoreFailedException Thrown if the message could not be restored
   */
  public JsMessage restoreJsMessage(List<DataSlice> slices, Object store) throws MessageRestoreFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "restoreJsMessage", slices);

    JsMessage newMsg = null;

    // The slices are those returned from a previous flatten, so we know the list
    // contains the following:
    //   slice 0 : Message class name & other information OR a single slice representing the whole buffer
    //   slice 1 : Header (or only) JSMessage
    //   slice 2 : Payload JSMessage (optional)
    // ... so we need to ensure we have at least 2 slices
    if ((slices == null) || (slices.size() < 1)) {
      String exString = nls.getFormattedMessage("NO_DATASLICES_IN_LIST_CWSIF0002"
                                               ,null
                                               ,"A null or empty DataSlice List was passed to the message restore");
      MessageRestoreFailedException e = new MessageRestoreFailedException(exString);
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMessageFactoryImpl.restore", "373");
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Exception: DataSlice list was null or empty");
      throw e;
    }

    // If there is only one slice it had better be the whole message, so just extract
    // the buffer and pass it to the old-style restore for processing.
    else if (slices.size() == 1) {
       newMsg = restoreJsMessage(slices.get(0).getBytes(), store);
    }

    // Otherwise, we have 2 or more slices we're really in the brave new world so carry on....
    else {

      String className;

      /* The first slice contains:                                              */
      /*     the message class name                                             */
      /*     the list of JMF schema ids needed to decode the message            */
      /*     the message itself                                                 */
      DataSlice slice0 = slices.get(0);
      byte[] buffer = slice0.getBytes();
      int offset = slice0.getOffset();
      // Trace the first slice just in case we get passed rubbish
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Slice0: ", SibTr.formatSlice(slice0));

      // Extract the classname from the buffer
      int temp = ArrayUtil.readInt(buffer, offset);               // the length of the class name
      offset += ArrayUtil.INT_SIZE;
      try {
        className = getClassName(buffer, offset, temp);         // the class name itself
      }
      catch (RuntimeException e) {
        // Dump both the buffer portion contained in slice0, and the entire buffer it is a portion of.
        // (These may be the same things as it may be an entire buffer.)
        FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMessageFactoryImpl.restoreJsMessage", "484", this,
                                                new Object[] {
                                                  new Object[]{ MfpConstants.DM_BUFFER, buffer, slice0.getOffset(), slice0.getLength()},
                                                  new Object[]{ MfpConstants.DM_BUFFER, buffer, 0, buffer.length}
                                                } );
        // This is a disaster - presumably the buffer is garbage - so we have to throw the exception on
        throw new MessageRestoreFailedException(e);
      }
      offset += temp;

      // Extract the message schema ids from the buffer, & ensure the schemas are available.
      offset = ensureSchemasAvailable(buffer, offset, store);

      // We have now read the stuff specific to a restore only, & the rest of the information
      // in the first DataSlice will also be needed to build the new JMO
      // We therefore update the first DataSlice in the list so
      // the JMO can ignore the class name information.
      DataSlice newSlice0 = new DataSlice(buffer, offset, slice0.getLength() - offset);
      slices.set(0, newSlice0);

      try {
        /* Create a new JMO from the slices  */
        JsMsgObject newJmo = new JsMsgObject(JsHdrAccess.schema, JsPayloadAccess.schema, slices);

        /* Get the class for the appropriate specialization */
        Class  msgClass = Class.forName(className);

        /* Now create the new JsMessage and set the JMO               */
        newMsg = (JsMessage)msgClass.newInstance();
        ((JsMessageImpl)newMsg).setJmo(newJmo);

        /* Set the approxLength as we know it */
        ((JsMessageImpl)newMsg).setApproximateLength(newJmo.getOriginalLength());
      }
      catch (ClassNotFoundException e1) {
        FFDCFilter.processException(e1, "com.ibm.ws.sib.mfp.impl.JsMessageFactoryImpl.restore", "446");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Exception " + e1.toString() + " finding class " + className);
        throw new MessageRestoreFailedException(e1);
      }
      catch (MessageDecodeFailedException e2) {
        /* No need to FFDC this as JsMsgObject will already have done so */
        // No FFDC code needed
        throw new MessageRestoreFailedException(e2);
      }
      catch (Exception e3) {
        /* Any other exceptions from instantiation should never happen */
        FFDCFilter.processException(e3, "com.ibm.ws.sib.mfp.impl.JsMessageFactoryImpl.restore", "457");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Exception " + e3.toString() + " instantiating class " + className);
        throw new MessageRestoreFailedException(e3);
      }

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "restoreJsMessage");
    return newMsg;
  }


  /**
   *  Restore a JsMessage of any specializaton from a 'flattenned' copy.
   *  Since SIB0112b, this method is only called by the preceding restoreJsMessage
   *  method so it is marked private.
   *
   *  @param  buffer The byte array representing the message.
   *  @param  store  The MesasgeStore from which the message is being recovered, may be null.
   *
   *  @return The new JsMessage of appropriate specialization
   *
   *  @exception MessageRestoreFailedException Thrown if the message could not be restored
   */
  private final JsMessage restoreJsMessage(byte[] buffer, Object store) throws MessageRestoreFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "restoreJsMessage" , buffer);

    JsMessage newMsg = null;

    String className;
    int offset = 0;

    /* The buffer contains:                                                   */
    /*     the message class name                                             */
    /*     the list of JMF schema ids needed to decode the message            */
    /*     the message itself                                                 */
    // Trace the first 256 bytes just in case we get passed rubbish
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Start of buffer: ", SibTr.formatBytes(buffer, 0, 256));

    // Extract the classname from the buffer
    int temp = ArrayUtil.readInt(buffer, offset);               // the length of the class name
    offset += ArrayUtil.INT_SIZE;
    try {
      className = getClassName(buffer, offset, temp);         // the class name itself
    }
    catch (RuntimeException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMessageFactoryImpl.restoreJsMessage", "573", this,
                                              new Object[]{ MfpConstants.DM_BUFFER, buffer, 0, buffer.length});
      // This is a disaster - presumably the buffer is garbage - so we have to throw the exception on
      throw new MessageRestoreFailedException(e);
    }
    offset += temp;

    // Extract the message schema ids from the buffer, & ensure the schemas are available.
    offset = ensureSchemasAvailable(buffer, offset, store);

    try {
      /* Create a new JMO from the remaining buffer  */
      JsMsgObject newJmo = new JsMsgObject(JsHdrAccess.schema, JsPayloadAccess.schema, buffer, offset, buffer.length-offset);

      /* Get the class for the appropriate specialization */
      Class  msgClass = Class.forName(className);

      /* Now create the new JsMessage and set the JMO               */
      newMsg = (JsMessage)msgClass.newInstance();
      ((JsMessageImpl)newMsg).setJmo(newJmo);

      /* Set the approxLength as we know it */
      ((JsMessageImpl)newMsg).setApproximateLength(newJmo.getOriginalLength());
    }
    catch (ClassNotFoundException e1) {
      FFDCFilter.processException(e1, "com.ibm.ws.sib.mfp.impl.JsMessageFactoryImpl.restore", "534");
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Exception " + e1.toString() + " finding class " + className);
      throw new MessageRestoreFailedException(e1);
    }
    catch (MessageDecodeFailedException e2) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageRestoreFailedException(e2);
    }
    catch (Exception e3) {
      /* Any other exceptions from instantiation should never happen */
      FFDCFilter.processException(e3, "com.ibm.ws.sib.mfp.impl.JsMessageFactoryImpl.restore", "545");
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Exception " + e3.toString() + " instantiating class " + className);
      throw new MessageRestoreFailedException(e3);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "restoreJsMessage");
    return newMsg;
  }


  /**
   * Extract the class name from the buffer containing the (first part of) a restored value.
   *
   * @param buffer    The buffer
   * @param offset    The offset of the classname's encoded bytes in the buffer
   * @param length    The length of the classname's encoded bytes
   *
   * @return String The class name of the message to restore
   *
   * @exception This method will throw runtime exceptions if the length is 0, the
   *            buffer isn't long enough, etc etc etc.
   *            The caller will catch and FFDC it, as the data to be FFDC depends
   *            on the caller.
   */
  private final static String getClassName(byte[] buffer, int offset, int length) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getClassName", new Object[]{offset, length});

    // If the classname has a length of 0 then we've been given rubbish so pack it in now
    if (length == 0) {
      throw new IllegalArgumentException("Invalid buffer: classname length = 0");
    }

    // The classname should be in UTF8, if that fails FFDC and default in the hope of carrying on.
    String className;
    try {
      className = new String(buffer, offset, length, "UTF8"); // the class name itself
    }
    catch (UnsupportedEncodingException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMessageFactoryImpl.getClassName", "644");
      className = new String(buffer, offset, length);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getClassName", className);
    return className;
  }


  /**
   * Utility method to extract the schema ids from a message buffer and, if a
   * message store is supplied, check that all the necessary schemas are available.
   *
   * @param buffer  The buffer containing the schema ids
   * @param offset  The offset into the buffer where the schema ids start
   * @param store   The MesasgeStore from which the message is being recovered, may be null.
   *
   * @return int The offset in the buffer of the first byte after the schema ids
   *
   * @exception MessageRestoreFailedException is thrown if the necessary schemas are not available.
   */
  private final static int ensureSchemasAvailable(byte[] buffer, int offset, Object store) throws MessageRestoreFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "ensureSchemasAvailable", new Object[] {offset, store});

    // If we have a message store we need to ensure all the schemas we'll
    // need to decode the message are restored from the store.
    int temp = ArrayUtil.readInt(buffer, offset);               // the number of encoding Ids
    offset += ArrayUtil.INT_SIZE;
    long[] decodeIds = new long[temp];
    for (int i = 0; i < temp; i++) {
      decodeIds[i] = ArrayUtil.readLong(buffer, offset);    // each encoding schema id
      offset += ArrayUtil.LONG_SIZE;
    }
    if (store != null && decodeIds.length > 0) {
      if (!(store instanceof MessageStore)) throw new IllegalArgumentException("store is not a MessageStore instance");
      SchemaStore.loadSchemas((MessageStore)store, decodeIds);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "ensureSchemasAvailable", offset);
    return offset;
  }



  /**
   * Returns a new JsMessage instance, of the subclass appropriate to the
   * message content.
   *
   * @param jmo        The JsMsgObject containing the guts of the message
   *
   * @return JsMessage A new instance of the appropriate JsMessage implementation
   */
  private final JsMessage createSpecialisedMessage(JsMsgObject jmo) {

    JsMessage message = null;

    /* For an API message, we need to return an instance of the appropriate   */
    /* specialisation.                                                        */
    if (jmo.getChoiceField(JsHdrAccess.API) == JsHdrAccess.IS_API_DATA) {

      /* If it is a JMS message, get the right specialisation                 */
      if (jmo.getField(JsHdrAccess.MESSAGETYPE).equals(MessageType.JMS.toByte())) {
        message = getJmsFactory().createInboundJmsMessage(jmo, ((Byte)jmo.getField(JsHdrAccess.SUBTYPE)).intValue());
      }      
    }

    /* If it isn't an API message, just return a JsMessageImpl                */
    else {
      message = new JsMessageImpl(jmo);
    }

    return message;
  }

}
