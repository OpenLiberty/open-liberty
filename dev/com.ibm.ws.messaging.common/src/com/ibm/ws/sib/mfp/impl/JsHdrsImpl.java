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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MessageType;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.ProducerType;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.schema.JsApiAccess;
import com.ibm.ws.sib.mfp.schema.JsHdr2Access;
import com.ibm.ws.sib.mfp.schema.JsHdrAccess;
import com.ibm.ws.sib.mfp.schema.JsPayloadAccess;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIMessageHandle;

/**
 * JsHdrsImpl extends JsMessageImpl and implements the methods from the
 * JsMessage interface which access or modify header fields.
 * <p>
 * It is extended by JsMessageImpl which implements the remainder of the
 * JsMessage interface.
 */
class JsHdrsImpl extends MessageImpl {

    private final static long serialVersionUID = 1L;

    private static final TraceComponent tc = SibTr.register(JsHdrsImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
    private static final TraceNLS exceptionMessages = TraceNLS.getTraceNLS(MfpConstants.EXCEPTION_MSG_BUNDLE);

    /* Ensure we register all the JMF schemas needed to process these messages */
    static {
        SchemaManager.ensureInitialized(); // d380323
    }

    /* *************************************************************************** */
    /* Transient values - some must be written to the message in updateDataFields */
    /* but others are only cached for frequent gets, with write-through */
    /*
     * occuring immediately on a set.
     * /* ***************************************************************************
     */

    // messageWaitTime must be written back to the message at updateDataFields
    // as setMessageWaitTime() only updates the cached value.
    private transient Long cachedMessageWaitTime = null;

    // The reliability is only cached to speed up getting it (which MP does umpteen times).
    // There is no need to write it back, as any set will write straight to the message
    // as well as setting the cached value.
    private transient Reliability cachedReliability = null;

    // The priority is only cached to speed up getting it (which MP does umpteen times).
    // There is no need to write it back, as any set will write straight to the message
    // as well as setting the cached value.
    private transient Integer cachedPriority = null;

    // The messageHandle is only cached to speed up getting it (which MP does umpteen times).
    // There is no need to write it back, as any set will write straight to the message
    // as well as clearing the cached value.
    private transient SIMessageHandle cachedMessageHandle = null;

    // deliveryCount must NOT be written back to the message as it is truly transient.
    // It is set by the Processor code when a message is obtained from the MessageStore,
    // and is only used for calculating the SI_RedeliveredCount, JMSRedivered and JMSXDeliveryCount
    // values for Selectors. The 'real' redeliveredCount field is updated before a consumer
    // is given the message, so this field is then redundant.
    // This field needs package level access as it is used by mfp.impl subclasses.
    transient int deliveryCount = 0;

    /* ************************************************************************* */
    /* Constructors */
    /* ************************************************************************* */

    /**
     * Constructor for a new Jetstream message.
     * 
     * This constructor should never be used explicitly.
     * It is only to be used implicitly by the sub-classes' no-parameter constructors.
     * The method must not actually do anything.
     */
    JsHdrsImpl() {}

    /**
     * Constructor for a new message of given root type.
     * 
     * @param flag Flag to distinguish different construction reasons.
     * 
     * @exception MessageDecodeFailedException Thrown if such a message can not be created
     */
    JsHdrsImpl(int flag) throws MessageDecodeFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JsHdrsImpl", Integer.valueOf(flag));

        setJmo(new JsMsgObject(JsHdrAccess.schema, JsPayloadAccess.schema));

        /* Set the Hdr2 message part */
        jmo.setPart(JsHdrAccess.HDR2, JsHdr2Access.schema);

        /* Default to no optional fields being included. */

        getHdr2().setChoiceField(JsHdr2Access.XAPPID, JsHdr2Access.IS_XAPPID_EMPTY);
        getHdr2().setChoiceField(JsHdr2Access.REQUESTMETRICS, JsHdr2Access.IS_REQUESTMETRICS_UNSET);

        if (flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ) {
            getHdr2().setChoiceField(JsHdr2Access.ROUTINGDESTINATION, JsHdr2Access.IS_ROUTINGDESTINATION_EMPTY);
            getHdr2().setChoiceField(JsHdr2Access.REPORTEXPIRY, JsHdr2Access.IS_REPORTEXPIRY_UNSET);
            getHdr2().setChoiceField(JsHdr2Access.REPORTCOD, JsHdr2Access.IS_REPORTCOD_UNSET);
            getHdr2().setChoiceField(JsHdr2Access.REPORTCOA, JsHdr2Access.IS_REPORTCOA_UNSET);
            getHdr2().setChoiceField(JsHdr2Access.AUDITSESSIONID, JsHdr2Access.IS_AUDITSESSIONID_EMPTY);
        }

        getHdr2().setChoiceField(JsHdr2Access.GUARANTEED, JsHdr2Access.IS_GUARANTEED_EMPTY);
        getHdr2().setChoiceField(JsHdr2Access.GUARANTEEDXBUS, JsHdr2Access.IS_GUARANTEEDXBUS_EMPTY);
        getHdr2().setChoiceField(JsHdr2Access.GUARANTEEDVALUE, JsHdr2Access.IS_GUARANTEEDVALUE_EMPTY);
        getHdr2().setChoiceField(JsHdr2Access.GUARANTEEDREMOTEBROWSE, JsHdr2Access.IS_GUARANTEEDREMOTEBROWSE_EMPTY);
        getHdr2().setChoiceField(JsHdr2Access.GUARANTEEDREMOTEGET, JsHdr2Access.IS_GUARANTEEDREMOTEGET_EMPTY);

        getHdr2().setChoiceField(JsHdr2Access.EXCEPTION, JsHdr2Access.IS_EXCEPTION_EMPTY);

        getHdr2().setChoiceField(JsHdr2Access.MESSAGECONTROLCLASSIFICATION, JsHdr2Access.IS_MESSAGECONTROLCLASSIFICATION_EMPTY);

        // We can skip these fields for an inbound MQ message, as any message with the constructor flag
        // set to INBOUND_MQ will be a JMS message and the fields will be set appropriately by the
        // MQJsMessageFactory implementation.
        if (flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ) {
            getHdr2().setChoiceField(JsHdr2Access.JMSDELIVERYMODE, JsHdr2Access.IS_JMSDELIVERYMODE_EMPTY);
            getHdr2().setChoiceField(JsHdr2Access.JMSEXPIRATION, JsHdr2Access.IS_JMSEXPIRATION_EMPTY);
            getHdr2().setChoiceField(JsHdr2Access.JMSDESTINATION, JsHdr2Access.IS_JMSDESTINATION_EMPTY);
            getHdr2().setChoiceField(JsHdr2Access.JMSREPLYTO, JsHdr2Access.IS_JMSREPLYTO_EMPTY);
            getHdr2().setChoiceField(JsHdr2Access.JMSTYPE, JsHdr2Access.IS_JMSTYPE_EMPTY);
            getHdr2().setChoiceField(JsHdr2Access.TRANSPORTVERSION, JsHdr2Access.IS_TRANSPORTVERSION_EMPTY);
            getHdr2().setChoiceField(JsHdr2Access.FINGERPRINTS, JsHdr2Access.IS_FINGERPRINTS_EMPTY);
            getHdr2().setChoiceField(JsHdr2Access.JMSDELIVERYTIME, JsHdr2Access.IS_JMSDELIVERYTIME_EMPTY);
        }

        // We can skip anything to do with the API and the Payload part for an inbound MQ message
        // as the MQJsMessageFactory and subclasses will set them to the apppropriate Encapsulations
        // and values as necessary.
        if (flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ) {
            jmo.setChoiceField(JsHdrAccess.API, JsHdrAccess.IS_API_EMPTY);
            jmo.getPayloadPart().setField(JsPayloadAccess.FORMAT, null);
            jmo.getPayloadPart().setChoiceField(JsPayloadAccess.PAYLOAD, JsPayloadAccess.IS_PAYLOAD_EMPTY);
            jmo.getPayloadPart().setChoiceField(JsPayloadAccess.CCSID, JsPayloadAccess.IS_CCSID_EMPTY); // d395685
            jmo.getPayloadPart().setChoiceField(JsPayloadAccess.ENCODING, JsPayloadAccess.IS_ENCODING_EMPTY); // d395685
        }

        /* Set all the non-optional header fields. */

        // We can skip these for any sort of inbound MQ message
        if ((flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ) && (flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ_BROKER)) {
            setForwardRoutingPath(null);
            setReverseRoutingPath(null);
        }

        setDiscriminator(null);

        // We can skip these fields for an inbound MQ message as the MQJsMessageFactory always sets them */
        if (flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ) {
            getHdr2().setIntField(JsHdr2Access.PRIORITY, -1); // Have to set directly as -1 is 'invalid'.
            setReliability(Reliability.NONE);
            setTimeToLive(0);
            setTimestamp(-1);
            setRedeliveredCount(0);
            getHdr2().setChoiceField(JsHdr2Access.DELIVERYDELAY, JsHdr2Access.IS_DELIVERYDELAY_EMPTY);
        }

        getHdr2().setLongField(JsHdr2Access.MESSAGEWAITTIME, 0L);
        jmo.setLongField(JsHdrAccess.ARRIVALTIMESTAMP, 0);
        jmo.setField(JsHdrAccess.SYSTEMMESSAGESOURCEUUID, null);
        jmo.setLongField(JsHdrAccess.SYSTEMMESSAGEVALUE, 0);
        setBus(null);
        setSecurityUserid(null);
        setSecurityUseridSentBySystem(false);

        /* Set the flags field directly - it needs to be primed before using the */
        /* individual set methods. */
        getHdr2().setField(JsHdr2Access.FLAGS, Byte.valueOf((byte) 0));

        // Set the MQMD Properties map to empty, even if it is coming from MQ. It only
        // holds ones which are explicitly set, NOT stuff that arrives in an MQMD.
        getHdr2().setChoiceField(JsHdr2Access.MQMDPROPERTIES, JsHdr2Access.IS_MQMDPROPERTIES_EMPTY);

        //Set the Xct Correlation ID to empty
        getHdr2().setChoiceField(JsHdr2Access.XCT_CORRELATION_ID, JsHdr2Access.IS_XCT_CORRELATION_ID_EMPTY);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JsHdrsImpl");
    }

