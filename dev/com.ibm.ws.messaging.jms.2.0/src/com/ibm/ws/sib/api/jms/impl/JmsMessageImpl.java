/*******************************************************************************
 * Copyright (c) 2012, 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jms.impl;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.BytesMessage;
import javax.jms.ConnectionMetaData;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsFactoryFactory;
import com.ibm.ws.sib.api.jms.EncodingLevel;
import com.ibm.ws.sib.api.jms.JmsInternalConstants;
import com.ibm.ws.sib.api.jms.JmsInternalsFactory;
import com.ibm.ws.sib.api.jms.service.JmsServiceFacade;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.mfp.JmsBodyType;
import com.ibm.ws.sib.mfp.JsJmsBytesMessage;
import com.ibm.ws.sib.mfp.JsJmsMapMessage;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.JsJmsMessageFactory;
import com.ibm.ws.sib.mfp.JsJmsObjectMessage;
import com.ibm.ws.sib.mfp.JsJmsStreamMessage;
import com.ibm.ws.sib.mfp.JsJmsTextMessage;
import com.ibm.ws.sib.mfp.PersistenceType;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;

/**
 * Implementation of the JMS Message class for Prototype
 * 
 * 
 * @author matrober
 * 
 */
public class JmsMessageImpl implements Message, JmsInternalConstants, Serializable
{
    private static TraceComponent tc = SibTr.register(JmsMessageImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    /**
     * This svUID assigned at version 1.95 of this class.
     */
    private static final long serialVersionUID = 7645122120151618834L;

    // ********************* PRIVATE STATE ******************

    /**
     * Instances of JMS Destination objects (Queues or Topics), that we cache
     * once we have created, since there is no point creating them from scratch
     * more than once. However there is no need to serialize this information as
     * we can recreate them from the MFP message object if necessary.
     */
    private transient Destination dest = null;
    private transient Destination replyTo = null;

    /**
     * This variable holds the actual state for this Message.
     * Note: Do not initialise this to null here, otherwise it will overwrite
     * the setup done by instantiateMessage!
     */
    private JsJmsMessage msg;

    /**
     * This object is driven when the user calls acknowledge(). The variable may
     * be set to null if this is not a consumed message (ie it outbound or
     * retrieved from a QueueBrowser), which results in acknowledge throwing an
     * IllegalStateException.
     */
    private transient JmsSessionImpl theSession = null;

    /**
     * A static reference to the MFP factory object which can be used by all
     * subclasses of this object. It is initialized the first time that the
     * JmsMessageImpl() constructor is called.
     */
    static JsJmsMessageFactory jmfact = null;

    /**
     * A static reference to the MFP factory object for creating destination address
     * references.
     */
    static SIDestinationAddressFactory destAddressFactory = null;

    /**
     * Variables to indicate if the message body and properties are read-only.
     * This occurs after a message has been received. The body and properties
     * subsequently become writeable again if clearBody() or clearProperties()
     * are called.
     */
    private boolean bodyReadOnly = false;
    private boolean propertiesReadOnly = false;

    /** JMS_IBM properties */
    private static Hashtable<String, Class> JMS_IBM_props = null;

    /**
     * The set of properties that require special treatment so that set
     * followed by get behaves correctly.
     * These properties are normally populated by 'smoke and mirrors' in
     * the MFP layer, and so would not return the set value unless we
     * take special action.
     */
    private static Set<String> localStorePropertyNames = null;

    /**
     * Hashtable of local values for properties requiring special handling.
     * The set of properties is defined by the contents of
     * localStorePropertyNames.
     */
    private Hashtable locallyStoredPropertyValues = null;

    /**
     * The JMSMessageID has a similar behaviour to the locally stored properties
     * above, but because it has specific get/set methods it is easier to use
     * a specific variable to store any locally held value.
     */
    private String localJMSMessageID = null;

    /**
     * This variable holds a cache of the message toString at the Message level.
     * A separate cache holds the subclass information. The cache is cleared
     * by changing any property of the message.
     */
    private transient String cachedToString = null;

    /**
     * This value is initialized by the message subclasses, and is used for the
     * first line of the toString.
     */
    protected String messageClass = null;

    /**
     * initialised from the constructor to reference the lock held in the session
     */
    private transient Object sessionSyncLock = null;

    /**
     * set true when setJMSReplyTo is called
     */
    private boolean rrpBusNameNeedsUpdating = false;

    /**
     * Indicates if producer has promised not to modify the payload
     */
    protected boolean producerWontModifyPayloadAfterSet = false;

    /**
     * Indicates if consumer has promised not to modify the payload
     */
    protected boolean consumerWontModifyPayloadAfterGet = false;

    /**
     * indicates if the Message is being used in AsyncSend. In this case, the headers
     * can not be changed.Default access as this would be accessed
     * directly by MessageProxyInvocationHandler
     */
    private transient volatile boolean inusebyAsyncSend = false;

    // *************************** STATIC INITIALIZATION **************************
    static {

        // setup the list of known JMS_IBM properties
        JMS_IBM_props = new Hashtable<String, Class>(30);
        JMS_IBM_props.put(ApiJmsConstants.FORMAT_PROPERTY, String.class);
        JMS_IBM_props.put(ApiJmsConstants.MSG_TYPE_PROPERTY, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.FEEDBACK_PROPERTY, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.PUT_APPL_TYPE_PROPERTY, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.REPORT_EXCEPTION_PROPERTY, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.REPORT_EXPIRATION_PROPERTY, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.REPORT_COA_PROPERTY, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.REPORT_COD_PROPERTY, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.REPORT_PAN_PROPERTY, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.REPORT_NAN_PROPERTY, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.REPORT_MSGID_PROPERTY, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.REPORT_CORRELID_PROPERTY, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.REPORT_DISCARD_PROPERTY, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.ENCODING_PROPERTY, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.CHARSET_PROPERTY, String.class);
        JMS_IBM_props.put(ApiJmsConstants.LAST_MSG_IN_GROUP_PROPERTY, Boolean.class);
        JMS_IBM_props.put(ApiJmsConstants.PUT_DATE_PROPERTY, String.class);
        JMS_IBM_props.put(ApiJmsConstants.PUT_TIME_PROPERTY, String.class);
        JMS_IBM_props.put(ApiJmsConstants.EXCEPTION_REASON, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.EXCEPTION_TIMESTAMP, Long.class);
        JMS_IBM_props.put(ApiJmsConstants.EXCEPTION_MESSAGE, String.class);
        JMS_IBM_props.put(ApiJmsConstants.EXCEPTION_PROBLEM_DESTINATION, String.class);
        JMS_IBM_props.put(ApiJmsConstants.EXCEPTION_PROBLEM_SUBSCRIPTION, String.class);
        JMS_IBM_props.put(ApiJmsConstants.SYSTEM_MESSAGEID_PROPERTY, String.class);

        // Add the ARM correlator properties. Technical the second one isn't an IBM
        // property, but we want to support it anyway so the most effective place to
        // define it is still here.
        JMS_IBM_props.put(ApiJmsConstants.IBM_ARM_CORRELATOR, String.class);
        JMS_IBM_props.put(ApiJmsConstants.TOG_ARM_CORRELATOR, String.class);

        // Add the JMS_IBM_MQMD_ properties for SIB0111c.
        JMS_IBM_props.put(ApiJmsConstants.JMS_IBM_MQMD_MSGID, byte[].class);
        JMS_IBM_props.put(ApiJmsConstants.JMS_IBM_MQMD_CORRELID, byte[].class);
        JMS_IBM_props.put(ApiJmsConstants.JMS_IBM_MQMD_PERSISTENCE, Integer.class);
        JMS_IBM_props.put(ApiJmsConstants.JMS_IBM_MQMD_REPLYTOQ, String.class);
        JMS_IBM_props.put(ApiJmsConstants.JMS_IBM_MQMD_REPLYTOQMGR, String.class);

        // NB - when adding new JMS_IBM properties be sure to update the initial size of
        //      the hashmap above to provide optimum initial memory usage.

        // setup the set of properties which require special handling for local values
        localStorePropertyNames = new HashSet<String>();
        localStorePropertyNames.add(ApiJmsConstants.EXCEPTION_REASON);
        localStorePropertyNames.add(ApiJmsConstants.EXCEPTION_TIMESTAMP);
        localStorePropertyNames.add(ApiJmsConstants.EXCEPTION_MESSAGE);
        localStorePropertyNames.add(ApiJmsConstants.JMSX_DELIVERY_COUNT);
        localStorePropertyNames.add(ApiJmsConstants.SYSTEM_MESSAGEID_PROPERTY);
        localStorePropertyNames.add(ApiJmsConstants.EXCEPTION_PROBLEM_DESTINATION);
        localStorePropertyNames.add(ApiJmsConstants.EXCEPTION_PROBLEM_SUBSCRIPTION);

        try {
            obtainMFPFactory();
        } catch (JMSException e) {
            // No FFDC code needed
            throw new RuntimeException(e);
        }
    }

    // *********************** CONSTRUCTORS *****************

    /**
     * This constructor is used to create a new message object (compare to the
     * other constructor which is used for the inbound message path).
     */
    public JmsMessageImpl() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>");

        JsJmsMessage newMsg = null;
        try {
            // Create and store the new message object - note that subclasses override
            // the instantiateMessage method to create the message subtype of their choice.
            newMsg = instantiateMessage();
        } catch (Exception e) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Exception creating message", e);

            // The instantiateMessage() methods don't ffdc the exception, so we do it here.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class
                                                            , "MSG_CREATE_FAILED_CWSIA0111"
                                                            , null
                                                            , e
                                                            , "JmsMessageImpl.<init>#2"
                                                            , this
                                                            , tc
                            );

        }

        setMsgReference(newMsg);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * d339880
     * This constructor is used to create a new message object
     * from an existing JsJmsMessage object
     * e.g. used by MQRequestReplyUtils.updateJmsDestinationProperty
     */
    public JmsMessageImpl(JsJmsMessage jsMsg) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", jsMsg);

        setMsgReference(jsMsg);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * This is used by the inbound factory method to create a vanilla JMS
     * Message object.
     * 
     * @see #inboundJmsInstance(JsJmsMessage, JmsSessionImpl)
     */
    JmsMessageImpl(JsJmsMessage newMsg, JmsSessionImpl newSess) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[] { newMsg, newSess });

        setMsgReference(newMsg);
        theSession = newSess;
        if (newSess != null)
            sessionSyncLock = newSess.getSessionSyncLock();
        messageClass = CLASS_NONE;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Construct a jetstream jms message from a (possibly non-jetstream)
     * vanilla jms message.
     * NB This routine is being modified to support sending foreign messages.
     * Other usages may no longer be appropriate.
     */
    JmsMessageImpl(Message message) throws JMSException {

        this();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", message);

        // copy headers that can be set by client
        setJMSCorrelationID(message.getJMSCorrelationID());
        setJMSReplyTo(message.getJMSReplyTo());
        setJMSType(message.getJMSType());

        // copy properties.
        Enumeration propertyNames = message.getPropertyNames();
        while (propertyNames.hasMoreElements()) {
            String name = (String) propertyNames.nextElement();
            Object value = message.getObjectProperty(name);
            // this might fail if it contains a reserved or invalid name, or a bad value,
            // so catch and ignore any exceptions
            try {
                setObjectProperty(name, value);
            } catch (Exception e) {
                // No FFDC code needed
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    // ***************** INTERFACE METHODS ******************

    /**
     * @see javax.jms.Message#getJMSMessageID()
     */
    @Override
    public String getJMSMessageID() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSMessageID");

        String id = null;
        if (localJMSMessageID == null) {
            id = msg.getApiMessageId();
        }
        else {
            id = localJMSMessageID;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSMessageID", id);
        return id;
    }

    /**
     * @see javax.jms.Message#setJMSMessageID(String)
     * 
     *      Note that this method is used to change the MessageID once the
     *      message has been received - it cannot be used to affect the assignment
     *      of the message ID, which is supplied by the provider on send.
     * 
     *      d255144 Method changed to set a local variable instead of calling
     *      down into the js message. Validation of the parameter removed.
     * 
     */
    @Override
    public void setJMSMessageID(String newMsgId) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSMessageID", newMsgId);

        localJMSMessageID = newMsgId;
        // Invalidate the toString cache
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJMSMessageID");
    }

    /**
     * @see javax.jms.Message#getJMSTimestamp()
     */
    @Override
    public long getJMSTimestamp() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSTimestamp");

        Long ts = msg.getTimestamp();

        long val = 0;
        if (ts != null) {
            val = ts.longValue();
        }
        else {
            val = 0;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSTimestamp", val);
        return val;
    }

    /**
     * @see javax.jms.Message#setJMSTimestamp(long)
     */
    @Override
    public void setJMSTimestamp(long time) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSTimestamp", time);

        msg.setTimestamp(time);
        // Invalidate the toString cache
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJMSTimestamp");
    }

    /**
     * Return the JMSCorrelationID as an array of bytes.
     * If the correlationID was set using the corresponding
     * setJMSCorrelationIDAsBytes method, then the return value is
     * simply a reference to the array that was created at that time
     * (a copy is taken of the original during the set method). If
     * the correlationID was set using the String form of setJMSCorrelationID
     * then the behaviour is slightly more complex. If the String starts with
     * "ID:" then we attempt to parse the string as a hex character sequence. If
     * the string isn't a valid hex format string then a JMSException will be thrown.
     * If the string doesn't begin with ID: then we return the byte representation
     * of the String in UTF8 encoding.<br>
     * The String -> byte[] conversion is cached to avoid repeat conversion.
     * 
     * @see javax.jms.Message#getJMSCorrelationIDAsBytes()
     */
    @Override
    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSCorrelationIDAsBytes");

        byte[] result;

        try {
            result = msg.getCorrelationIdAsBytes();
        } catch (IllegalArgumentException iae) {
            // No FFDC code needed as JmsErrorUtils will perform one

            // d222492 review.
            // No documentation for why getCorrelationIdAsBytes might throw an IAE,
            // presumably this happens when the correlID was set as a string starting ID:
            // but followed by an illegal character sequence. If so, we probably need
            // a better message.
            // On investigation - badly formed ID: strings are trapped at set time, so
            // it should be hard to get to this code path. Default exception ok.
            // d238447 FFDC review. From the above, "hard to get" sounds like a justification for FFDC.

            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0115",
                                                            new Object[] { iae, "JmsMessageImpl.getCorrelationIdAsBytes" },
                                                            iae,
                                                            "JmsMessageImpl.getJMSCorrelationIDAsBytes#1",
                                                            this,
                                                            tc
                            );

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSCorrelationIDAsBytes", result);
        return result;
    }

    /**
     * Convert the byte[] into an ID:&lt;hex string&gt; and set into
     * the JS msg.
     * 
     * @see javax.jms.Message#setJMSCorrelationIDAsBytes(byte[])
     */
    @Override
    public void setJMSCorrelationIDAsBytes(byte[] cidB) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSCorrelationIDAsBytes", cidB);

        try {
            // and set this into the JS msg
            msg.setCorrelationIdAsBytes(cidB);
            // Invalidate the toString cache
            cachedToString = null;
        } catch (IllegalArgumentException iae) {
            // No FFDC code needed
            // d238447 FFDC review. App' error, no FFDC required.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "INVALID_VALUE_CWSIA0116",
                                                            new Object[] { "JMSCorrelationID", cidB },
                                                            iae,
                                                            null, // null ProbeId = no FFDC
                                                            this,
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJMSCorrelationIDAsBytes");
    }

    /**
     * @see javax.jms.Message#setJMSCorrelationID(String)
     */
    @Override
    public void setJMSCorrelationID(String correl) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSCorrelationID", correl);

        try {
            msg.setCorrelationId(correl);
            // Invalidate the toString cache
            cachedToString = null;
        } catch (IllegalArgumentException iae) {
            // No FFDC code needed
            // d238447 FFDC review. App' error, no FFDC required.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "INVALID_VALUE_CWSIA0116",
                                                            new Object[] { "JMSCorrelationID", correl },
                                                            iae,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJMSCorrelationID");
    }

    /**
     * @see javax.jms.Message#getJMSCorrelationID()
     */
    @Override
    public String getJMSCorrelationID() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSCorrelationID");

        String correl = msg.getCorrelationId();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJMSCorrelationID", correl);
        return correl;
    }

    /**
     * @see javax.jms.Message#getJMSReplyTo()
     * 
     *      d246604 Review error logic.
     *      This method uses 3 mechanisms to determine the type of the replyTo destination:
     *      a) The JMS specific replyURIBytes
     *      b) using coreConnection.getDestinationConfiguration()
     *      c) Guessing based on the presence/absence of a discriminator
     * 
     *      Prior to 246604, errors in b) are returned to the application. This will be changed so that
     *      a failure in codepath b) causes fallback to c).
     * 
     */
    @Override
    public Destination getJMSReplyTo() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSReplyTo");

        // If we have not cached the replyTo destination.
        if (replyTo == null) {
            List<SIDestinationAddress> rrp = msg.getReverseRoutingPath();

            SICoreConnection siConn = null;
            if (theSession != null)
                siConn = theSession.getCoreConnection();

            // Use this utility method to obtain the full representation & store in the cache
            replyTo = JmsDestinationImpl.getJMSReplyToInternal(msg, rrp, siConn);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSReplyTo", replyTo);
        return replyTo;
    }

    /**
     * @see javax.jms.Message#setJMSReplyTo(Destination)
     */
    @Override
    public void setJMSReplyTo(Destination destination) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSReplyTo", destination);

        // The given destination is a native (jetstream) jms destination,
        // so set js reply destination, and jms reply destination (in encoded form).
        if (destination instanceof JmsDestinationImpl) {

            replyTo = destination;
            // This method takes the destination supplied and uses it to set the reply header
            // properties in the core message object.
            setReplyHeader((JmsDestinationImpl) replyTo);
            msg.setJmsReplyTo(((JmsDestinationImpl) destination).encodeToBytes(EncodingLevel.MINIMAL));
            // d317373 the msg type must be a request now we have a replyTo
            msg.setNonNullProperty(ApiJmsConstants.MSG_TYPE_PROPERTY, Integer.valueOf(ApiJmsConstants.MQMT_REQUEST));
        }

        // d314796 null our replyTo field as well as the core msg fields.
        // unset js reply destination and jms reply destination.
        else if (destination == null) {

            replyTo = null;
            // Clean out the reply header.
            setReplyHeader(null);
            msg.setJmsReplyTo(null);
            // d317373 the msg type must be a datagram now there is no replyTo
            msg.setNonNullProperty(ApiJmsConstants.MSG_TYPE_PROPERTY, Integer.valueOf(ApiJmsConstants.MQMT_DATAGRAM));
        }

        else {
            // d314796 throw an exception when the destination is foreign
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

        // Invalidate the toString cache
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJMSReplyTo");
    }

    /**
     * @see javax.jms.Message#getJMSDestination()
     */
    @Override
    public Destination getJMSDestination() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSDestination");

        // If a destination object is not cached, then the destination has
        // not been set yet, or the message has been received and the destination
        // has not yet been reconstructed from the underlying MFP encoding - in
        // either case try to get one from MFP (will return null if never set).
        // A foreign (non-jetstream) destination could be set at the sending end
        // of a flow if this jetstream jms message is being sent by some other
        // jms provider. In this case it will already be cached.
        if (dest == null) {
            byte[] byteForm = msg.getJmsDestination();
            if (byteForm != null) {
                dest = JmsInternalsFactory.getMessageDestEncodingUtils().getDestinationFromMsgRepresentation(byteForm);
            }
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "null was returned by getJmsDestination");
                dest = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSDestination", dest);
        return dest;
    }

    /**
     * @see javax.jms.Message#setJMSDestination(Destination)
     */
    @Override
    public void setJMSDestination(Destination destination) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSDestination", destination);

        setDestReference(destination);

        // The given destination is a native (jetstream) jms destination,
        // so set js destination, and jms destination (in encoded form).
        if (destination instanceof JmsDestinationImpl) {
            msg.setJmsDestination(((JmsDestinationImpl) destination).encodeToBytes(EncodingLevel.FULL));
        }

        // The given destination is a foreign (non-jetstream) jms destination,
        // so unset js destination and jms destination.
        else {
            msg.uncheckedSetForwardRoutingPath(null);
            msg.setJmsDestination(null);
        }

        // Invalidate the toString cache
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setJMSDestination(Destination)");
    }

    /**
     * @see javax.jms.Message#getJMSDeliveryMode()
     */
    @Override
    public int getJMSDeliveryMode() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSDeliveryMode");

        PersistenceType pt = msg.getJmsDeliveryMode();

        int tempDM = 0;

        // NB. The use of == in this switch statement _is_ the correct thing to
        // do because the PersistenceType constants are singletons, and it's quicker
        // than doing .equals.
        if (pt == PersistenceType.PERSISTENT) {
            tempDM = DeliveryMode.PERSISTENT;
        }
        else if (pt == PersistenceType.NON_PERSISTENT) {
            tempDM = DeliveryMode.NON_PERSISTENT;
        }
        else if (pt == PersistenceType.UNKNOWN) {
            // We don't expect to get UNKNOWN, but the value exists, and some
            // levels have returned it during the CoreToJms testcase.
            // Mapping to NON_PERSISTENT is arguably better than falling in a heap.
            tempDM = DeliveryMode.NON_PERSISTENT;
        }
        else {
            // getJMSDeliveryMode should always return one of the above, so if we get here
            // we have some sort of internal error
            // d238447 FFDC review. Need to generate an FFDC for this case.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "INTERNAL_ERROR_CWSIA0499",
                                                            new Object[] { "JMSDeliveryMode", pt },
                                                            null,
                                                            "JmsMessageImpl.getJMSDeliveryMode#1",
                                                            this,
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSDeliveryMode", tempDM);
        return tempDM;
    }

    /**
     * @see javax.jms.Message#setJMSDeliveryMode(int)
     */
    @Override
    public void setJMSDeliveryMode(int dm) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSDeliveryMode", dm);

        switch (dm) {

            case DeliveryMode.NON_PERSISTENT:
                msg.setJmsDeliveryMode(PersistenceType.NON_PERSISTENT);
                break;

            case DeliveryMode.PERSISTENT:
                msg.setJmsDeliveryMode(PersistenceType.PERSISTENT);
                break;

            default:
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Invalid value");
                // d238447 FFDC review. App' error, no FFDC required.
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "INVALID_VALUE_CWSIA0116",
                                                                new Object[] { "JMSDeliveryMode", String.valueOf(dm) },
                                                                tc
                                );
        } //switch

        // Invalidate the toString cache
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJMSDeliveryMode");
    }

    /**
     * @see javax.jms.Message#getJMSRedelivered()
     */
    @Override
    public boolean getJMSRedelivered() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSRedelivered");

        boolean redeliv;
        Boolean red = msg.getJmsRedelivered();
        if (red != null) {
            redeliv = red.booleanValue();
        }
        else {
            // d238447 FFDC review. App' error, no FFDC required. (This is impossible as MFP never returns null!)
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "PROPERTY_NOT_SET_CWSIA0101",
                                                            new Object[] { "JMSRedelivered" },
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSRedelivered");
        return redeliv;
    }

    /**
     * @see javax.jms.Message#setJMSRedelivered(boolean)
     * 
     *      Note that this value will be overriden by the provider on receive.
     */
    @Override
    public void setJMSRedelivered(boolean arg0) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSRedelivered", arg0);

        if (arg0 == false) {
            // This is well defined.
            msg.setRedeliveredCount(0);
        }
        else {
            // This is quite arbitrary, however the user application only calls
            // this if the want to alter the value after the message has been received,
            // in which case they get what they deserve.
            msg.setRedeliveredCount(1);
        }

        // Invalidate the toString cache
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJMSRedelivered");
    }

    /**
     * @see javax.jms.Message#getJMSType()
     */
    @Override
    public String getJMSType() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSType");

        String type = msg.getJmsType();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSType", type);
        return type;
    }

    /**
     * @see javax.jms.Message#setJMSType(String)
     */
    @Override
    public void setJMSType(String arg0) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSType", arg0);

        msg.setJmsType(arg0);
        // Invalidate the toString cache
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJMSType");
    }

    /**
     * @see javax.jms.Message#getJMSExpiration()
     */
    @Override
    public long getJMSExpiration() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSExpiration");

        Long ex = msg.getJmsExpiration();
        long exl = 0;
        if (ex != null) {
            exl = ex.longValue();
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Property was not set");
            // d238447 FFDC review. App' error, no FFDC required.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "PROPERTY_NOT_SET_CWSIA0101",
                                                            new Object[] { "JMSExpiration" },
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSExpiration", exl);
        return exl;
    }

    /**
     * @see javax.jms.Message#setJMSExpiration(long)
     */
    @Override
    public void setJMSExpiration(long exp) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSExpiration", exp);

        msg.setJmsExpiration(exp);
        // Invalidate the toString cache
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJMSExpiration");
    }

    /**
     * @see javax.jms.Message#getJMSPriority()
     */
    @Override
    public int getJMSPriority() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSPriority");

        Integer tempPri = msg.getPriority();
        int p = 0;
        if (tempPri != null) {
            p = tempPri.intValue();
            // f  204527.1  JMS API layer cater for JsMessage priority defaulting to -1
            // MP need to be able to tell if a message priority has not been set when
            // they receive it for send. This defends against the user calling this
            // method before they send the message.
            if (p == -1) {
                p = Message.DEFAULT_PRIORITY;
            }
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Property was not set");
            // d238447 FFDC review. App' error, no FFDC required.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "PROPERTY_NOT_SET_CWSIA0101",
                                                            new Object[] { "JMSPriority" },
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSPriority", p);
        return p;
    }

    /**
     * @see javax.jms.Message#setJMSPriority(int)
     */
    @Override
    public void setJMSPriority(int newPriority) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSPriority", newPriority);

        try {
            // The range checking for this property is handled by the MFP object.
            msg.setPriority(newPriority);
            // Invalidate the toString cache
            cachedToString = null;
        } catch (IllegalArgumentException iae) {
            // No FFDC code needed
            // d238447 FFDC review. App' error, no FFDC required.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "INVALID_VALUE_CWSIA0116",
                                                            new Object[] { "JMSPriority", "" + newPriority },
                                                            iae,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJMSPriority");
    }

    /**
     * @see javax.jms.Message#clearProperties()
     */
    @Override
    public void clearProperties() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearProperties");

        // clear properties in underlying message
        msg.clearProperties();

        // JMS clearProperties() also makes read-only message properties writeable.
        propertiesReadOnly = false;

        // Invalidate the toString cache
        cachedToString = null;

        // clear any locally held values
        if (locallyStoredPropertyValues != null)
            locallyStoredPropertyValues.clear();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearProperties");
    }

    /**
     * @see javax.jms.Message#propertyExists(String)
     */
    @Override
    public boolean propertyExists(String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "propertyExists", name);

        boolean ex = msg.propertyExists(name);
        if (locallyStoredPropertyValues != null)
            ex = ex || locallyStoredPropertyValues.containsKey(name);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "propertyExists", ex);
        return ex;
    }

    /**
     * @see javax.jms.Message#getBooleanProperty(String)
     */
    @Override
    public boolean getBooleanProperty(String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getBooleanProperty", name);

        Object obj = getObjByName(name);
        boolean value = JmsMessageImpl.parseBoolean(obj, name);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getBooleanProperty", value);
        return value;
    }

    /**
     * @see javax.jms.Message#getByteProperty(String)
     */
    @Override
    public byte getByteProperty(String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getByteProperty", name);

        Object obj = getObjByName(name);
        byte value = JmsMessageImpl.parseByte(obj, name);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getByteProperty", value);
        return value;
    }

    /**
     * @see javax.jms.Message#getShortProperty(String)
     */
    @Override
    public short getShortProperty(String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getShortProperty", name);

        Object obj = getObjByName(name);
        short value = JmsMessageImpl.parseShort(obj, name);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getShortProperty", value);
        return value;
    }

    /**
     * @see javax.jms.Message#getIntProperty(String)
     */
    @Override
    public int getIntProperty(String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getIntProperty", name);

        Object obj = getObjByName(name);
        int value = JmsMessageImpl.parseInt(obj, name);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getIntProperty", value);
        return value;
    }

    /**
     * @see javax.jms.Message#getLongProperty(String)
     */
    @Override
    public long getLongProperty(String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getLongProperty", name);

        Object obj = getObjByName(name);
        long value = JmsMessageImpl.parseLong(obj, name);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getLongProperty", value);
        return value;
    }

    /**
     * @see javax.jms.Message#getFloatProperty(String)
     */
    @Override
    public float getFloatProperty(String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getFloatProperty", name);

        Object obj = getObjByName(name);
        float value = JmsMessageImpl.parseFloat(obj, name);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getFloatProperty", value);
        return value;
    }

    /**
     * @see javax.jms.Message#getDoubleProperty(String)
     */
    @Override
    public double getDoubleProperty(String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDoubleProperty", name);

        Object obj = getObjByName(name);
        double value = JmsMessageImpl.parseDouble(obj, name);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDoubleProperty", value);
        return value;
    }

    /**
     * @see javax.jms.Message#getStringProperty(String)
     */
    @Override
    public String getStringProperty(String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getStringProperty", name);

        String value = null;
        Object obj = getObjByName(name);

        if ((obj instanceof String) || (obj == null)) {
            value = (String) obj;
        }
        else {
            value = obj.toString();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getStringProperty", value);
        return value;
    }

    /**
     * @see javax.jms.Message#getObjectProperty(String)
     */
    @Override
    public Object getObjectProperty(String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getObjectProperty", name);

        // Retrieve the object from the underlying MFP component.
        Object obj = getObjByName(name);

        // Special case for JMS_IBM_Character_Set.
        // If set as an integer, convert to String.
        if (name.equals(ApiJmsConstants.CHARSET_PROPERTY) && obj instanceof Integer) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "special case for charset, setting as string");
            obj = String.valueOf(obj);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getObjectProperty", obj);
        return obj;
    }

    /**
     * @see javax.jms.Message#getPropertyNames()
     */
    @Override
    public Enumeration getPropertyNames() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPropertyNames");

        // Get the set of properties from the underlying message
        Set<String> pnSet = msg.getPropertyNameSet();

        // Add in any locally held properties
        if (locallyStoredPropertyValues != null)
            pnSet.addAll(locallyStoredPropertyValues.keySet());

        final Iterator<String> it = pnSet.iterator();

        // Convert between iteration and enumeration using an anonymous class
        Enumeration vEnum = new Enumeration() {
            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }

            @Override
            public Object nextElement() {
                return it.next();
            }
        };

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getPropertyNames", vEnum);
        return vEnum;
    }

    /**
     * @see javax.jms.Message#setBooleanProperty(String, boolean)
     */
    @Override
    public void setBooleanProperty(String name, boolean val) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setBooleanProperty", new Object[] { name, val });

        checkPropertiesWriteable("setBooleanProperty");
        checkPropName(name, "setBooleanProperty");
        checkSettablePropertyNameAndType(name, Boolean.class);
        msg.setNonNullProperty(name, Boolean.valueOf(val));
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setBooleanProperty");
    }

    /**
     * @see javax.jms.Message#setByteProperty(String, byte)
     */
    @Override
    public void setByteProperty(String name, byte val) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setByteProperty", new Object[] { name, val });

        checkPropertiesWriteable("setByteProperty");
        checkPropName(name, "setByteProperty");
        checkSettablePropertyNameAndType(name, Byte.class);
        msg.setNonNullProperty(name, Byte.valueOf(val));
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setByteProperty");
    }

    /**
     * @see javax.jms.Message#setShortProperty(String, short)
     */
    @Override
    public void setShortProperty(String name, short val) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setShortProperty", new Object[] { name, val });

        checkPropertiesWriteable("setShortProperty");
        checkPropName(name, "setShortProperty");
        checkSettablePropertyNameAndType(name, Short.class);
        msg.setNonNullProperty(name, Short.valueOf(val));
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setShortProperty");
    }

    /**
     * @see javax.jms.Message#setIntProperty(String, int)
     */
    @Override
    public void setIntProperty(String name, int val) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setIntProperty", new Object[] { name, val });

        checkPropertiesWriteable("setIntProperty");
        checkPropName(name, "setIntProperty");

        // Special case for JMS_IBM_Character_Set - if set as an int, convert to String and set.
        if (name.equals(ApiJmsConstants.CHARSET_PROPERTY)) {
            setStringProperty(name, String.valueOf(val));
        }

        else {
            checkSettablePropertyNameAndType(name, Integer.class);

            // check for special cases which are locally stored
            if (localStorePropertyNames.contains(name)) {
                // create on first use
                if (locallyStoredPropertyValues == null)
                    locallyStoredPropertyValues = new Hashtable();
                locallyStoredPropertyValues.put(name, Integer.valueOf(val));
            }
            else {
                //default behaviour
                msg.setNonNullProperty(name, Integer.valueOf(val));
            }

            cachedToString = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setIntProperty");
    }

    /**
     * @see javax.jms.Message#setLongProperty(String, long)
     */
    @Override
    public void setLongProperty(String name, long val) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setLongProperty", new Object[] { name, val });

        checkPropertiesWriteable("setLongProperty");
        checkPropName(name, "setLongProperty");
        checkSettablePropertyNameAndType(name, Long.class);

        if (localStorePropertyNames.contains(name)) {
            // create on first use
            if (locallyStoredPropertyValues == null)
                locallyStoredPropertyValues = new Hashtable();
            locallyStoredPropertyValues.put(name, Long.valueOf(val));
        }
        else {
            msg.setNonNullProperty(name, Long.valueOf(val));
        }

        cachedToString = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setLongProperty");
    }

    /**
     * @see javax.jms.Message#setFloatProperty(String, float)
     */
    @Override
    public void setFloatProperty(String name, float val) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setFloatProperty", new Object[] { name, val });

        checkPropertiesWriteable("setFloatProperty");
        checkPropName(name, "setFloatProperty");
        checkSettablePropertyNameAndType(name, Float.class);
        msg.setNonNullProperty(name, Float.valueOf(val));
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setFloatProperty");
    }

    /**
     * @see javax.jms.Message#setDoubleProperty(String, double)
     */
    @Override
    public void setDoubleProperty(String name, double val) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDoubleProperty", new Object[] { name, val });

        checkPropertiesWriteable("setDoubleProperty");
        checkPropName(name, "setDoubleProperty");
        checkSettablePropertyNameAndType(name, Double.class);
        msg.setNonNullProperty(name, Double.valueOf(val));
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDoubleProperty");
    }

    /**
     * @see javax.jms.Message#setStringProperty(String, String)
     */
    @Override
    public void setStringProperty(String name, String val) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setStringProperty", new Object[] { name, val });

        checkPropertiesWriteable("setStringProperty");
        checkPropName(name, "setStringProperty");
        checkSettablePropertyNameAndType(name, String.class);

        // (If the user is setting the AppID - note that this is not the way that we
        // set the appID on send (which will probably override this value)).
        if (("JMSXAppID".equals(name))
            || (ApiJmsConstants.JMSX_USERID.equals(name))) {
            // Go straight to the real underlying message
            msg.setObjectProperty(name, val);
        }

        // Otherwise check the local property store first
        else if (localStorePropertyNames.contains(name)) {
            // create on first use, if we need it
            if (val != null) {
                if (locallyStoredPropertyValues == null)
                    locallyStoredPropertyValues = new Hashtable();
                locallyStoredPropertyValues.put(name, val);
            }
            else {
                if (locallyStoredPropertyValues != null)
                    locallyStoredPropertyValues.remove(name);
            }
        }

        // Mainline path.
        else {
            msg.setObjectProperty(name, val);
        }

        cachedToString = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setStringProperty");
    }

    /**
     * @see javax.jms.Message#setObjectProperty(String, Object)
     */
    @Override
    public void setObjectProperty(String name, Object obj) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setObjectProperty", new Object[] { name, obj });

        checkPropertiesWriteable("setObjectProperty");
        checkPropName(name, "setObjectProperty");

        // 238965 - we should make the checkSettable call based on the
        //   class type of the parameter, and not just 'Object'.
        // If the value is null, then the type is irrelevant.
        Class type = null;
        if (obj != null) {
            type = obj.getClass();
        }
        checkSettablePropertyNameAndType(name, type);

        // If the value is null, we still need to 'set' it. Leave MFP to deal with
        // whether that means it exists or not.
        if (obj == null) {
            // if this is a special case property, remove it from the map if it is there
            if (localStorePropertyNames.contains(name)) {
                if (locallyStoredPropertyValues != null)
                    locallyStoredPropertyValues.remove(name);
            }
            // otherwise, set it into the MFP message
            else {
                msg.setObjectProperty(name, null);
            }
        }

        // See 3.5.5 for details of what is allowed.
        // But note that properties starting with JMS could be out of line and their
        // types already checked by checkSettablePropertyNameAndType().
        else if ((obj instanceof Boolean)
                 || (obj instanceof Number)
                 || (obj instanceof String)
                 || (name.startsWith("JMS"))) {

            // if this is a special case property, store it locally
            if (localStorePropertyNames.contains(name)) {
                // create on first use
                if (locallyStoredPropertyValues == null)
                    locallyStoredPropertyValues = new Hashtable();
                locallyStoredPropertyValues.put(name, obj);
            }
            // otherwise, set it into the MFP message
            else {
                msg.setObjectProperty(name, obj);
            }
        }

        else {
            String objClassName = null;
            if (obj != null)
                objClassName = obj.getClass().getName();
            throw (MessageFormatException) JmsErrorUtils.newThrowable(MessageFormatException.class,
                                                                      "INVALID_OBJECT_TYPE_CWSIA0102",
                                                                      new Object[] { objClassName, name },
                                                                      tc);
        }

        cachedToString = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setObjectProperty");
    }

    /**
     * @see javax.jms.Message#acknowledge()
     */
    @Override
    public void acknowledge() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "acknowledge");

        // Throw an exception if this message is outbound, deserialized, or came
        // from a QueueBrowser.
        if (theSession == null) {
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "INVALID_FOR_UNCONSUMED_MSG_CWSIA0110",
                                                                               new Object[] { "acknowledge" },
                                                                               tc
                            );
        }

        synchronized (sessionSyncLock) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "got lock");

            // Throw an exception if the session is closed.
            theSession.checkNotClosed();

            // throw an exception if the acknowledge method conflicts with async usage
            theSession.checkSynchronousUsage("acknowledge");

            // Perform the appropriate action for session's ack mode. The action for
            // a dups ok session is somewhat unspecified, so we choose to commit.
            int sessAck = theSession.getAcknowledgeMode();
            if ((sessAck == Session.CLIENT_ACKNOWLEDGE)
                || (sessAck == Session.DUPS_OK_ACKNOWLEDGE)) {
                theSession.commitTransaction();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "acknowledge");
    }

    /**
     * @see javax.jms.Message#clearBody()
     */
    @Override
    public void clearBody() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearBody");

        // This is not applicable to the generic Message class, which only needs to
        // support the header fields.
        msg.clearBody();

        // JMS clearBody() also makes a read-only message body writeable.
        bodyReadOnly = false;

        cachedToString = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearBody");
    }

    /**
     * Over-ride the Object.toString method to provide useful message related
     * information.
     */
    @Override
    public String toString() {

        if (cachedToString == null) {
            try {
                // set the cachedToString to a non-null value on a temp basis
                // to prevent recursion in the case of one of the getJMSxxx methods
                // throwing an exception while we are creating toString. In this case,
                // if trace is turned on the message will be traces a param to newThrowable(),
                // causing toString to be called recursively.
                cachedToString = "TO_STRING_IN_PROGRESS";

                StringBuffer sb = new StringBuffer();

                sb.append("\n  JMSMessage class: " + messageClass);
                sb.append("\n  JMSType:          " + getJMSType());
                sb.append("\n  JMSDeliveryMode:  " + getJMSDeliveryMode());
                sb.append("\n  JMSExpiration:    " + getJMSExpiration());
                sb.append("\n  JMSPriority:      " + getJMSPriority());
                sb.append("\n  JMSMessageID:     " + getJMSMessageID());
                sb.append("\n  JMSTimestamp:     " + getJMSTimestamp());
                sb.append("\n  JMSCorrelationID: " + getJMSCorrelationID());

                // Treat the JMSDestination and JMSReplyTo carefully since they rely
                // on the efficient byte[] representation having been set correctly.
                sb.append("\n  JMSDestination:   ");

                try {
                    sb.append(getJMSDestination());

                } catch (Exception e) {
                    // No FFDC code needed
                    // Trace the exception in case we want to find out more info.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.exception(this, tc, e);
                    sb.append("<ERROR>");
                }

                sb.append("\n  JMSReplyTo:       ");
                try {
                    sb.append(getJMSReplyTo());
                } catch (Exception e) {
                    // No FFDC code needed
                    // Trace the exception in case we want to find out more info.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.exception(this, tc, e);
                    sb.append("<ERROR>");
                }

                sb.append("\n  JMSRedelivered:   " + getJMSRedelivered());
                sb.append("\n  JMSDeliveryTime:  " + getJMSDeliveryTime());

                // Now look at the user defined properties.
                Enumeration propertyNames = getPropertyNames();

                while (propertyNames.hasMoreElements()) {
                    String name = (String) (propertyNames.nextElement());
                    sb.append("\n    " + name + ": " + getObjectProperty(name));
                }

                // Store the output in the cache (which will be invalidated by any
                // change in the message state at this level.
                cachedToString = sb.toString();

            } catch (JMSException e) {
                // No FFDC code needed
                // Ignore any errors.
            }

        }

        return cachedToString;
    }

    // *********************** INTERNAL METHODS *********************

    /**
     * This method carries out the instantiation of the MFP message object for this
     * JMS message class. The method is overridden by subclasses to instantiate the
     * correct subclass and carry out any reference setting required.
     * 
     * It is safe to assume by this point that the jmfact reference has been
     * initialized, otherwise you wouldn't have got this far!
     */
    protected JsJmsMessage instantiateMessage() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "instantiateMessage");

        // Create a new message object.
        JsJmsMessage newMsg = jmfact.createJmsMessage();
        messageClass = CLASS_NONE;

        // d317373 default the MSG_TYPE to be DATAGRAM until setReplyTo is called
        newMsg.setNonNullProperty(ApiJmsConstants.MSG_TYPE_PROPERTY, Integer.valueOf(ApiJmsConstants.MQMT_DATAGRAM));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "instantiateMessage", newMsg);
        return newMsg;
    }

    /**
     * Get a Jetstream message representing the content of this JMS message.
     * Used during the sending of messages.
     * 
     * @return a Jetstream message which can be used to represent this message.
     * @throws JMSException if a problem arises during the transformation of the JMS message
     *             to a Jetstream message.
     */
    protected JsJmsMessage getMsgReference() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMsgReference");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMsgReference", msg);
        return msg;
    }

    /**
     * 
     * @param newMsg The message object to set
     */
    protected void setMsgReference(JsJmsMessage newMsg) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setMsgReference", newMsg);

        msg = newMsg;
        // Invalidate the toString cache
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setMsgReference");
    }

    /**
     * Update the destination cache reference without setting any
     * data into the core message.
     * 
     * @param d
     */
    protected void setDestReference(Destination d) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDestReference", d);
        dest = d;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setDestReference");
    }

    /**
     * If the message body is read-only, throw a MessageNotWriteableException.
     */
    protected void checkBodyWriteable(String callingMethodName) throws JMSException {
        if (bodyReadOnly) {
            throw (javax.jms.MessageNotWriteableException) JmsErrorUtils.newThrowable(javax.jms.MessageNotWriteableException.class,
                                                                                      "READ_ONLY_MESSAGE_BODY_CWSIA0107",
                                                                                      new Object[] { callingMethodName },
                                                                                      tc
                            );
        }
    }

    /**
     * If the message body is write-only, throw a MessageNotReadableException.
     */
    protected void checkBodyReadable(String callingMethodName) throws MessageNotReadableException
    {
        if (!bodyReadOnly) {
            throw (javax.jms.MessageNotReadableException) JmsErrorUtils.newThrowable(javax.jms.MessageNotReadableException.class,
                                                                                     "WRITE_ONLY_MESSAGE_BODY_CWSIA0109",
                                                                                     new Object[] { callingMethodName },
                                                                                     tc
                            );
        }
    }

    /**
     * If the message properties are read-only, throw a MessageNotWriteableException.
     */
    protected void checkPropertiesWriteable(String callingMethodName) throws JMSException {
        if (propertiesReadOnly) {
            throw (javax.jms.MessageNotWriteableException) JmsErrorUtils.newThrowable(javax.jms.MessageNotWriteableException.class,
                                                                                      "READ_ONLY_MESSAGE_PROPERTY_CWSIA0108",
                                                                                      new Object[] { callingMethodName },
                                                                                      tc
                            );
        }
    }

    /**
     * This method retrieves the underlying reliability that is set for this
     * message. The outgoing value can also be determined by examining the
     * persistence and NPM properties, however inbound cannot be found in this
     * way.
     * 
     * @return Reliablity
     */
    public Reliability getReliability() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getReliability");
        Reliability r = msg.getReliability();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getReliablity", r);
        return r;
    }

    /**
     * Obtains the reliability field from the reply header
     * 
     * @return Reliability
     */
    public Reliability getReplyReliability() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getReplyReliability");
        Reliability r = msg.getReplyReliability();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getReplyReliability", r);
        return r;
    }

    /*
     * Now that JMSDestinations do not have QOS mapping properties,
     * the role of replyReliability is reduced to a hint for non-JMS
     * applications. JMS will ignore the replyReliability.
     * 
     * d317816 the exception to this rule is when the internal message does have
     * a replyReliability set (perhaps by a mediation). In this case, the
     * replyReliability is set in the replyToDest, and we will use it here.
     * 
     * When no replyReliability is available in the replyToDest,
     * the reliability is set based on the deliveryMode of the replyToDest
     * 
     * DeliveryMode --> Reliability
     * --------------------------------------------------------------
     * a Persistent Set at send time from connection factory mapping
     * b NonPersistent Set at send time from connection factory mapping
     * d Application NONE
     * 
     * @param nonPerReliability The reliability associated with non-persistent messages
     * 
     * @param perReliability The reliability associated with persistent messages
     */
    void updateReplyReliability(Reliability nonPerReliability, Reliability perReliability) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "updateReplyReliability", new Object[] { nonPerReliability, perReliability });

        // is there a JMSReplyTo set?
        if (replyTo != null && replyTo instanceof JmsDestinationImpl) {

            // d317816 use the reply reliability saved in the destination if it's been set
            Reliability replyReliability = ((JmsDestinationImpl) replyTo).getReplyReliability();
            if ((replyReliability != null) && (replyReliability != Reliability.NONE)) {
                msg.uncheckedSetReplyReliability(replyReliability);
            }
            else {
                // set the reliability base on the deliveryMode of the replyToDest
                String dMode = ((JmsDestinationImpl) replyTo).getDeliveryMode();
                if (ApiJmsConstants.DELIVERY_MODE_PERSISTENT.equals(dMode)) {
                    // use the reliability defined by the connection factory persistent mapping
                    msg.uncheckedSetReplyReliability(perReliability);

                }
                else if (ApiJmsConstants.DELIVERY_MODE_NONPERSISTENT.equals(dMode)) {
                    // use the reliability defined by the connection factory non-persistent mapping
                    msg.uncheckedSetReplyReliability(nonPerReliability);

                }
                else if (ApiJmsConstants.DELIVERY_MODE_APP.equals(dMode)) {
                    // set to NONE
                    msg.uncheckedSetReplyReliability(Reliability.NONE);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "updateReplyReliability");
    }

    /**
     * Needed for decision paths in JmsBytesMessageImpl
     * 
     * @return bodyReadOnly flag.
     */
    protected boolean isBodyReadOnly() {
        return bodyReadOnly;
    }

    /**
     * Mark the body of the message as read only.
     * Needed for the reset methods of Bytes and Stream messages.
     */
    protected void setBodyReadOnly() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setBodyReadOnly");
        bodyReadOnly = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setBodyReadOnly");
    }

    // ************ PACKAGE ACCESS METHODS CALLED BY JmsMsgProducerImpl **********

    /**
     * Clear special case properties held in this
     * message. Invoked at send time.
     * 
     */
    void clearLocalProperties() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearLocalProperties");

        if (locallyStoredPropertyValues != null)
            locallyStoredPropertyValues.clear();
        // d255144 Also clear the local version of the messageID
        localJMSMessageID = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearLocalProperties");
    }

    /**
     * Called by the sendMessage method of JmsMsgProducerImpl in order to
     * invalidate the toString of the message when we send it.
     */
    void invalidateToStringCache() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "invalidateToStringCache");

        // Invalidate the toString cache.
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "invalidateToStringCache");
    }

    /**
     * Indicates whether the busName needs to be set in the RRP
     * 
     * @return true - needs updating, false doesn't
     */
    public boolean isRrpBusNameNeedsUpdating() {
        return rrpBusNameNeedsUpdating;
    }

    /**
     * set true the "busName needs updating" flag
     */
    public void setRrpBusNameNeedsUpdating(boolean rrpBusNameNeedsUpdating) {
        this.rrpBusNameNeedsUpdating = rrpBusNameNeedsUpdating;
    }

    // ********** STATIC HELPER METHODS ALSO USED BY OTHER IMPL CLASSES **********

    /**
     * This method checks the property name to see whether it is null or empty,
     * and throws an IllegalArgumentException if it is.
     * Note that this is used from MapMessage for the body, as well as from this
     * class for the header.
     */
    static void checkPropName(String name, String callingMethodName) throws IllegalArgumentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkPropName", new Object[] { name, callingMethodName });

        if ((name == null) || ("".equals(name))) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Invalid field name: " + name + " as parameter to " + callingMethodName);
            throw (IllegalArgumentException) JmsErrorUtils.newThrowable(IllegalArgumentException.class,
                                                                        "INVALID_FIELD_NAME_CWSIA0106",
                                                                        null, // no inserts for CWSIA0106
                                                                        tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkPropName");
    }

    /**
     * This static method is a factory for producing Message objects from
     * JsJmsMessage objects.
     * It is called by JmsSharedUtilsImpl.inboundMessagePath(...)
     * 
     * Note that this method cannot be easily moved to the shared utils
     * class because it access private state of the JmsMessageImpl object.
     * 
     * @param newMsg The inbound MFP message component we wish to get a JMS
     *            representation of.
     * @param newSess The session to associate with the Message objects
     * @param passThruProps Session-like properties, some of which are passed to the message objects.
     *            These properties originate in ConnectionFactory and ActivationSpec admin objects, and are relevant
     *            to message objects.
     * @return Message A JMS Message instance of the associated type to the
     *         parameter MFP component.
     */
    static Message inboundJmsInstance(JsJmsMessage newMsg, JmsSessionImpl newSess, Map passThruProps) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "inboundJmsInstance", new Object[] { newMsg, newSess, passThruProps });

        // This is where the mapping of MFP components to JMS components takes
        // place. The list of mappings should increase as we support more method
        // types.

        JmsMessageImpl jmsMsg = null;

        JmsBodyType bt = newMsg.getBodyType();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "body type: " + bt);

        int bodyTypeInt = -2; // This is not one of the JmsBodyType constants.
        if (bt != null) {
            bodyTypeInt = bt.toInt();
        }

        switch (bodyTypeInt) {

            case JmsBodyType.NULL_INT:
                jmsMsg = new JmsMessageImpl(newMsg, newSess);
                break;

            case JmsBodyType.TEXT_INT:
                jmsMsg = new JmsTextMessageImpl((JsJmsTextMessage) newMsg, newSess);
                break;

            case JmsBodyType.MAP_INT:
                jmsMsg = new JmsMapMessageImpl((JsJmsMapMessage) newMsg, newSess);
                break;

            case JmsBodyType.OBJECT_INT:
                jmsMsg = new JmsObjectMessageImpl((JsJmsObjectMessage) newMsg, newSess);
                break;

            case JmsBodyType.BYTES_INT:
                jmsMsg = new JmsBytesMessageImpl((JsJmsBytesMessage) newMsg, newSess);
                break;

            case JmsBodyType.STREAM_INT:
                jmsMsg = new JmsStreamMessageImpl((JsJmsStreamMessage) newMsg, newSess);
                break;

            default:
                // Catch-all situation to translate to a JMS Message instance - we won't be
                // able to get to any of the body data, but the header fields would be visible.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "UNKNOWN MESSAGE TYPE FOUND: " + bt);
                jmsMsg = new JmsMessageImpl(newMsg, newSess);
                break;

        }

        // Need to transfer certain performance related properties from the pass through properties
        // to the new message here. Only attempt this if pass thru props have been provided
        if (passThruProps != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Pass through properties provided - attempting to assign to new message");

            // Discover the properties from the pass thru props
            String producerProp = (String) passThruProps.get(JmsraConstants.PRODUCER_DOES_NOT_MODIFY_PAYLOAD_AFTER_SET);
            String consumerProp = (String) passThruProps.get(JmsraConstants.CONSUMER_DOES_NOT_MODIFY_PAYLOAD_AFTER_GET);

            // Assign attributes to the message according to value of properties
            if (producerProp != null) {
                jmsMsg.producerWontModifyPayloadAfterSet = producerProp.equalsIgnoreCase(ApiJmsConstants.WILL_NOT_MODIFY_PAYLOAD);
            }
            if (consumerProp != null) {
                jmsMsg.consumerWontModifyPayloadAfterGet = consumerProp.equalsIgnoreCase(ApiJmsConstants.WILL_NOT_MODIFY_PAYLOAD);
            }
        }

        // When a JMS Message has been received, it's body and properties are read-only
        // until clearBody() or clearProperties() are called.
        jmsMsg.bodyReadOnly = true;
        jmsMsg.propertiesReadOnly = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "inboundJmsInstance");
        return jmsMsg;
    }

    /**
     * This static method is a factory for producing JmsMessageImpl objects from
     * Message objects.
     * It is called by JmsMsgProducerImpl.send(...).
     */
    static JmsMessageImpl messageToJmsMessageImpl(Message message) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "messageToJmsMessageImpl", message);

        JmsMessageImpl jmsMessage = null;

        if (message instanceof BytesMessage) {
            jmsMessage = new JmsBytesMessageImpl((BytesMessage) message);
        }
        else if (message instanceof MapMessage) {
            jmsMessage = new JmsMapMessageImpl((MapMessage) message);
        }
        else if (message instanceof ObjectMessage) {
            jmsMessage = new JmsObjectMessageImpl((ObjectMessage) message);
        }
        else if (message instanceof StreamMessage) {
            jmsMessage = new JmsStreamMessageImpl((StreamMessage) message);
        }
        else if (message instanceof TextMessage) {
            jmsMessage = new JmsTextMessageImpl((TextMessage) message);
        }
        else if (message instanceof Message) {
            jmsMessage = new JmsMessageImpl(message);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "messageToJmsMessageImpl", jmsMessage);
        return jmsMessage;
    }

    /**
     * This method puts together the message to be returned to the user when they
     * attempt an invalid property conversion - for example trying to read an Int
     * property as a Byte.
     * It attempts to resolve the problem of integrating with the Bhattal Exception Handling Utility (tm)
     * 
     * @param obj The object containing the actual property
     * @param propName The name of the property that is being retrieved
     * @param dType The data type that it is being converted into
     * @param tc the TraceComponent to use for trace.
     */
    static MessageFormatException newBadConvertException(Object obj, String propName, String dType, TraceComponent xtc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "newBadConvertException", new Object[] { obj, propName, dType, xtc });

        // We expect the object to be something like java.lang.Double
        String clsName = null;

        if (!(obj instanceof byte[])) {
            clsName = obj.getClass().getName();
            int index = 0;
            // If there is a . in the class name (ie the package name) then remove it to
            // give us a concise class name.
            if ((index = clsName.lastIndexOf('.')) != 0) {
                clsName = clsName.substring(index + 1);
            }
        }
        else {
            clsName = "Byte[]";
        }

        MessageFormatException mfe = (javax.jms.MessageFormatException) JmsErrorUtils.newThrowable(javax.jms.MessageFormatException.class,
                                                                                                   "INVALID_TYPE_CONVERSION_CWSIA0104",
                                                                                                   new Object[] { propName, clsName, dType },
                                                                                                   xtc
                        );

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getBadConvertException");
        return mfe;
    }

    /**
     * This method parses a boolean value from an Object using in accordance with
     * the matrix of property conversions defined by JMS. It is called from both
     * the Message.getBooleanProperty and MapMessage.getBoolean methods.
     * 
     * @param obj The object we wish to convert
     * @param name The name of the property of field that we are looking up
     * @throws JMSException The conversion is not supported by JMS.
     */
    protected static boolean parseBoolean(Object obj, String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "parseBoolean", new Object[] { obj, name });
        boolean value = false;

        if (obj instanceof Boolean) {
            value = ((Boolean) obj).booleanValue();
        }
        else if (obj instanceof String) {
            value = Boolean.valueOf((String) obj).booleanValue();
        }
        else if (obj == null) {
            // This property had not been set so generate a RuntimeException.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "value null, generating rte");
            value = Boolean.valueOf(null).booleanValue();
        }
        else {
            // Boolean can only be retrieved as boolean and String (see 3.5.4).
            JMSException e = newBadConvertException(obj, name, "Boolean", tc);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Invalid convert", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "parseBoolean", value);
        return value;
    }

    /**
     * This method parses a byte value from an Object using in accordance with
     * the matrix of property conversions defined by JMS. It is called from both
     * the Message.getByteProperty and MapMessage.getByte methods.
     * 
     * @param obj The object we wish to convert
     * @param name The name of the property of field that we are looking up
     */
    protected static byte parseByte(Object obj, String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "parseByte", new Object[] { obj, name });
        byte value = 0;

        if (obj instanceof Byte) {
            value = ((Byte) obj).byteValue();
        }
        else if (obj instanceof String) {
            try {
                value = Byte.parseByte((String) obj);
            } catch (RuntimeException e) {
                // No FFDC code needed
                // d238447 Don't call processThrowable for app' errors
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Error parsing byte: " + obj, e);
                throw e;
            }
        }
        else if (obj == null) {
            // This property had not been set so generate a RuntimeException.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Generating exception for null->byte conversion");
            value = Byte.valueOf(null).byteValue();
        }
        else {
            JMSException e = newBadConvertException(obj, name, "Byte", tc);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "invalid convert", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "parseByte", value);
        return value;
    }

    /**
     * This method parses a double value from an Object using in accordance with
     * the matrix of property conversions defined by JMS. It is called from both
     * the Message.getDoubleProperty and MapMessage.getDouble methods.
     * 
     * @param obj The object we wish to convert
     * @param name The name of the property of field that we are looking up
     */
    protected static double parseDouble(Object obj, String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "parseDouble", new Object[] { obj, name });
        double value = 0;

        if (obj instanceof Double) {
            value = ((Double) obj).doubleValue();
        }
        else if (obj instanceof Float) {
            value = ((Float) obj).doubleValue();
        }
        else if (obj instanceof String) {
            try {
                value = Double.parseDouble((String) obj);
            } catch (RuntimeException e) {
                // No FFDC code needed
                // 238447 Don't generate FFDCs for app' errors.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Error parsing double: " + obj, e);
                throw e;
            }
        }
        else if (obj == null) {
            // This property had not been set so throw a RuntimeException.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "retrieved object is null, generating rte");
            throw new NullPointerException();
        }
        else {
            JMSException e = newBadConvertException(obj, name, "Double", tc);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "invalid convert", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "parseDouble", value);
        return value;
    }

    /**
     * This method parses a float value from an Object using in accordance with
     * the matrix of property conversions defined by JMS. It is called from both
     * the Message.getFloatProperty and MapMessage.getFloat methods.
     * 
     * @param obj The object we wish to convert
     * @param name The name of the property of field that we are looking up
     */
    protected static float parseFloat(Object obj, String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "parseFloat", new Object[] { obj, name });
        float value = 0;

        if (obj instanceof Float) {
            value = ((Float) obj).floatValue();
        }
        else if (obj instanceof String) {
            try {
                value = Float.parseFloat((String) obj);
            } catch (RuntimeException e) {
                // No FFDC code needed
                // 238447 Don't generate FFDCs for app' errors.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Error parsing float: " + obj, e);
                throw e;
            }
        }
        else if (obj == null) {
            // This property had not been set so throw a RuntimeException.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "retrieved object is null, generating rte");
            throw new NullPointerException();
        }
        else {
            JMSException e = newBadConvertException(obj, name, "Float", tc);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "invalid convert", e);
            throw e;

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "parseFloat", value);
        return value;
    }

    /**
     * This method parses a int value from an Object using in accordance with
     * the matrix of property conversions defined by JMS. It is called from both
     * the Message.getIntProperty and MapMessage.getInt methods.
     * 
     * @param obj The object we wish to convert
     * @param name The name of the property of field that we are looking up
     */
    protected static int parseInt(Object obj, String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "parseInt", new Object[] { obj, name });
        int value = 0;

        if (obj instanceof Integer) {
            value = ((Integer) obj).intValue();
        }
        else if (obj instanceof String) {
            try {
                value = Integer.parseInt((String) obj);
            } catch (RuntimeException e) {
                // No FFDC code needed
                // 238447 Don't generate FFDCs for app' errors.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Error parsing int: " + obj, e);
                throw e;
            }
        }
        else if (obj instanceof Byte) {
            value = ((Byte) obj).intValue();
        }
        else if (obj instanceof Short) {
            value = ((Short) obj).intValue();
        }
        else if (obj == null) {
            // This property had not been set so generate a RuntimeException.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "retrieved object is null, generating rte");
            value = Integer.valueOf(null).intValue();
        }
        else {
            JMSException e = newBadConvertException(obj, name, "Integer", tc);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "invalid convert", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "parseInt", value);
        return value;
    }

    /**
     * This method parses a long value from an Object using in accordance with
     * the matrix of property conversions defined by JMS. It is called from both
     * the Message.getLongProperty and MapMessage.getLong methods.
     * 
     * @param obj The object we wish to convert
     * @param name The name of the property of field that we are looking up
     */
    protected static long parseLong(Object obj, String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "parseLong", new Object[] { obj, name });
        long value = 0;

        if (obj instanceof Long) {
            value = ((Long) obj).longValue();
        }
        else if (obj instanceof String) {
            try {
                value = Long.parseLong((String) obj);
            } catch (RuntimeException e)
            {
                // No FFDC code needed
                // 238447 Don't generate FFDCs for app' errors.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Error parsing long: " + obj, e);
                throw e;
            }
        }
        else if (obj instanceof Byte) {
            value = ((Byte) obj).longValue();
        }
        else if (obj instanceof Short) {
            value = ((Short) obj).longValue();
        }
        else if (obj instanceof Integer) {
            value = ((Integer) obj).longValue();
        }
        else if (obj == null) {
            // This property had not been set so generate a RuntimeException.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "retrieved object is null, generating rte");
            value = Long.valueOf(null).longValue();
        }
        else {
            JMSException e = newBadConvertException(obj, name, "Long", tc);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "invalid convert", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "parseLong", value);
        return value;
    }

    /**
     * This method parses a short value from an Object using in accordance with
     * the matrix of property conversions defined by JMS. It is called from both
     * the Message.getShortProperty and MapMessage.getShort methods.
     * 
     * @param obj The object we wish to convert
     * @param name The name of the property of field that we are looking up
     */
    protected static short parseShort(Object obj, String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "parseShort", new Object[] { obj, name });
        short value = 0;

        if (obj instanceof Short) {
            value = ((Short) obj).shortValue();
        }
        else if (obj instanceof String) {
            try {
                value = Short.parseShort((String) obj);
            } catch (RuntimeException e) {
                // No FFDC code needed
                // 238447 Don't generate FFDCs for app' errors.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Error parsing short: " + obj, e);
                throw e;
            }
        }
        else if (obj instanceof Byte) {
            value = ((Byte) obj).shortValue();
        }
        else if (obj == null) {
            // This property had not been set so generate a RuntimeException.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "retrieved object is null, generating rte");
            value = Short.valueOf(null).shortValue();
        }
        else {
            JMSException e = newBadConvertException(obj, name, "Short", tc);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "invalid convert", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "parseShort", value);
        return value;
    }

    // *************************** PRIVATE METHODS  ******************************

    private static void obtainMFPFactory() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "obtainMFPFactory");
        // Ensure that we have a reference to the MFP factory object that this
        // (and subclasses) can work with. We only do this the first time that
        // this constructor is run.
        if (JmsMessageImpl.jmfact == null) {

            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Retrieving reference to MFP factory.");
                jmfact = JsJmsMessageFactory.getInstance();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Retrieving reference to DestinationAddress factory.");
                destAddressFactory = JmsServiceFacade.getSIDestinationAddressFactory();
            } catch (Exception e) {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Exception getting MFP factory", e);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr
                                    .exit(tc, "JmsMessageImpl");

                // d238447 FFDC review. Generate an FFDC for this case.
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class
                                                                , "INITIALIZATION_ERROR_CWSIA0002"
                                                                , new Object[] { e }
                                                                , e
                                                                , "JmsMessageImpl.obtainMFPFactory#1"
                                                                , JmsMessageImpl.class
                                                                , tc
                                );
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "obtainMFPFactory");
    }

    /**
     * Takes the parameter object and uses it to set the reply header data in the
     * core message object. Note that the parameter to this object may be null, in which
     * case sensible defaults will be used (wiping out any existing data).
     * 
     * @param replyTo
     */
    private void setReplyHeader(JmsDestinationImpl replyTo) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReplyHeader", replyTo);

        if (replyTo != null) {
            SIDestinationAddress siDest = replyTo.getConsumerSIDestinationAddress();

            // d317373 perf. Set a flag telling sendMessage that the rrp needs updating with the busName
            String busName = siDest.getBusName();
            if (busName == null) {
                setRrpBusNameNeedsUpdating(true);
            }

            // Set the reverse routing path and discriminator.

            // Need to use the checked set method since we have not checked it anywhere else.
            msg.setReplyDiscriminator(replyTo.getDestDiscrim());

            // As described in 179339, when a JmsDestination is used for a replyTo we
            // ignore any forward or reverse routing paths that are set on it and take
            // only the 'final' destination, hence the array of size 1.
            List<SIDestinationAddress> reverseList = new ArrayList<SIDestinationAddress>(1);
            reverseList.add(siDest);
            msg.uncheckedSetReverseRoutingPath(reverseList);

            // Set the priority if necessary.
            msg.uncheckedSetReplyPriority(replyTo.getPriority());

            // Now set the reply timeout.
            msg.uncheckedSetReplyTimeToLive(replyTo.getTimeToLive());

            // Reliability is now set per-send call by updateReplyReliability
        }

        else {
            // ReplyTo dest parameter was null
            // In this case we want to blank out all the data to save space when the
            // message is transmitted.
            msg.clearReplyFields();
            msg.uncheckedSetReverseRoutingPath(null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setReplyHeader");
    }

    /**
     * Get a property from the underlying JS message.
     * Check for special case names like JMSXDeliveryCount.
     * 
     * @param name name of the object to retrieve
     */
    private Object getObjByName(String name) {
        Object obj = null;

        // d234039 get*Property(null) throws NPE.
        // Throw IllegalArgumentException instead
        if (name == null) {
            throw (IllegalArgumentException) JmsErrorUtils.newThrowable(IllegalArgumentException.class,
                                                                        "INVALID_PROPNAME_CWSIA0112",
                                                                        new Object[] { null },
                                                                        tc);
        }

        if ("".equals(name)) {
            throw (IllegalArgumentException) JmsErrorUtils.newThrowable(IllegalArgumentException.class,
                                                                        "INVALID_PROPNAME_CWSIA0112",
                                                                        new Object[] { "\"\"" },
                                                                        tc);
        }

        if ((locallyStoredPropertyValues != null) && locallyStoredPropertyValues.containsKey(name)) {
            obj = locallyStoredPropertyValues.get(name);
        }

        // Get the value from the underlying message
        // d325186.1 Special processing for JMSX and JMS_IBM properties has moved
        // to the underlying message's getObjectProperty() method to improve
        // serviceability.
        else {
            obj = msg.getObjectProperty(name);
        }

        return obj;
    }

    /**
     * This method checks the String provided to see if it is valid as a client-settable Property Name.
     * Mostly copied from MA88 JMSMessage.java, version 1.58 (see 183428)
     * 
     * If the property name is not ok, throws MessageFormatException. This seems a slightly strange
     * exception to use, but the JMS spec/javadoc doesn't specify what to do, and this gives the same
     * behaviour as MA88.
     * 
     * Note that this does not really check for validity of the name according to the JMS
     * spec. as it does not check for NULL, TRUE, FALSE, NOT... etc etc etc. However, we can't
     * introduce such checking as v6 is already in use by customers.
     * 
     * @param propertyName java.lang.String
     * @param propType java.lang.Class
     * @throws MessageFormatException if the property name is not acceptable.
     */
    static void checkSettablePropertyNameAndType(String propertyName, Class propType) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkSettablePropertyNameAndType", new Object[] { propertyName, propType });

        /***************************************************************************/
        /*                                                                         */
        /* First we run checks to see if it is a valid Java identifier */
        /*                                                                         */
        /***************************************************************************/

        if (propertyName == null || Character.isJavaIdentifierStart(propertyName.charAt(0)) == false) {
            throw (MessageFormatException) JmsErrorUtils.newThrowable(MessageFormatException.class,
                                                                      "INVALID_PROPNAME_CWSIA0112",
                                                                      new Object[] { propertyName },
                                                                      tc);
        }

        for (int i = 1; i < propertyName.length(); i++) {
            if (Character.isJavaIdentifierPart(propertyName.charAt(i)) == false) {
                throw (MessageFormatException) JmsErrorUtils.newThrowable(MessageFormatException.class,
                                                                          "INVALID_PROPNAME_CWSIA0112",
                                                                          new Object[] { propertyName },
                                                                          tc);
            }
        }

        /***************************************************************************/
        /* If it starts with JMS then it may well be invalid. We allow through */
        /* a set of JMS_IBM_ properties, but the only JMSX properties that can */
        /* be set from the client are GroupID and GroupSeq */
        /***************************************************************************/
        if (propertyName.startsWith("JMS_")) {

            // All the supported JMS_IBM_ property names are held in the JMS_IBM_props
            // table. We check that the correct type of class is trying to be set by
            // the users call to set<type>Property(). If the user is trying
            // to set the property to a type not supported, throw a JMSException.
            // d238447 FFDC review. No FFDC required
            if (JMS_IBM_props.containsKey(propertyName)) {
                if (propType != null) {
                    Class expectedPropType = JMS_IBM_props.get(propertyName);
                    if (propType != expectedPropType) {
                        throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                        "JMS_IBM_INVALID_TYPE_CWSIA0114",
                                                                        new Object[] { propertyName, expectedPropType.getName(), propType.getName() },
                                                                        tc);
                    }
                }
            }

            // JMSX property name
            else if (propertyName.startsWith("JMSX")) {

                // This is one of the JMSX properties. As a result of 183428 we allow
                // the user to set any of the JMSX properties that we support. Note that
                // in general they should only really set GroupID and GroupSeq, so we
                // fastpath those ones.
                if (propertyName.equals("JMSXGroupID") ||
                    propertyName.equals("JMSXGroupSeq")) {
                    // OOPS!!! Should have checked the type here, butr it is too late to
                    // start now - hence any type will be allowed for these properties.
                    // The wrong types will potentially fail when encoded for MQ.
                }

                // We now look at the list of supported properties to see whether we should
                // let the user set these. Note that since this should not happen in normal
                // operation we don't worry too much about it being efficient.
                // None of the types are checked here either, which may be an oversight.
                else {
                    ConnectionMetaData cmd = JmsFactoryFactory.getInstance().getMetaData();
                    Enumeration supportedJMSX = cmd.getJMSXPropertyNames();
                    boolean found = false;

                    // Look at each property in turn.
                    while (supportedJMSX.hasMoreElements()) {
                        String nextProp = (String) supportedJMSX.nextElement();
                        if (nextProp.equals(propertyName)) {
                            found = true;
                            break;
                        }
                    }

                    // If we did not find this property, we need to throw an exception.
                    if (!found) {
                        throw (MessageFormatException) JmsErrorUtils.newThrowable(MessageFormatException.class,
                                                                                  "RESERVED_PROPNAME_CWSIA0113",
                                                                                  new Object[] { propertyName },
                                                                                  tc);
                    }
                }
            }

            // If the name started with JMS but NOT with JMS_IBM or JMSX, it is invalid
            else {
                throw (MessageFormatException) JmsErrorUtils.newThrowable(MessageFormatException.class,
                                                                          "RESERVED_PROPNAME_CWSIA0113",
                                                                          new Object[] { propertyName },
                                                                          tc);
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkSettablePropertyNameAndType");
    }

    /**
     * @param paramClass
     * @return
     * @throws JMSException
     * @throws MessageFormatException
     */
    @Override
    public <T> T getBody(Class<T> paramClass) throws JMSException, MessageFormatException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getBody", new Object[] { paramClass });

        T returnObj = null;
        try {
            int bodyType = msg.getBodyType().toInt();
            Object _messageObject = null;
            switch (bodyType) {
                case JmsBodyType.TEXT_INT:
                    try {
                        _messageObject = ((JsJmsTextMessage) msg).getText();
                    } catch (UnsupportedEncodingException exp) {
                        throw getJMSException(paramClass, exp);
                    }
                    break;

                case JmsBodyType.OBJECT_INT:
                    try {
                        _messageObject = ((JsJmsObjectMessage) msg).getRealObject();

                        //As per spec: if the message is an ObjectMessage and object deserialization fails 
                        //then throw MessageFormatException.
                        if (_messageObject != null && !Serializable.class.isAssignableFrom(paramClass)) {
                            throw getMessageFormatException(paramClass);
                        }

                    } catch (ClassNotFoundException cnfe) {
                        throw getJMSException(paramClass, cnfe);
                    } catch (IOException ioe) {
                        throw getJMSException(paramClass, ioe);
                    }
                    break;

                case JmsBodyType.MAP_INT:
                    try {
                        _messageObject = getMapMessage();

                        //As per spec: If the message is a MapMessage then this parameter must be set to java.util.Map.class (or java.lang.Object.class). 
                        //then throw MessageFormatException.
                        if (_messageObject != null && !paramClass.isAssignableFrom(Map.class)) {
                            throw getMessageFormatException(paramClass);
                        }
                    } catch (UnsupportedEncodingException uee) {
                        throw getJMSException(paramClass, uee);
                    }
                    break;

                case JmsBodyType.BYTES_INT:
                    byte[] byteMsg = ((JsJmsBytesMessage) msg).getBytes();
                    _messageObject = (byteMsg != null && byteMsg.length > 0) ? byteMsg : null;

                    if (_messageObject != null && !paramClass.isAssignableFrom(byte[].class)) {
                        throw getMessageFormatException(paramClass);
                    }
                    break;

                case JmsBodyType.STREAM_INT:
                    throw getMessageFormatException(paramClass);

                case JmsBodyType.NULL_INT:
                    _messageObject = null;
                    break;
            }
            if (_messageObject != null) {
                if (paramClass.isAssignableFrom(_messageObject.getClass())) {
                    returnObj = (T) _messageObject;
                } else {
                    throw getMessageFormatException(paramClass);
                }
            }
            return returnObj;
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getBody", returnObj);
        }
    }

    /**
     * @return
     * @throws UnsupportedEncodingException
     */
    private Map getMapMessage() throws UnsupportedEncodingException {
        Map<String, Object> mapBody = null;
        try {
            Enumeration mapNames = ((JsJmsMapMessage) msg).getMapNames();
            mapBody = new HashMap<String, Object>();
            while (mapNames.hasMoreElements()) {
                String name = (String) mapNames.nextElement();
                mapBody.put(name, ((JsJmsMapMessage) msg).getObject(name));
            }
        } catch (UnsupportedEncodingException uee) {
            throw uee;
        }
        return (mapBody != null && mapBody.isEmpty()) ? null : mapBody;
    }

    /**
     * @param paramClass
     * @return
     */
    private <T> MessageFormatException getMessageFormatException(T paramClass) {
        return (javax.jms.MessageFormatException) JmsErrorUtils.newThrowable(javax.jms.MessageFormatException.class,
                                                                             "INVALID_CLASS_TYPE_CWSIA0118", new Object[] { paramClass }, tc);
    }

    /**
     * @param paramClass
     * @param exp
     * @return
     */
    private <T> JMSException getJMSException(T paramClass, Exception exp) {
        return (JMSException) JmsErrorUtils.newThrowable(JMSException.class, "GET_MSG_BODY_FAILED_CWSIA0120", null, exp, null, this, tc);
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Message#getJMSDeliveryTime()
     */
    @Override
    public long getJMSDeliveryTime() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSDeliveryTime");

        Long ex = msg.getJmsDeliveryTime();
        long exl = 0;
        if (ex != null) {
            exl = ex.longValue();
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Property was not set");
            // d238447 FFDC review. App' error, no FFDC required.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "PROPERTY_NOT_SET_CWSIA0101",
                                                            new Object[] { "JMSDeliveryTime" },
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSDeliveryTime", exl);
        return exl;
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Message#isBodyAssignableTo(java.lang.Class)
     */
    @Override
    public boolean isBodyAssignableTo(Class paramClass) throws JMSException, MessageFormatException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isBodyAssignableTo", new Object[] { paramClass });

        boolean decision = false;
        //If paramClass is null, then return false;
        if (paramClass == null)
            return false;
        try {
            // get the body type
            int bodyType = msg.getBodyType().toInt();

            if (bodyType == JmsBodyType.TEXT_INT) {
                decision = String.class.isAssignableFrom(paramClass);
            } else if (bodyType == JmsBodyType.OBJECT_INT) {
                decision = Serializable.class.isAssignableFrom(paramClass);

                // try if the object can be deserialized, if not then return false
                if (decision) {
                    try {
                        Object object = ((JsJmsObjectMessage) msg).getRealObject();
                        decision = (paramClass.isAssignableFrom(object.getClass()));
                    } catch (IOException jmse) {
                        return false;
                    } catch (ClassNotFoundException e) {
                        return false;
                    }
                }
            } else if (bodyType == JmsBodyType.MAP_INT) {
                decision = Map.class.isAssignableFrom(paramClass);
            } else if (bodyType == JmsBodyType.BYTES_INT) {
                decision = byte[].class.isAssignableFrom(paramClass);
            } else if (bodyType == JmsBodyType.STREAM_INT) {
                decision = false;
            } else if (bodyType == JmsBodyType.NULL_INT) { // return false if message does not have a body
                decision = false;
            }

        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "isBodyAssignableTo", new Object[] { decision });
        }
        return decision;

    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Message#setJMSDeliveryTime(long)
     */
    @Override
    public void setJMSDeliveryTime(long value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJMSDeliveryTime", value);

        msg.setJmsDeliveryTime(value);
        // Invalidate the toString cache
        cachedToString = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJMSDeliveryTime");

    }

    /**
     * @return the inusebyAsyncSend
     */
    boolean isAsynSendInProgress() {
        return inusebyAsyncSend;
    }

    /**
     * @param inusebyAsyncSend the inusebyAsyncSend to set
     */
    void setAsyncSendInProgress(boolean inusebyAsyncSend) {
        this.inusebyAsyncSend = inusebyAsyncSend;
    }
}
