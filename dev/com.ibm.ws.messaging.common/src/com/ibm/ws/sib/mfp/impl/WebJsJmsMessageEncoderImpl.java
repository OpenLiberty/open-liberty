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
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.websphere.sib.SIProperties;
import com.ibm.ws.sib.mfp.JsJmsBytesMessage;
import com.ibm.ws.sib.mfp.JsJmsMapMessage;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.JsJmsObjectMessage;
import com.ibm.ws.sib.mfp.JsJmsStreamMessage;
import com.ibm.ws.sib.mfp.JsJmsTextMessage;
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.ObjectFailedToSerializeException;
import com.ibm.ws.sib.mfp.WebJsMessageEncoder;
import com.ibm.ws.sib.utils.HexString;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 * Write a JMS message to a simple text encoding that can be interpreted by the
 * Web client.  The encoding consists of a number of text fields separated by
 * tilda '~' characters.  The following fields are encoded:
 *
 * Version:  The current encoding version number.  Set to '1'.
 *
 * MessageId: The JMSCorrelationID.  This will be the 'bytes' form of the
 *            correlation id, encoded as two ascii hex digits per byte.
 *
 * MessageType: The JMSXAppID property.  If the property has the single byte
 *              'compact' encoding we use 'SIB' for messages originating in
 *              Jetstream and 'Web' for messages originating on the Web client.
 *              Any other values is a URL-encoded string.
 *
 * MessageFormat: The 'format' string.  As we're encoding a JMS message this will
 *                be one of the 'JMS:xxxx' values.
 *
 * Topic: The message 'discriminator' value.  A URL-encoded string.
 *
 * Properties: The message properties encoded as a set of 'name=value' pairs
 *             separated by ampersand '&' characters.  The name will be a URL
 *             encoded string, the value will be a URL-encoded string form of the
 *             property value.
 *
 * Body: The JMS body.  This may be omitted if there is no body.
 *       Text:   URL-encoded text body
 *       Bytes:  The byte stream encoded with two ascii hex digits per byte.
 *       Object: Encoded as a bytes message.  Byte stream is the serialized message.
 *       Stream: A sequence of URL-encoded, ampersand separated strings of the items in the stream
 *       Map:    A set of ampersand separated 'name=value' pairs, like properties.
 */

public class WebJsJmsMessageEncoderImpl implements WebJsMessageEncoder {
  private static TraceComponent tc = SibTr.register(WebJsJmsMessageEncoderImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
  private JsJmsMessage jmsMsg;

  /*
   * Construct a new WebJsJmsMessageEncoder
   * @param msg The original JsJmsMessage that is to be encoded
   */
  WebJsJmsMessageEncoderImpl(JsJmsMessage msg) {
    jmsMsg = msg;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.WebJsMessageEncoder#encodeForWebClient()
   */
  public String encodeForWebClient() throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "encodeForWebClient");
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Encoding Web Message", ((MessageImpl)jmsMsg).debugMsg());

    // Make sure any transients necessary are written back to the message
    ((JsJmsMessageImpl)jmsMsg).updateDataFields(MfpConstants.UDF_ENCODE);

    StringBuffer result = new StringBuffer();

    // Version number
    result.append(1);

    // Message Id
    result.append('~');
    byte[] id = jmsMsg.getCorrelationIdAsBytes();
    if (id != null)
      HexString.binToHex(id, 0, id.length, result);

    // Message type
    result.append('~');
    String type = jmsMsg.getJmsxAppId();
    if (type == null || type.equals(MfpConstants.WPM_JMSXAPPID_VALUE))
      type = "SIB";
    URLEncode(result, type);

    // Message format
    result.append('~');
    result.append(jmsMsg.getFormat());

    // Topic
    result.append('~');
    String topic = jmsMsg.getDiscriminator();
    if (topic != null)
      URLEncode(result, topic);

    // Properties
    result.append('~');
    Set<String> names = jmsMsg.getPropertyNameSet();
    boolean first = true;
    for (Iterator<String> i = names.iterator(); i.hasNext(); ) {
      String name = i.next();

      // JMSXAppID is stored in the header field instead so ignore it here
      if (!name.equals(SIProperties.JMSXAppID)) {

        // All objects
        Object value = jmsMsg.getObjectProperty(name);
        if (value != null) {
          encodePair(result, name, value, first);
          first = false;
        }
      }
    }

    // Body
    if (jmsMsg instanceof JsJmsTextMessage)
      encodeTextBody(result, (JsJmsTextMessage)jmsMsg);
    else if (jmsMsg instanceof JsJmsBytesMessage)
      encodeBytesBody(result, (JsJmsBytesMessage)jmsMsg);
    else if (jmsMsg instanceof JsJmsObjectMessage)
      encodeObjectBody(result, (JsJmsObjectMessage)jmsMsg);
    else if (jmsMsg instanceof JsJmsStreamMessage)
      encodeStreamBody(result, (JsJmsStreamMessage)jmsMsg);
    else if (jmsMsg instanceof JsJmsMapMessage)
      encodeMapBody(result, (JsJmsMapMessage)jmsMsg);

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Encoded as web data", result);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "encodeForWebClient");
    return result.toString();
  }