    /**
     * Constructor for an inbound message.
     * 
     * @param inJmo The JsMsgObject representing the inbound message
     */
    JsHdrsImpl(JsMsgObject inJmo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JsHdrsImpl", "Inbound JMO");
        setJmo(inJmo);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JsHdrsImpl");
    }

    /* ************************************************************************* */
    /* Methods for checking for optional data */
    /* ************************************************************************* */

    /*
     * Return a boolean indicating whether the message header contains
     * Guaranteed Delivery Remote Browse fields.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final boolean isGuaranteedRemoteBrowse() {
        return (getHdr2().getChoiceField(JsHdr2Access.GUARANTEEDREMOTEBROWSE) == JsHdr2Access.IS_GUARANTEEDREMOTEBROWSE_SET);
    }

    /*
     * Return a boolean indicating whether the AuditSessionId field has been set
     * in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public boolean isAuditSessionIdSet() {
        return (getHdr2().getChoiceField(JsHdr2Access.AUDITSESSIONID) == JsHdr2Access.IS_AUDITSESSIONID_DATA);
    }

    /* ************************************************************************* */
    /* Methods for checking whether Routing Paths are empty */
    /* ************************************************************************* */

    /*
     * Return a boolean indicating whether the ForwardRoutingPath is empty.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public boolean isForwardRoutingPathEmpty() {
        /* If it is never been set, or been set to null, localonly will be null. */
        if (getHdr2().getField(JsHdr2Access.FORWARDROUTINGPATHLOCALONLY) == null) {
            return true;
        }
        /* Otherwise it is possible for it to be 0 length. */
        else if (((byte[]) getHdr2().getField(JsHdr2Access.FORWARDROUTINGPATHLOCALONLY)).length == 0) {
            return true;
        }
        else
            return false;
    }

    /*
     * Return a boolean indicating whether the ReverseRoutingPath is empty.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public boolean isReverseRoutingPathEmpty() {
        /* If it is never been set, or been set to null, localonly will be null. */
        if (getHdr2().getField(JsHdr2Access.REVERSEROUTINGPATHLOCALONLY) == null) {
            return true;
        }
        /* Otherwise it is possible for it to be 0 length. */
        else if (((byte[]) getHdr2().getField(JsHdr2Access.REVERSEROUTINGPATHLOCALONLY)).length == 0) {
            return true;
        }
        else
            return false;
    }

    /* ************************************************************************* */
    /* Method for checking whether the Message has been sent */
    /* ************************************************************************* */

    /*
     * Return a boolean indicating whether the Message is deemed sent by the
     * Message Processor component.
     * 
     * @return boolean true is the message should be considered 'sent'
     */
    boolean isSent() {
        /* systemMessageSourceUuid is null initially, but will be set by MP if */
        /* the message has been 'sent' */
        if (jmo.getField(JsHdrAccess.SYSTEMMESSAGESOURCEUUID) == null) {
            return false;
        }
        else {
            return true;
        }
    }

    /* ************************************************************************* */
    /* Method for determining whether or not the message is an API message */
    /* ************************************************************************* */

    /*
     * Method to distinguish between API message instances and System message instances.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public boolean isApiMessage() {
        MessageType mType = getJsMessageType();
        return (mType == MessageType.JMS || mType == MessageType.SDO);
    }

    /*
     * Method to distinguish between Control messages and other message instances.
     * 
     * Javadoc description supplied by the AbstractMessage interface.
     */
    public boolean isControlMessage() {
        // Always return false, as only implementations of ControlMessage should return true.
        return false;
    }

    /* ************************************************************************* */
    /* Methods for loop detection (fingerprinting) */
    /* ************************************************************************* */

    /**
     * The returns the ordered list of the Fingerprints of the MEs and/or MQ Clusters
     * the message has visited, or null if the message is not a Publish or has not
     * visited any MEs/Clusters.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public List<String> getFingerprints() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getFingerprints");
        List<String> ids = (List<String>) getHdr2().getField(JsHdr2Access.FINGERPRINTS_RFPLIST_ID);
        if (ids != null) {
            // Don't return the actual JSVaryingListImpl contained in the message.
            ids = new ArrayList<String>(ids);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getFingerprints", ids);
        return ids;
    }

    /*
     * The method adds an MEUuid fingerprint to the end of the list of fingerprints.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public void addFingerprint(SIBUuid8 meUuid) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addFingerprint", meUuid);

        String meUuidString = meUuid.toString();

        // If the fingerprint list is empty, we need to create an empty list and add
        // the given MEUuid to it.
        if (getHdr2().getChoiceField(JsHdr2Access.FINGERPRINTS) == JsHdr2Access.IS_FINGERPRINTS_EMPTY) {
            List<String> ids = new ArrayList<String>();
            ids.add(meUuidString);
            getHdr2().setField(JsHdr2Access.FINGERPRINTS_RFPLIST_ID, ids);
        }

        // If there is a list, we need to extract it. There is no point cacheing the
        // list as this will only ever be called once per message in an ME.
        else {
            List<String> ids = (List<String>) getHdr2().getField(JsHdr2Access.FINGERPRINTS_RFPLIST_ID);

            // A JSList does not implement add(), because JMF Lists can't be added to
            // 'in situ', so we have to 'clone' it, add to the clone & set the clone into the message.
            List<String> newIds = new ArrayList<String>(ids);
            newIds.add(meUuidString);
            getHdr2().setField(JsHdr2Access.FINGERPRINTS_RFPLIST_ID, newIds);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addFingerprint");
    }

    /*
     * Clear the fingerprint list from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public void clearFingerprints() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearFingerprints");
        getHdr2().setChoiceField(JsHdr2Access.FINGERPRINTS, JsHdr2Access.IS_FINGERPRINTS_EMPTY);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearFingerprints");
    }

    /* ************************************************************************* */
    /* Get Methods */
    /* ************************************************************************* */

    /*
     * Get the contents of the ForwardRoutingPath field from the message header.
     * The List returned is a copy of the header field, so no updates to it
     * affect the Message header itself.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public final List<SIDestinationAddress> getForwardRoutingPath() {
        List<String> fNames = (List<String>) getHdr2().getField(JsHdr2Access.FORWARDROUTINGPATH_DESTINATIONNAME);
        List<byte[]> fMEs = (List<byte[]>) getHdr2().getField(JsHdr2Access.FORWARDROUTINGPATH_MEID);
        byte[] fLos = (byte[]) getHdr2().getField(JsHdr2Access.FORWARDROUTINGPATHLOCALONLY);
        List<String> fBuses = (List<String>) getHdr2().getField(JsHdr2Access.FORWARDROUTINGPATH_BUSNAME);
        return new RoutingPathList(fNames, fLos, fMEs, fBuses);
    }

    /*
     * Get the contents of the ReverseRoutingPath field from the message header
     * The List returned is a copy of the header field, so no updates to it
     * affect the Message header itself.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public final List<SIDestinationAddress> getReverseRoutingPath() {
        List<String> fNames = (List<String>) getHdr2().getField(JsHdr2Access.REVERSEROUTINGPATH_DESTINATIONNAME);
        List<byte[]> fMEs = (List<byte[]>) getHdr2().getField(JsHdr2Access.REVERSEROUTINGPATH_MEID);
        byte[] fLos = (byte[]) getHdr2().getField(JsHdr2Access.REVERSEROUTINGPATHLOCALONLY);
        List<String> fBuses = (List<String>) getHdr2().getField(JsHdr2Access.REVERSEROUTINGPATH_BUSNAME);
        return new RoutingPathList(fNames, fLos, fMEs, fBuses);
    }

    /*
     * Get the contents of the topic Discriminator field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public final String getDiscriminator() {
        return (String) jmo.getField(JsHdrAccess.DISCRIMINATOR);
    }

    /*
     * Get the value of the Priority field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public final Integer getPriority() {
        // If the transient is not set, get the int value from the message and cache it.
        if (cachedPriority == null) {
            cachedPriority = (Integer) getHdr2().getField(JsHdr2Access.PRIORITY);
        }
        // Return the (possibly newly) cached value
        return cachedPriority;
    }

    /*
     * Get the value of the Reliability field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public final Reliability getReliability() {
        // If the transient is not set, get the int value from the message then
        // obtain the corresponding Reliability instance and cache it.
        if (cachedReliability == null) {
            Byte rType = (Byte) getHdr2().getField(JsHdr2Access.RELIABILITY);
            cachedReliability = Reliability.getReliability(rType);
        }
        // Return the (possibly newly) cached value
        return cachedReliability;
    }

    /*
     * Get the value of the TimeToLive field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public final Long getTimeToLive() {
        return (Long) getHdr2().getField(JsHdr2Access.TIMETOLIVE);
    }

    /*
     * Get the value of the DeliveryDelay field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public final Long getDeliveryDelay() {

        /*
         * deliveryDelay can be null when you have version incompatibility.
         * Suppose older JMS client/ME sends a message , it will not have deliverydelay
         * field in the MFP.Hence in that case it is ideal to return the default
         * deliveryDelay value i.e 0
         */
        if (getHdr2().getChoiceField(JsHdr2Access.DELIVERYDELAY) == JsHdr2Access.IS_DELIVERYDELAY_EMPTY) {
            return 0L;
        }
        else {
            return getHdr2().getLongField(JsHdr2Access.DELIVERYDELAY_DATA);
        }
    }

    /*
     * Get the remaining time in milliseconds before the message expires.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public final long getRemainingTimeToLive() {
        long ttl = getHdr2().getLongField(JsHdr2Access.TIMETOLIVE);
        if (ttl > 0) {
            return ttl - getMessageWaitTime().longValue();
        }
        else {
            return -1;
        }
    }

    /*
     * Get the contents of the ReplyDiscriminator field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl.
     * The JsHdrsImpl implementation is just a no-op.
     */
    public String getReplyDiscriminator() {
        return null;
    }

    /*
     * Get the value of the ReplyPriority field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl.
     * The JsHdrsImpl implementation is just a no-op.
     */
    public Integer getReplyPriority() {
        return null;
    }

    /*
     * Get the value of the ReplyReliability field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl.
     * The JsHdrsImpl implementation is just a no-op.
     */
    public Reliability getReplyReliability() {
        return null;
    }

    /*
     * Get the value of the ReplyTimeToLive field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl.
     * The JsHdrsImpl implementation is just a no-op.
     */
    public Long getReplyTimeToLive() {
        return null;
    }

    /*
     * Get the value of the Timestamp field from the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final Long getTimestamp() {
        return (Long) getHdr2().getField(JsHdr2Access.TIMESTAMP);
    }

    /*
     * Get the value of the MessageWaitTime field from the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final Long getMessageWaitTime() {
        /* If the transient has never been set, get the value in the message */
        if (cachedMessageWaitTime == null) {
            cachedMessageWaitTime = (Long) getHdr2().getField(JsHdr2Access.MESSAGEWAITTIME);
        }
        return cachedMessageWaitTime;
    }

    /*
     * Get the value of the CurrentMEArrivalTimestamp field from the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final Long getCurrentMEArrivalTimestamp() {
        return (Long) jmo.getField(JsHdrAccess.ARRIVALTIMESTAMP);
    }

    /*
     * Get the value of the RedeliveredCount field from the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final Integer getRedeliveredCount() {
        return (Integer) getHdr2().getField(JsHdr2Access.REDELIVEREDCOUNT);
    }

    /*
     * Get the unique system message id from the message header.
     * 
     * NOTE: WPS have a requirement that the returned value has a length of
     * no more than 255. Currently it is substantially shorter than that, but
     * it needs to be borne in mind if anyone has any grandiose ideas for the
     * SystemMessageId in the future.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public String getSystemMessageId() {
        SIBUuid8 u = getSystemMessageSourceUuid();
        if (u != null) {
            StringBuffer buff = new StringBuffer(u.toString());
            buff.append(MfpConstants.MESSAGE_HANDLE_SEPARATOR);
            buff.append(Long.toString(getSystemMessageValue()));
            return buff.toString();
        }
        else {
            return null;
        }
    }

    /*
     * Get the value of the SystemMessageSourceUuid field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public SIBUuid8 getSystemMessageSourceUuid() {
        byte[] b = (byte[]) jmo.getField(JsHdrAccess.SYSTEMMESSAGESOURCEUUID);
        if (b != null)
            return new SIBUuid8(b);
        return null;
    }

    /*
     * Get the value of the SystemMessageValue field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public long getSystemMessageValue() {
        return jmo.getLongField(JsHdrAccess.SYSTEMMESSAGEVALUE);
    }

    /**
     * Get the message handle which uniquely identifies this message.
     * 
     * @return An SIMessageHandle which identifies this message.
     */
    public SIMessageHandle getMessageHandle() {
        // If the transient is not set, build the handle from the values in the message and cache it.
        if (cachedMessageHandle == null) {
            byte[] b = (byte[]) jmo.getField(JsHdrAccess.SYSTEMMESSAGESOURCEUUID);
            if (b != null) {
                cachedMessageHandle = new JsMessageHandleImpl(new SIBUuid8(b), (Long) jmo.getField(JsHdrAccess.SYSTEMMESSAGEVALUE));
            }
            else {
                cachedMessageHandle = new JsMessageHandleImpl(null, (Long) jmo.getField(JsHdrAccess.SYSTEMMESSAGEVALUE));
            }
        }
        // Return the (possibly newly) cached value
        return cachedMessageHandle;
    }

    /*
     * Get the contents of the Bus field from the message header
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final String getBus() {
        return (String) getHdr2().getField(JsHdr2Access.BUS);
    }

    /*
     * Get the contents of the SecurityUserid field from the message header
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final String getSecurityUserid() {
        return (String) jmo.getField(JsHdrAccess.SECURITYUSERID);
    }

    /*
     * Indicate whether the message with a SecurityUserid field was sent by
     * a system user.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final boolean isSecurityUseridSentBySystem() {
        return jmo.getBooleanField(JsHdrAccess.SECURITYSENTBYSYSTEM);
    }

    /*
     * Get the value of the ProducerType field from the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final ProducerType getProducerType() {
        /* Get the int value and get the corresponding ProducerType to return */
        Byte pType = (Byte) getHdr2().getField(JsHdr2Access.PRODUCERTYPE);
        return ProducerType.getProducerType(pType);
    }

    /*
     * Get the value of the JsMessageType from the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final MessageType getJsMessageType() {
        /* Get the int value and get the corresponding MessageType to return */
        Byte mType = (Byte) jmo.getField(JsHdrAccess.MESSAGETYPE);
        return MessageType.getMessageType(mType);
    }

    /*
     * Determine whether the message has been mediated when the mediation point and
     * queueing point are distributed.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final boolean isMediated() {
        return getFlagValue(MEDIATED_FLAG);
    }

    /*
     * Determine whether an RFH2 is allowed if encoding for MQ
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final boolean isMQRFH2Allowed() {
        return getFlagValue(MQRFH2ALLOWED_FLAG);
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Request Metrics */
    /* ------------------------------------------------------------------------ */

    /*
     * Get the RM correlator from the header
     * Javadoc description supplied by JsMessage interface.
     */
    public final String getRMCorrelator() {
        return (String) getHdr2().getField(JsHdr2Access.REQUESTMETRICS_CORRELATOR_RM);
    }

    /*
     * Get the ARM correlator from the header
     * Javadoc description supplied by JsMessage interface.
     */
    public final String getARMCorrelator() {
        return (String) getHdr2().getField(JsHdr2Access.REQUESTMETRICS_CORRELATOR_ARM);
    }

    /* ------------------------------------------------------------------------ */
    /* Optional AuditSessionId */
    /* ------------------------------------------------------------------------ */

    /*
     * Get the value of the AuditSessionId from the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public String getAuditSessionId() {
        return (String) getHdr2().getField(JsHdr2Access.AUDITSESSIONID_DATA);
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Routing Destination */
    /* ------------------------------------------------------------------------ */

    /*
     * Get the optional RoutingDestination field from the message header.
     * The JsDeststinationAddress returned is a copy of the header field,
     * so no updates to it affect the Message header itself.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public JsDestinationAddress getRoutingDestination() {
        String name = (String) getHdr2().getField(JsHdr2Access.ROUTINGDESTINATION_VALUE_NAME);
        /* If the name is null, the RoutingDestination has never been set. */
        if (name != null) {
            byte[] b = (byte[]) getHdr2().getField(JsHdr2Access.ROUTINGDESTINATION_VALUE_MEID);
            return new JsDestinationAddressImpl(name
                            , false
                            , (b == null) ? null : new SIBUuid8(b)
                            , (String) getHdr2().getField(JsHdr2Access.ROUTINGDESTINATION_VALUE_BUSNAME));
        }
        else {
            return null;
        }
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Header-level Report information */
    /* ------------------------------------------------------------------------ */

    /*
     * Get the Report Expiry field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public final Byte getReportExpiry() {
        return (Byte) getHdr2().getField(JsHdr2Access.REPORTEXPIRY_VALUE);
    }

    /*
     * Get the Report COD field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public final Byte getReportCOD() {
        return (Byte) getHdr2().getField(JsHdr2Access.REPORTCOD_VALUE);
    }

    /*
     * Get the Report COA field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public final Byte getReportCOA() {
        return (Byte) getHdr2().getField(JsHdr2Access.REPORTCOA_VALUE);
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Guaranteed Delivery information */
    /* ------------------------------------------------------------------------ */

    /*
     * Get the source Messaging Engine that originated the message.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final SIBUuid8 getGuaranteedSourceMessagingEngineUUID() {
        byte[] b = (byte[]) getHdr2().getField(JsHdr2Access.GUARANTEED_SET_SOURCEMEUUID);
        if (b != null)
            return new SIBUuid8(b);
        return null;
    }

    /*
     * Get the next Messaging Engine that will store the message.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final SIBUuid8 getGuaranteedTargetMessagingEngineUUID() {
        byte[] b = (byte[]) getHdr2().getField(JsHdr2Access.GUARANTEED_SET_TARGETMEUUID);
        if (b != null)
            return new SIBUuid8(b);
        return null;
    }

    /*
     * Get the identity of the destination definition (not localisation)
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final SIBUuid12 getGuaranteedTargetDestinationDefinitionUUID() {
        byte[] b = (byte[]) getHdr2().getField(JsHdr2Access.GUARANTEED_SET_TARGETDESTDEFUUID);
        if (b != null)
            return new SIBUuid12(b);
        return null;
    }

    /*
     * Get the unique StreamId used by the flush protocol to determine
     * whether a stream is active or flushed.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final SIBUuid12 getGuaranteedStreamUUID() {
        byte[] b = (byte[]) getHdr2().getField(JsHdr2Access.GUARANTEED_SET_STREAMUUID);
        if (b != null)
            return new SIBUuid12(b);
        return null;
    }

    /**
     * Get the unique GatheringTargetUUID.
     * 
     * @return The unique GatheringTarget Id.
     *         If Guaranteed Delivery information is not included in the
     *         message, this field will not be set.
     */
    public final SIBUuid12 getGuaranteedGatheringTargetUUID() {
        byte[] b = (byte[]) getHdr2().getField(JsHdr2Access.GUARANTEED_SET_GATHERINGTARGETUUID_VALUE);
        if (b != null)
            return new SIBUuid12(b);
        return null;
    }

    /*
     * Get the class of protocol
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final ProtocolType getGuaranteedProtocolType() {
        Byte value = (Byte) getHdr2().getField(JsHdr2Access.GUARANTEED_SET_PROTOCOLTYPE);
        return ProtocolType.getProtocolType(value);
    }

    /*
     * Get the version of the guaranteed delivery protocol used by this message.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public byte getGuaranteedProtocolVersion() {
        Byte value = (Byte) getHdr2().getField(JsHdr2Access.GUARANTEED_SET_PROTOCOLVERSION);
        return (value == null) ? 0 : value.byteValue();
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Guaranteed Delivery Cross-Bus information */
    /* ------------------------------------------------------------------------ */

    /*
     * Get the LinkName for cross-bus guaranteed delivery from the message.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final String getGuaranteedCrossBusLinkName() {
        return (String) getHdr2().getField(JsHdr2Access.GUARANTEEDXBUS_SET_LINKNAME);
    }

    /*
     * Get the SourceBusUUID for cross-bus guaranteed delivery from the message.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final SIBUuid8 getGuaranteedCrossBusSourceBusUUID() {
        byte[] b = (byte[]) getHdr2().getField(JsHdr2Access.GUARANTEEDXBUS_SET_SOURCEBUSUUID);
        if (b != null)
            return new SIBUuid8(b);
        return null;
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Guaranteed Delivery Value information */
    /* ------------------------------------------------------------------------ */

    /*
     * Get the Guaranteed Delivery Value Start Tick value from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final long getGuaranteedValueStartTick() {
        return getHdr2().getLongField(JsHdr2Access.GUARANTEEDVALUE_SET_STARTTICK);
    }

    /*
     * Get the Guaranteed Delivery Value End Tick value from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final long getGuaranteedValueEndTick() {
        return getHdr2().getLongField(JsHdr2Access.GUARANTEEDVALUE_SET_ENDTICK);
    }

    /*
     * Get the Guaranteed Delivery Value Value Tick value from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final long getGuaranteedValueValueTick() {
        return getHdr2().getLongField(JsHdr2Access.GUARANTEEDVALUE_SET_VALUETICK);
    }

    /*
     * Get the Guaranteed Delivery Value CompletedPrefix from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final long getGuaranteedValueCompletedPrefix() {
        return getHdr2().getLongField(JsHdr2Access.GUARANTEEDVALUE_SET_COMPLETEDPREFIX);
    }

    /*
     * Get the Guaranteed Delivery Value RequestedOnly value from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final boolean getGuaranteedValueRequestedOnly() {
        return getHdr2().getBooleanField(JsHdr2Access.GUARANTEEDVALUE_SET_REQUESTEDONLY);
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Guaranteed Remote Browse information */
    /* ------------------------------------------------------------------------ */

    /*
     * Get the Guaranteed Delivery Remote Browse ID value from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final long getGuaranteedRemoteBrowseID() {
        return getHdr2().getLongField(JsHdr2Access.GUARANTEEDREMOTEBROWSE_SET_BROWSEID);
    }

    /*
     * Get the Guaranteed Delivery Remote Browse Sequence Number from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final long getGuaranteedRemoteBrowseSequenceNumber() {
        return getHdr2().getLongField(JsHdr2Access.GUARANTEEDREMOTEBROWSE_SET_SEQNUM);
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Guaranteed Remote Get information */
    /* ------------------------------------------------------------------------ */

    /*
     * Get the Guaranteed Delivery Remote Get WaitTime value from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final long getGuaranteedRemoteGetWaitTime() {
        return getHdr2().getLongField(JsHdr2Access.GUARANTEEDREMOTEGET_SET_WAITTIME);
    }

    /*
     * Get the Guaranteed Delivery Remote Get Prev Tick value from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final long getGuaranteedRemoteGetPrevTick() {
        return getHdr2().getLongField(JsHdr2Access.GUARANTEEDREMOTEGET_SET_PREVTICK);
    }

    /*
     * Get the Guaranteed Delivery Remote Get Start Tick value from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final long getGuaranteedRemoteGetStartTick() {
        return getHdr2().getLongField(JsHdr2Access.GUARANTEEDREMOTEGET_SET_STARTTICK);
    }

    /*
     * Get the Guaranteed Delivery Remote Get Value Tick value from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final long getGuaranteedRemoteGetValueTick() {
        return getHdr2().getLongField(JsHdr2Access.GUARANTEEDREMOTEGET_SET_VALUETICK);
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Exception information */
    /* ------------------------------------------------------------------------ */

    /*
     * Get the value of the ExceptionReason from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public final Integer getExceptionReason() {
        return (Integer) getHdr2().getField(JsHdr2Access.EXCEPTION_DETAIL_REASON);
    }

    /*
     * Get the value of the ExceptionInserts from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public final String[] getExceptionInserts() {
        String[] arr = null;
        List<String> l = (List<String>) getHdr2().getField(JsHdr2Access.EXCEPTION_DETAIL_INSERTS);
        if (l != null) {
            try {
                arr = l.toArray(new String[l.size()]);
            } catch (ArrayStoreException e) {
                FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsHdrsImpl.getExceptionInserts", "jsm1400");
                /* This should mever happen as only Strings can be written into the field */
            }
        }
        return arr;
    }

    /*
     * Get the value of the ExceptionTimestamp from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public final Long getExceptionTimestamp() {
        return (Long) getHdr2().getField(JsHdr2Access.EXCEPTION_DETAIL_TIMESTAMP);
    }

    /*
     * Get the value of the Exception Problem Destination from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public String getExceptionProblemDestination() {
        return (String) getHdr2().getField(JsHdr2Access.EXCEPTION_DETAIL_PROBLEMDESTINATION_DATA);
    }

    /*
     * Get the value of the Exception Problem Subscription from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public String getExceptionProblemSubscription() {
        return (String) getHdr2().getField(JsHdr2Access.EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION_DATA);
    }

    /*
     * Get the Exception Message for the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final String getExceptionMessage() {
        /* If the Exception information is set, return the translated message */
        if (getHdr2().getChoiceField(JsHdr2Access.EXCEPTION) != JsHdr2Access.IS_EXCEPTION_EMPTY) {
            return exceptionMessages.getFormattedMessage(MfpConstants.EXCEPTION_MESSAGE_KEY_PREFIX + getExceptionReason().toString()
                                                         , getExceptionInserts()
                                                         , "Failed to deliver message, rc=" + getExceptionReason() + ". Inserts: " + Arrays.asList(getExceptionInserts())
                            );
        }
        /* If no, just return null. */
        else {
            return null;
        }
    }

    /* ************************************************************************* */
    /* Set Methods */
    /* ************************************************************************* */

    /*
     * Set the contents of the ForwardRoutingPath field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl
     */
    public void setForwardRoutingPath(List<SIDestinationAddress> value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setForwardRoutingPath");
        setFRP(value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setForwardRoutingPath");
    }

    /*
     * Set the contents of the ReverseRoutingPath field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl
     */
    public void setReverseRoutingPath(List<SIDestinationAddress> value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReverseRoutingPath");
        setRRP(value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReverseRoutingPath");
    }

    /*
     * Set the contents of the topic Discriminator field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl
     */
    public void setDiscriminator(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDiscriminator", value);
        jmo.setField(JsHdrAccess.DISCRIMINATOR, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDiscriminator");
    }

    /*
     * Set the value of the Priority field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl
     */
    public void setPriority(int value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setPriority", Integer.valueOf(value));
        /* Set the cached value */
        cachedPriority = Integer.valueOf(value);
        // Set the value into the message itself
        getHdr2().setIntField(JsHdr2Access.PRIORITY, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setPriority");
    }

    /*
     * Set the value of the Reliability field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl
     */
    public void setReliability(Reliability value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReliability", value);
        /* Set the cached value */
        cachedReliability = value;
        /* Get the int value of the Reliability instance and set it in the field */
        getHdr2().setField(JsHdr2Access.RELIABILITY, value.toByte());
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReliability");
    }

    /*
     * Set the value of the TimeToLive field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl
     */
    public void setTimeToLive(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTimeToLive", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.TIMETOLIVE, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTimeToLive");
    }

    /*
     * Set the value of the DeliveryDelay field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl
     */
    public void setDeliveryDelay(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDeliveryDelay", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.DELIVERYDELAY_DATA, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDeliveryDelay");
    }

    /*
     * Set the remaining time in milliseconds before the message expires.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public final void setRemainingTimeToLive(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setRemainingTimeToLive", Long.valueOf(value));
        if (value < 0) {
            getHdr2().setLongField(JsHdr2Access.TIMETOLIVE, 0);
        }
        else if (value == 0) {
            getHdr2().setLongField(JsHdr2Access.TIMETOLIVE, 1);
        }
        else {
            long ttl = getMessageWaitTime().longValue() + value;
            /* Call setTimeToLive so that bounds checking takes place for an API message. */
            setTimeToLive(ttl);
        }
        wasTimeToLiveChanged = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setRemainingTimeToLive");
    }

    /*
     * Set the contents of the ReplyDiscriminator field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl.
     * The JsHdrsImpl implementation is just a no-op.
     */
    public void setReplyDiscriminator(String value) {}

    /*
     * Set the value of the ReplyPriority field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl.
     * The JsHdrsImpl implementation is just a no-op.
     */
    public void setReplyPriority(int value) {}

    /*
     * Set the value of the ReplyReliability field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl.
     * The JsHdrsImpl implementation is just a no-op.
     */
    public void setReplyReliability(Reliability value) {}

    /*
     * Set the value of the ReplyTimeToLive field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl.
     * The JsHdrsImpl implementation is just a no-op.
     */
    public void setReplyTimeToLive(long value) {}

    /*
     * Clear the four ReplyXxxx fields in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     * This method is not final as it is overridden in JsApiMessageImpl.
     * The JsHdrsImpl implementation is just a no-op.
     */
    public void clearReplyFields() {}

    /*
     * Set the value of the Timestamp field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setTimestamp(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTimestamp", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.TIMESTAMP, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTimestamp");
    }

    /*
     * Set the value of the MessageWaitTime field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setMessageWaitTime(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setMessageWaitTime", Long.valueOf(value));
        /* Just set the transient value - it will be written back later */
        cachedMessageWaitTime = Long.valueOf(value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setMessageWaitTime");
    }

    /*
     * Set the value of the CurrentMEArrivalTimestamp field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setCurrentMEArrivalTimestamp(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCurrentMEArrivalTimestamp", Long.valueOf(value));
        jmo.setLongField(JsHdrAccess.ARRIVALTIMESTAMP, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setCurrentMEArrivalTimestamp");
    }

    /*
     * Set the value of the RedeliverdCount field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setRedeliveredCount(int value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setRedeliveredCount", Integer.valueOf(value));
        getHdr2().setIntField(JsHdr2Access.REDELIVEREDCOUNT, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setRedeliveredCount");
    }

    /*
     * Set the value of the deliveryCount transient which is used by Selectors
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public void setDeliveryCount(int value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDeliveryCount", Integer.valueOf(value));
        deliveryCount = value;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDeliveryCount");
    }

    /*
     * Set the value of the SystemMessageSourceUuid field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public void setSystemMessageSourceUuid(SIBUuid8 value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setSystemMessageSourceUuid", value);
        // Clear the cached MessageHandle
        cachedMessageHandle = null;
        // Set the value into the message itself
        if (value != null) {
            jmo.setField(JsHdrAccess.SYSTEMMESSAGESOURCEUUID, value.toByteArray());
        }
        else {
            jmo.setField(JsHdrAccess.SYSTEMMESSAGESOURCEUUID, null);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setSystemMessageSourceUuid");
    }

    /*
     * Set the value of the SystemMessageValue field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public void setSystemMessageValue(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setSystemMessageValue", Long.valueOf(value));
        // Clear the cached MessageHandle
        cachedMessageHandle = null;
        // Set the value into the message itself
        jmo.setLongField(JsHdrAccess.SYSTEMMESSAGEVALUE, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setSystemMessageValue");
    }

    /*
     * Set the contents of the Bus field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setBus(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setBus", value);
        getHdr2().setField(JsHdr2Access.BUS, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setBus");
    }

    /*
     * Set the contents of the SecurityUserid field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setSecurityUserid(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setSecurityUserid", value);
        jmo.setField(JsHdrAccess.SECURITYUSERID, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setSecurityUserid");
    }

    /*
     * Set whether the message with a SecurityUserid field was sent by
     * a system user.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setSecurityUseridSentBySystem(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setSecurityUseridSentBySystem", Boolean.valueOf(value));
        jmo.setBooleanField(JsHdrAccess.SECURITYSENTBYSYSTEM, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setSecurityUseridSentBySystem");
    }

    /**
     * Set the value of the ProducerType field in the message header.
     * This method is only used by message constructors so is not public final.
     * 
     * @param value The ProducerType representing the Producer Type of the message (e.g. Core, API, TRM)
     */
    final void setProducerType(ProducerType value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProducerType", value);
        /* Get the int value of the ProducerType and set that into the field */
        getHdr2().setField(JsHdr2Access.PRODUCERTYPE, value.toByte());
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setProducerType");
    }

    /**
     * Set the value of the JsMessageType in the message header.
     * This method is only used by message constructors so is not public final.
     * 
     * @param value The MessageType representing the Message Type (e.g. JMS) of the message.
     */
    final void setJsMessageType(MessageType value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJsMessageType", value);
        /* Get the int value of the MessageType and set that into the field */
        jmo.setField(JsHdrAccess.MESSAGETYPE, value.toByte());
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJsMessageType");
    }

    /*
     * Set whether or not the message has been mediated when the mediation point and
     * queueing point are distributed.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final void setMediated(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setMediated", Boolean.valueOf(value));
        setFlagValue(MEDIATED_FLAG, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setMediated");
    }

    /*
     * Set whether or not an RFH2 is allowed if encoding for MQ
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setMQRFH2Allowed(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setMQRFH2Allowed", Boolean.valueOf(value));
        setFlagValue(MQRFH2ALLOWED_FLAG, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setMQRFH2Allowed");
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Request Metrics */
    /* ------------------------------------------------------------------------ */

    /*
     * Set the RM correlator in the header
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setRMCorrelator(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setRMCorrelator", value);
        getHdr2().setField(JsHdr2Access.REQUESTMETRICS_CORRELATOR_RM, value);
        // The ARM and RM fields must be set as a pair, so if ARM appears unset
        // ensure it is null.
        if (getHdr2().getField(JsHdr2Access.REQUESTMETRICS_CORRELATOR_ARM) == null)
            getHdr2().setField(JsHdr2Access.REQUESTMETRICS_CORRELATOR_ARM, null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setRMCorrelator");
    }

    /*
     * Set the ARM correlator inthe header
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setARMCorrelator(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setARMCorrelator", value);
        getHdr2().setField(JsHdr2Access.REQUESTMETRICS_CORRELATOR_ARM, value);
        // The ARM and RM fields must be set as a pair, so if RM appears unset
        // ensure it is null.
        if (getHdr2().getField(JsHdr2Access.REQUESTMETRICS_CORRELATOR_RM) == null)
            getHdr2().setField(JsHdr2Access.REQUESTMETRICS_CORRELATOR_RM, null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setARMCorrelator");
    }

    /* ------------------------------------------------------------------------ */
    /* Optional AuditSessionId */
    /* ------------------------------------------------------------------------ */

    /*
     * Set the optional AuditSessionId in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public void setAuditSessionId(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setAuditSessionId", value);
        getHdr2().setField(JsHdr2Access.AUDITSESSIONID_DATA, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setAuditSessionId");
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Routing Destination */
    /* ------------------------------------------------------------------------ */

    /*
     * Set the optional RoutingDestination field in the message header.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public void setRoutingDestination(JsDestinationAddress value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setRoutingDestination", value);
        if (value != null) {
            getHdr2().setField(JsHdr2Access.ROUTINGDESTINATION_VALUE_NAME, value.getDestinationName());
            if (value.getME() != null) {
                getHdr2().setField(JsHdr2Access.ROUTINGDESTINATION_VALUE_MEID, value.getME().toByteArray());
            }
            else {
                getHdr2().setField(JsHdr2Access.ROUTINGDESTINATION_VALUE_MEID, null);
            }
            getHdr2().setField(JsHdr2Access.ROUTINGDESTINATION_VALUE_BUSNAME, value.getBusName());
        }
        else {
            getHdr2().setChoiceField(JsHdr2Access.ROUTINGDESTINATION, JsHdr2Access.IS_ROUTINGDESTINATION_EMPTY);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setRoutingDestination");
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Header-level Report information */
    /* ------------------------------------------------------------------------ */

    /*
     * Set the Report Expiry field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public final void setReportExpiry(Byte value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReportExpiry", value);
        if (value != null) {
            getHdr2().setField(JsHdr2Access.REPORTEXPIRY_VALUE, value);
        }
        else {
            getHdr2().setChoiceField(JsHdr2Access.REPORTEXPIRY, JsHdr2Access.IS_REPORTEXPIRY_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReportExpiry");
    }

    /*
     * Set the Report COD field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public final void setReportCOD(Byte value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReportCOD", value);
        if (value != null) {
            getHdr2().setField(JsHdr2Access.REPORTCOD_VALUE, value);
        }
        else {
            getHdr2().setChoiceField(JsHdr2Access.REPORTCOD, JsHdr2Access.IS_REPORTCOD_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReportCOD");
    }

    /*
     * Set the Report COA field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public final void setReportCOA(Byte value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReportCOA", value);
        if (value != null) {
            getHdr2().setField(JsHdr2Access.REPORTCOA_VALUE, value);
        }
        else {
            getHdr2().setChoiceField(JsHdr2Access.REPORTCOA, JsHdr2Access.IS_REPORTCOA_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReportCOA");
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Guaranteed Delivery information */
    /* ------------------------------------------------------------------------ */

    /*
     * Set the source Messaging Engine that originated the message.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final void setGuaranteedSourceMessagingEngineUUID(SIBUuid8 value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedSourceMessagingEngineUUID", value);
        if (value != null)
            getHdr2().setField(JsHdr2Access.GUARANTEED_SET_SOURCEMEUUID, value.toByteArray());
        else
            getHdr2().setField(JsHdr2Access.GUARANTEED_SET_SOURCEMEUUID, null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedSourceMessagingEngineUUID");
    }

    /*
     * Set the next Messaging Engine that will store the message.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final void setGuaranteedTargetMessagingEngineUUID(SIBUuid8 value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedTargetMessagingEngineUUID", value);
        if (value != null)
            getHdr2().setField(JsHdr2Access.GUARANTEED_SET_TARGETMEUUID, value.toByteArray());
        else
            getHdr2().setField(JsHdr2Access.GUARANTEED_SET_TARGETMEUUID, null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedTargetMessagingEngineUUID");
    }

    /*
     * Set the identity of the destination definition (not localisation)
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final void setGuaranteedTargetDestinationDefinitionUUID(SIBUuid12 value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedTargetDestinationDefinitionUUID", value);
        if (value != null)
            getHdr2().setField(JsHdr2Access.GUARANTEED_SET_TARGETDESTDEFUUID, value.toByteArray());
        else
            getHdr2().setField(JsHdr2Access.GUARANTEED_SET_TARGETDESTDEFUUID, null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedTargetDestinationDefinitionUUID");
    }

    /*
     * Set the unique StreamId used by the flush protocol to determine
     * whether a stream is active or flushed.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final void setGuaranteedStreamUUID(SIBUuid12 value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedStreamUUID", value);
        if (value != null)
            getHdr2().setField(JsHdr2Access.GUARANTEED_SET_STREAMUUID, value.toByteArray());
        else
            getHdr2().setField(JsHdr2Access.GUARANTEED_SET_STREAMUUID, null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedStreamUUID");
    }

    /**
     * Set the unique GatheringTargetUUID.
     * 
     * @param value The unique GatheringTarget Id.
     */
    public final void setGuaranteedGatheringTargetUUID(SIBUuid12 value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedGatheringTargetUUID", value);
        if (value != null)
            getHdr2().setField(JsHdr2Access.GUARANTEED_SET_GATHERINGTARGETUUID_VALUE, value.toByteArray());
        else
            getHdr2().setField(JsHdr2Access.GUARANTEED_SET_GATHERINGTARGETUUID, JsHdr2Access.IS_GUARANTEED_SET_GATHERINGTARGETUUID_EMPTY);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedGatheringTargetUUID");
    }

    /*
     * Set the class of protocol
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final void setGuaranteedProtocolType(ProtocolType value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedProtocolType", value);
        getHdr2().setField(JsHdr2Access.GUARANTEED_SET_PROTOCOLTYPE, value.toByte());
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedProtocolType");
    }

    /*
     * Set the version of the guaranteed delivery protocol used by this message.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public void setGuaranteedProtocolVersion(byte value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedProtocolVersion", Byte.valueOf(value));
        getHdr2().setField(JsHdr2Access.GUARANTEED_SET_PROTOCOLVERSION, Byte.valueOf(value));
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedProtocolVersion");
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Guaranteed Delivery Cross-Bus information */
    /* ------------------------------------------------------------------------ */

    /*
     * Set the LinkName for cross-bus guaranteed delivery into the message.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final void setGuaranteedCrossBusLinkName(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedCrossBusLinkName", value);
        getHdr2().setField(JsHdr2Access.GUARANTEEDXBUS_SET_LINKNAME, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedCrossBusLinkName");
    }

    /*
     * Set the SourceBusUUID for cross-bus guaranteed delivery into the message.
     * 
     * Javadoc description supplied by CommonMessageHeaders interface.
     */
    public final void setGuaranteedCrossBusSourceBusUUID(SIBUuid8 value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedCrossBusSourceBusUUID", value);
        if (value != null)
            getHdr2().setField(JsHdr2Access.GUARANTEEDXBUS_SET_SOURCEBUSUUID, value.toByteArray());
        else
            getHdr2().setField(JsHdr2Access.GUARANTEEDXBUS_SET_SOURCEBUSUUID, null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedCrossBusSourceBusUUID");
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Guaranteed Delivery Value information */
    /* ------------------------------------------------------------------------ */

    /*
     * Set the Guaranteed Delivery Value Start Tick value in the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setGuaranteedValueStartTick(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedValueStartTick", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.GUARANTEEDVALUE_SET_STARTTICK, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedValueStartTick");
    }

    /*
     * Set the Guaranteed Delivery Value End Tick value in the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setGuaranteedValueEndTick(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedValueEndTick", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.GUARANTEEDVALUE_SET_ENDTICK, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedValueEndTick");
    }

    /*
     * Set the Guaranteed Delivery Value Value Tick value in the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setGuaranteedValueValueTick(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedValueValueTick", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.GUARANTEEDVALUE_SET_VALUETICK, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedValueValueTick");
    }

    /*
     * Set the Guaranteed Delivery Value CompletedPrefix in the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setGuaranteedValueCompletedPrefix(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedValueCompletedPrefix", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.GUARANTEEDVALUE_SET_COMPLETEDPREFIX, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedValueCompletedPrefix");
    }

    /*
     * Set the Guaranteed Delivery Value RequestedOnly value in the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setGuaranteedValueRequestedOnly(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedValueRequestedOnly", Boolean.valueOf(value));
        getHdr2().setBooleanField(JsHdr2Access.GUARANTEEDVALUE_SET_REQUESTEDONLY, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedValueRequestedOnly");
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Guaranteed Remote Browse information */
    /* ------------------------------------------------------------------------ */

    /*
     * Clear the Guaranteed Delivery Remote Browse information in the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public void clearGuaranteedRemoteBrowse() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearGuaranteedRemoteBrowse");
        getHdr2().setChoiceField(JsHdr2Access.GUARANTEEDREMOTEBROWSE, JsHdr2Access.IS_GUARANTEEDREMOTEBROWSE_EMPTY);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearGuaranteedRemoteBrowse");
    }

    /*
     * Set the Guaranteed Delivery Remote Browse ID value in the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setGuaranteedRemoteBrowseID(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedRemoteBrowseID", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.GUARANTEEDREMOTEBROWSE_SET_BROWSEID, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedRemoteBrowseID");
    }

    /*
     * Set the Guaranteed Delivery Remote Browse Sequence Number in the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setGuaranteedRemoteBrowseSequenceNumber(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedRemoteBrowseSequenceNumber", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.GUARANTEEDREMOTEBROWSE_SET_SEQNUM, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedRemoteBrowseSequenceNumber");
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Guaranteed Remote Get information */
    /* ------------------------------------------------------------------------ */

    /*
     * Clear the Guaranteed Delivery Remote Get information in the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public void clearGuaranteedRemoteGet() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearGuaranteedRemoteGet");
        getHdr2().setChoiceField(JsHdr2Access.GUARANTEEDREMOTEGET, JsHdr2Access.IS_GUARANTEEDREMOTEGET_EMPTY);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearGuaranteedRemoteGet");
    }

    /*
     * Set the Guaranteed Delivery Remote Get WaitTime value from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setGuaranteedRemoteGetWaitTime(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedRemoteGetWaitTime", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.GUARANTEEDREMOTEGET_SET_WAITTIME, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedRemoteGetWaitTime");
    }

    /**
     * Set the Guaranteed Delivery Remote Get Prev Tick value from the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setGuaranteedRemoteGetPrevTick(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedRemoteGetPrevTick", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.GUARANTEEDREMOTEGET_SET_PREVTICK, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedRemoteGetPrevTick");
    }

    /*
     * Set the Guaranteed Delivery Remote Get Start Tick value in the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setGuaranteedRemoteGetStartTick(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedRemoteGetStartTick", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.GUARANTEEDREMOTEGET_SET_STARTTICK, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedRemoteGetStartTick");
    }

    /*
     * Set the Guaranteed Delivery Remote Get Value Tick value in the message.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setGuaranteedRemoteGetValueTick(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGuaranteedRemoteGetValueTick", Long.valueOf(value));
        getHdr2().setLongField(JsHdr2Access.GUARANTEEDREMOTEGET_SET_VALUETICK, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGuaranteedRemoteGetValueTick");
    }

    /* ------------------------------------------------------------------------ */
    /* Optional Exception information */
    /* ------------------------------------------------------------------------ */

    /*
     * Set the value of the ExceptionReason in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setExceptionReason(int value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setExceptionReason", Integer.valueOf(value));
        boolean wasEmpty = (getHdr2().getChoiceField(JsHdr2Access.EXCEPTION) == JsHdr2Access.IS_EXCEPTION_EMPTY);
        getHdr2().setIntField(JsHdr2Access.EXCEPTION_DETAIL_REASON, value);
        if (wasEmpty)
        {
            // Need to mark the optional exception fields as empty
            getHdr2().setChoiceField(JsHdr2Access.EXCEPTION_DETAIL_PROBLEMDESTINATION, JsHdr2Access.IS_EXCEPTION_DETAIL_PROBLEMDESTINATION_EMPTY);
            getHdr2().setChoiceField(JsHdr2Access.EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION, JsHdr2Access.IS_EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION_EMPTY);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setExceptionReason");
    }

    /*
     * Set the value of the ExceptionInserts in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setExceptionInserts(String[] values) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setExceptionInserts", values);
        boolean wasEmpty = (getHdr2().getChoiceField(JsHdr2Access.EXCEPTION) == JsHdr2Access.IS_EXCEPTION_EMPTY);
        if (values != null) {
            getHdr2().setField(JsHdr2Access.EXCEPTION_DETAIL_INSERTS, values);
        }
        else {
            getHdr2().setField(JsHdr2Access.EXCEPTION_DETAIL_INSERTS, null);
        }
        if (wasEmpty)
        {
            // Need to mark the optional exception fields as empty
            getHdr2().setChoiceField(JsHdr2Access.EXCEPTION_DETAIL_PROBLEMDESTINATION, JsHdr2Access.IS_EXCEPTION_DETAIL_PROBLEMDESTINATION_EMPTY);
            getHdr2().setChoiceField(JsHdr2Access.EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION, JsHdr2Access.IS_EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION_EMPTY);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setExceptionInserts");
    }

    /*
     * Set the value of the ExceptionTimestamp in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public final void setExceptionTimestamp(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setExceptionTimestamp", Long.valueOf(value));
        boolean wasEmpty = (getHdr2().getChoiceField(JsHdr2Access.EXCEPTION) == JsHdr2Access.IS_EXCEPTION_EMPTY);
        getHdr2().setLongField(JsHdr2Access.EXCEPTION_DETAIL_TIMESTAMP, value);
        if (wasEmpty)
        {
            // Need to mark the optional exception fields as empty
            getHdr2().setChoiceField(JsHdr2Access.EXCEPTION_DETAIL_PROBLEMDESTINATION, JsHdr2Access.IS_EXCEPTION_DETAIL_PROBLEMDESTINATION_EMPTY);
            getHdr2().setChoiceField(JsHdr2Access.EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION, JsHdr2Access.IS_EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION_EMPTY);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setExceptionTimestamp");
    }

    /*
     * Set the value of the Exception Problem Destination in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public void setExceptionProblemDestination(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setExceptionProblemDestination", value);
        boolean wasEmpty = (getHdr2().getChoiceField(JsHdr2Access.EXCEPTION) == JsHdr2Access.IS_EXCEPTION_EMPTY);
        getHdr2().setField(JsHdr2Access.EXCEPTION_DETAIL_PROBLEMDESTINATION_DATA, value);
        if (wasEmpty)
        {
            // Need to mark the other optional exception field as empty
            getHdr2().setChoiceField(JsHdr2Access.EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION, JsHdr2Access.IS_EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION_EMPTY);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setExceptionProblemDestination");
    }

    /*
     * Set the value of the Exception Problem Subscription in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public void setExceptionProblemSubscription(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setExceptionProblemSubscription", value);
        boolean wasEmpty = (getHdr2().getChoiceField(JsHdr2Access.EXCEPTION) == JsHdr2Access.IS_EXCEPTION_EMPTY);
        getHdr2().setField(JsHdr2Access.EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION_DATA, value);
        if (wasEmpty)
        {
            // Need to mark the other optional exception field as empty
            getHdr2().setChoiceField(JsHdr2Access.EXCEPTION_DETAIL_PROBLEMDESTINATION, JsHdr2Access.IS_EXCEPTION_DETAIL_PROBLEMDESTINATION_EMPTY);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setExceptionProblemSubscription");
    }

    /*
     * Clear all of the Exception fields from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public void clearExceptionData() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearExceptionData");
        getHdr2().setChoiceField(JsHdr2Access.EXCEPTION, JsHdr2Access.IS_EXCEPTION_EMPTY);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearExceptionData");
    }

    /* ************************************************************************* */
    /* Dummy Get/Set methods for API Message meta-data fields */
    /* For a non-API message these fields do not exist. JsApiMessageImpl */
    /* contains the functional implementations. */
    /* ************************************************************************* */

    /*
     * Get the contents of the UserId field from the message header.
     * Javadoc description supplied by JsMessage interface.
     */
    public String getApiUserId() {
        return null;
    }

    /*
     * Get the contents of the ApiMessageId field from the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public byte[] getApiMessageIdAsBytes() {
        return null;
    }

    /*
     * Get the contents of the CorrelationId field from the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public byte[] getCorrelationIdAsBytes() {
        return null;
    }

    /*
     * Set the contents of the UserId field int the message header.
     * Javadoc description supplied by JsMessage interface.
     */
    public void setApiUserId(String value) {}

    /*
     * Set the contents of the ApiMessageId field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public void setApiMessageIdAsBytes(byte[] value) {}

    /*
     * Set the contents of the CorrelationId field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public void setCorrelationIdAsBytes(byte[] value) {}

    /* ------------------------------------------------------------------------ */
    /* ConnectionUuid */
    /* ------------------------------------------------------------------------ */

    /*
     * Get the value of the ConnectionUuid field from the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public SIBUuid12 getConnectionUuid() {
        return null;
    }

    /*
     * Set the value of the ConnectionUuid field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    public void setConnectionUuid(SIBUuid12 value) {}

    /* ------------------------------------------------------------------------ */
    /* Optional Report Message information */
    /* ------------------------------------------------------------------------ */

    /*
     * Get the Report Exception field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public Byte getReportException() {
        return null;
    }

    /*
     * Get the Report PAN field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public Boolean getReportPAN() {
        return Boolean.FALSE;
    }

    /*
     * Get the Report NAN field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public Boolean getReportNAN() {
        return Boolean.FALSE;
    }

    /*
     * Get the Report PassMsgId field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public Boolean getReportPassMsgId() {
        return Boolean.FALSE;
    }

    /*
     * Get the Report PassCorrelId field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public Boolean getReportPassCorrelId() {
        return Boolean.FALSE;
    }

    /*
     * Get the Report DiscardMsg field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public Boolean getReportDiscardMsg() {
        return Boolean.FALSE;
    }

    /*
     * Get the Report Feedback field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public Integer getReportFeedback() {
        return null;
    }

    /*
     * Set the Report Exception field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public void setReportException(Byte value) {}

    /*
     * Set the Report PAN field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public void setReportPAN(Boolean value) {}

    /**
     * Set the Report NAN field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public void setReportNAN(Boolean value) {}

    /**
     * Set the Report PassMsgId field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public void setReportPassMsgId(Boolean value) {}

    /**
     * Set the Report PassCorrelId field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public void setReportPassCorrelId(Boolean value) {}

    /**
     * Set the Report DiscardMsg field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public void setReportDiscardMsg(Boolean value) {}

    /**
     * Set the Report Feedback field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    public void setReportFeedback(Integer value) {}

    /* ************************************************************************* */
    /* Dummy methods for clearing API Message properties and payload. */
    /* For a non-API message these methods do nothing. JsApiMessageImpl */
    /* contains the functional implementations. */
    /* ************************************************************************* */

    /*
     * Delete all the Properties from an API Message.
     * 
     * Javadoc description supplied by the JsMessage interface.
     */
    public void clearMessageProperties() {}

    /*
     * Delete the Payload of an API Message.
     * 
     * Javadoc description supplied by the JsMessage interface.
     */
    public void clearMessagePayload() {}

    /* ************************************************************************* */
    /* Package and Private Methods */
    /* ************************************************************************* */

    /**
     * Return the integer value of the field which indicates the subtype of the
     * message.
     * 
     * @return int The int value of the Message SubType field
     */
    final int getSubtype() {
        return ((Byte) jmo.getField(JsHdrAccess.SUBTYPE)).intValue();
    }

    /**
     * Set the Byte field which indicates the subtype of the message.
     * 
     * @param value The Byte value of the Message SubType field
     */
    final void setSubtype(Byte value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setSubtype", value);
        jmo.setField(JsHdrAccess.SUBTYPE, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setSubtype");
    }

    /**
     * Set the Byte field which indicates the subtype of the message.
     * This method is only required until the Control Messages are reworked.
     * 
     * @param value The int value of the Message SubType field
     */
    final void setSubtype(int value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setSubtype", Integer.valueOf(value));
        jmo.setField(JsHdrAccess.SUBTYPE, Byte.valueOf((byte) value));
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setSubtype");
    }

    /* ************************************************************************* */
    /* Methods for determining if the setRemainingTimeToLive was invoked */
    /* ************************************************************************* */

    protected transient boolean wasTimeToLiveChanged = false;

    public void clearWasRemainingTimeToLiveChanged() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearWasRemainingTimeToLiveChanged");
        wasTimeToLiveChanged = false;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearWasRemainingTimeToLiveChanged");
    }

    public boolean wasRemainingTimeToLiveChanged() {
        return wasTimeToLiveChanged;
    }

    /* ************************************************************************* */
    /* Helper methods for writing back transient data */
    /* ************************************************************************* */

    /**
     * Helper method used by the JMO to rewrite any transient data back into the
     * underlying JMF message.
     * Package level visibility as used by the JMO.
     * 
     * @param why The reason why updateDataFields is being called
     */
    @Override
    void updateDataFields(int why) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "updateDataFields");

        super.updateDataFields(why);
        setFlags();
        /* If the cachedMessageWaitTime transient has ever been set, write it back */
        if (cachedMessageWaitTime != null) {
            getHdr2().setField(JsHdr2Access.MESSAGEWAITTIME, cachedMessageWaitTime);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "updateDataFields");
    }

    /**
     * Copies any 'interesting' transient data into the given message copy.
     * 
     * Sub-classes should override this (calling super.copyTransients()) to copy
     * any transient cached data into new copies of messages.
     * 
     * Note that:
     * The part caches must NOT be copied.
     * wasTimeToLiveChanged need not be copied as it is only used during a Mediation
     * deliveryCount should not be copied as it is only a temporary value
     * 
     * @param copy The message copy whose transients should be set to the same as this one.
     */
    void copyTransients(JsHdrsImpl copy) {
        copy.cachedMessageWaitTime = cachedMessageWaitTime;
        copy.cachedReliability = cachedReliability;
        copy.cachedPriority = cachedPriority;
        copy.cachedMessageHandle = cachedMessageHandle;
        copy.flags = flags;
        copy.gotFlags = gotFlags;
    }

    /* ************************************************************************* */
    /* Methods for accessing the message parts described by nested schemas */
    /* ************************************************************************* */

    // The three cached payload parts must only be read and updated while holding
    // the appropriate level of lock. This is necessary because methods such as
    // getCopy() require the cached values to be thrown away and not repopulated
    // until the copy is complete.
    private transient JsMsgPart hdr2 = null;
    private transient JsMsgPart api = null;
    private transient JsMsgPart payload = null;

    /**
     * Get the JsMsgPart which contains the JMF Message described by the
     * JsHdr2 schema.
     * Once obtained, the value is cached, and the cached value is returned until
     * the cache is cleared.
     * 
     * @return JsMsgPart The message part described by the JsHdr2 schema
     */
    synchronized final JsMsgPart getHdr2() {
        if (hdr2 == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "getHdr2 will call getPart");
            hdr2 = jmo.getPart(JsHdrAccess.HDR2, JsHdr2Access.schema);
        }
        return hdr2;
    }

    /**
     * Get the JsMsgPart which contains the JMF Message described by the
     * JsHdr2 schema if it is already fluffed up (i.e. cached).
     * If the part is not already fluffed and cached, just return null as the caller
     * does NOT want to fluff it up.
     * 
     * @return JsMsgPart The message part described by the JsHdr2 schema, or null
     */
    synchronized final JsMsgPart getHdr2IfFluffed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (hdr2 == null)
                SibTr.debug(this, tc, "getHdr2IfFluffed returning null");
        }
        return hdr2;
    }

    /**
     * Get the JsMsgPart which contains the JMF Message described by the
     * JsApi schema.
     * Once obtained, the value is cached, and the cached value is returned until
     * the cache is cleared.
     * 
     * @return JsMsgPart The message part described by the JsApi schema
     */
    synchronized final JsMsgPart getApi() {
        if (api == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "getApi will call getPart");
            api = jmo.getPart(JsHdrAccess.API_DATA, JsApiAccess.schema);
        }
        return api;
    }

    /**
     * Get the JsMsgPart which contains the JMF Message described by the
     * JsApi schema if it is already fluffed up (i.e. cached).
     * If the part is not already fluffed and cached, just return null as the caller
     * does NOT want to fluff it up.
     * 
     * @return JsMsgPart The message part described by the JsApi schema, or null
     */
    synchronized final JsMsgPart getApiIfFluffed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (api == null)
                SibTr.debug(this, tc, "getApiIfFluffed returning null");
        }
        return api;
    }

    /**
     * Get the JsMsgPart which contains the JMF Message described by the
     * schema for the data field in the JsPayload Schema.
     * Once obtained, the value is cached, and the cached value is returned until
     * the cache is cleared.
     * 
     * @param schema The schema to use to decode the data field of the JsPayload.
     * 
     * @return JsMsgPart The message part for the JsPayload schema's data field
     */
    synchronized final JsMsgPart getPayload(JMFSchema schema) {
        if (payload == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "getPayload will call getPart");
            payload = jmo.getPayloadPart().getPart(JsPayloadAccess.PAYLOAD_DATA, schema);
        }
        return payload;
    }

    /**
     * Get the JsMsgPart which contains the JMF Message described by the
     * schema for the data field in the JsPayload Schema, if it has already been
     * fluffed up and cached.
     * If the part is not already fluffed and cached, just return null as the caller
     * does NOT want to fluff it up.
     * 
     * @return JsMsgPart The message part for the JsPayload schema's data field, or null
     */
    synchronized final JsMsgPart getPayloadIfFluffed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (payload == null)
                SibTr.debug(this, tc, "getPayloadIfFluffed returning null");
        }
        return payload;
    }

    /**
     * Clear the cached references to other message parts.
     */
    @Override
    synchronized final void clearPartCaches() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearPartCaches");
        hdr2 = api = payload = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearPartCaches");
    }

    /* ************************************************************************* */
    /* Package and private methods for single bit flags */
    /* ************************************************************************* */

    /* Each flag is represented by one bit of the byte */
    private static final byte MEDIATED_FLAG = (byte) 0x01;
    private static final byte MQRFH2ALLOWED_FLAG = (byte) 0x02;

    private transient boolean gotFlags = false;
    private transient byte flags = 0;

    /**
     * Get the boolean 'value' of one of the flags in the FLAGS field of th message.
     * 
     * @param flagBit A byte with a single bit set on, marking the position
     *            of the required flag.
     * 
     * @return boolean true if the required flag is set, otherwise false
     */
    private final boolean getFlagValue(byte flagBit) {
        if ((getFlags() & flagBit) == 0) {
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * Set the boolean 'value' of one of the flags in the FLAGS field of th message.
     * 
     * @param flagBit A byte with a single bit set on, marking the position
     *            of the required flag.
     * @param value true if the required flag is to be set on, otherwise false
     */
    private final void setFlagValue(byte flagBit, boolean value) {
        if (value) {
            flags = (byte) (getFlags() | flagBit);
        }
        else {
            flags = (byte) (getFlags() & (~flagBit));
        }
    }

    /**
     * Get the byte value of the FLAGS field from the cached variable or directly
     * from the message.
     * 
     * @return byte The value of the (possibly cached) FLAGS field.
     */
    private final byte getFlags() {
        if (!gotFlags) {
            flags = ((Byte) getHdr2().getField(JsHdr2Access.FLAGS)).byteValue();
            gotFlags = true;
        }
        return flags;
    }

    /**
     * Set the chached value of the FLAGS field back into the message.
     */
    private final void setFlags() {
        if (gotFlags) {
            getHdr2().setField(JsHdr2Access.FLAGS, Byte.valueOf(flags));
            gotFlags = false;
        }
    }

    /* ************************************************************************* */
    /* Package and private methods for Routing Path support */
    /* ************************************************************************* */

    /*
     * Set the contents of the ForwardRoutingPath field in the message header.
     * The field in the message must be set to a copy so that any updates to the
     * original list are not reflected in the message.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    final void setFRP(List<SIDestinationAddress> value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setFRP", value);

        if (value == null) {
            getHdr2().setField(JsHdr2Access.FORWARDROUTINGPATH_DESTINATIONNAME, null);
            getHdr2().setField(JsHdr2Access.FORWARDROUTINGPATH_MEID, null);
            getHdr2().setField(JsHdr2Access.FORWARDROUTINGPATHLOCALONLY, null);
            getHdr2().setField(JsHdr2Access.FORWARDROUTINGPATH_BUSNAME, null);
        } else {
            // The List provided should either be one of our RoutingPathList objects or
            // a standard Java List containing (non-null) SIDestinationAddress elements.
            // If it's the latter we just create a RoutingPathList to wrap it.
            RoutingPathList rp;
            if (value instanceof RoutingPathList)
                rp = (RoutingPathList) value;
            else
                rp = new RoutingPathList(value);

            getHdr2().setField(JsHdr2Access.FORWARDROUTINGPATH_DESTINATIONNAME, rp.getNames());
            getHdr2().setField(JsHdr2Access.FORWARDROUTINGPATH_MEID, rp.getMEs());
            getHdr2().setField(JsHdr2Access.FORWARDROUTINGPATHLOCALONLY, rp.getLocalOnlys());
            getHdr2().setField(JsHdr2Access.FORWARDROUTINGPATH_BUSNAME, rp.getBusNames());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setFRP");
    }

    /*
     * Set the contents of the ReverseRoutingPath field in the message header.
     * The field in the message must be set to a copy so that any updates to the
     * original list are not reflected in the message.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    final void setRRP(List<SIDestinationAddress> value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setRRP", value);

        if (value == null) {
            getHdr2().setField(JsHdr2Access.REVERSEROUTINGPATH_DESTINATIONNAME, null);
            getHdr2().setField(JsHdr2Access.REVERSEROUTINGPATH_MEID, null);
            getHdr2().setField(JsHdr2Access.REVERSEROUTINGPATHLOCALONLY, null);
            getHdr2().setField(JsHdr2Access.REVERSEROUTINGPATH_BUSNAME, null);
        } else {
            // The List provided should either be one of our RoutingPathList objects or
            // a standard Java List containing (non-null) SIDestinationAddress elements.
            // If it's the latter we just create a RoutingPathList to wrap it.
            RoutingPathList rp;
            if (value instanceof RoutingPathList)
                rp = (RoutingPathList) value;
            else
                rp = new RoutingPathList(value);

            getHdr2().setField(JsHdr2Access.REVERSEROUTINGPATH_DESTINATIONNAME, rp.getNames());
            getHdr2().setField(JsHdr2Access.REVERSEROUTINGPATH_MEID, rp.getMEs());
            getHdr2().setField(JsHdr2Access.REVERSEROUTINGPATHLOCALONLY, rp.getLocalOnlys());
            getHdr2().setField(JsHdr2Access.REVERSEROUTINGPATH_BUSNAME, rp.getBusNames());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setRRP");
    }

    /*
     * Gets the XCT Correlation ID from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public String getXctCorrelationID() {
        return (String) getHdr2().getField(JsHdr2Access.XCT_CORRELATION_ID_DATA);
    }

    /*
     * Sets the XCT Correlation ID into the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    public void setXctCorrelationID(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setXctCorrelationID", value);

        if (value != null) {
            getHdr2().setField(JsHdr2Access.XCT_CORRELATION_ID_DATA, value);
        }
        else {
            getHdr2().setChoiceField(JsHdr2Access.XCT_CORRELATION_ID, JsHdr2Access.IS_XCT_CORRELATION_ID_EMPTY);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setXctCorrelationID");
    }

}
