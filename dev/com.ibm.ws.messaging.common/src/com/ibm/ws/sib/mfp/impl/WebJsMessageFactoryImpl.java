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
import java.net.URLDecoder;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.websphere.sib.SIProperties;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsJmsBytesMessage;
import com.ibm.ws.sib.mfp.JsJmsMapMessage;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.JsJmsObjectMessage;
import com.ibm.ws.sib.mfp.JsJmsStreamMessage;
import com.ibm.ws.sib.mfp.JsJmsTextMessage;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.HexString;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class is delgated to by JsMessageFactoryImpl to handle the decoding of inbound
 * Web client messages.
 */
public class WebJsMessageFactoryImpl {
    private static TraceComponent tc = SibTr.register(WebJsMessageFactoryImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

    /**
     * Create a JsMessage from the simple text string encoding received from a Web client.
     */
    static JsMessage createInboundWebMessage(String data) throws MessageDecodeFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createInboundWebMessage");
        JsJmsMessage result = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Decoding message from web data", data);

        // The inbound data consists of a number of '~' delimited fields. Refer to the
        // description in WebJsJmsMessageEncoderImpl for details.
        StringTokenizer st = new StringTokenizer(data, "~", true);

        // Pull out the header fields and the message payload.  Remember that it is possible
        // for fields to be empty strings which will be encoded consecutive delimiters.
        String version, id, type, format, topic, props, body;
        version = id = type = format = topic = props = body = null;
        try {
            version = st.nextToken();
            st.nextToken();
            String s = st.nextToken();
            if (!s.equals("~")) {
                id = s;
                st.nextToken();
            }
            s = st.nextToken();
            if (!s.equals("~")) {
                type = s;
                st.nextToken();
            }
            s = st.nextToken();
            if (!s.equals("~")) {
                format = s;
                st.nextToken();
            }
            s = st.nextToken();
            if (!s.equals("~")) {
                topic = s;
                st.nextToken();
            }
            s = st.nextToken();
            if (!s.equals("~")) {
                props = s;
                // This may be the end
                if (st.hasMoreTokens())
                    st.nextToken();
            }
            // The body may be omitted completely
            if (st.hasMoreTokens()) {
                body = st.nextToken();
            }
        } catch (NoSuchElementException e) {
            FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.WebJsMessageFactoryImpl.createInboundWebMessage", "119");
            throw new MessageDecodeFailedException("Inccorect WebClient encoding", e);
        }

        // Verify the data
        if (!version.equals("1"))
            throw new MessageDecodeFailedException("Incorrect WebClient encoding version: " + version);
        if (!format.startsWith(SIApiConstants.JMS_FORMAT))
            throw new MessageDecodeFailedException("Incorrect WebClient message format: " + format);

        // Decode the mesasge
        if (format.equalsIgnoreCase(SIApiConstants.JMS_FORMAT_TEXT))
            result = decodeTextBody(body);
        else if (format.equalsIgnoreCase(SIApiConstants.JMS_FORMAT_BYTES))
            result = decodeBytesBody(body);
        else if (format.equalsIgnoreCase(SIApiConstants.JMS_FORMAT_OBJECT))
            result = decodeObjectBody(body);
        else if (format.equalsIgnoreCase(SIApiConstants.JMS_FORMAT_STREAM))
            result = decodeStreamBody(body);
        else if (format.equalsIgnoreCase(SIApiConstants.JMS_FORMAT_MAP))
            result = decodeMapBody(body);
        else
            result = new JsJmsMessageImpl(MfpConstants.CONSTRUCTOR_NO_OP);

        decodeHeader(result, id, type, topic, props);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Decoded Web Message", ((MessageImpl) result).debugMsg());
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createInboundWebMessage");
        return result;
    }

