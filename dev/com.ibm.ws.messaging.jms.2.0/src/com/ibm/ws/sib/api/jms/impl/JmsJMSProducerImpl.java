/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jms.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jms.CompletionListener;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.MessageFormatRuntimeException;
import javax.jms.MessageNotWriteableException;
import javax.jms.MessageNotWriteableRuntimeException;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsJMSProducer;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.HexString;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class internally uses messageProducer do get things done.This class maintains
 * has-a relationship with messageProducer.
 * <p>
 * jmsProducer does not have close method.Though we are still using messageProducer we
 * do not call close() on messageProducer.This is because the prod(producer session) is null and we are not adding
 * the messageProducer to the session list
 * <p>
 * All the send method implemented will use connection to send the message. The reason is message producer is created with null destiantion
 * which inturn does not set the prod(producer session)
 * <p>
 * Message properties may be may be specified using one or more of nine setProperty methods. Any message properties set using these methods
 * will override any message properties that have been set directly on the message.
 * <p>
 * 
 */
public class JmsJMSProducerImpl implements JmsJMSProducer {

    // ************************** TRACE INITIALISATION ***************************

    private static TraceComponent tc = SibTr.register(JmsJMSProducerImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);
    private static TraceNLS nls = TraceNLS.getTraceNLS(MfpConstants.MSG_BUNDLE);

    /* Message Producer Object */
    private final JmsMsgProducerImpl messageProducer;

    // Hashtable to store all the jmsproducer properties
    private HashMap<String, Object> jmsProducerProperties;

    /**
     * Specifies that messages sent using this JMSProducer will have their JMSCorrelationID header value set to the specified correlation ID, where correlation ID is specified as a
     * String.
     * This will override any JMSCorrelationID header value that is already set on the message being sent.
     */
    private String jmsCorrelationID;

    /* Flag to indicate if the jmsCorrelationID is set by the producer */
    private boolean jmsCorrelationIDSet = false;

    /**
     * Specifies that messages sent using this JMSProducer will have their JMSCorrelationID header value set to the specified correlation ID, where correlation ID is specified as
     * an array of bytes.
     * This will override any JMSCorrelationID header value that is already set on the message being sent.
     */
    private byte[] jmsCorrelationIDAsBytes;
    /* Flag to indicate if the jmsCorrelationIDAsBytes is set by the producer */
    private boolean jmsCorrelationIDAsBytesSet = false;

    /**
     * Specifies that messages sent using this JMSProducer will have their JMSReplyTo header value set to the specified Destination object.
     * This will override any JMSReplyTo header value that is already set on the message being sent.
     */
    private Destination jmsReplyTo;
    /* Flag to indicate if the replyTo is set by the producer */
    private boolean replyToSet = false;
    /**
     * Specifies that messages sent using this JMSProducer will have their JMSType header value set to the specified message type.
     * This will override any JMSType header value that is already set on the message being sent.
     */
    private String jmsType;
    /* Flag to indicate if the jmsType is set by the producer */
    private boolean jmsTypeSet = false;

    private final static String ID_STRING = "ID:";

    /**
     * holds the completion listner object set by user. This value can be changed dynamically
     */
    private volatile CompletionListener _completionListerner = null;

