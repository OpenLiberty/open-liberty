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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.mfp.IncorrectMessageTypeException;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.JsJmsMessageFactory;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.ws.sib.mfp.MessageType;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.WebJsMessageEncoder;
import com.ibm.ws.sib.mfp.control.SubscriptionMessage;
import com.ibm.ws.sib.mfp.schema.JsHdr2Access;
import com.ibm.ws.sib.mfp.trm.TrmMessage;
import com.ibm.ws.sib.mfp.trm.TrmMessageFactory;
import com.ibm.ws.sib.mfp.util.HexUtil;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;

//Venu Liberty change: Moved the in64bit() to SIB Utils
import com.ibm.ws.sib.utils.RuntimeInfo;


/**
 *  JsMessageImpl is the implementation class for the JsMessage interface.
 *  <p>
 *  The JsMessageImpl instance extends JsHdrsImpl, which provides all the get/set
 *  methods for header fields. JsHdrsImpl extends MessageImpl which contains the
 *  JsMsgObject which is the internal object which represents a Message of any type.
 *  The implementation classes for all the specialised messages extend
 *  JsMessageImpl, either directly or indirectly, as well as implementing
 *  their specialised interface. JsMessage and its subclasses have no instance
 *  variables.
 *  <p>
 *  JsMessageImpl also implements MatchSpaceKey which allows the Matching
 *  Engine to obtain the value of the fields used as Message Selectors.
 */
class JsMessageImpl extends JsHdrsImpl implements JsMessage, MatchSpaceKey {

