/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.mfp.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.SIProperties;
import com.ibm.ws.sib.mfp.JmsBodyType;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MessageType;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.PersistenceType;
import com.ibm.ws.sib.mfp.WebJsMessageEncoder;
import com.ibm.ws.sib.mfp.schema.JsApiAccess;
import com.ibm.ws.sib.mfp.schema.JsHdr2Access;
import com.ibm.ws.sib.mfp.schema.JsHdrAccess;
import com.ibm.ws.sib.mfp.schema.JsPayloadAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * JsJmsMessageImpl extends JsApiMessageImpl and hence JsMessageImpl,
 * and is the implementation class for the JsJmsMessage interface.
 * <p>
 * The JsMessageImpl instance contains the JsMsgObject which is the
 * internal object which represents the API Message.
 * The implementation classes for all the specialised JMS message types extend
 * JsJmsMessageImpl, as well as implementing their specialised interface.
 */
class JsJmsMessageImpl extends JsApiMessageImpl implements JsJmsMessage {

    private final static long serialVersionUID = 1L;
    private final static byte[] flattenedClassName; // SIB0112b.mfp.2

    private static TraceComponent tc = SibTr.register(JsJmsMessageImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

    // Guesstimate of the number of bytes in the flattened version of the Payload JSMessageImpl,
    // including the overhead of its sub-message, but excluding the actual data in the body.
    // This constant is used by the sub-classes when guessing their flattened data size.
    final static int FLATTENED_PAYLOAD_PART = 40;

    // A guesstimate at the base additional fluffed size for being a JMS message.
    private final static int FLUFFED_JMS_BASE_SIZE;
    // A guesstimate at the base additional fluffed size for having any payload data.
    private final static int FLUFFED_PAYLOAD_BASE_SIZE;

    /* Get the flattened form of the classname SIB0112b.mfp.2 */
    static {
        flattenedClassName = flattenClassName(JsJmsMessageImpl.class.getName());
    }

    static {
        // Make a guess at the base additional size for being a JMS message.
        // We have already included the JsPayload's JSMessaegImpl & JsMsgPart in
        // JsMessageImpl's calculation. Now add:
        //         the cache - 4 * REF
        //         format  - String + 20 (10 chars is the biggest JMS format we have)
        //         encoding & charset - mostly not set, so ignore them
        FLUFFED_JMS_BASE_SIZE = FLUFFED_REF_SIZE * 4
                                + FLUFFED_STRING_OVERHEAD + 20;
        // If there is payload data, we will have another JSMessageImpl, JsMsgPart
        // & single entry cache.
        FLUFFED_PAYLOAD_BASE_SIZE = FLUFFED_JMF_MESSAGE_SIZE
                                    + FLUFFED_JSMSGPART_SIZE
                                    + FLUFFED_REF_SIZE;
    }

    /* ************************************************************************* */
    /* Constructors */
    /* ************************************************************************* */

    /**
     * Constructor for a new Jetstream JMS message.
     * 
     * This constructor should never be used explicitly.
     * It is only to be used implicitly by the sub-classes' no-parameter constructors.
     * The method must not actually do anything.
     */
    JsJmsMessageImpl() {}

    /**
     * Constructor for a new Jetstream JMS message.
     * 
     * @param flag Flag to distinguish different constructors.
     * 
     * @exception MessageDecodeFailedException Thrown if such a message can not be created
     */
    JsJmsMessageImpl(int flag) throws MessageDecodeFailedException {
        super(flag);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "<init>");

        /* Set the JMS message fields */
        setJsMessageType(MessageType.JMS);
        setSubtype(JmsBodyType.NULL_INT);

        /* Set some of the JMS specific header values unless flagged not to */
        /* For an Inbound MQ message the fields will be set by the */
        /* MQJsMessageFactory from fields in the MQMD and/or MQRFH2 so there is */
        /* no point initializing them here. d339872 */
        if (flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ) {
            setJmsDeliveryMode(PersistenceType.PERSISTENT);
            setJmsExpiration(0);
            setJmsType(null); // If not set to null, will be 'derived' when got
            setJmsDeliveryTime(0);

        }

        /* Set the JMS format information */
        setFormat(SIApiConstants.JMS_FORMAT);
    }