    public JmsJMSProducerImpl(MessageProducer messageProducer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsJMSProducerImpl", messageProducer);
        this.messageProducer = (JmsMsgProducerImpl) messageProducer;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsJMSProducerImpl");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#clearProperties()
     */
    @Override
    public JMSProducer clearProperties() throws JMSRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearProperties");
        if (jmsProducerProperties != null)
            jmsProducerProperties.clear();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearProperties");
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getAsync()
     */
    @Override
    public CompletionListener getAsync() throws JMSRuntimeException {
        return _completionListerner;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getBooleanProperty(java.lang.String)
     */
    @Override
    public boolean getBooleanProperty(String name) throws JMSRuntimeException, MessageFormatRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getBooleanProperty");

        Object obj = null;
        boolean value = false;

        try {
            obj = getObjByName(name);
            value = JmsMessageImpl.parseBoolean(obj, name);

        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getBooleanProperty", value);
        }
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getByteProperty(java.lang.String)
     */
    @Override
    public byte getByteProperty(String name) throws JMSRuntimeException, MessageFormatRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getByteProperty");

        Object obj = null;
        byte value = 0;

        try {
            obj = getObjByName(name);
            value = JmsMessageImpl.parseByte(obj, name);

        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getByteProperty", value);
        }
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getDeliveryDelay()
     */
    @Override
    public long getDeliveryDelay() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDeliveryDelay");
        long deliveryDelay = -1;
        try {
            deliveryDelay = messageProducer.getDeliveryDelay();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getDeliveryDelay", deliveryDelay);
        }
        return deliveryDelay;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getDeliveryMode()
     */
    @Override
    public int getDeliveryMode() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDeliveryMode");
        int deliveryMode = -1;
        try {
            deliveryMode = messageProducer.getDeliveryMode();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getDeliveryMode", deliveryMode);
        }
        return deliveryMode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getDisableMessageID()
     */
    @Override
    public boolean getDisableMessageID() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDisableMessageID");
        boolean disableMessageID = false;
        try {
            disableMessageID = messageProducer.getDisableMessageID();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getDisableMessageID", disableMessageID);
        }
        return disableMessageID;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getDisableMessageTimestamp()
     */
    @Override
    public boolean getDisableMessageTimestamp() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDisableMessageTimestamp");
        boolean disableMessageTimestamp = false;
        try {
            disableMessageTimestamp = messageProducer.getDisableMessageTimestamp();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getDisableMessageTimestamp", disableMessageTimestamp);
        }
        return disableMessageTimestamp;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getDoubleProperty(java.lang.String)
     */
    @Override
    public double getDoubleProperty(String name) throws JMSRuntimeException, MessageFormatRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDoubleProperty");

        Object obj = null;
        double value = 0;

        try {
            obj = getObjByName(name);
            value = JmsMessageImpl.parseDouble(obj, name);

        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getDoubleProperty", value);
        }
        return value;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getFloatProperty(java.lang.String)
     */
    @Override
    public float getFloatProperty(String name) throws JMSRuntimeException, MessageFormatRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getFloatProperty");

        Object obj = null;
        float value = 0;

        try {
            obj = getObjByName(name);
            value = JmsMessageImpl.parseFloat(obj, name);

        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getFloatProperty", value);
        }
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getIntProperty(java.lang.String)
     */
    @Override
    public int getIntProperty(String name) throws JMSRuntimeException, MessageFormatRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getIntProperty");

        Object obj = null;
        int value = 0;

        try {
            obj = getObjByName(name);
            value = JmsMessageImpl.parseInt(obj, name);

        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getIntProperty", value);
        }
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getJMSCorrelationID()
     */
    @Override
    public String getJMSCorrelationID() throws JMSRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSCorrelationID");

        String correl = null;

        /* If it is stored as a String, just extract it */
        if (jmsCorrelationID != null) {
            correl = jmsCorrelationID;
        } else if (jmsCorrelationIDAsBytes != null) { /* If it is stored as a binary, we have to convert it */

            /* It'll be more economical to get the length right immediately */
            StringBuffer sbuf = new StringBuffer((jmsCorrelationIDAsBytes.length * 2) + 3);

            /* Insert the ID: then add on the binary value as a hex string */
            sbuf.append(ID_STRING);
            HexString.binToHex(jmsCorrelationIDAsBytes, 0, jmsCorrelationIDAsBytes.length, sbuf);

            /* Return the String representation */
            correl = sbuf.toString();

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSCorrelationID", correl);
        return correl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getJMSCorrelationIDAsBytes()
     */
    @Override
    public byte[] getJMSCorrelationIDAsBytes() throws JMSRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSCorrelationIDAsBytes");

        byte[] value = null;

        /* If it is stored as a binary just extract it */
        if (jmsCorrelationIDAsBytes != null) {
            byte[] temp = jmsCorrelationIDAsBytes;
            value = new byte[temp.length];
            System.arraycopy(temp, 0, value, 0, temp.length);
        } else if (jmsCorrelationID != null) { /* If it is stored as a String, we have to extract and convert it */

            String strValue = jmsCorrelationID;

            /* If it is a String representation of binary, return the binary */
            if (strValue.startsWith(ID_STRING)) {
                value = HexString.hexToBin(strValue, 3);
            }
            /* Otherwise get the UTF8 byte encoding */
            else {
                try {
                    value = strValue.getBytes("UTF8");
                } catch (java.io.UnsupportedEncodingException e) {
                    /* No FFDC code needed */
                    /* Falling back to the default encoding seems to be acceptable to JMS */
                    value = strValue.getBytes();
                }
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSCorrelationIDAsBytes", jmsCorrelationIDAsBytes);
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getJMSReplyTo()
     */
    @Override
    public Destination getJMSReplyTo() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSReplyTo");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSReplyTo", jmsReplyTo);
        return jmsReplyTo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getJMSType()
     */
    @Override
    public String getJMSType() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSType");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSType", jmsType);
        return jmsType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getLongProperty(java.lang.String)
     */
    @Override
    public long getLongProperty(String name) throws JMSRuntimeException, MessageFormatRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getLongProperty");

        Object obj = null;
        long value = 0;

        try {
            obj = getObjByName(name);
            value = JmsMessageImpl.parseLong(obj, name);

        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getLongProperty", value);
        }
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getObjectProperty(java.lang.String)
     */
    @Override
    public Object getObjectProperty(String name) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getObjectProperty", name);

        Object obj = null;
        try {
            obj = getObjByName(name);
            // Special case for JMS_IBM_Character_Set.
            // If set as an integer, convert to String.
            if (name.equals(ApiJmsConstants.CHARSET_PROPERTY) && obj instanceof Integer) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "special case for charset, setting as string");
                obj = String.valueOf(obj);
            }
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getObjectProperty", obj);
        }
        return obj;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getPriority()
     */
    @Override
    public int getPriority() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPriority");

        int priority = -1;
        try {
            priority = messageProducer.getPriority();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getPriority", priority);
        }
        return priority;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getPropertyNames()
     */
    @Override
    public Set<String> getPropertyNames() throws JMSRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPropertyNames");

        Set<String> propNames = null;
        try {
            if (jmsProducerProperties != null) {
                // Make it unmodifiable according to the jms2.0 spec
                propNames = Collections.unmodifiableSet(jmsProducerProperties.keySet());
            }
        } catch (Exception e) {
            throw (JMSRuntimeException) JmsErrorUtils.newThrowable(
                                                                   JMSRuntimeException.class,
                                                                   "INTERNAL_ERROR_CWSIA0386",
                                                                   null,
                                                                   e,
                                                                   null, // null probeId = no FFDC
                                                                   this,
                                                                   tc
                            );
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getPropertyNames", propNames);
        }
        return propNames;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getShortProperty(java.lang.String)
     */
    @Override
    public short getShortProperty(String name) throws JMSRuntimeException, MessageFormatRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getShortProperty");

        Object obj = null;
        short value = 0;

        try {
            obj = getObjByName(name);
            value = JmsMessageImpl.parseShort(obj, name);

        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getShortProperty", value);
        }
        return value;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getStringProperty(java.lang.String)
     */
    @Override
    public String getStringProperty(String name) throws JMSRuntimeException, MessageFormatRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getStringProperty");

        Object obj = null;
        String value = null;

        try {
            obj = getObjByName(name);

            if ((obj instanceof String) || (obj == null)) {
                value = (String) obj;
            }
            else {
                value = obj.toString();
            }

        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getStringProperty", value);
        }
        return value;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#getTimeToLive()
     */
    @Override
    public long getTimeToLive() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTimeToLive");
        long ttl = -1;
        try {
            ttl = messageProducer.getTimeToLive();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getTimeToLive");
        }
        return ttl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#propertyExists(java.lang.String)
     */
    @Override
    public boolean propertyExists(String name) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "propertyExists", name);