  private static final TraceComponent tc = SibTr.register(JsMessageImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
  private static final TraceNLS nls = TraceNLS.getTraceNLS(MfpConstants.MSG_BUNDLE);

  private final static long serialVersionUID = 1L;

  private final static JsJmsMessageFactoryImpl   jmsFactory     = getJmsFactory();
  private final static TrmMessageFactoryImpl     trmFactory     = getTrmFactory();

  // The overhead for a single object in the heap.
  // The value will depend on whether we are running on 32-bit or 64-bit.
  final static int FLUFFED_OBJECT_OVERHEAD;
  // The overhead for a single String in the heap. It includes the overheads for
  // the String instance, its value fields & the underlying char[]'s overhead.
  // The value will depend on whether we are running on 32-bit or 64-bit.
  final static int FLUFFED_STRING_OVERHEAD;
  // The size of an Object Reference in the heap.
  // The value will depend on whether we are running on 32-bit or 64-bit.
  final static int FLUFFED_REF_SIZE;
  // The shallow size of an MFP JsXxxxxMessageImpl in the heap.
  // The value will depend on whether we are running on 32-bit or 64-bit.
  private final static int FLUFFED_MFP_MESSAGE_SIZE;
  // The shallow size of a JMF JSMessageImpl in the heap.
  // The value will depend on whether we are running on 32-bit or 64-bit.
  final static int FLUFFED_JMF_MESSAGE_SIZE;
  // The shallow size of a JsMsgPart in the heap.
  // The value will depend on whether we are running on 32-bit or 64-bit.
  final static int FLUFFED_JSMSGPART_SIZE;
  // The shallow size of a JMF JSVaryingListImpl in the heap.
  // The value will depend on whether we are running on 32-bit or 64-bit.
  final static int FLUFFED_JMF_LIST_SIZE;
  // A guess at the overhead for a fluffed up message map.
  // The value will depend on whether we are running on 32-bit or 64-bit.
  final static int FLUFFED_MAP_OVERHEAD;
  // A guess at the size of a fluffed up map entry.
  // The value will depend on whether we are running on 32-bit or 64-bit.
  final static int FLUFFED_MAP_ENTRY_SIZE;
  // The base fluffed size for the normal message header, without optional
  // sets of data or any API-level or payload stuff.
  // The value will depend on whether we are running on 32-bit or 64-bit.
  private final static int FLUFFED_HEADER_BASE_SIZE;

  static {
	  
    // Check whether we are running on 64-bit or 32-bit
	//Venu Liberty change:
    boolean is64bit = RuntimeInfo.is64bit();

    // Set the overhead constants according to whether we are ruuning on a 32-bit
    // or 64-bit system. The values have been determined by analysing heapdumps.
    if (!is64bit) {
      FLUFFED_OBJECT_OVERHEAD = 12;
      FLUFFED_STRING_OVERHEAD = 32;
      FLUFFED_REF_SIZE = 4;
      FLUFFED_JMF_MESSAGE_SIZE = 72;
      FLUFFED_MFP_MESSAGE_SIZE = 96;
      FLUFFED_JSMSGPART_SIZE = 16;     // It is just an Object with one reference.
      FLUFFED_JMF_LIST_SIZE = 72;
    }
    else {
      FLUFFED_OBJECT_OVERHEAD = 24;
      FLUFFED_STRING_OVERHEAD = 48;
      FLUFFED_REF_SIZE = 8;
      FLUFFED_JMF_MESSAGE_SIZE = 136;
      FLUFFED_MFP_MESSAGE_SIZE = 112;  // Is that all? Needs checking.
      FLUFFED_JSMSGPART_SIZE = 32;     // It is just an Object with one reference.
      FLUFFED_JMF_LIST_SIZE = 136;     // This is a guess - look in a 64-bit heapdump
    }

    // Calculate the overhead for a fluffed up message map.
    // This consists of a JsMsgMap plus 2 lists of some sort, so we'll assume,
    // JMF Lists as they are probably the most expensive.
    FLUFFED_MAP_OVERHEAD = FLUFFED_OBJECT_OVERHEAD
                         + FLUFFED_REF_SIZE*2
                         + FLUFFED_JMF_LIST_SIZE*2;

    // A guess at the size of a map entry, including the overhead of a String (name)
    // and Object (value).
    // Guess each name entry has an average of 15 chars, so size=30,.
    // Guess each value has a data size of average 20.
    FLUFFED_MAP_ENTRY_SIZE = FLUFFED_STRING_OVERHEAD + FLUFFED_OBJECT_OVERHEAD + 50;

    // Calculate the FLUFFED_BASE_HEADER_SIZE
    // Start with the approx size of the  JsXxxxMessageImpl i.e. MFP_MESSAGE_SIZE
    //                                  + JsMsgObject       i.e. OBJECT + 3*REF + 4
    //                                  + 3 x JsMsgPart
    //                                  + 3 x JSMessageImpl
    // We include 3 JSMsgParts & 3 JMF messages here as JsHdr & JsPayload will always exist
    // for a normal message, and Processor are bound to have fluffed up JsHdr2 as well.
    int size = FLUFFED_MFP_MESSAGE_SIZE
             + FLUFFED_OBJECT_OVERHEAD + (FLUFFED_REF_SIZE*3) + 4
             + FLUFFED_JSMSGPART_SIZE*3
             + FLUFFED_JMF_MESSAGE_SIZE*3;

    // Make a guess at the contribution of the header. The header size is
    // reasonably constant, with the exception of the forward/reverse routing
    // paths.
    // We have to include the caches for the JSMessageImpl instances for each of
    // JsHdrSchema (small) and JsHdr2Schema. Also, a potentially large
    // number of primitive wrapper classes as well as the Strings.
    // However Bytes, Booleans, and most Integers won't take real space for the
    // actual instance as they are shared.
    size += FLUFFED_REF_SIZE*10;               // JsHdrSchema's cache (REF*10)
    size += FLUFFED_OBJECT_OVERHEAD*5 + 120;   // JsHdrSchema's data (incl. 5 non-singleton object overheads)
    size += FLUFFED_REF_SIZE*62;               // JsHdr2Schema's cache (REF*62)
    size += FLUFFED_OBJECT_OVERHEAD*13 + 300;  // JsHdr2Schema's data  (incl. 13 non-singleton object overheads)
    FLUFFED_HEADER_BASE_SIZE = size;
  }

  /* Get all the factory instances we need for making inbound messages        */
  private static JsJmsMessageFactoryImpl getJmsFactory() {
    try {
      return (JsJmsMessageFactoryImpl)JsJmsMessageFactory.getInstance();
    } catch (Exception e) {
      /* A Console error message will have been written for the underlying problem */
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMessageImpl.getJmsFactory", "jsm101");
      return null;
    }
  }

  private static TrmMessageFactoryImpl getTrmFactory() {
    try {
      return (TrmMessageFactoryImpl)TrmMessageFactory.getInstance();
    } catch (Exception e) {
      /* A Console error message will have been written for the underlying problem */
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMessageImpl.getTrmFactory", "jsm102");
      return null;
    }
  }


  /* Get the flattened form of a classname                     SIB0112b.mfp.2 */
  static byte[] flattenClassName(String className) {
    byte[] flattened;
    try {
      flattened = className.getBytes("UTF8");
    }
    catch (UnsupportedEncodingException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsSdoMessageImpl.<clinit>", "50");
      flattened = className.getBytes();
    }
    return flattened;
  }


  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream message.
   *
   *  This constructor should never be used explicitly.
   *  It is only to be used implicitly by the sub-classes' no-parameter constructors.
   *  The method must not actually do anything.
   */
  JsMessageImpl() {
  }

  /**
   *  Constructor for a new message of given root type.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  JsMessageImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>");
  }

  /**
   *  Constructor for an inbound message.
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  JsMessageImpl(JsMsgObject inJmo) {
    super(inJmo);

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>, inbound jmo ");

    if (inJmo.getOriginalLength() != 0) {
      approxLength = inJmo.getOriginalLength();
    }
  }


  /* **************************************************************************/
  /* Methods for making more specialised messages                             */
  /* **************************************************************************/

  /*
   *  Convert the existing inbound JsMessage into a JsJmsMessage.
   *  The JsJmsMessage returned will actually be of the appropriate
   *  sub-class - e.g. JsJmsTextMessage if the inbound message is actually
   *  a JMS TextMessage or of a form where TextMessage is the most appropriate
   *  JMS representation. A null-bodied message will be returned as a
   *  JsJmsMessage.
   *
   *  Javadoc description supplied by JsMessage interface.
   */
  public final JsJmsMessage makeInboundJmsMessage()  throws IncorrectMessageTypeException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "makeInboundJmsMessage");