    // Decode a text message
    private static JsJmsMessage decodeTextBody(String body) throws MessageDecodeFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "decodeTextBody");
        JsJmsTextMessage result = new JsJmsTextMessageImpl(MfpConstants.CONSTRUCTOR_NO_OP);
        if (body != null) {
            result.setText(URLDecode(body));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "decodeTextBody");
        return result;
    }

    // Decode a bytes message
    private static JsJmsMessage decodeBytesBody(String body) throws MessageDecodeFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "decodeBytesBody");
        JsJmsBytesMessage result = new JsJmsBytesMessageImpl(MfpConstants.CONSTRUCTOR_NO_OP);
        if (body != null)
            result.setBytes(HexString.hexToBin(body, 0));
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "decodeBytesBody");
        return result;
    }

    // Decode an object message
    private static JsJmsMessage decodeObjectBody(String body) throws MessageDecodeFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "decodeObjectBody");
        JsJmsObjectMessage result = new JsJmsObjectMessageImpl(MfpConstants.CONSTRUCTOR_NO_OP);
        if (body != null)
            result.setSerializedObject(HexString.hexToBin(body, 0));
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "decodeObjectBody");
        return result;
    }

    // Decode a stream message
    private static JsJmsMessage decodeStreamBody(String body) throws MessageDecodeFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "decodeStreamBody");
        JsJmsStreamMessage result = new JsJmsStreamMessageImpl(MfpConstants.CONSTRUCTOR_NO_OP);
        if (body != null) {
            try {
                StringTokenizer st = new StringTokenizer(body, "&");
                while (st.hasMoreTokens())
                    result.writeObject(decodeObject(st.nextToken()));
            } catch (UnsupportedEncodingException e) {
                FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.WebJsJmsMessageFactoryImpl.decodeStreamBody", "196");
                // This can't happen, as the Exception can only be thrown for an MQ message
                // which isn't what we have.
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "decodeStreamBody");
        return result;
    }

    // Decode a map message
    private static JsJmsMessage decodeMapBody(String body) throws MessageDecodeFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "decodeMapBody");
        JsJmsMapMessage result = new JsJmsMapMessageImpl(MfpConstants.CONSTRUCTOR_NO_OP);
        if (body != null) {
            try {
                StringTokenizer st = new StringTokenizer(body, "&");
                while (st.hasMoreTokens()) {
                    Object[] pair = decodePair(st.nextToken());
                    result.setObject((String) pair[0], pair[1]);
                }
            } catch (UnsupportedEncodingException e) {
                FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.WebJsJmsMessageFactoryImpl.decodeMapBody", "218");
                // This can't happen, as the Exception can only be thrown for an MQ message
                // which isn't what we have.
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "decodeMapBody");
        return result;
    }

    // Decode and set the header fields
    private static void decodeHeader(JsJmsMessage msg, String id, String type, String topic, String props) {
        // id should be hexEncoded byte array but if it looks too short we prepend a '0' to
        // allow clients to simply use an integer if they want.
        if (id != null) {
            if (id.length() % 2 != 0)
                id = "0" + id;
            msg.setCorrelationIdAsBytes(HexString.hexToBin(id, 0));
        }

        // type goes in the JMSXAppId property.  A type of 'SIB' means the special case
        // compact form.
        if (type != null) {
            if (type.equals("SIB"))
                msg.setJmsxAppId(MfpConstants.WPM_JMSXAPPID);
            else
                msg.setJmsxAppId(URLDecode(type));
        }

        // Topic goes to discriminator
        if (topic != null)
            msg.setDiscriminator(URLDecode(topic));

        // And the properties
        if (props != null) {
            StringTokenizer st = new StringTokenizer(props, "&");
            while (st.hasMoreTokens()) {
                Object[] pair = decodePair(st.nextToken());
                // JMSXAppID has already been set from the header field
                if (!((String) pair[0]).equals(SIProperties.JMSXAppID)) {
                    try {
                        msg.setObjectProperty((String) pair[0], pair[1]);
                    } catch (Exception e) {
                        // No FFDC code needed
                        // No FFDC needed as setObjectProperty() can only throw a JMSException
                        // for a JMS_IBM_Report or JMS_IBM_Feedback property & we don't call
                        // it for any of those.
                    }
                }
            }
        }
    }

    // Decode a name=value pair
    private static Object[] decodePair(String text) {
        Object[] result = new Object[2];
        int i = text.indexOf('=');
        result[0] = URLDecode(text.substring(0, i));
        result[1] = decodeObject(text.substring(i + 1));
        return result;
    }

    // Decode an object as a string or byte array
    private static Object decodeObject(String text) {
        Object result = null;
        if (text.startsWith("[]"))
            result = HexString.hexToBin(text, 2);
        else
            result = URLDecode(text);
        return result;
    }

    // URL decode a string
    private static String URLDecode(String text) {
        String result = null;
        try {
            result = URLDecoder.decode(text, "UTF8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen - all JDKs must support UTF8
            FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.WebJsMessageFactoryImpl.URLDecode", "293");
        }
        return result;
    }
}