        boolean exists = false;
        try {
            if (jmsProducerProperties != null)
                exists = jmsProducerProperties.containsKey(name);

        } catch (Exception e) {
            throw (JMSRuntimeException) JmsErrorUtils.newThrowable(
                                                                   JMSRuntimeException.class,
                                                                   "INTERNAL_ERROR_CWSIA0386",
                                                                   null,
                                                                   e,
                                                                   null, // null probeId = no FFDC
                                                                   this,
                                                                   tc
                            );
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "propertyExists", exists);
        }

        return exists;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#send(javax.jms.Destination, byte[])
     */
    @Override
    public JMSProducer send(Destination dest, byte[] body) throws MessageFormatRuntimeException, InvalidDestinationRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send", new Object[] { dest, body });
        try {
            // Create a byte message
            JmsBytesMessageImpl msg = new JmsBytesMessageImpl();
            // fill the message body
            if (body != null)
                msg.writeBytes(body);
            inheritProducerProperties(msg);
            messageProducer.send_internal(dest, msg, _completionListerner);
        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "send", new Object[] { this });
        }

        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#send(javax.jms.Destination, java.util.Map)
     */
    @Override
    public JMSProducer send(Destination dest, Map<String, Object> body) throws MessageFormatRuntimeException, InvalidDestinationRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send", new Object[] { dest, body });
        try {
            // Create a map message with empty body and then iterate through the Map to fill in the message body
            JmsMapMessageImpl msg = new JmsMapMessageImpl();
            if (body != null) {
                Iterator<Entry<String, Object>> it = body.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, Object> entry = it.next();
                    msg.setObject(entry.getKey(), entry.getValue());
                }
            }
            inheritProducerProperties(msg);
            messageProducer.send_internal(dest, msg, _completionListerner);
        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "send", new Object[] { this });
        }

        return this;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#send(javax.jms.Destination, javax.jms.Message)
     */
    @Override
    public JMSProducer send(Destination dest, Message msg) throws MessageFormatRuntimeException, InvalidDestinationRuntimeException, MessageNotWriteableRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send", new Object[] { dest, msg });

        try {
            //Inherit non-conflicting producer properties
            inheritProducerProperties(msg);
            messageProducer.send_internal(dest, msg, _completionListerner);
        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (MessageNotWriteableException mnwe) {
            throw (MessageNotWriteableRuntimeException) JmsErrorUtils.getJMS2Exception(mnwe, MessageNotWriteableRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "send", new Object[] { this });
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#send(javax.jms.Destination, java.io.Serializable)
     */
    @Override
    public JMSProducer send(Destination dest, Serializable body) throws MessageFormatRuntimeException, InvalidDestinationRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send", new Object[] { dest, body });
        try {
            // Create a Object message

            JmsObjectMessageImpl msg = null;
            if (body != null)
                msg = new JmsObjectMessageImpl(body);
            else
                msg = new JmsObjectMessageImpl();

            inheritProducerProperties(msg);
            messageProducer.send_internal(dest, msg, _completionListerner);
        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "send", new Object[] { this });
        }

        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#send(javax.jms.Destination, java.lang.String)
     */
    @Override
    public JMSProducer send(Destination dest, String body) throws MessageFormatRuntimeException, InvalidDestinationRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send", new Object[] { dest, body });
        try {
            // Create a text message
            TextMessage msg = null;
            if (body != null)
                msg = new JmsTextMessageImpl(body);
            else
                msg = new JmsTextMessageImpl();

            inheritProducerProperties(msg);
            messageProducer.send_internal(dest, msg, _completionListerner);
        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "send", new Object[] { this });
        }
        return this;
    }

    /**
     * Sets all the message properties of this producer to the Message.Producer properties will have higher precedence over the properties set over Message
     * <p>
     * 
     * Headers of JMS producer {jmsCorrelationID, jmsCorrelationIDAsBytes, replyTo, jmsType} if set will override any
     * value that is already set on the message being sent.
     * <p>
     * 
     * @param msg The Message which has to be inherit the properties of producer
     * @throws JMSException
     */
    private void inheritProducerProperties(Message msg) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "inheritProducerProperties", new Object[] { msg });

        if (msg != null) {
            // Set the headers of JMS producer to the message if set by the producer. 
            // Headers set in producer has precedence over the message headers 
            if (jmsCorrelationIDSet)
                msg.setJMSCorrelationID(jmsCorrelationID);
            if (jmsCorrelationIDAsBytesSet)
                msg.setJMSCorrelationIDAsBytes(jmsCorrelationIDAsBytes);
            if (replyToSet)
                msg.setJMSReplyTo(jmsReplyTo);
            if (jmsTypeSet)
                msg.setJMSType(jmsType);

            // Set all the producer properties to the message and conflicting properties will be overwritten in the Message 
            if (jmsProducerProperties != null) { // if producerProperties is  null then nothing for message to inherit

                Set<String> producerProperties = jmsProducerProperties.keySet();
                Iterator<String> it = producerProperties.iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    msg.setObjectProperty(key, jmsProducerProperties.get(key));
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "inheritProducerProperties", new Object[] { msg });

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setAsync(javax.jms.CompletionListener)
     */
    @Override
    public JMSProducer setAsync(CompletionListener cListerner) throws JMSRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setAsync", cListerner);

        if (messageProducer.isManaged()) {
            JMSException jmse = (javax.jms.JMSException) JmsErrorUtils.newThrowable(javax.jms.JMSException.class,
                                                                                    "MGD_ENV_CWSIA0084",
                                                                                    new Object[] { "JMSProducer.setAsync" },
                                                                                    tc);
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        }

        //set completion lister which is set by user. Subsequent send calls use this listener to decide
        //whether the send call is asynchronous or synchronous
        _completionListerner = cListerner;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setAsync");

        return this;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setDeliveryDelay(long)
     */
    @Override
    public JMSProducer setDeliveryDelay(long value) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDeliveryDelay", value);

        try {
            messageProducer.setDeliveryDelay(value);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setDeliveryDelay", new Object[] { this });
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setDeliveryMode(int)
     */
    @Override
    public JMSProducer setDeliveryMode(int deliveryMode) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDeliveryMode", deliveryMode);
        try {
            messageProducer.setDeliveryMode(deliveryMode);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setDeliveryMode", new Object[] { this });
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setDisableMessageID(boolean)
     */
    @Override
    public JMSProducer setDisableMessageID(boolean value) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDisableMessageID", value);

        try {
            messageProducer.setDisableMessageID(value);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setDisableMessageID", new Object[] { this });
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setDisableMessageTimestamp(boolean)
     */
    @Override
    public JMSProducer setDisableMessageTimestamp(boolean value) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDisableMessageTimestamp", value);

        try {
            messageProducer.setDisableMessageTimestamp(value);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setDisableMessageTimestamp", new Object[] { this });
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setJMSCorrelationID(java.lang.String)
     */
    @Override
    public JMSProducer setJMSCorrelationID(String value) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSCorrelationID", value);

        try {
            /* If the String starts with ID_STRING we have to check it is valid, and */
            /* convert it to lower-case before storing it. */
            if ((value != null) && (value.startsWith(ID_STRING))) {
                boolean needsConvert = false;
                int length = value.length();
                char ch;

                /* Including the ID: it must have an odd number of characters */
                if ((length % 2) == 0) {
                    String nlsMsg = nls.getFormattedMessage("BAD_HEX_STRING_CWSIF0191"
                                                            , new Object[] { value }
                                                            , "The hexadecimal string " + value + " is incorrectly formatted for a Correlation Id"
                                    );

                    JMSException e = new JMSException(nlsMsg);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(this, tc, "setCorrelationId", e);
                    throw e;
                }

                /* Check each character is valid and for upper-case */
                for (int i = 3; i < length; i++) {
                    ch = value.charAt(i);
                    if (Character.digit(ch, 16) < 0) {
                        String nlsMsg = nls.getFormattedMessage("BAD_HEX_STRING_CWSIF0191"
                                                                , new Object[] { value }
                                                                , "The hexadecimal string " + value + " is incorrectly formatted for a Correlation Id"
                                        );
                        JMSException e = new JMSException(nlsMsg);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(this, tc, "setCorrelationId", e);
                        throw e;
                    }
                    if (Character.isUpperCase(ch))
                        needsConvert = true;
                }

                /* Convert if necessary */
                if (needsConvert) {
                    value = ID_STRING + value.substring(3).toLowerCase();
                }

            }
            /* If the value is null its OK because when we do setJMSCorelationID on Message during send it will be taken care */
            jmsCorrelationID = value;
            jmsCorrelationIDSet = true;

        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setJMSCorrelationID");
        }

        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setJMSCorrelationIDAsBytes(byte[])
     */
    @Override
    public JMSProducer setJMSCorrelationIDAsBytes(byte[] value) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCorrelationIdAsBytes", value);
        try {
            if (value != null) {
                /* Set the binary variant to the given value */
                byte[] copy = new byte[value.length];
                System.arraycopy(value, 0, copy, 0, value.length);
                jmsCorrelationIDAsBytes = copy;
            }
            else {
                /* If the value is null its OK because when we do setJMSCorelationID on Message during send it will be taken care */
                jmsCorrelationIDAsBytes = null;
            }
            jmsCorrelationIDAsBytesSet = true;
        } catch (Exception e) {
            throw (JMSRuntimeException) JmsErrorUtils.newThrowable(
                                                                   JMSRuntimeException.class,
                                                                   "INTERNAL_ERROR_CWSIA0386",
                                                                   null,
                                                                   e,
                                                                   null, // null probeId = no FFDC
                                                                   this,
                                                                   tc
                            );
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setCorrelationIdAsBytes");
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setJMSReplyTo(javax.jms.Destination)
     */
    @Override
    public JMSProducer setJMSReplyTo(Destination destination) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSReplyTo", destination);
        try {
            if (destination instanceof JmsDestinationImpl) {
                jmsReplyTo = destination;
            } else if (destination == null) {
                /* If the value is null its OK because when we do setJMSReplyTo on Message during send it will be taken care */
                jmsReplyTo = null;
            } else {

                // The given destination is a foreign (non-jetstream) jms destination,
                // we can't handle this, so throw an exception
                // FFDC handled by JmsErrorUtils. no FFDC required.
                throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class,
                                                                               "INVALID_VALUE_CWSIA0116",
                                                                               new Object[] { "JMSReplyTo", destination.getClass().toString() },
                                                                               null,
                                                                               "JmsMessageImpl.setJMSReplyTo#1",
                                                                               this,
                                                                               tc
                                );

            }

            replyToSet = true;

        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setJMSReplyTo");
        }

        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setJMSType(java.lang.String)
     */
    @Override
    public JMSProducer setJMSType(String jmsType) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSType", new Object[] { jmsType });

        this.jmsType = jmsType;
        jmsTypeSet = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJMSType");
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setPriority(int)
     */
    @Override
    public JMSProducer setPriority(int priority) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setPriority", new Object[] { priority });
        try {
            messageProducer.setPriority(priority);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setPriority");
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setProperty(java.lang.String, boolean)
     */
    @Override
    public JMSProducer setProperty(String name, boolean value) throws JMSRuntimeException, IllegalArgumentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProperty", new Object[] { name, value });

        try {
            JmsMessageImpl.checkPropName(name, "setProperty");
            JmsMessageImpl.checkSettablePropertyNameAndType(name, Boolean.class);

            if (jmsProducerProperties == null)
                jmsProducerProperties = new HashMap<String, Object>();

            jmsProducerProperties.put(name, Boolean.valueOf(value));

        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setProperty");
        }

        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setProperty(java.lang.String, byte)
     */
    @Override
    public JMSProducer setProperty(String name, byte value) throws JMSRuntimeException, IllegalArgumentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProperty", new Object[] { name, value });

        try {
            JmsMessageImpl.checkPropName(name, "setProperty");
            JmsMessageImpl.checkSettablePropertyNameAndType(name, Byte.class);

            if (jmsProducerProperties == null)
                jmsProducerProperties = new HashMap<String, Object>();

            jmsProducerProperties.put(name, Byte.valueOf(value));

        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setProperty");
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setProperty(java.lang.String, double)
     */
    @Override
    public JMSProducer setProperty(String name, double value) throws JMSRuntimeException, IllegalArgumentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProperty", new Object[] { name, value });

        try {
            JmsMessageImpl.checkPropName(name, "setProperty");
            JmsMessageImpl.checkSettablePropertyNameAndType(name, Double.class);

            if (jmsProducerProperties == null)
                jmsProducerProperties = new HashMap<String, Object>();
            jmsProducerProperties.put(name, Double.valueOf(value));
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setProperty");
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setProperty(java.lang.String, float)
     */
    @Override
    public JMSProducer setProperty(String name, float value) throws JMSRuntimeException, IllegalArgumentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProperty", new Object[] { name, value });

        try {
            JmsMessageImpl.checkPropName(name, "setProperty");
            JmsMessageImpl.checkSettablePropertyNameAndType(name, Float.class);

            if (jmsProducerProperties == null)
                jmsProducerProperties = new HashMap<String, Object>();
            jmsProducerProperties.put(name, Float.valueOf(value));
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setProperty");
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setProperty(java.lang.String, int)
     */
    @Override
    public JMSProducer setProperty(String name, int value) throws JMSRuntimeException, IllegalArgumentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProperty", new Object[] { name, value });

        try {
            JmsMessageImpl.checkPropName(name, "setProperty");
            JmsMessageImpl.checkSettablePropertyNameAndType(name, Integer.class);

            if (jmsProducerProperties == null)
                jmsProducerProperties = new HashMap<String, Object>();
            jmsProducerProperties.put(name, Integer.valueOf(value));
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setProperty");
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setProperty(java.lang.String, long)
     */
    @Override
    public JMSProducer setProperty(String name, long value) throws JMSRuntimeException, IllegalArgumentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProperty", new Object[] { name, value });

        try {
            JmsMessageImpl.checkPropName(name, "setProperty");
            JmsMessageImpl.checkSettablePropertyNameAndType(name, Long.class);

            if (jmsProducerProperties == null)
                jmsProducerProperties = new HashMap<String, Object>();
            jmsProducerProperties.put(name, Long.valueOf(value));
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setProperty");
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setProperty(java.lang.String, java.lang.Object)
     */
    @Override
    public JMSProducer setProperty(String name, Object value) throws JMSRuntimeException, IllegalArgumentException, MessageFormatRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProperty", new Object[] { name, value });

        try {
            JmsMessageImpl.checkPropName(name, "setProperty");
            Class type = null;
            if (value != null) {
                type = value.getClass();
            }
            JmsMessageImpl.checkSettablePropertyNameAndType(name, type);

            // check that the object is of acceptable type
            if (!((value == null)
                  || (value instanceof String)
                  || (value instanceof Number)
                  || (value instanceof Boolean)
                  || (value instanceof Character)
                  || (value instanceof byte[]))) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "value is bad type: " + value.getClass().getName());
                // d238447 FFDC review. Don't generate FFDC for this case.
                throw (MessageFormatException) JmsErrorUtils.newThrowable(
                                                                          MessageFormatException.class,
                                                                          "BAD_OBJECT_CWSIA0188",
                                                                          new Object[] { value.getClass().getName() },
                                                                          tc);
            }

            if (value instanceof byte[]) {
                // take a copy so that the application can change the original without affecting the message.
                byte[] v = (byte[]) value;
                byte[] tmp = new byte[v.length];
                System.arraycopy(v, 0, tmp, 0, v.length);
                value = tmp;
            }

            if (jmsProducerProperties == null)
                jmsProducerProperties = new HashMap<String, Object>();
            jmsProducerProperties.put(name, value);
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (MessageFormatException jmse) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, MessageFormatRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setProperty");
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setProperty(java.lang.String, short)
     */
    @Override
    public JMSProducer setProperty(String name, short value) throws JMSRuntimeException, IllegalArgumentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProperty", new Object[] { name, value });

        try {
            JmsMessageImpl.checkPropName(name, "setProperty");
            JmsMessageImpl.checkSettablePropertyNameAndType(name, Short.class);

            if (jmsProducerProperties == null)
                jmsProducerProperties = new HashMap<String, Object>();
            jmsProducerProperties.put(name, Short.valueOf(value));
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setProperty");
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setProperty(java.lang.String, java.lang.String)
     */
    @Override
    public JMSProducer setProperty(String name, String value) throws JMSRuntimeException, IllegalArgumentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProperty", new Object[] { name, value });

        try {
            JmsMessageImpl.checkPropName(name, "setProperty");
            JmsMessageImpl.checkSettablePropertyNameAndType(name, String.class);

            if (jmsProducerProperties == null)
                jmsProducerProperties = new HashMap<String, Object>();
            jmsProducerProperties.put(name, String.valueOf(value));
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setProperty");
        }
        return this;
    }

    /**
     * Get a property from the underlying jmsProducerProperties
     * 
     * @param name name of the object to retrieve
     * @throws JMSException
     */
    private Object getObjByName(String name) throws JMSException {
        Object obj = null;

        if (name == null) {
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "INVALID_PROPNAME_CWSIA0112",
                                                            new Object[] { null },
                                                            tc);
        }

        if ("".equals(name)) {
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "INVALID_PROPNAME_CWSIA0112",
                                                            new Object[] { "\"\"" },
                                                            tc);
        }

        if ((jmsProducerProperties != null) && jmsProducerProperties.containsKey(name)) {
            obj = jmsProducerProperties.get(name);
        }

        return obj;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSProducer#setTimeToLive(long)
     */
    @Override
    public JMSProducer setTimeToLive(long ttl) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTimeToLive", new Object[] { ttl });

        try {
            messageProducer.setTimeToLive(ttl);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setTimeToLive");
        }
        return this;
    }
}