    JsJmsMessageImpl jmsMessage = null;

    /* The message should always already be a JsJmsMessage of the correct   */
    /* specialisation, even if it has arrived over the wire from another ME */
    /* or MQ.                                                               */
    if (this instanceof JsJmsMessageImpl) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Incoming message is already a JsJmsMessage");
      jmsMessage = (JsJmsMessageImpl)this;
    }
    /* If the incoming message is already a JMS message, just determine which */
    /* flavour and create the right specialization to return.                 */
    else if (getJsMessageType() == MessageType.JMS) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Incoming message has MessageType of JMS");
      updateDataFields(MfpConstants.UDF_MAKE_INBOUND_JMS);
      jmsMessage = (JsJmsMessageImpl)jmsFactory.createInboundJmsMessage(jmo, getSubtype());
    }
    else {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Not a JMS Message");
      String exString = nls.getFormattedMessage("UNEXPECTED_MESSAGE_TYPE_CWSIF0102"
                                               ,new Object[] {getJsMessageType(), MessageType.JMS}
                                               ,"The Message can not be represented as JMS Message");
      throw new IncorrectMessageTypeException(exString);
    }

    /* Fix up any default/derived JMS fields necessary.                       */
    jmsMessage.setDerivedJmsFields();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "makeInboundJmsMessage");
    return jmsMessage;
  }
  
  /*
   *  Convert the existing inbound JsMessage into a TrmMessage.
   *  The TrmMessage returned will actually be of the appropriate
   *  sub-class - e.g. TrmRouteData if the inbound message is actually
   *  a TRM Route Data Message. A TRM Message of unknown type will be
   *  returned as a TrmMessage.
   *
   *  Javadoc description supplied by JsMessage interface.
   */
  public final TrmMessage makeInboundTrmMessage() throws IncorrectMessageTypeException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "makeInboundTrmMessage");

    TrmMessage trmMessage = null;

    /* If the message has never been flattened/encoded, it will already be */
    /* a TrmMessage so we can just proceed with this recast.                */
    if (this instanceof TrmMessageImpl) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Incoming message is already a TrmMessage");
      trmMessage = (TrmMessageImpl)this;
    }
    /* If not, we need to make it an instance of the right sub-class        */
    /* The incoming message should already be a TRM message, so determine     */
    /* which flavour and create the right specialization to return.           */
    else if (getJsMessageType() == MessageType.TRM) {
      // Ensure pending changes are flushed to JMF before passing the JMO around
      updateDataFields(MfpConstants.UDF_MAKE_INBOUND_OTHER);
      trmMessage = trmFactory.createInboundTrmMessage(jmo, getSubtype());
    }
    /* If the incoming message is some other sort of message it is an error.  */
    else {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Not a TRM Message");
      String exString = nls.getFormattedMessage("UNEXPECTED_MESSAGE_TYPE_CWSIF0102"
          ,new Object[] {getJsMessageType(), MessageType.TRM}
          ,"The Message is not a TRM Message");
      throw new IncorrectMessageTypeException(exString);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "makeInboundTrmMessage");
    return trmMessage;
  }


  /*
   *  Convert the existing inbound JsMessage into a SubscriptionMessage.
   *
   *  Javadoc description supplied by JsMessage interface.
   */
  public final SubscriptionMessage makeInboundSubscriptionMessage() throws IncorrectMessageTypeException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "makeInboundSubscriptionMessage");

    SubscriptionMessage subMessage = null;

    /* If the message has never been flattened/encoded, it will already be  */
    /* a SubscriptionMessage so we can just proceed with this recast.       */
    /* Otherwise we will need to convert it into a SubscriptionMessage.     */
    if (this instanceof SubscriptionMessageImpl) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Incoming message is already a SubscriptionMessage");
      subMessage = (SubscriptionMessageImpl)this;
    }
    /* The incoming message should be a SUBSCRIPTION message.                 */
    else if (getJsMessageType() == MessageType.SUBSCRIPTION) {
      // Ensure pending changes are flushed to JMF before passing the JMO around
      updateDataFields(MfpConstants.UDF_MAKE_INBOUND_OTHER);
      subMessage = new SubscriptionMessageImpl(jmo);
    }
    /* If the incoming message is some other sort of message it is an error.  */
    else {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Not a SUBSCRIPTION Message");
      String exString = nls.getFormattedMessage("UNEXPECTED_MESSAGE_TYPE_CWSIF0102"
                                               ,new Object[] {getJsMessageType(), MessageType.SUBSCRIPTION}
                                               ,"The Message is not a SUBSCRIPTION Message");
      throw new IncorrectMessageTypeException(exString);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "makeInboundSubscriptionMessage");
    return subMessage;
  }


  /* **************************************************************************/
  /* Methods for obtaining 'safe copies' of messages                          */
  /* **************************************************************************/

  /*
   *  Perform any send-time processing and return a 'safe copy' of the JsMessage.
   *  If the copy parameter is false, the 'safe copy' is the original message
   *  as no copy is actually required.
   *  <p>
   *  This method must be called by the Message processor during 'send'
   *  processing, AFTER the headers are set. The Message Processor should then
   *  use the returned message in the bus, which allows an Application to change the
   *  message it 'owns' without affectng the sent message.
   *
   *  Javadoc description supplied by JsMessage interface.
   */
  public JsMessage getSent(boolean copy) throws MessageCopyFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSent", copy);

    /* Now get the JsMessage to return */
    JsMessageImpl newMsg;

    /* If a copy is required, obtain one */
    if (copy) {
      newMsg = createNew();

      /* Reset the original's cached lengths as the content may be about to be  */
      /* completely changed by the producer.                                    */
      clearCachedLengths();
    }

    /* Otherwise we just return the original message. */
    else {
      newMsg = this;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSent", newMsg);
    return newMsg;
  }

  /*
   *  Return a 'safe copy' of the JsMessage.
   *  This method must be called by the Message processor during 'receive'
   *  processing for a pub/sub message. It must be called BEFORE the headers are
   *  set for the receive. The Message Processor should then return this
   *  'copy' to the receiver, which can then make any changes without affecting
   *  the message in the bus or the copy given to any other receivers.
   *
   *  Javadoc description supplied by JsMessage interface.
   */
  public final JsMessage getReceived() throws MessageCopyFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getReceived");

     /* Now get the JsMessage to return */
    JsMessage newMsg = createNew();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getReceived", newMsg);
    return newMsg;
  }


  /* **************************************************************************/
  /* Methods for encoding and flattening                                     */
  /* **************************************************************************/

  /*
   *  Flatten the message into a List of DataSlices for for transmission.
   *
   *  Javadoc description supplied by JsMessage interface.
   */
  public final List<DataSlice> encodeFast(Object conn) throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "encode");
    return jmo.encodeFast(conn);
  }

  /**
   * Transcribe this message to pure JMF
   *
   * Javadoc description supplied by JsMessage interface.
   */
  public JsMessage transcribeToJmf() throws MessageCopyFailedException
                                          , IncorrectMessageTypeException
                                          , UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "transcribeToJmf");
    JsMsgObject newJmo = null;

    // Try/catch block in case we get an MFPUnsupportedEncodingRuntimeException because the
    // payload is in an unsupported codepage.   d252277.2
    try {

     if (this instanceof JsJmsMessageImpl) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Transcribing from JsJmsMessageImpl");
        newJmo = jmo.transcribeToJmf();
      }

      else {
        // Non-JMS messages are not supported - they should have been converted into JMS by now
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Non-JMS Messages are not yet supported");
        String exString = nls.getFormattedMessage("UNEXPECTED_MESSAGE_TYPE_CWSIF0102"
                                                 ,new Object[] {getJsMessageType(), MessageType.JMS}
                                                 ,"The Message can not be represented as a pure JMF Message");
        throw new IncorrectMessageTypeException(exString);

      }
    }
    catch (MFPUnsupportedEncodingRuntimeException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMessageImpl.transcribeToJmf", "721");
      // Throw the original UnsupportedEncodingException
      throw (UnsupportedEncodingException)e.getCause();
    }

    JsMessage newMsg;
    // if we have a new jmo, create a new message from it
    if (newJmo != this.jmo) {
      newMsg = createNewGeneralized(newJmo);
    }
    // otherwise, just return ourself
    else {
      newMsg = this;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "transcribeToJmf", newMsg);
    return newMsg;
  }

  /*
   *  Flatten the message into a List of DataSlices for persisting into
   *  the Message Store.
   *  Most of the processing has moved to JsMsgObject to avoid the potential
   *  for deadlock between the layers.                                 d364050
   *
   *  Javadoc description supplied by JsMessage interface.
   *
   *  The flattened message needs to include:
   *     the message class name
   *     the list of JMF schema ids needed to decode the message
   *     the message itself
   */
  public final List<DataSlice> flatten(Object store) throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "flatten");

    List<DataSlice> slices = null;                                              // d395685

    try {                                                                       // d395685
      /* The JMO does the real work */
      slices = jmo.flatten(store, getFlattenedClassName());     // SIB0112b.mfp.2
    }                                                                           // d395685

    // If the flatten fails, it may be an UnsupportedEncodingException which means
    // we have an underlying MQ message with an unsupported CCSID for the data.
    // If that is the case, the message may well be on the ExceptionDestination
    // already so we have to be able to flatten it.                                d395685
    catch (MessageEncodeFailedException e) {
      // Lower-levels have already FFDC'd everything that could possible be useful.
      // No FFDC code needed

      // Is the exception caused by an unsupported CCSID?
      Throwable e1 = e.getCause();
      while ((e1 != null) && !(e1 instanceof UnsupportedEncodingException)) {
        e1 = e1.getCause();
      }
      // If so, we therefore transcribe it to JMF and have another go.
      if (e1 != null) {
        try {
          JsMessageImpl tempMsg = (JsMessageImpl)this.transcribeToJmf();
          slices = tempMsg.jmo.flatten(store, getFlattenedClassName());
        }
        // If we can't transcribe there must be soemthing else wrong so give up
        catch (Exception newe) {
          // Lower-levels have already FFDC'd everything that could possible be useful.
          // No FFDC code needed
          if (newe instanceof MessageEncodeFailedException) {
            throw (MessageEncodeFailedException)newe;
          }
          else {
            throw new MessageEncodeFailedException(newe);
          }
        }
      }

      // If we got to the end of the exception chain without finding UnsupportedEncodingException
      // we must have a disaster, so we just throw the original exception on.
      else {
        throw e;
      }
    }

    /* Now we know the real size of the flattened message, store it           */
    int temp = 0;
    for (int i=0; i < slices.size(); i++) {
      temp = temp + slices.get(i).getLength();
    }
    // Setting approxLength isn't synchronized as a) it is atomic & b) the correctness isn't vital
    approxLength = temp;

    /* and return the result */
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "flatten");
    return slices;
  }

  /**
   * Return an approximate size for the flattened message.  It is important that
   * this method is quick and cheap, rather than highly accurate.
   *
   * Javadoc description supplied by JsMessage interface.
   */
  public int getApproximateLength() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getApproximateLength");

    if (approxLength == -1) approxLength = guessApproxLength();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getApproximateLength", Integer.valueOf(approxLength));
    return approxLength;
  }

  /**
   * Get the approximate size of the message in memory.
   * This will be a will be a quick and inexpensive (but potentially very
   * inaccurate) calculation of the amount of space occupied in the heap by
   * a message which is both fully-fluffy and flattened (i.e. worst case).
   * Being both fluffy and flattened is also a description of a badger when it
   * meets a lorry. (GW)
   *
   * Javadoc description supplied by JsMessage interface.
   */
  public int getInMemorySize() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getInMemorySize");

    // The fluffed size needs to be calculated if it hasn't already been set.
    if (fluffedSize == -1) {
      fluffedSize = guessFluffedSize();
    }

    // Add together the values for the approximate encoded length & the fluffed size.
    // Use getApproximateLength() for the encoded length, so that we pick up any cached value.
    int inMemorySize = getApproximateLength() + fluffedSize;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getInMemorySize", inMemorySize);
    return inMemorySize;
  }

  /*
   * Obtain a WebJsMessageEncoder for writing to the Web client.
   *
   * Javadoc description supplied by JsMessage interface.
   */
  public WebJsMessageEncoder getWebEncoder() {
    // Implemented only for JsJmsMessage currently.
    throw new UnsupportedOperationException();
  }

  /**
   * @return a message encode capable of encoding this message into an MQMsg2
   * (MQ Java classes message format).
   * @see com.ibm.ws.sib.mfp.JsMessage#getMQMsg2Encoder()
   
  public MQMsg2Encoder getMQMsg2Encoder()
  {
     return new MQMsg2EncoderImpl(this);
  }*/

  /* ************************************************************************ */
  /* Size approximations                                                      */
  /* ************************************************************************ */

  // Assuming an initial value for these transients is not a problem, as the
  // superclass's readObject method explicitly sets them.
  private transient int approxLength = -1;
  private transient int fluffedSize = -1;

  /**
   * clearCachedLengths
   * Clear the cached approxLength and fluffedSize transient values to -1
   * which represents 'unset'.
   * This is non-final, so that sub-classes can clear any of their own cached
   * lengths as well as calling super.
   */
  void clearCachedLengths() {
    setApproximateLength(-1);
    setFluffedSize(-1);
  }


  /**
   * setApproximateLength
   * Set the approxLength transient to the given value.
   *
   * @param length            The approximate encoded length of the message, or
   *                          -1 which represents unset.
   */
  final void setApproximateLength(int length) {
    approxLength = length;
  }


  /**
   * setFluffedSize
   * Set the fluffedSize transient to the given value.
   *
   * @param size            The approximate in-memory size of the fluffed message, or
   *                        -1 which represents unset.
   */
  final void setFluffedSize(int size) {
    fluffedSize = size;
  }


  /**
   * Provide the contribution of this part to the estimated encoded length
   * Subclasses that wish to contribute to a quick guess at the length of a
   * flattened/encoded message should override this method, invoke their
   * superclass and add on their own contribution.
   *
   * @return int Description of returned value
   */
  int guessApproxLength() {

    // Make a guess at the contribution of the header. The header size is
    // reasonably constant, with the exception of the forward/reverse routing
    // paths.
    int total = 400;   // Approx 400 bytes in normal use header fields

    int size = 0;

    // Only get the FRP list out of the message if it is NOT the singleton empty list
    // as we don't want to spawn a real one unnecessarily
    if (getHdr2().isNotEMPTYlist(JsHdr2Access.FORWARDROUTINGPATH_DESTINATIONNAME)) {
      List frp = (List)getHdr2().getField(JsHdr2Access.FORWARDROUTINGPATH_DESTINATIONNAME);
      if (frp != null) size += frp.size();
    }

    // Only get the RRP list out of the message if it is NOT the singleton empty list
    // as we don't want to spawn a real one unnecessarily
    if (getHdr2().isNotEMPTYlist(JsHdr2Access.REVERSEROUTINGPATH_DESTINATIONNAME)) {
      List rrp = (List)getHdr2().getField(JsHdr2Access.REVERSEROUTINGPATH_DESTINATIONNAME);
      if (rrp != null) size += rrp.size();
    }

    // Assume about 50 bytes per routing path entry (2 strings + UUID)
    if (size > 0) {
      total += size * 50;
    }

    // Add on the contents of the first slice of a flattened message (i.e. classname,
    // Schema ids, etc)
    total += 77;
    return total;
  }


  /**
   * Provide the contribution of this part to the estimated 'fluffed' message size.
   * Subclasses that wish to contribute to a quick guess at the length of a
   * fluffed message should override this method, invoke their superclass and add on
   * their own contribution.
   *
   * For this class, we should return an approximation of the size of the JMF
   * JSMessageImpls which represent the JsHdr & JsHdr2 Schemas,
   * (including everything, except JSDynamics, under their trees)
   * for a populated but not assembled message.
   *
   * @return int A guesstimate of the fluffed size of the message
   */
  int guessFluffedSize() {

    // Start with the base fluffed size of the message headers.
    int total = FLUFFED_HEADER_BASE_SIZE;

    // For each routing path, get the estimates size for the destination name list.
    // The rp also contains a list of String busnames, a list of SIBUuid8s & a byte
    // array, so we guess at triple the estimate of the names size.
    int listSize = getHdr2().estimateFieldValueSize(JsHdr2Access.FORWARDROUTINGPATH_DESTINATIONNAME);
    total += listSize*3;
    listSize = getHdr2().estimateFieldValueSize(JsHdr2Access.REVERSEROUTINGPATH_DESTINATIONNAME);
    total += listSize*3;

    // Add more if the various 'guaranteed' variants are set
    if (getHdr2().getChoiceField(JsHdr2Access.GUARANTEED) == JsHdr2Access.IS_GUARANTEED_SET) {
      total += 350;
    }
    if (getHdr2().getChoiceField(JsHdr2Access.GUARANTEEDXBUS) == JsHdr2Access.IS_GUARANTEEDXBUS_SET) {
      total += 100;
    }
    if (getHdr2().getChoiceField(JsHdr2Access.GUARANTEEDVALUE) == JsHdr2Access.IS_GUARANTEEDVALUE_SET) {
      total += 200;
    }
    if (getHdr2().getChoiceField(JsHdr2Access.GUARANTEEDREMOTEBROWSE) == JsHdr2Access.IS_GUARANTEEDREMOTEBROWSE_SET) {
      total += 100;
    }
    if (getHdr2().getChoiceField(JsHdr2Access.GUARANTEEDREMOTEGET) == JsHdr2Access.IS_GUARANTEEDREMOTEGET_SET) {
      total += 180;
    }

    return total;
  }


  /* **************************************************************************/
  /* Implementation of MatchSpaceKey interface                                */
  /* **************************************************************************/

  /*
   *  Evaluate the message field determined by the given Identifier
   *
   *  Usually the field is either an SI or JMS header field or an SI, JMS or user
   *  property, but SIB0136 introduces selection using XPATH expressions against
   *  the message payload.
   *
   *  Javadoc description supplied by MatchSpaceKey interface.
   *
   *  If the message is not a JsJmsMessage message, selectors are not supported.
   *  If it is a JsJmsMessage. this method will have been overridden.
   */
  public Object getIdentifierValue(Identifier id,
                                   boolean ignoreType)
                             throws BadMessageFormatMatchingException {

    throw new BadMessageFormatMatchingException(nls.getFormattedMessage("NOT_API_MESSAGE_CWSIF0101"
                                                                       ,new Object[] { getSystemMessageId() }
                                                                       ,"Message selectors are not supported for message " + getSystemMessageId()
                                                                       ));
  }

  public Object getIdentifierValue(Identifier id,
                                   boolean ignoreType,
                                   Object contextValue,
                                   boolean returnList)
                             throws BadMessageFormatMatchingException {

    throw new BadMessageFormatMatchingException(nls.getFormattedMessage("NOT_API_MESSAGE_CWSIF0101"
                                                                       ,new Object[] { getSystemMessageId() }
                                                                       ,"Message selectors are not supported for message " + getSystemMessageId()
                                                                       ));
  }

  /*
   *  Provided for use in XPath support where the MatchSpace calls MFP
   *  in order to retrieve the top most Node in a DOM tree.
   *
   *  Javadoc description supplied by MatchSpaceKey interface.
   *  If the message is not an API message, there is no Node to return.
   *  If it is an API message, this method will be overridden in JsApiMessageImpl.
   */
  public Object getRootContext() throws BadMessageFormatMatchingException {
    throw new BadMessageFormatMatchingException(nls.getFormattedMessage("NOT_API_MESSAGE_CWSIF0101"
                                                                       ,new Object[] { getSystemMessageId() }
                                                                       ,"Message selectors are not supported for message " + getSystemMessageId()
                                                                       ));
  }

  /* **************************************************************************/
  /* toString()                                                               */
  /* **************************************************************************/

  /**
   * Return a String representation of the message including the
   * System Message ID
   *
   * @return String a String representation of the message
   */
  public String toString() {
    return super.toString() + "{SysMsgId=" + this.getSystemMessageId() + "}";
  }


  /* **************************************************************************/
  /* Package and Private Methods                                              */
  /* **************************************************************************/

  /**
   * Return the name of the concrete implementation class encoded into bytes
   * using UTF8.
   * A concrete implementation of this method ahould be provided by all the
   * instantiable subclasses, as they can cache the vakue to be returned. The
   * implementation here is a fall-back for any missing overriding method.
   *
   * @return byte[] The name of the implementation class encoded into bytes.
   */
  byte[] getFlattenedClassName() {
    return flattenClassName(getClass().getName());
  }

  /**
   *  Return a new Jsmessage, of the same specialization as this, containing the
   *  given JsMsgObject.
   *
   *  @param newJmo  The JsMsgObject the new JsMessage will contain.
   *
   *  @return JsMessage A new JsMessage instance of the appropriate specialization.
   *
   *  @exception MessageCopyFailedException A MessageCopyFailedException is thrown if
   *     either the constructor could not be found or the instantiation failed. The
   *     Exception contains the toString() result of the original Exception.
   */
  private final JsMessageImpl createNew() throws MessageCopyFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createNew");

    JsMessageImpl newMsg = null;
    Class msgClass = this.getClass();

    try {
      /* Create the new JsMessage*/
      newMsg = (JsMessageImpl)msgClass.newInstance();
      /* copy transients before copying JMO to ensure safe ordering - see SIB0121.mfp.2 */
      copyTransients(newMsg);

      /* Get a new copy of the JMO and set it into the new message*/
      JsMsgObject newJmo = jmo.getCopy();
      newMsg.setJmo(newJmo);
    }
    catch (IllegalAccessException e1) {
      FFDCFilter.processException(e1, "com.ibm.ws.sib.mfp.impl.JsMessageImpl.createNew", "jsm1600");
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "IllegalAccessException " + e1.getMessage() + " instantiating class " + msgClass.getName());
      throw new MessageCopyFailedException(e1);
    }
    catch (InstantiationException e2) {
      FFDCFilter.processException(e2, "com.ibm.ws.sib.mfp.impl.JsMessageImpl.createNew", "jsm1610");
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "InstantiationException " + e2.getMessage() + " instantiating class " + msgClass.getName());
      throw new MessageCopyFailedException(e2);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createNew", newMsg);
    return newMsg;
  }

  /**
   *  Return a new JsMessage, generalizing the message to just be a JsMessageImpl.
   *  The new message contains the given JsMsgObject.
   *
   *  @param newJmo  The JsMsgObject the new JsMessage will contain.
   *
   *  @return JsMessage A new JsMessage instance at the JsMessageImpl specialization.
   */
  private final JsMessageImpl createNewGeneralized(JsMsgObject newJmo) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createNewGeneralized");

    JsMessageImpl newMsg = null;
    /* Now create the new JsMessage  */
    newMsg = new JsMessageImpl(newJmo);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createNewGeneralized", newMsg);
    return newMsg;
  }

  /*
   * Copies any transient data into the copy of the message passed.
   *
   * Sub-classes should override this (calling super.copyTransients()) to copy
   * any transient cached data into new copies of messages.
   *
   */
  void copyTransients(JsMessageImpl copy) {
    super.copyTransients(copy);
    copy.setApproximateLength(approxLength);
    copy.setFluffedSize(fluffedSize);
    return;
  }

  /*
   * Common prefix for all trace summary lines of messages that stem from the
   * JsMessage side of the MFP class structure.
   * All subclasses should call this, and then append any additional details
   * if applicable. 
   */
  public void getTraceSummaryLine(StringBuilder buff) {
    buff.append("MESSAGE:");
    buff.append("type=");
    buff.append(getJsMessageType());
    buff.append(",id=");
    buff.append(getSystemMessageId());
    buff.append(",proto=");
    buff.append(getGuaranteedProtocolType());
    buff.append(",tick=");
    buff.append(getGuaranteedValueValueTick());
  }
  
  /**
   * Helper method to append a list (which doesn't have a standard toString
   * as in the case of JsVaringListImpl) to a summary trace string.
   * Also provides hex output for byte arrays.
   */
  protected void appendList(StringBuilder buff, String name, List list) {
    buff.append(',');
    buff.append(name);
    buff.append("=[");
    if (list != null) {
      // Take a copy of the array, to be very sure it doesn't change under us
      Object[] items = list.toArray();
      for (int i = 0; i < items.length; i++) {
        if (i != 0) buff.append(',');
        if (items[i] instanceof byte[]) {
          // Print out the bytes in hex.
          buff.append(HexUtil.toString((byte[])items[i]));
        }
        else {
          // Just use the default toString
          buff.append(items[i]);
        }
      }
    }
    buff.append(']');
  }
}