  // Encode a text message body
  private void encodeTextBody(StringBuffer result, JsJmsTextMessage msg) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "encodeTextBody");
    try {
      String body = msg.getText();
      if (body != null) {
        result.append('~');
        URLEncode(result, body);
      }
    }
    catch (UnsupportedEncodingException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.WebJsJmsMessageEncoderImpl.encodeTextBody", "193");
      // Just treat it as null.
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "encodeTextBody");
  }

  // Encode a bytes message body
  private void encodeBytesBody(StringBuffer result, JsJmsBytesMessage msg) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "encodeBytesBody");
    byte[] body = msg.getBytes();
    if (body != null) {
      result.append('~');
      HexString.binToHex(body, 0, body.length, result);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "encodeBytesBody");
  }

  // Encode an object message body
  private void encodeObjectBody(StringBuffer result, JsJmsObjectMessage msg) throws MessageEncodeFailedException{
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "encodeObjectBody");
    try {
      byte[] body = msg.getSerializedObject();
      if (body != null) {
        result.append('~');
        HexString.binToHex(body, 0, body.length, result);
      }
    }
    catch (ObjectFailedToSerializeException ofse) {
      // This should not be possible, as updateDataFields will have been called
      // earlier so any unserializable object will already have been dealt with.
      FFDCFilter.processException(ofse, "com.ibm.ws.sib.mfp.impl.WebJsJmsMessageEncoderImpl.encodeObjectBody", "225");
      throw new MessageEncodeFailedException(ofse);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "encodeObjectBody");
  }

  // Encode a stream message body
  private void encodeStreamBody(StringBuffer result, JsJmsStreamMessage msg) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "encodeStreamBody");
    // Internal method to get the stream as a List
    try {
      List<Object> body = ((JsJmsStreamMessageImpl)msg).getBodyList();
      if (body.size() > 0) {
        result.append('~');
        for (int i = 0; i < body.size(); i++) {
          encodeObject(result, body.get(i));
          if (i < body.size()-1)
            result.append('&');
        }
      }
    }
    catch (UnsupportedEncodingException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.WebJsJmsMessageEncoderImpl.encodeStreamBody", "245");
      // Just treat it as null.
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "encodeStreamBody");
  }

  // Encode a map message
  private void encodeMapBody(StringBuffer result, JsJmsMapMessage msg) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "encodeMapBody");
    // Internal method to get the map as a Map
    try {
      Map<String, Object> body = ((JsJmsMapMessageImpl)msg).getBodyMap();
      if (body.size() > 0) {
        result.append('~');
        Set<Map.Entry<String, Object>> entries = body.entrySet();
        boolean first = true;
        for (Iterator<Map.Entry<String, Object>> i = entries.iterator(); i.hasNext(); ) {
          Map.Entry<String, Object> entry = i.next();
          encodePair(result, entry.getKey(), entry.getValue(), first);
          first = false;
        }
      }
    }
    catch (UnsupportedEncodingException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.WebJsJmsMessageEncoderImpl.encodeMapBody", "269");
      // Just treat it as null.
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "encodeMapBody");
  }

  // Encode a 'name=value' pair
  private void encodePair(StringBuffer result, String name, Object value, boolean first) {
    if (!first)
      result.append('&');
    URLEncode(result, name);
    result.append('=');
    encodeObject(result, value);
  }

  // Encode an arbitrary value
  private void encodeObject(StringBuffer result, Object value) {
    if (value != null) {
      if (value instanceof byte[]) {
        result.append("[]");
        HexString.binToHex((byte[])value, 0, ((byte[])value).length, result);
      } else
        URLEncode(result, value.toString());
    }
  }

  // URL encode a string and append to the result buffer.  We need to extend the standard
  // java.net.URLEncoder to force 'space' characters to encode as '%20' and not '+', due
  // to problems (bugs?) in some browsers where the Web client runs.
  private void URLEncode(StringBuffer result, String text) {
    try {
      String enc = URLEncoder.encode(text, "UTF8");
      if (enc.indexOf('+') != -1)
        enc = enc.replaceAll("\\+", "%20");
      result.append(enc);
    } catch (UnsupportedEncodingException e) {
      // Should never happen - all JDKs must support UTF8
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.WebJsJmsMessageEncoderImpl.URLEncode", "306");
    }
  }
}