    /**
     * Constructor for an inbound message.
     * (Only to be called by a superclass make method.)
     * 
     * @param inJmo The JsMsgObject representing the inbound method.
     */
    JsJmsMessageImpl(JsMsgObject inJmo) {
        super(inJmo);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "<init>, inbound jmo ");
        /* Do not set any fields - they should already exist in the message */
    }

    /* ************************************************************************* */
    /* Message Body methods */
    /* ************************************************************************* */

    // Cached length of the payload
    private transient int payloadLength = 0;

    /**
     * clearCachedLengths
     * Clear any cached length values, including those belonging to the superclasses.
     */
    @Override
    final void clearCachedLengths() {
        super.clearCachedLengths();
        payloadLength = 0;
    }

    /*
     * Provide the contribution of this part to the estimated encoded length
     */
    @Override
    int guessApproxLength() {
        // If we don't have a cached payloadLength, get it now
        if (payloadLength == 0) {
            payloadLength = guessPayloadLength();
        }

        int total = super.guessApproxLength() + payloadLength;
        return total;
    }

    // Override this method in the JMS subclasses
    int guessPayloadLength() {
        return 0;
    }

    /**
     * Provide the contribution of this part to the estimated 'fluffed' message size.
     * Subclasses that wish to contribute to a quick guess at the length of a
     * fluffed message should override this method, invoke their superclass and add on
     * their own contribution.
     * 
     * For this class, we should add an approximation of the size of the JMF
     * JSMessageImpl which represents the JsPayloadSchema. If there is a body, we then
     * call the subclass to add its specific size estimate.
     * 
     * @return int A guesstimate of the fluffed size of the message
     */
    @Override
    int guessFluffedSize() {

        // Get the contribution from the superclass(es)
        int total = super.guessFluffedSize();

        // Add the basic additional size for being a JMS message.
        total += FLUFFED_JMS_BASE_SIZE;

        // If there is a body in the message.....
        if (jmo.getPayloadPart().getChoiceField(JsPayloadAccess.PAYLOAD) != JsPayloadAccess.IS_PAYLOAD_EMPTY) {

            // Add in the extra overhead for having a fluffed body
            total += FLUFFED_PAYLOAD_BASE_SIZE;

            // And call the method to get the size of the fluffed data
            total += guessFluffedDataSize();
        }

        return total;
    }

    /**
     * guessFluffedDataSize
     * Return the estimated fluffed size of the payload data.
     * If there is no subclass overriding this method, the answer is always 0.
     * 
     * @return int Always returns 0.
     */
    int guessFluffedDataSize() {
        return 0;
    }

    /*
     * Delete the Payload of any JMS Message.
     * 
     * Javadoc description supplied by the JsMessage interface.
     * This method is required in order to properly implement the interfaces.
     * It is used by Report Message support and for a JMS message just needs to
     * clear the JMS message body.
     */
    @Override
    public final void clearMessagePayload() {
        clearBody();
    }

    /*
     * Clear the message body.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     * 
     * Note: this method will normally be overridden by JMS subclass message
     * types, as they will know how to clear specific body types.
     */
    @Override
    public void clearBody() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearBody");
        jmo.getPayloadPart().setChoiceField(JsPayloadAccess.PAYLOAD, JsPayloadAccess.IS_PAYLOAD_EMPTY);
        clearCachedLengths();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearBody");
    }

    /* ************************************************************************* */
    /* Oddments */
    /* ************************************************************************* */

    /*
     * Determine whether the message should be considered to have already been
     * 'sent' into the SIB system.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final boolean alreadySent() {
        // "already sent" has the same meaning as "is sent" (at the moment)e
        return isSent();
    }

    /*
     * Perform any specific send-time processing and then call the superclass method
     * and return the result.
     * <p>
     * The specific send-time processing for a JsJmsMessage is to write back
     * the JMS_IBM_MQMD_MsgId property, if set, into the ApiMessageId.
     * <p>
     * This method must be called by the Message processor during 'send'
     * processing, AFTER the headers are set.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    @Override
    public JsMessage getSent(boolean copy) throws MessageCopyFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getSent", copy);

        // We don't want to fluff up an MQMD PropertyMap unnecessarily, so we try to
        // figure out first whether there have been any MQMD properties set....
        if (hasMQMDPropertiesSet()) {

            // If there may be some explicitly set JMS_IBM_MQMD_ properties, get it from
            // the map. Do NOT just call getMQMDProperty() as that would also look in any
            // underlying MQMD.
            byte[] apiMsgId = (byte[]) getMQMDSetPropertiesMap().get(SIProperties.JMS_IBM_MQMD_MsgId);
            if (apiMsgId != null) {
                setApiMessageIdAsBytes(apiMsgId);
            }
        }

        // Finally call the superclass's method & return the result.
        JsMessage newMsg = super.getSent(copy);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getSent", newMsg);
        return newMsg;
    }

    /* ************************************************************************* */
    /* Get Methods for header fields not already implemented in JsApiMessageImpl */
    /* ************************************************************************* */

    /*
     * Get the type of the message body.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final JmsBodyType getBodyType() {
        /* Get the subtype and get the corresponding JmsBodyType to return */
        int bType = getSubtype();
        return JmsBodyType.getJmsBodyType(Byte.valueOf((byte) bType));
    }

    /* ************************************************************************* */
    /* Package-level method for defaulting unset JMS specific fields */
    /* ************************************************************************* */

    /**
     * If the JMS-only header fields are empty, set them to the appropriate
     * derived or deafult values.
     * There is no need to do this for JmsDestination and JmsReplyTo - it just wastes space // d339872
     */
    final void setDerivedJmsFields() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDerivedJmsFields");

        if (getHdr2().getChoiceField(JsHdr2Access.JMSDELIVERYMODE) == JsHdr2Access.IS_JMSDELIVERYMODE_EMPTY) {
            setJmsDeliveryMode(getDerivedJmsDeliveryMode());
        }

        if (getHdr2().getChoiceField(JsHdr2Access.JMSEXPIRATION) == JsHdr2Access.IS_JMSEXPIRATION_EMPTY) {
            setJmsExpiration(getDerivedJmsExpiration());
        }

        if (getHdr2().getChoiceField(JsHdr2Access.JMSDELIVERYTIME) == JsHdr2Access.IS_JMSDELIVERYTIME_EMPTY) {
            setJmsDeliveryTime(getDerivedJmsDeliveryTime());
        }

        if (getHdr2().getChoiceField(JsHdr2Access.JMSTYPE) == JsHdr2Access.IS_JMSTYPE_EMPTY) {
            setJmsType(getDerivedJmsType());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDerivedJmsFields");
    }

    /* ************************************************************************* */
    /* Set Methods for header fields */
    /* ************************************************************************* */

    /*
     * Set the value of the JMSRedelivered field in the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void setJmsDeliveryMode(PersistenceType value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJmsDeliveryMode", value);
        getHdr2().setField(JsHdr2Access.JMSDELIVERYMODE_DATA, value.toByte());
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJmsDeliveryMode");
    }

    /*
     * Set the contents of the JMSExpiration field in the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void setJmsExpiration(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJmsExpiration", Long.valueOf(value));
        getHdr2().setField(JsHdr2Access.JMSEXPIRATION_DATA, Long.valueOf(value));
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJmsExpiration");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.mfp.JsJmsMessage#setJmsDeliveryTime(long)
     */
    @Override
    public void setJmsDeliveryTime(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJmsDeliveryTime", Long.valueOf(value));
        getHdr2().setField(JsHdr2Access.JMSDELIVERYTIME_DATA, Long.valueOf(value));
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJmsDeliveryTime");

    }

    /*
     * Set the contents of the JMSDestination field in the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void setJmsDestination(byte[] value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJmsDestination", value);
        /* There is no need to take a copy because the JMS API layer will never */
        /* change the byte array passed in. */
        getHdr2().setField(JsHdr2Access.JMSDESTINATION_DATA, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJmsDestination");
    }

    /*
     * Set the contents of the JMSReplyTo field in the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void setJmsReplyTo(byte[] value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJmsReplyTo", value);
        /* There is no need to take a copy because the JMS API layer will never */
        /* change the byte array passed in. */
        getHdr2().setField(JsHdr2Access.JMSREPLYTO_DATA, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJmsReplyTo");
    }

    /**
     * Set the type of the message body.
     * This method is only used by message constructors.
     * 
     * @param value The JmsBodyType instance indicating the type of the body
     *            i.e. Null, Bytes, Map, etc.
     */
    final void setBodyType(JmsBodyType value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setBodyType", value);
        setSubtype(value.toByte());
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setBodyType");
    }

    /* ************************************************************************* */
    /* Unchecked Set Methods for header and API meta-data fields */
    /* ************************************************************************* */

    /*
     * Set the contents of the ForwardRoutingPath field in the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void uncheckedSetForwardRoutingPath(List<SIDestinationAddress> value) {
        setFRP(value);
    }

    /*
     * Set the contents of the ReverseRoutingPath field in the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void uncheckedSetReverseRoutingPath(List<SIDestinationAddress> value) {
        setRRP(value);
    }

    /*
     * Set the contents of the topic Discriminator field in the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void uncheckedSetDiscriminator(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "uncheckedSetDiscriminator", value);
        jmo.setField(JsHdrAccess.DISCRIMINATOR, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "uncheckedSetDiscriminator");
    }

    /*
     * Set the value of the TimeToLive field in the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void uncheckedSetTimeToLive(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "uncheckedSetTimeToLive", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.TIMETOLIVE, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "uncheckedSetTimeToLive");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.mfp.JsJmsMessage#uncheckedSetDeliveryDelay(long)
     */
    @Override
    public void uncheckedSetDeliveryDelay(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "uncheckedSetDeliveryDelay", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.DELIVERYDELAY_DATA, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "uncheckedSetDeliveryDelay");

    }

    /*
     * Set the contents of the ReplyDiscriminator field in the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void uncheckedSetReplyDiscriminator(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "uncheckedSetReplyDiscriminator", value);
        if (value != null) {
            getApi().setField(JsApiAccess.REPLYDISCRIMINATOR_VALUE, value);
        }
        else {
            getApi().setChoiceField(JsApiAccess.REPLYDISCRIMINATOR, JsApiAccess.IS_REPLYDISCRIMINATOR_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "uncheckedSetReplyDiscriminator");
    }

    /*
     * Set the value of the ReplyPriority field in the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void uncheckedSetReplyPriority(Integer value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "uncheckedSetReplyPriority", value);
        if (value != null) {
            getApi().setField(JsApiAccess.REPLYPRIORITY_VALUE, value);
        }
        else {
            getApi().setChoiceField(JsApiAccess.REPLYPRIORITY, JsApiAccess.IS_REPLYPRIORITY_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "uncheckedSetReplyPriority");
    }

    /*
     * Set the value of the ReplyReliability field in the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void uncheckedSetReplyReliability(Reliability value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "uncheckedSetReplyReliability", value);
        if (value != null) {
            getApi().setField(JsApiAccess.REPLYRELIABILITY_VALUE, value.toByte());
        }
        else {
            getApi().setChoiceField(JsApiAccess.REPLYRELIABILITY, JsApiAccess.IS_REPLYRELIABILITY_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "uncheckedSetReplyReliability");
    }

    /*
     * Set the value of the ReplyTimeToLive field in the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void uncheckedSetReplyTimeToLive(Long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "uncheckedSetReplyTimeToLive", value);
        if (value != null) {
            getApi().setField(JsApiAccess.REPLYTIMETOLIVE_VALUE, value);
        }
        else {
            getApi().setChoiceField(JsApiAccess.REPLYTIMETOLIVE, JsApiAccess.IS_REPLYTIMETOLIVE_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "uncheckedSetReplyTimeToLive");
    }

    /* ************************************************************************* */
    /* Get Method for JMS Properties */
    /* ************************************************************************* */

    /*
     * Return the JMS property value with the given name as an Object.
     * 
     * This method is used to return in objectified format any property
     * that has been stored in the Message with any of the setXxxxProperty
     * method calls. If a primitive was stored, the Object returned will be
     * the corresponding object - e.g. an int will be returned as an Integer.
     * Null is returned if no property with the given name was set.
     * 
     * Many 'special' properties are stored in specific fields in the message
     * for faster access by Jetstream component code. The code to access the
     * JMSX and JMS_IBM_ special properties has been moved from
     * com.ibm.ws.sib.api.jms.impl.JmsMessageImpl to this method under d325186.1
     * to improve serviceability.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final Object getObjectProperty(String name) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getObjectProperty", name);
        Object value = null;

        /* Some JMSX and JMS_IBM properties need special processing for the JMS */
        /* layer */
        if (SIProperties.JMSXDeliveryCount.equals(name)) {

            int deliveryCount = getJmsxDeliveryCount();
            if (deliveryCount == 0) {
                value = null;
            }
            else {
                value = Integer.valueOf(deliveryCount);
            }

        }

        //special case for JMS_IBM_MsgType
        else if (name.equals(SIProperties.JMS_IBM_MsgType)) {

            // If value wasn't set from feedback, check for a carried value
            if (value == null) {
                if (mayHaveMappedJmsSystemProperties()) {
                    value = getJmsSystemPropertyMap().get(name);
                }
            }
        }

        // ARM Correlator properties
        else if (SIProperties.JMS_IBM_ArmCorrelator.equals(name)) {
            value = getARMCorrelator();
        }

        else if (SIProperties.JMS_TOG_ARM_Correlator.equals(name)) {
            value = getARMCorrelator();
        }

        /* The other JMSX and JMS_IBM_ properties can be obtained correctly */
        /* simply by calling the appropriate super-class methods */
        else if (name.startsWith(JMS_PREFIX)) {
            if (name.startsWith(JMS_IBM_MQMD_PREFIX)) {
                value = getMQMDProperty(name);
            }
            else {
                value = getJMSSystemProperty(name, false);
            }
        }

        /* For property names which do not start with JMS */
        /* Maelstrom's transportVersion has its own field - the rest are in the */
        /* JmsUserPropertyMap */
        else {
            if (name.equals(MfpConstants.PRP_TRANSVER)) {
                value = getTransportVersion();
            }
            else {
                if (mayHaveJmsUserProperties()) {
                    value = getJmsUserPropertyMap().get(name);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getObjectProperty", value);
        return value;
    }

    /* ************************************************************************* */
    /* Set Methods for JMS Properties */
    /* ************************************************************************* */

    /*
     * Set a Java object property value with the given name into the JMS Message.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void setObjectProperty(String name, Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setObjectProperty", new Object[] { name, value });

        /* If the property is either JMSXUserID or JMSXAppID, we set it into the */
        /* message regardless of whether it is null. We know the value is a */
        /* String as the JMS API layer has vetted it. Moved code under d325186.1 */
        if (SIProperties.JMSXAppID.equals(name)) {
            setJmsxAppId((String) value);
        }
        else if (SIProperties.JMSXUserID.equals(name)) {
            setUserid((String) value);
        }
        /* Ditto for JMS_IBM_ArmCorrelator & JMS_TOG_ARM_Correlator */
        else if (SIProperties.JMS_IBM_ArmCorrelator.equals(name)) {
            setARMCorrelator((String) value);
        }
        else if (SIProperties.JMS_TOG_ARM_Correlator.equals(name)) {
            setARMCorrelator((String) value);
        }

        /* else, if the value is not null, set the Property */
        else if (value != null) {
            setNonNullProperty(name, value);
        }

        /* Otherwise, remove it */
        else {

            /* Maelstrom's transportVersion has its own field - the rest are in the */
            /* JmsPropertyMap */
            if (!name.startsWith(JMS_PREFIX)) {
                if (name.equals(MfpConstants.PRP_TRANSVER)) {
                    clearTransportVersion();
                }
                else {
                    if (mayHaveJmsUserProperties()) {
                        getJmsUserPropertyMap().remove(name);
                    }
                }
            }
            else {
                if (name.startsWith(JMS_IBM_MQMD_PREFIX)) {
                    deleteMQMDProperty(name);
                }
                else {
                    if (mayHaveMappedJmsSystemProperties()) {
                        getJmsSystemPropertyMap().remove(name); // 234893
                    }
                    // JMS_IBM_Character_Set and JMS_IBM_Encoding need to be cleared in the
                    // message itself too.                                          d395685
                    if (name.equals(SIProperties.JMS_IBM_Character_Set)) {
                        setCcsid(null);
                    }
                    else if (name.equals(SIProperties.JMS_IBM_Encoding)) {
                        setEncoding(null);
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setObjectProperty");
    }

    /*
     * Set a non-null Object into the appropriate Property map.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     * 
     * If the property starts with JMS it is put into the SystemProperty map,
     * otherwise it is put into the JMSUSerProperty map.
     * The only occasion where the JMS API affects a User property of a
     * non-JMS type is when a property of the same name is set to a JMS type.
     * In that case we have to delete the corresponding property from the other
     * map.
     */
    @Override
    public final void setNonNullProperty(String name, Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setNonNullProperty", new Object[] { name, value });

        // For 'user' properties.....
        if (!name.startsWith(JMS_PREFIX)) {

            /* Maelstrom's transportVersion has its own field - the other user ones */
            /* are in the JmsPropertyMap */
            if (name.equals(MfpConstants.PRP_TRANSVER)) {
                setTransportVersion(value);
            }
            else {
                getJmsUserPropertyMap().put(name, value);
            }

            /* Remove it from the other Property Map if it is there */
            if (mayHaveOtherUserProperties()) {
                getOtherUserPropertyMap().remove(name);
            }
        }

        // For JMS_IBM_ properties.....
        else {
            if (name.startsWith(JMS_IBM_MQMD_PREFIX)) {
                setMQMDProperty(name, value);
            } else {
                getJmsSystemPropertyMap().put(name, value);
                // JMS_IBM_Character_Set and JMS_IBM_Encoding need to be stashed in the
                // message itself too.                                          d395685
                if (name.equals(SIProperties.JMS_IBM_Character_Set)) {
                    setCcsid(value);
                }
                else if (name.equals(SIProperties.JMS_IBM_Encoding)) {
                    setEncoding(value);
                }
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setNonNullProperty");
    }

    /* ************************************************************************* */
    /* Set Methods for calculated property value(s) */
    /* ************************************************************************* */

    /*
     * Set the JMSXAppId value to a compact byte representation.
     * This method is to be used by other Jetstream components.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void setJmsxAppId(Byte value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJmsxAppId", value);
        getHdr2().setField(JsHdr2Access.XAPPID_COMPACT, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJmsxAppId");
    }

    /* ************************************************************************* */
    /* Miscellaneous Methods for JMS Properties */
    /* ************************************************************************* */

    /*
     * Clear all the JMS Properties from the Message.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final void clearProperties() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearProperties");

        if (mayHaveJmsUserProperties()) {
            getJmsUserPropertyMap().clear();
        }
        if (mayHaveMappedJmsSystemProperties()) {
            getJmsSystemPropertyMap().clear();
        }
        clearSmokeAndMirrorsProperties();
        if (hasMQMDPropertiesSet()) {
            getMQMDSetPropertiesMap().clear();
        }

        // Clear the super-classes cached lengths, but NOT anything pertaining to the payload
        super.clearCachedLengths();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearProperties");
    }

    /**
     * Return a Set of just the non-smoke-and-mirrors property names
     * 
     * @return A set containing the names of all the non-smoke-and-mirrors properties
     */
    private final Set<String> getNonSmokeAndMirrorsPropertyNameSet() {

        /* Get the names of all the properties in the JMS Property Maps */
        /* We need a copy so that we can add extra items, and so can the caller */
        Set<String> names = new HashSet<String>();

        // Add the names for the two flavours of properties, without creating lists
        // and maps unnecessarily.
        if (mayHaveJmsUserProperties()) {
            names.addAll(getJmsUserPropertyMap().keySet());
        }
        if (mayHaveMappedJmsSystemProperties()) {
            names.addAll(getJmsSystemPropertyMap().keySet());
        }

        // The MQMD properties may be in the JmsSystemPropertyMap AND the MQMSetPropertiesMap
        // however, the Set will cater for this as it doesn't allow duplicates.
        if (hasMQMDPropertiesSet()) {
            names.addAll(getMQMDSetPropertiesMap().keySet());
        }
        return names;
    }

    /*
     * Return a Set containing all of the property names.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final Set<String> getPropertyNameSet() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPropertyNameSet");

        /* Get the names of the real properties in the JMS Property Maps */
        Set<String> names = getNonSmokeAndMirrorsPropertyNameSet();

        /* Maelstrom's transportVersion may be in the specific variant field */
        if (isTransportVersionSet())
            names.add(MfpConstants.PRP_TRANSVER);

        /* JMSXAppId */
        if (getHdr2().getChoiceField(JsHdr2Access.XAPPID) != JsHdr2Access.IS_XAPPID_EMPTY) {
            names.add(SIProperties.JMSXAppID);
        }

        /* JMSXUserID */
        if (getUserid() != null) {
            names.add(SIProperties.JMSXUserID);
        }

        /* JMSXDeliveryCount is considered set if the message has been sent */
        if (isSent()) {
            names.add(SIProperties.JMSXDeliveryCount);
        }

        /* ARM Correlator */
        if (getARMCorrelator() != null) {
            names.add(SIProperties.JMS_IBM_ArmCorrelator);
            names.add(SIProperties.JMS_TOG_ARM_Correlator);
        }

        /* If any Exception field is set, all must be */
        if (getHdr2().getChoiceField(JsHdr2Access.EXCEPTION) != JsHdr2Access.IS_EXCEPTION_EMPTY) {
            names.add(SIProperties.JMS_IBM_ExceptionReason);
            names.add(SIProperties.JMS_IBM_ExceptionTimestamp);
            names.add(SIProperties.JMS_IBM_ExceptionMessage);
            /* ... but JMS_IBM_ExceptionProblemDestination could be set to null */
            if (getExceptionProblemDestination() != null) {
                names.add(SIProperties.JMS_IBM_ExceptionProblemDestination);
            }
            /* ... but JMS_IBM_ExceptionSubscription could also be set to null */
            if (getExceptionProblemSubscription() != null) {
                names.add(SIProperties.JMS_IBM_ExceptionProblemSubscription);
            }
        }

        /* If the SystemMessageId has been set, JMS_IBM_System_MessageID exists */
        if (getSystemMessageId() != null) {
            names.add(SIProperties.JMS_IBM_System_MessageID);
        }

        /* JMS_IBM_Feedback */
        if ((getApi().getChoiceField(JsApiAccess.REPORTFEEDBACK) != JsApiAccess.IS_REPORTFEEDBACK_UNSET) ||
            (getApi().getChoiceField(JsApiAccess.REPORTFEEDBACKINT) != JsApiAccess.IS_REPORTFEEDBACKINT_UNSET)) {
            names.add(SIProperties.JMS_IBM_Feedback);
        }

        /* JMS_IBM_Report_Xxxxxx */
        if (getHdr2().getChoiceField(JsHdr2Access.REPORTEXPIRY) != JsHdr2Access.IS_REPORTEXPIRY_UNSET) {
            names.add(SIProperties.JMS_IBM_Report_Expiration);
        }
        if (getHdr2().getChoiceField(JsHdr2Access.REPORTCOD) != JsHdr2Access.IS_REPORTCOD_UNSET) {
            names.add(SIProperties.JMS_IBM_Report_COD);
        }
        if (getHdr2().getChoiceField(JsHdr2Access.REPORTCOA) != JsHdr2Access.IS_REPORTCOA_UNSET) {
            names.add(SIProperties.JMS_IBM_Report_COA);
        }
        if (getApi().getChoiceField(JsApiAccess.REPORTEXCEPTION) != JsApiAccess.IS_REPORTEXCEPTION_UNSET) {
            names.add(SIProperties.JMS_IBM_Report_Exception);
        }
        if (getApi().getChoiceField(JsApiAccess.REPORTPAN) != JsApiAccess.IS_REPORTPAN_UNSET) {
            names.add(SIProperties.JMS_IBM_Report_PAN);
        }
        if (getApi().getChoiceField(JsApiAccess.REPORTNAN) != JsApiAccess.IS_REPORTNAN_UNSET) {
            names.add(SIProperties.JMS_IBM_Report_NAN);
        }
        if (getApi().getChoiceField(JsApiAccess.REPORTPASSMSGID) != JsApiAccess.IS_REPORTPASSMSGID_UNSET) {
            names.add(SIProperties.JMS_IBM_Report_Pass_Msg_ID);
        }
        if (getApi().getChoiceField(JsApiAccess.REPORTPASSCORRELID) != JsApiAccess.IS_REPORTPASSCORRELID_UNSET) {
            names.add(SIProperties.JMS_IBM_Report_Pass_Correl_ID);
        }
        if (getApi().getChoiceField(JsApiAccess.REPORTDISCARDMSG) != JsApiAccess.IS_REPORTDISCARDMSG_UNSET) {
            names.add(SIProperties.JMS_IBM_Report_Discard_Msg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getPropertyNameSet", names);
        return names;

    }

    /*
     * Return a boolean indicating whether a property with the given name has been set.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public final boolean propertyExists(String name) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "propertyExists", name);

        boolean result = false;

        /* Got to check Maelstrom's transportVersion first as performance for it */
        /* is critical. */
        if (name.equals(MfpConstants.PRP_TRANSVER) && isTransportVersionSet()) {
            result = true;
        }

        /* Next try the user property map as the most likely */
        else if ((mayHaveJmsUserProperties()) && (getJmsUserPropertyMap().containsKey(name))) {
            result = true;
        }

        /* If name starts with JMS it may be a smoke-and-mirrors property */
        else if (name.startsWith(JMS_PREFIX)) {

            /* If it is a JMSX property.... */
            if (name.charAt(JMS_LENGTH) == JMSX_EXTRA_PREFIX) {
                int count = name.length() - JMSX_LENGTH;

                if (name.regionMatches(JMSX_LENGTH, SIProperties.JMSXDeliveryCount, JMSX_LENGTH, count)) {
                    if (isSent())
                        result = true;
                }
                else if (name.regionMatches(JMSX_LENGTH, SIProperties.JMSXAppID, JMSX_LENGTH, count)) {
                    if (getHdr2().getChoiceField(JsHdr2Access.XAPPID) != JsHdr2Access.IS_XAPPID_EMPTY)
                        result = true;
                }
                else if (name.regionMatches(JMSX_LENGTH, SIProperties.JMSXUserID, JMSX_LENGTH, count)) {
                    if (getUserid() != null)
                        result = true;
                }
                /* The other supported JMSX properties just live in the system map */
                else if (mayHaveMappedJmsSystemProperties()) {
                    result = getJmsSystemPropertyMap().containsKey(name);
                }

            }
            /* All the remaining header properties start with JMS_IBM_ */
            else if (name.startsWith(JMS_IBM_EXTRA_PREFIX, JMS_LENGTH)) {
                int count;

                /* First check for the Report ones */
                if (name.regionMatches(JMS_IBM_LENGTH, REPORT, 0, REPORT_LENGTH)) {

                    count = name.length() - REPORT_OFFSET;

                    if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_Expiration, REPORT_OFFSET, count)) {
                        if (getHdr2().getChoiceField(JsHdr2Access.REPORTEXPIRY) != JsHdr2Access.IS_REPORTEXPIRY_UNSET)
                            result = true;
                    }
                    else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_COA, REPORT_OFFSET, count)) {
                        if (getHdr2().getChoiceField(JsHdr2Access.REPORTCOA) != JsHdr2Access.IS_REPORTCOA_UNSET)
                            result = true;
                    }
                    else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_COD, REPORT_OFFSET, count)) {
                        if (getHdr2().getChoiceField(JsHdr2Access.REPORTCOD) != JsHdr2Access.IS_REPORTCOD_UNSET)
                            result = true;
                    }
                    else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_Exception, REPORT_OFFSET, count)) {
                        if (getApi().getChoiceField(JsApiAccess.REPORTEXCEPTION) != JsApiAccess.IS_REPORTEXCEPTION_UNSET)
                            result = true;
                    }
                    else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_PAN, REPORT_OFFSET, count)) {
                        if (getApi().getChoiceField(JsApiAccess.REPORTPAN) != JsApiAccess.IS_REPORTPAN_UNSET)
                            result = true;
                    }
                    else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_NAN, REPORT_OFFSET, count)) {
                        if (getApi().getChoiceField(JsApiAccess.REPORTNAN) != JsApiAccess.IS_REPORTNAN_UNSET)
                            result = true;
                    }
                    else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_Pass_Msg_ID, REPORT_OFFSET, count)) {
                        if (getApi().getChoiceField(JsApiAccess.REPORTPASSMSGID) != JsApiAccess.IS_REPORTPASSMSGID_UNSET)
                            result = true;
                    }
                    else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_Pass_Correl_ID, REPORT_OFFSET, count)) {
                        if (getApi().getChoiceField(JsApiAccess.REPORTPASSCORRELID) != JsApiAccess.IS_REPORTPASSCORRELID_UNSET)
                            result = true;
                    }
                    else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_Discard_Msg, REPORT_OFFSET, count)) {
                        if (getApi().getChoiceField(JsApiAccess.REPORTDISCARDMSG) != JsApiAccess.IS_REPORTDISCARDMSG_UNSET)
                            result = true;
                    }

                }
                else {
                    /* Then try the other smoke-and-mirrors ones */
                    count = name.length() - JMS_IBM_LENGTH;

                    if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_ExceptionReason, JMS_IBM_LENGTH, count)) {
                        if (getHdr2().getChoiceField(JsHdr2Access.EXCEPTION) != JsHdr2Access.IS_EXCEPTION_EMPTY)
                            result = true;
                    }
                    else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_ExceptionTimestamp, JMS_IBM_LENGTH, count)) {
                        if (getHdr2().getChoiceField(JsHdr2Access.EXCEPTION) != JsHdr2Access.IS_EXCEPTION_EMPTY)
                            result = true;
                    }
                    else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_ExceptionMessage, JMS_IBM_LENGTH, count)) {
                        if (getHdr2().getChoiceField(JsHdr2Access.EXCEPTION) != JsHdr2Access.IS_EXCEPTION_EMPTY)
                            result = true;
                    }
                    else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_ExceptionProblemDestination, JMS_IBM_LENGTH, count)) {
                        if (getExceptionProblemDestination() != null)
                            result = true;
                    }
                    else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_ExceptionProblemSubscription, JMS_IBM_LENGTH, count)) {
                        if (getExceptionProblemSubscription() != null)
                            result = true;
                    }
                    else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_Feedback, JMS_IBM_LENGTH, count)) {
                        if ((getApi().getChoiceField(JsApiAccess.REPORTFEEDBACK) != JsApiAccess.IS_REPORTFEEDBACK_UNSET) ||
                            (getApi().getChoiceField(JsApiAccess.REPORTFEEDBACKINT) != JsApiAccess.IS_REPORTFEEDBACKINT_UNSET))
                            result = true;
                    }
                    else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_System_MessageID, JMS_IBM_LENGTH, count)) {
                        if (getSystemMessageId() != null)
                            result = true;
                    }
                    else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_ArmCorrelator, JMS_IBM_LENGTH, count)) {
                        if (getARMCorrelator() != null)
                            result = true;
                    }

                    /* The other supported JMS_IBM_ properties just live in one (or both) of the maps */
                    else {
                        if (mayHaveMappedJmsSystemProperties()) {
                            result = getJmsSystemPropertyMap().containsKey(name);
                        }
                        if (result == false) {
                            result = getMQMDSetPropertiesMap().containsKey(name);
                        }
                    }
                } // end of else of if Report
            } // end of if JMS_IBM_

            // JMS_TOG_ARM_Correlator
            else if (name.equals(SIProperties.JMS_TOG_ARM_Correlator)) {
                if (getARMCorrelator() != null)
                    result = true;
            }

        } // end of if JMS

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "propertyExists", Boolean.valueOf(result));
        return result;

    }

    /**
     * getJMSXGroupSeq
     * Return the value of the JMSXGroupSeq property if it exists.
     * We can return it as object, as that is all JMS API wants. Actually it only
     * really cares whether it exists, but we'll return Object rather than
     * changing the callng code unnecessarily.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public Object getJMSXGroupSeq() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSXGroupSeq");
        Object result = null;

        if (mayHaveMappedJmsSystemProperties()) {
            result = getObjectProperty(SIProperties.JMSXGroupSeq);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSXGroupSeq", result);
        return result;
    }

    /* ************************************************************************* */
    /* Methods for obtaining encoders */
    /* ************************************************************************* */

    /*
     * Obtain a WebMessageEncoder for writing to the Web client.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    @Override
    public WebJsMessageEncoder getWebEncoder() {
        return new WebJsJmsMessageEncoderImpl(this);
    }

    /* ************************************************************************* */
    /* Misc Package and Private Methods */
    /* ************************************************************************* */

    /**
     * Return the name of the concrete implementation class encoded into bytes
     * using UTF8. SIB0112b.mfp.2
     * 
     * @return byte[] The name of the implementation class encoded into bytes.
     */
    @Override
    byte[] getFlattenedClassName() {
        return flattenedClassName;
    }

}
