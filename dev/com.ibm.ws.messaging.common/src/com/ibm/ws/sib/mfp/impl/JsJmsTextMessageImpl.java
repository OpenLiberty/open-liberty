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
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.ws.sib.mfp.JmsBodyType;
import com.ibm.ws.sib.mfp.JsJmsTextMessage;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.schema.JmsTextBodyAccess;
import com.ibm.ws.sib.mfp.schema.JsPayloadAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 *  JsJmsTextMessageImpl extends JsJmsMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the JsJmsTextMessage interface.
 */
final class JsJmsTextMessageImpl extends JsJmsMessageImpl implements JsJmsTextMessage {

  private final static long serialVersionUID = 1L;
  private final static byte[] flattenedClassName;                               // SIB0112b.mfp.2

  private static TraceComponent tc = SibTr.register(JsJmsTextMessageImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

 
  /* Get the flattened form of the classname                   SIB0112b.mfp.2 */
  static {
    flattenedClassName = flattenClassName(JsJmsTextMessageImpl.class.getName());
  }

  // Cached DOM Document representation of the payload text
  private transient SoftReference<Document> softRefToDocument = null;

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream JMS TextMessage.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  JsJmsTextMessageImpl() {
  }

  /**
   *  Constructor for a new Jetstream JMS TextMessage.
   *  To be called only by the JsJmsMessageFactory.
   *
   *  @param flag Flag to distinguish different construction reasons.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  JsJmsTextMessageImpl(int flag)  throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>");

    /* Set the JMS format & body information */
    setFormat(SIApiConstants.JMS_FORMAT_TEXT);
    setBodyType(JmsBodyType.TEXT);

    // We can skip this for an inbound MQ message as the MQJsMessageFactory will
    // replace the PAYLOAD_DATA with an MQJsApiEncapsulation.
    if (flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ) {
      jmo.getPayloadPart().setPart(JsPayloadAccess.PAYLOAD_DATA, JmsTextBodyAccess.schema);
      clearBody();
    }
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound method.
   */
  JsJmsTextMessageImpl(JsMsgObject inJmo) {
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
  public String getText() throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getText");
    String text = null;
    try {
      text = (String)getPayload().getField(JmsTextBodyAccess.BODY_DATA_VALUE);
    }
    catch (MFPUnsupportedEncodingRuntimeException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsJmsTextMessageImpl.getText", "148");
      throw (UnsupportedEncodingException)e.getCause();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
      if ((text == null) || text.length() < 257) {
        SibTr.exit(this, tc, "getText", text);
      }
      else {
        SibTr.exit(this, tc, "getText", new Object[]{text.length(), text.substring(0,200)+"..."});
      }
    }
    return text;
  }


  /*
   *  Set the body (payload) of the message.
   *
   *  Javadoc description supplied by JsJmsTextMessage interface.
   */
  public void setText(String text) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
      if ((text == null) || text.length() < 257) {
        SibTr.entry(this, tc, "setText", text);
      }
      else {
        SibTr.entry(this, tc, "setText", new Object[]{text.length(), text.substring(0,200)+"..."});
      }
    }

    // If the payload is changing, we need to clear all the cached values.
    softRefToDocument = null;
    clearCachedLengths();

    // Set the payload data
    getPayload().setField(JmsTextBodyAccess.BODY_DATA_VALUE, text);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setText");
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
    getPayload().setChoiceField(JmsTextBodyAccess.BODY, JmsTextBodyAccess.IS_BODY_EMPTY);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "clearBody");
  }


  /*
   * Provide an estimate of encoded length of the payload
   */
  int guessPayloadLength() {
    int length = 0;
    // It is likely that the String length will be the same as the length in bytes
    // when it has been encoded (to UTF8) plus a small overhead.
    try {
      if (jmo.getPayloadPart().getChoiceField(JsPayloadAccess.PAYLOAD) != JsPayloadAccess.IS_PAYLOAD_EMPTY) {
        String payload = (String)getPayload().getField(JmsTextBodyAccess.BODY_DATA_VALUE);
        if (payload != null) length = payload.length() + 24;
      }
    }
    catch (MFPUnsupportedEncodingRuntimeException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsJmsTextMessageImpl.getText", "129");
      // hmm... how do we figure out a reasonable length
    }
    return length;
  }


  /**
   * guessFluffedDataSize
   * Return the estimated fluffed size of the payload data.
   *
   * For this class, we should return an approximation of the fluffed up payload
   * data, which is a String.
   *
   * @return int A guesstimate of the fluffed size of the payload datae
   */
  int guessFluffedDataSize() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "guessFluffedDataSize");

    int total = 0;

    // Add the estimate for the fluffed payload size
    // If the body's JMF message is already fluffed up & cached, ask it for the size.
    // Do NOT hold on to this JSMsgPart, as it could lose validity at any time.
    JsMsgPart part = getPayloadIfFluffed();
    if (part != null) {
      total += part.estimateFieldValueSize(JmsTextBodyAccess.BODY_DATA_VALUE);
    }

    // If the JMF message hasn't been fluffed up, find the total assembled length of
    // the payload message if possible.
    else {
      // If we have a valid length, remove a bit & assume the rest is the encoded text.
      // Assume it's in UTF8, so double it & add an overhead to get the total String size.
      int payloadSize = jmo.getPayloadPart().getAssembledLengthIfKnown();
      if (payloadSize != -1) {
        total += (payloadSize-FLATTENED_PAYLOAD_PART)*2 + FLUFFED_STRING_OVERHEAD;
      }
      // If the payloadSize == -1, then the body message must have been fluffed up
      // but not yet cached, so we'll locate & cache it now.
      else {
        total += getPayload().estimateFieldValueSize(JmsTextBodyAccess.BODY_DATA_VALUE);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "guessFluffedDataSize",  total);
    return total;
  }


  // Convenience method to get the payload as a JmsTextBodySchema
  JsMsgPart getPayload() {
    return getPayload(JmsTextBodyAccess.schema);
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
                                             IOException,
                                             UnsupportedEncodingException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getPayloadDocument");

    Document domDoc = null;

    // If we already have a Document cached we don't need to build it again
    if (softRefToDocument != null) {
      domDoc = softRefToDocument.get();
    }

    // Otherwise we'll take the hit of building it, and cache it in a soft reference
    if (domDoc == null) {
      String text = getText();
      if ((text != null) && (text.length() > 0)) {
        DocumentBuilderFactory domFactory =DocumentBuilderFactory.newInstance();
        // We need to tell the factory that we want it to be namespace aware.
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(text));
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
