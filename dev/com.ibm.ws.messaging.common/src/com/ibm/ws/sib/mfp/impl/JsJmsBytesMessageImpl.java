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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.ws.sib.mfp.*;
import com.ibm.ws.sib.mfp.schema.JmsBytesBodyAccess;
import com.ibm.ws.sib.mfp.schema.JsPayloadAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 *  JsJmsTextMessageImpl extends JsJmsMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the JsJmsTextMessage interface.
 */
final class JsJmsBytesMessageImpl extends JsJmsMessageImpl implements JsJmsBytesMessage {

  private final static long serialVersionUID = 1L;
  private final static byte[] flattenedClassName;                               // SIB0112b.mfp.2

  private static TraceComponent tc = SibTr.register(JsJmsBytesMessageImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* Get the flattened form of the classname                   SIB0112b.mfp.2 */
  static {
    flattenedClassName = flattenClassName(JsJmsBytesMessageImpl.class.getName());
  }

  // Cached DOM Document representation of the payload text
  private transient SoftReference<Document> softRefToDocument = null;

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream JMS BytesMessage.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  JsJmsBytesMessageImpl() {
  }

  /**
   *  Constructor for a new Jetstream JMS BytesMessage.
   *  To be called only by the JsJmsMessageFactory.
   *
   *  @param flag Flag to distinguish different construction reasons.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  JsJmsBytesMessageImpl(int flag)  throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>");

    /* Set the JMS format & body information */
    setFormat(SIApiConstants.JMS_FORMAT_BYTES);
    setBodyType(JmsBodyType.BYTES);

    // We can skip this for an inbound MQ message as the MQJsMessageFactory will
    // replace the PAYLOAD_DATA with an MQJsApiEncapsulation.
    if (flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ) {
      jmo.getPayloadPart().setPart(JsPayloadAccess.PAYLOAD_DATA, JmsBytesBodyAccess.schema);
      clearBody();
    }
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound method.
   */
  JsJmsBytesMessageImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>, inbound jmo ");
  }

  /* ************************************************************************ */
  /* Payload Methods                                                          */
  /* ************************************************************************ */

  /*
   *  Get the body (payload) of the message.
   *
   *  Javadoc description supplied by JsJmsTextMessage interface.
   */
  public byte[] getBytes(){
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBytes");

    byte[] payload = (byte[])getPayload().getField(JmsBytesBodyAccess.BODY_DATA_VALUE);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
      if (payload == null) {
        SibTr.exit(this, tc, "getBytes", null);
      }
      else {
        SibTr.exit(this, tc, "getBytes", new Object[]{payload, payload.length});
      }
    }
    return payload;
  }


  /*
   *  Set the body (payload) of the message.
   *
   *  Javadoc description supplied by JsJmsTextMessage interface.
   *
   *  The act of setting the payload must cause a copy of the byte array to be
   *  made, in order to ensure that the payload sent matches the byte array
   *  passed in. If no copy was made it would be possible for the content to
   *  changed before the message was transmitted or delivered.
   *  However, any copying required is performed by the calling component and
   *  not the MFP component.
   */
  public void setBytes(byte[] payload) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
      if (payload == null) {
        SibTr.entry(this, tc, "setBytes", null);
      }
      else {
        SibTr.entry(this, tc, "setBytes", new Object[]{payload, payload.length});
      }
    }
    softRefToDocument = null;
    clearCachedLengths();
    getPayload().setField(JmsBytesBodyAccess.BODY_DATA_VALUE, payload);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBytes");
  }


  /*
   *  Clear the message body.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void clearBody() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "clearBody");
    softRefToDocument = null;
    clearCachedLengths();
    getPayload().setChoiceField(JmsBytesBodyAccess.BODY, JmsBytesBodyAccess.IS_BODY_EMPTY);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "clearBody");
  }


  /*
   * Provide an estimate of encoded length of the payload
   */
  int guessPayloadLength() {
    int length = 0;
    byte[] payload = (byte[])getPayload().getField(JmsBytesBodyAccess.BODY_DATA_VALUE);
    if (payload != null) {
      length = payload.length + 24;
    }
    return length;
  }


  /**
   * guessFluffedDataSize
   * Return the estimated fluffed size of the payload data.
   *
   * For this class, we should return an approximation of the fluffed up payload
   * data, which is a byte array.
   *
   * @return int A guesstimate of the fluffed size of the payload data
   */
  int guessFluffedDataSize() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "guessFluffedDataSize");

    int total = 0;

    // Add the estimate for the fluffed payload size
    // If the body's JMF message is already fluffed up & cached, ask it for the size.
    // Do NOT hold on to this JSMsgPart, as it could lose validity at any time.
    JsMsgPart part = getPayloadIfFluffed();
    if (part != null) {
      total += part.estimateFieldValueSize(JmsBytesBodyAccess.BODY_DATA_VALUE);
    }

    // If the JMF message hasn't been fluffed up, find the total assembled length of
    // the payload message if possible.
    else {
      // If we have a valid length, remove a bit & assume the rest is the encoded text.
      // As it is a byte[], just add an overhead of the array itself.
      int payloadSize = jmo.getPayloadPart().getAssembledLengthIfKnown();
      if (payloadSize != -1) {
        total += (payloadSize-FLATTENED_PAYLOAD_PART) + FLUFFED_OBJECT_OVERHEAD;
      }
      // If the payloadSize == -1, then the body message must have been fluffed up
      // but not yet cached, so we'll locate & cache it now.
      else {
        total += getPayload().estimateFieldValueSize(JmsBytesBodyAccess.BODY_DATA_VALUE);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "guessFluffedDataSize",  total);
    return total;
  }


  // Convenience method to get the payload as a JmsBytesBodySchema
  JsMsgPart getPayload() {
    return getPayload(JmsBytesBodyAccess.schema);
  }

  /* **************************************************************************/
  /* Misc Package and Private Methods                                         */
  /* **************************************************************************/

  /**
   * Return the name of the concrete implementation class encoded into bytes
   * using UTF8.                                              SIB0112b.mfp.2
   *
   * @return byte[] The name of the implementation class encoded into bytes.
   */
  final byte[] getFlattenedClassName() {
    return flattenedClassName;
  }

  /**
   * Return a DOM Document representation of the message payload.
   *
   * Javadoc description supplied by the JsApiMessageImpl superclass.
   */
  final Document getPayloadDocument() throws ParserConfigurationException,
                                             IOException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getPayloadDocument");

    Document domDoc = null;

    // If we already have a Document cached we don't need to build it again
    if (softRefToDocument != null) {
      domDoc = softRefToDocument.get();
    }

    // Otherwise we'll take the hit of building it, and cache it in a soft reference
    if (domDoc == null) {
      byte[] bytes = getBytes();
      if ((bytes != null) && (bytes.length > 0)) {
        DocumentBuilderFactory domFactory =DocumentBuilderFactory.newInstance();
        // We need to tell the factory that we want it to be namespace aware.
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        InputStream is = new ByteArrayInputStream(bytes);
        try {
          domDoc = builder.parse(is);
          softRefToDocument = new SoftReference<Document>(domDoc);
        }
        catch (SAXException e) {
          // No FFDC code needed
          // This is not really an error - it just means the data is not XML.
          // As the XPath Expression can't evaluate to True we just set the document to null;
          domDoc = null;
        }
      }
      else {
        domDoc = null;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getPayloadDocument", domDoc);
    return domDoc;
  }

}
