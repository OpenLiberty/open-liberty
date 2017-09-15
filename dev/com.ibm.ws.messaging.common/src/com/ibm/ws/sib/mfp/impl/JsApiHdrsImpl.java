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

import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.PersistenceType;
import com.ibm.ws.sib.mfp.ProducerType;
import com.ibm.ws.sib.mfp.schema.JsApiAccess;
import com.ibm.ws.sib.mfp.schema.JsHdr2Access;
import com.ibm.ws.sib.mfp.schema.JsHdrAccess;
import com.ibm.ws.sib.mfp.schema.JsPayloadAccess;
import com.ibm.ws.sib.utils.HexString;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * JsApiHdrsImpl extends JsMessageImpl and implements the methods from the
 * JsApiMessage interface which access or modify header fields.
 * <p>
 * The JsMessageImpl instance contains the JsMsgObject which is the
 * internal object which represents the API Message.
 * The implementation classes for all the specialised API messages extend
 * JsApiMessageImpl, either directly or indirectly, as well as implementing
 * their specialised interface.
 */
abstract class JsApiHdrsImpl extends JsMessageImpl {

    private static TraceComponent tc = SibTr.register(JsApiHdrsImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
    private static TraceNLS nls = TraceNLS.getTraceNLS(MfpConstants.MSG_BUNDLE);

    private final static long serialVersionUID = 1L;

    private final static String ID_STRING = "ID:";

    // A guess at the contribution of the API headers to the fluffed size of a message.
    private final static int FLUFFED_API_HDRS_SIZE;

    static {
        // Make a guess at the contribution of the API section. This size is
        // reasonably constant, with the exception of the message properties
        // which are dealt with by a subclass.
        // The actual data is small, but the fluffed size needs to take account of
        // the JSMessageImpl itself, its wrapping JsMsgPart & its cache (45 references).
        // There are potentially a lot of little primitive wrapper classes, but they will
        // all be the pre-canned singletons (Byte, Boolean, small Integers). There
        // also about 4 Strings & byte arrays which need accounting for.
        // For MQ PSC & PSCR containing messages, there will be many more fields.
        FLUFFED_API_HDRS_SIZE = FLUFFED_JMF_MESSAGE_SIZE
                                + FLUFFED_JSMSGPART_SIZE
                                + FLUFFED_REF_SIZE * 45
                                + FLUFFED_OBJECT_OVERHEAD * 2
                                + FLUFFED_STRING_OVERHEAD * 2
                                + 60; // For String/byte array content
    }

    /* ************************************************************************* */
    /* Constructors */
    /* ************************************************************************* */

    /**
     * Constructor for a new Jetstream API message.
     * 
     * This constructor should never be used explicitly.
     * It is only to be used implicitly by the sub-classes' no-parameter constructors.
     * The method must not actually do anything.
     */
    JsApiHdrsImpl() {}

    /**
     * Constructor for a new Jetstream API message.
     * 
     * @param flag Flag to distinguish different construction reasons.
     * 
     * @exception MessageDecodeFailedException Thrown if such a message can not be created
     */
    JsApiHdrsImpl(int flag) throws MessageDecodeFailedException {
        super(flag);

        // Set ProducerType field, as the subclass constructors/factories may not do it
        setProducerType(ProducerType.API);

        // We can skip all of this for an inbound MQ message as the MQJsMessageFactory will
        // replace the API_DATA with an MQjsApiEncapsulation.
        if ((flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ) && (flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ_BROKER)) {

            // Set in a new JsApi message part to contain the API level data
            jmo.setPart(JsHdrAccess.API_DATA, JsApiAccess.schema);

            /* Set all the defaultable header values. */
            setApiMessageIdAsBytes(null);
            getApi().setChoiceField(JsApiAccess.CORRELATIONID, JsApiAccess.IS_CORRELATIONID_EMPTY);
            setUserid(null);

            getApi().setChoiceField(JsApiAccess.CONNECTIONUUID, JsApiAccess.IS_CONNECTIONUUID_UNSET);

            getApi().setChoiceField(JsApiAccess.REPLYDISCRIMINATOR, JsApiAccess.IS_REPLYDISCRIMINATOR_UNSET);
            getApi().setChoiceField(JsApiAccess.REPLYPRIORITY, JsApiAccess.IS_REPLYPRIORITY_UNSET);
            getApi().setChoiceField(JsApiAccess.REPLYRELIABILITY, JsApiAccess.IS_REPLYRELIABILITY_UNSET);
            getApi().setChoiceField(JsApiAccess.REPLYTIMETOLIVE, JsApiAccess.IS_REPLYTIMETOLIVE_UNSET);

            getApi().setChoiceField(JsApiAccess.REPORTEXCEPTION, JsApiAccess.IS_REPORTEXCEPTION_UNSET);
            getApi().setChoiceField(JsApiAccess.REPORTPAN, JsApiAccess.IS_REPORTPAN_UNSET);
            getApi().setChoiceField(JsApiAccess.REPORTNAN, JsApiAccess.IS_REPORTNAN_UNSET);
            getApi().setChoiceField(JsApiAccess.REPORTPASSMSGID, JsApiAccess.IS_REPORTPASSMSGID_UNSET);
            getApi().setChoiceField(JsApiAccess.REPORTPASSCORRELID, JsApiAccess.IS_REPORTPASSCORRELID_UNSET);
            getApi().setChoiceField(JsApiAccess.REPORTDISCARDMSG, JsApiAccess.IS_REPORTDISCARDMSG_UNSET);
            getApi().setChoiceField(JsApiAccess.REPORTFEEDBACK, JsApiAccess.IS_REPORTFEEDBACK_UNSET);
            getApi().setChoiceField(JsApiAccess.REPORTFEEDBACKINT, JsApiAccess.IS_REPORTFEEDBACKINT_UNSET);

            getApi().setChoiceField(JsApiAccess.PSC, JsApiAccess.IS_PSC_EMPTY);
            getApi().setChoiceField(JsApiAccess.PSCR, JsApiAccess.IS_PSCR_EMPTY);

            getApi().setField(JsApiAccess.SYSTEMPROPERTY_NAME, null);
            getApi().setField(JsApiAccess.SYSTEMPROPERTY_VALUE, null);
            getApi().setField(JsApiAccess.JMSPROPERTY_NAME, null);
            getApi().setField(JsApiAccess.JMSPROPERTY_VALUE, null);
            getApi().setField(JsApiAccess.OTHERPROPERTY_NAME, null);
            getApi().setField(JsApiAccess.OTHERPROPERTY_VALUE, null);
            getApi().setField(JsApiAccess.SYSTEMCONTEXT_NAME, null);
            getApi().setField(JsApiAccess.SYSTEMCONTEXT_VALUE, null);

        }

    }

    /**
     * Constructor for an inbound message.
     * (Only to be called by JsMessage.makeApiMessage().)
     * 
     * @param inJmo The JsMsgObject representing the inbound method.
     */
    JsApiHdrsImpl(JsMsgObject inJmo) {
        super(inJmo);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "<init>, inbound jmo ");
    }

    /*
     * Provide the contribution of this part to the estimated encoded length
     * This contributes the API header fields.
     */
    @Override
    int guessApproxLength() {
        int total = super.guessApproxLength();

        // Make a guess at the contribution of the API section. This size is
        // reasonably constant, with the exception of the message properties.
        total += 120; // Approx 120 bytes in fixed fields

        return total;
    }

    /**
     * Provide the contribution of this part to the estimated 'fluffed' message size.
     * Subclasses that wish to contribute to a quick guess at the length of a
     * fluffed message should override this method, invoke their superclass and add on
     * their own contribution.
     * 
     * For this class, we should return an approximation of the size of the JMF
     * JSMessageImpls which represents the JsApiSchema, excluding the property maps,
     * for a populated but not assembled message.
     * 
     * @return int A guesstimate of the fluffed size of the message
     */
    @Override
    int guessFluffedSize() {

        // Get the contribution from the superclass(es) and
        // add the guess at the contribution of the API-level headers.
        int total = super.guessFluffedSize() + FLUFFED_API_HDRS_SIZE;

        return total;
    }

    /* ************************************************************************* */
    /* Set Methods for header fields */
    /* These overrride JsMessageImpl implementations to provide checking */
    /* ************************************************************************* */

    // Validate a discriminator value conforms to this regular expression:
    //   ([^:./*][^:/*]*)(/[^:./*][^:/*]*)*
    private boolean isValidDiscriminator(String s) {
        // Could simply do this regex match, but the actual code here is about 60x faster
        // return java.util.regex.Pattern.matches("([^:./*][^:/*]*)(/[^:./*][^:/*]*)*", s);
        if (s.indexOf(':') != -1 || s.indexOf('*') != -1)
            return false;
        int last = s.length();
        if (last > 0 && (s.charAt(0) == '/' || s.charAt(0) == '.'))
            return false;
        int i = s.indexOf('/');
        while (i++ != -1) {
            if (i == last || s.charAt(i) == '/' || s.charAt(i) == '.')
                return false;
            i = s.indexOf('/', i);
        }
        return true;
    }

    /*
     * Set the contents of the ForwardRoutingPath field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final void setForwardRoutingPath(List<SIDestinationAddress> value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setForwardRoutingPath", value);
        /* It is OK to pass in null for the List */
        if (value != null) {
            for (int i = 0; i < value.size(); i++) {
                /* It is not valid to have a null entry in the List */
                if (value.get(i) == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(this, tc, "setForwardRoutingPath", "NullPointerException");
                    throw new NullPointerException();
                }
                /* All values must be instances of SIDestinationAddress */
                if (!(value.get(i) instanceof SIDestinationAddress)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(this, tc, "setForwardRoutingPath", "IllegalArgumentException");
                    throw new IllegalArgumentException(value.toString());
                }
            }
        }
        setFRP(value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setForwardRoutingPath");
    }

    /*
     * Set the contents of the ReverseRoutingPath field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final void setReverseRoutingPath(List<SIDestinationAddress> value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReverseRoutingPath", value);
        /* It is OK to pass in null for the List */
        if (value != null) {

            for (int i = 0; i < value.size(); i++) {
                /* It is not valid to have a null entry in the List */
                if (value.get(i) == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(this, tc, "setReverseRoutingPath", "NullPointerException");
                    throw new NullPointerException();
                }
                /* All values must be instances of SIDestinationAddress */
                if (!(value.get(i) instanceof SIDestinationAddress)) {
                    IllegalArgumentException e = new IllegalArgumentException(value.toString());
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(this, tc, "setReverseRoutingPath", e);
                    throw e;
                }
            }
        }
        setRRP(value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReverseRoutingPath");
    }

    /*
     * Set the contents of the topic Discriminator field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final void setDiscriminator(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDiscriminator", value);
        if (value == null || isValidDiscriminator(value)) {
            super.setDiscriminator(value);
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setDiscriminator", "IllegalArgumentException");
            throw new IllegalArgumentException(value);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDiscriminator");
    }

    /*
     * Set the value of the Priority field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final void setPriority(int value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setPriority", Integer.valueOf(value));
        if ((value >= MfpConstants.MIN_PRIORITY) && (value <= MfpConstants.MAX_PRIORITY)) {
            /* Call the superclass's method to set the value into the field */
            super.setPriority(value);
        }
        else {
            IllegalArgumentException e = new IllegalArgumentException(Integer.toString(value));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setPriority", e);
            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setPriority");
    }

    /*
     * Set the value of the Reliability field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final void setReliability(Reliability value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReliability", value);
        if (value != null) {
            /* Call the superclass's method to get the int value of the Reliability instance and set it into the field */
            super.setReliability(value);
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setReliability", "NullPointerException");
            throw new NullPointerException();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReliability");
    }

    /*
     * Set the value of the TimeToLive field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final void setTimeToLive(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTimeToLive", Long.valueOf(value));
        if ((value >= MfpConstants.MIN_TIME_TO_LIVE) && (value <= MfpConstants.MAX_TIME_TO_LIVE)) {
            super.setTimeToLive(value);
        }
        else {
            IllegalArgumentException e = new IllegalArgumentException(Long.toString(value));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setTimeToLive", e);
            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTimeToLive");
    }

    /*
     * Set the value of the DeliveryDelay field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final void setDeliveryDelay(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDeliveryDelay", Long.valueOf(value));
        if ((value >= MfpConstants.MIN_DELIVERY_DELAY) && (value <= MfpConstants.MAX_DELIVERY_DELAY)) {
            super.setDeliveryDelay(value);
        }
        else {
            IllegalArgumentException e = new IllegalArgumentException(Long.toString(value));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setDeliveryDelay", e);
            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDeliveryDelay");
    }

    /* ************************************************************************* */
    /* Get and Set Methods for additional API-level header fields */
    /* ************************************************************************* */

    /*
     * Get the contents of the ReplyDiscriminator field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final String getReplyDiscriminator() {
        return (String) getApi().getField(JsApiAccess.REPLYDISCRIMINATOR_VALUE);
    }

    /*
     * Get the value of the ReplyPriority field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final Integer getReplyPriority() {
        return (Integer) getApi().getField(JsApiAccess.REPLYPRIORITY_VALUE);
    }

    /*
     * Get the value of the ReplyReliability field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final Reliability getReplyReliability() {
        Byte rType = (Byte) getApi().getField(JsApiAccess.REPLYRELIABILITY_VALUE);
        /* If set, return the corresponding Reliability instance */
        return (rType == null) ? null : Reliability.getReliability(rType);
    }

    /*
     * Get the value of the ReplyTimeToLive field from the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final Long getReplyTimeToLive() {
        return (Long) getApi().getField(JsApiAccess.REPLYTIMETOLIVE_VALUE);
    }

    /*
     * Set the contents of the ReplyDiscriminator field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final void setReplyDiscriminator(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReplyDiscriminator", value);
        if (value != null) {
            if (isValidDiscriminator(value)) {
                getApi().setField(JsApiAccess.REPLYDISCRIMINATOR_VALUE, value);
            }
            else {
                IllegalArgumentException e = new IllegalArgumentException(value);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "setReplyDiscriminator", e);
                throw e;
            }
        }
        else {
            getApi().setChoiceField(JsApiAccess.REPLYDISCRIMINATOR, JsApiAccess.IS_REPLYDISCRIMINATOR_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReplyDiscriminator");
    }

    /*
     * Set the value of the ReplyPriority field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final void setReplyPriority(int value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReplyPriority", Integer.valueOf(value));
        if ((value >= MfpConstants.MIN_PRIORITY) && (value <= MfpConstants.MAX_PRIORITY)) {
            getApi().setIntField(JsApiAccess.REPLYPRIORITY_VALUE, value);
        }
        else {
            IllegalArgumentException e = new IllegalArgumentException(Integer.toString(value));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setReplyPriority", e);
            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReplyPriority");
    }

    /*
     * Set the value of the ReplyReliability field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final void setReplyReliability(Reliability value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReplyReliability", value);
        if (value != null) {
            /* Get the int value of the Reliability instance and set it in the field */
            getApi().setField(JsApiAccess.REPLYRELIABILITY_VALUE, value.toByte());
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setReplyReliability", "NullPointerException");
            throw new NullPointerException();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReplyReliability");
    }

    /*
     * Set the value of the ReplyTimeToLive field in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final void setReplyTimeToLive(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReplyTimeToLive", Long.valueOf(value));
        if ((value >= MfpConstants.MIN_TIME_TO_LIVE) && (value <= MfpConstants.MAX_TIME_TO_LIVE)) {
            getApi().setLongField(JsApiAccess.REPLYTIMETOLIVE_VALUE, value);
        }
        else {
            IllegalArgumentException e = new IllegalArgumentException(Long.toString(value));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setReplyTimeToLive", e);
            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReplyTimeToLive");
    }

    /*
     * Clear the four ReplyXxxx fields in the message header.
     * 
     * Javadoc description supplied by SIBusMessage interface.
     */
    @Override
    public final void clearReplyFields() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearReplyFields");
        getApi().setChoiceField(JsApiAccess.REPLYDISCRIMINATOR, JsApiAccess.IS_REPLYDISCRIMINATOR_UNSET);
        getApi().setChoiceField(JsApiAccess.REPLYPRIORITY, JsApiAccess.IS_REPLYPRIORITY_UNSET);
        getApi().setChoiceField(JsApiAccess.REPLYRELIABILITY, JsApiAccess.IS_REPLYRELIABILITY_UNSET);
        getApi().setChoiceField(JsApiAccess.REPLYTIMETOLIVE, JsApiAccess.IS_REPLYTIMETOLIVE_UNSET);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearReplyFields");
    }

    /* ************************************************************************* */
    /* Get Methods for API Meta Data */
    /* ************************************************************************* */

    /*
     * Get the contents of the ApiMessageId field from the message header.
     * 
     * Javadoc description supplied by JsApiMessage interface.
     */
    public final String getApiMessageId() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getApiMessageId");

        String value = null;

        /* The ApiMessageId is held as hexBinary & so we need to convert to an */
        /* ID:xxxx format. */
        byte[] binValue = (byte[]) getApi().getField(JsApiAccess.MESSAGEID);
        if (binValue != null) {
            /* It'll be more economical to get the length right immediately */
            StringBuffer sbuf = new StringBuffer((binValue.length * 2) + 3);

            /* Insert the ID: then add on the binary value as a hex string */
            sbuf.append(ID_STRING);
            HexString.binToHex(binValue, 0, binValue.length, sbuf);

            /* Return the String representation */
            value = sbuf.toString();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getApiMessageId", value);

        return value;
    }

    /*
     * Get the contents of the ApiMessageId field from the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    @Override
    public final byte[] getApiMessageIdAsBytes() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getApiMessageIdAsBytes");

        byte[] value = (byte[]) getApi().getField(JsApiAccess.MESSAGEID);
        if (value != null) {
            byte[] copy = new byte[value.length];
            System.arraycopy(value, 0, copy, 0, value.length);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getApiMessageIdAsBytes", copy);
            return copy;
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getApiMessageIdAsBytes", null);
            return null;
        }
    }

    /*
     * Get the contents of the CorrelationId field from the message header.
     * 
     * Javadoc description supplied by JsApiMessage interface.
     */
    public final String getCorrelationId() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getCorrelationId");

        String value = null;
        int choice = getApi().getChoiceField(JsApiAccess.CORRELATIONID);

        /* If it is stored as a String, just extract it */
        if (choice == JsApiAccess.IS_CORRELATIONID_STRINGVALUE) {
            value = (String) getApi().getField(JsApiAccess.CORRELATIONID_STRINGVALUE);
        }

        /* If it is stored as a binary, we have to convert it */
        else if (choice == JsApiAccess.IS_CORRELATIONID_BINARYVALUE) {
            byte[] binValue = (byte[]) getApi().getField(JsApiAccess.CORRELATIONID_BINARYVALUE);

            if (binValue != null) {
                /* It'll be more economical to get the length right immediately */
                StringBuffer sbuf = new StringBuffer((binValue.length * 2) + 3);

                /* Insert the ID: then add on the binary value as a hex string */
                sbuf.append(ID_STRING);
                HexString.binToHex(binValue, 0, binValue.length, sbuf);

                /* Return the String representation */
                value = sbuf.toString();
            }

        }

        /* If the choice was 'Empty' do nothing as value is already null. */

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getCorrelationId", value);

        return value;
    }

    /*
     * Get the contents of the CorrelationId field from the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    @Override
    public final byte[] getCorrelationIdAsBytes() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getCorrelationIdAsBytes");

        byte[] value = null;
        int choice = getApi().getChoiceField(JsApiAccess.CORRELATIONID);

        /* If it is stored as a binary just extract it */
        if (choice == JsApiAccess.IS_CORRELATIONID_BINARYVALUE) {
            byte[] temp = (byte[]) getApi().getField(JsApiAccess.CORRELATIONID_BINARYVALUE);
            value = new byte[temp.length];
            System.arraycopy(temp, 0, value, 0, temp.length);
        }

        /* If it is stored as a String, we have to extract and convert it */
        else if (choice == JsApiAccess.IS_CORRELATIONID_STRINGVALUE) {

            String strValue = (String) getApi().getField(JsApiAccess.CORRELATIONID_STRINGVALUE);

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

        /* If the choice was 'Empty' do nothing as value is already null. */

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getCorrelationIdAsBytes", value);
        return value;

    }

    /*
     * Get the contents of the Userid field from the message header.
     * 
     * Javadoc description supplied by JsApiMessage interface.
     */
    public final String getUserid() {
        return (String) getApi().getField(JsApiAccess.USERID);
    }

    // Need to override the JsHdrsImpl version of this method and simply do
    // the same as getUserid()
    @Override
    public final String getApiUserId() {
        return getUserid();
    }

    /*
     * Get the contents of the Format field from the message header.
     * 
     * Javadoc description supplied by JsApiMessage interface.
     */
    public final String getFormat() {
        return (String) jmo.getPayloadPart().getField(JsPayloadAccess.FORMAT);
    }

    /* ************************************************************************* */
    /* Set Methods for API Meta Data */
    /* ************************************************************************* */

    /*
     * Set the contents of the ApiMessageId field in the message header.
     * 
     * Javadoc description supplied by JsApiMessage interface.
     */
    public final void setApiMessageId(String value) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setApiMessageId", value);
        if (value == null) {
            getApi().setField(JsApiAccess.MESSAGEID, null);
        }
        else if (value.startsWith(ID_STRING)) {
            byte[] binValue = HexString.hexToBin(value, 3);
            getApi().setField(JsApiAccess.MESSAGEID, binValue);
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setApiMessageId", "IllegalArgumentException");
            throw new IllegalArgumentException();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setApiMessageId");
    }

    /*
     * Set the contents of the ApiMessageId field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    @Override
    public final void setApiMessageIdAsBytes(byte[] value) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setApiMessageIdAsBytes", value);

        if (value != null) {
            byte[] copy = new byte[value.length];
            System.arraycopy(value, 0, copy, 0, value.length);
            getApi().setField(JsApiAccess.MESSAGEID, copy);
        }
        else {
            getApi().setField(JsApiAccess.MESSAGEID, null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setApiMessageIdAsBytes");
    }

    /*
     * Set the contents of the CorrelationId field in the message header.
     * 
     * Javadoc description supplied by JsApiMessage interface.
     */
    public final void setCorrelationId(String value) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCorrelationId", value);
        /* If the String starts with ID_STRING we have to chack it is valid, and */
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

                IllegalArgumentException e = new IllegalArgumentException(nlsMsg);
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
                    IllegalArgumentException e = new IllegalArgumentException(nlsMsg);
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

        if (value != null) {
            /* If we got to here, we have a the String and it must be OK */
            getApi().setField(JsApiAccess.CORRELATIONID_STRINGVALUE, value);
        }
        else {
            /* otherwise set the variant to empty */
            getApi().setChoiceField(JsApiAccess.CORRELATIONID, JsApiAccess.IS_CORRELATIONID_EMPTY);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setCorrelationId");
    }

    /*
     * Set the contents of the CorrelationId field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    @Override
    public final void setCorrelationIdAsBytes(byte[] value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCorrelationIdAsBytes", value);
        if (value != null) {
            /* Set the binary variant to the given value */
            byte[] copy = new byte[value.length];
            System.arraycopy(value, 0, copy, 0, value.length);
            getApi().setField(JsApiAccess.CORRELATIONID_BINARYVALUE, copy);
        }
        else {
            /* Set the variant to empty */
            getApi().setChoiceField(JsApiAccess.CORRELATIONID, JsApiAccess.IS_CORRELATIONID_EMPTY);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setCorrelationIdAsBytes");
    }

    /*
     * Set the contents of the Userid field in the message header.
     * 
     * Javadoc description supplied by JsApiMessage interface.
     */
    public final void setUserid(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setUserid", value);
        getApi().setField(JsApiAccess.USERID, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setUserid");
    }

    // Need to override the JsHdrsImpl version of this method and simply do
    // the same as setUserid()
    @Override
    public final void setApiUserId(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setApiUserId", value);
        setUserid(value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setApiUserId");
    }

    /*
     * Set the contents of the Format field in the message header.
     * 
     * Javadoc description supplied by JsApiMessage interface.
     */
    public final void setFormat(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setFormat", value);
        jmo.getPayloadPart().setField(JsPayloadAccess.FORMAT, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setFormat");
    }

    /* ************************************************************************* */
    /* Get & set optional ConnectionUuid */
    /* ************************************************************************* */

    /*
     * Get the value of the ConnectionUuid field from the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    @Override
    public SIBUuid12 getConnectionUuid() {
        byte[] b = (byte[]) getApi().getField(JsApiAccess.CONNECTIONUUID_VALUE);
        if (b != null)
            return new SIBUuid12(b);
        return null;
    }

    /*
     * Set the value of the ConnectionUuid field in the message header.
     * 
     * Javadoc description supplied by JsMessage interface.
     */
    @Override
    public void setConnectionUuid(SIBUuid12 value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setConnectionUuid", value);
        if (value != null)
            getApi().setField(JsApiAccess.CONNECTIONUUID_VALUE, value.toByteArray());
        else
            getApi().setField(JsApiAccess.CONNECTIONUUID_VALUE, null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setConnectionUuid");
    }

    /* ************************************************************************* */
    /* Get Optional Report Message information */
    /* ************************************************************************* */

    /*
     * Get the Report Exception field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final Byte getReportException() {
        return (Byte) getApi().getField(JsApiAccess.REPORTEXCEPTION_VALUE);
    }

    /*
     * Get the Report PAN field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final Boolean getReportPAN() {
        Boolean value = (Boolean) getApi().getField(JsApiAccess.REPORTPAN_VALUE);
        return (value == null) ? Boolean.FALSE : value;
    }

    /*
     * Get the Report NAN field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final Boolean getReportNAN() {
        Boolean value = (Boolean) getApi().getField(JsApiAccess.REPORTNAN_VALUE);
        return (value == null) ? Boolean.FALSE : value;
    }

    /*
     * Get the Report PassMsgId field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final Boolean getReportPassMsgId() {
        Boolean value = (Boolean) getApi().getField(JsApiAccess.REPORTPASSMSGID_VALUE);
        return (value == null) ? Boolean.FALSE : value;
    }

    /*
     * Get the Report PassCorrelId field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final Boolean getReportPassCorrelId() {
        Boolean value = (Boolean) getApi().getField(JsApiAccess.REPORTPASSCORRELID_VALUE);
        return (value == null) ? Boolean.FALSE : value;
    }

    /*
     * Get the Report DiscardMsg field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final Boolean getReportDiscardMsg() {
        Boolean value = (Boolean) getApi().getField(JsApiAccess.REPORTDISCARDMSG_VALUE);
        return (value == null) ? Boolean.FALSE : value;
    }

    /*
     * Get the Report Feedback field from the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final Integer getReportFeedback() {
        Integer result = (Integer) getApi().getField(JsApiAccess.REPORTFEEDBACKINT_VALUE);
        if (result == null) {
            // if the integer report feedback field is unset theres a chance that the byte
            // version may have been set by a down level sender. If the message is backed
            // by an MQ encapsulation this wont be the case since both these operations
            // end up looking in the same place in the MQMD,  but if its native JMF
            // then the values may differ.
            Byte temp = (Byte) getApi().getField(JsApiAccess.REPORTFEEDBACK_VALUE);
            result = (temp == null) ? null : Integer.valueOf(temp.intValue());
        }
        return result;
    }

    /* ************************************************************************* */
    /* Set Optional Report Message information */
    /* ************************************************************************* */

    /*
     * Set the Report Exception field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final void setReportException(Byte value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReportException", value);
        if (value != null) {
            getApi().setField(JsApiAccess.REPORTEXCEPTION_VALUE, value);
        }
        else {
            getApi().setChoiceField(JsApiAccess.REPORTEXCEPTION, JsApiAccess.IS_REPORTEXCEPTION_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReportException");
    }

    /*
     * Set the Report PAN field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final void setReportPAN(Boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReportPAN", value);
        if (value != null) {
            getApi().setField(JsApiAccess.REPORTPAN_VALUE, value);
        }
        else {
            getApi().setChoiceField(JsApiAccess.REPORTPAN, JsApiAccess.IS_REPORTPAN_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReportPAN");
    }

    /*
     * Set the Report NAN field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final void setReportNAN(Boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReportNAN", value);
        if (value != null) {
            getApi().setField(JsApiAccess.REPORTNAN_VALUE, value);
        }
        else {
            getApi().setChoiceField(JsApiAccess.REPORTNAN, JsApiAccess.IS_REPORTNAN_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReportNAN");
    }

    /*
     * Set the Report PassMsgId field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final void setReportPassMsgId(Boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReportPassMsgId", value);
        if (value != null) {
            getApi().setField(JsApiAccess.REPORTPASSMSGID_VALUE, value);
        }
        else {
            getApi().setChoiceField(JsApiAccess.REPORTPASSMSGID, JsApiAccess.IS_REPORTPASSMSGID_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReportPassMsgId");
    }

    /*
     * Set the Report PassCorrelId field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final void setReportPassCorrelId(Boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReportPassCorrelId", value);
        if (value != null) {
            getApi().setField(JsApiAccess.REPORTPASSCORRELID_VALUE, value);
        }
        else {
            getApi().setChoiceField(JsApiAccess.REPORTPASSCORRELID, JsApiAccess.IS_REPORTPASSCORRELID_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReportPassCorrelId");
    }

    /*
     * Set the Report DiscardMsg field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final void setReportDiscardMsg(Boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReportDiscardMsg", value);
        if (value != null) {
            getApi().setField(JsApiAccess.REPORTDISCARDMSG_VALUE, value);
        }
        else {
            getApi().setChoiceField(JsApiAccess.REPORTDISCARDMSG, JsApiAccess.IS_REPORTDISCARDMSG_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReportDiscardMsg");
    }

    /*
     * Set the Report Feedback field in the message header.
     * 
     * Javadoc description supplied by the SIBusMessage interface.
     */
    @Override
    public final void setReportFeedback(Integer value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReportFeedback", value);
        if (value != null) {
            // if the value fits in a byte then we set the value both into the byte and integer
            // versions of the feedback field. This may or may not be a redundant operation.
            // If the message is backed by an MQ encapsulation then we end up setting the value
            // in the real MQMD twice - but if its native JMF we need to set the value in both
            // fields so that if we send the message off to an old style client who uses the
            // old compatible schema, they can still read the feedback value from the byte field.
            if ((value.intValue() >= Byte.MIN_VALUE) && (value.intValue() <= Byte.MAX_VALUE)) { // 254045
                getApi().setField(JsApiAccess.REPORTFEEDBACK_VALUE, Byte.valueOf(value.byteValue()));
            }
            getApi().setField(JsApiAccess.REPORTFEEDBACKINT_VALUE, value);
        }
        else {
            getApi().setChoiceField(JsApiAccess.REPORTFEEDBACK, JsApiAccess.IS_REPORTFEEDBACK_UNSET);
            getApi().setChoiceField(JsApiAccess.REPORTFEEDBACKINT, JsApiAccess.IS_REPORTFEEDBACKINT_UNSET);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReportFeedback");
    }

    /* ************************************************************************* */
    /* Method for deleting the payload - must be overridden by each API message */
    /* ************************************************************************* */

    /*
     * Delete the Payload of an API Message.
     * 
     * Javadoc description supplied by the JsMessage interface.
     * This must be implemented by each API specialisation.
     */
    @Override
    public abstract void clearMessagePayload();

    /* ************************************************************************* */
    /* Get Methods for JMS header fields & calculated property values. */
    /* These methods have to be at this level as they are used by the SIMessage */
    /* property methods. */
    /* ************************************************************************* */

    /*
     * Get the value of the JMSDeliveryMode field from the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    public final PersistenceType getJmsDeliveryMode() {
        if (getHdr2().getChoiceField(JsHdr2Access.JMSDELIVERYMODE) == JsHdr2Access.IS_JMSDELIVERYMODE_EMPTY) {
            return getDerivedJmsDeliveryMode();
        }
        else {
            Byte pType = (Byte) getHdr2().getField(JsHdr2Access.JMSDELIVERYMODE_DATA);
            return PersistenceType.getPersistenceType(pType);
        }
    }

    /*
     * Get the contents of the JMSExpiration field from the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    public final Long getJmsExpiration() {
        if (getHdr2().getChoiceField(JsHdr2Access.JMSEXPIRATION) == JsHdr2Access.IS_JMSEXPIRATION_EMPTY) {
            return Long.valueOf(getDerivedJmsExpiration());
        }
        else {
            return (Long) getHdr2().getField(JsHdr2Access.JMSEXPIRATION_DATA);
        }
    }

    /*
     * Get the contents of the JMSDeliveryTime field from the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    public final Long getJmsDeliveryTime() {
        if (getHdr2().getChoiceField(JsHdr2Access.JMSDELIVERYTIME) == JsHdr2Access.IS_JMSDELIVERYTIME_EMPTY) {
            return Long.valueOf(getDerivedJmsDeliveryTime());
        }
        else {
            return (Long) getHdr2().getField(JsHdr2Access.JMSDELIVERYTIME_DATA);
        }
    }

    /*
     * Get the contents of the JMSDestination field from the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    public final byte[] getJmsDestination() {
        return (byte[]) getHdr2().getField(JsHdr2Access.JMSDESTINATION_DATA);
    }

    /*
     * Get the contents of the JMSReplyTo field from the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    public final byte[] getJmsReplyTo() {
        return (byte[]) getHdr2().getField(JsHdr2Access.JMSREPLYTO_DATA);
    }

    /*
     * Get the contents of the JMSType field from the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    public final String getJmsType() {
        if (getHdr2().getChoiceField(JsHdr2Access.JMSTYPE) == JsHdr2Access.IS_JMSTYPE_EMPTY) {
            return getDerivedJmsType();
        }
        else {
            return (String) getHdr2().getField(JsHdr2Access.JMSTYPE_DATA);
        }
    }

    /*
     * Get the JMSRedelivered value
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     * 
     * The value to return is TRUE if the JsMessage RedeliveredCount value is
     * > 0, otherwise FALSE.
     */
    public final Boolean getJmsRedelivered() {
        if (getRedeliveredCount().intValue() > 0) {
            return Boolean.TRUE;
        }
        else {
            return Boolean.FALSE;
        }
    }

    /*
     * Get the JMSXDeliveryCount value
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     * 
     * The value to return is the number of times this message has been
     * delivered. If the message has been delivered at all, it should be
     * RedeliveredCount + 1, otherwise it should be 0.
     * MP set currentMEArrivalTimeStamp at send time AFTER calling getSent, so
     * any message created by the app will still have 0 for that field, hence
     * we can use it to determine whether the message must have been received
     * or not.
     */
    public final int getJmsxDeliveryCount() {
        if (isSent()) {
            return getRedeliveredCount().intValue() + 1;
        }
        else {
            return 0;
        }
    }

    /*
     * Get the JMSXAppId value.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     * 
     * The value to return depends on the JMF choice in the JMS xappid field in
     * the message header. If the choice is EMPTY, the only option is to return
     * null. If it is STRINGVALUE, then the value contained is returned as is.
     * If the choice is COMPACT then the return String is looked up in a table.
     */
    public final String getJmsxAppId() {
        int choice = getHdr2().getChoiceField(JsHdr2Access.XAPPID);
        if (choice == JsHdr2Access.IS_XAPPID_EMPTY) {
            return null;
        }
        else if (choice == JsHdr2Access.IS_XAPPID_COMPACT) {
            try {
                return MfpConstants.JMSXAPPIDS[((Byte) getHdr2().getField(JsHdr2Access.XAPPID_COMPACT)).intValue()];
            } catch (ArrayIndexOutOfBoundsException e) {
                FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsApiHdrsImpl.getJmsxAppId", "1059");
                return null;
            }
        }
        else {
            return (String) getHdr2().getField(JsHdr2Access.XAPPID_STRINGVALUE);
        }
    }

    /* ************************************************************************* */
    /* Set Methods for JMS header fields & calculated property values. */
    /* These methods have to be at this level as they are used by the SIMessage */
    /* property methods. */
    /* ************************************************************************* */

    /*
     * Set the contents of the JMSType field in the message header.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    public final void setJmsType(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJmsType", value);
        getHdr2().setField(JsHdr2Access.JMSTYPE_DATA, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJmsType");
    }

    /*
     * Set the JMSXAppId value to a String value.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    public final void setJmsxAppId(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setJmsxAppId", value);
        getHdr2().setField(JsHdr2Access.XAPPID_STRINGVALUE, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setJmsxAppId");
    }

    /****************************************************************************/
    /* Package methods for default values of JMS fields if non-JMS message */
    /****************************************************************************/

    /**
     * Return the derived JmsDeliveryMode for a non-JMS/MQ produced message.
     * The value is derived from the Reliability field of the message.
     * 
     * @return PersistenceType The Persistence of the JMS message.
     */
    final PersistenceType getDerivedJmsDeliveryMode() {
        return PersistenceType.getPersistenceType(getReliability().getPersistence());
    }

    /**
     * Return the derived JmsExpiration for a non-JMS/MQ produced message.
     * The value is derived from the Timestamp and TimeToLive values.
     * d336582 - now want to return 0 if Timestamp or TimeToLive are 'not set'.
     * 
     * @return long The JmsExpiration value for the JMS message.
     */
    final long getDerivedJmsExpiration() {
        long ts = getTimestamp().longValue();
        long ttl = getTimeToLive().longValue();
        if ((ts <= 0) || (ttl <= 0)) {
            return 0;
        }
        else {
            return (ts + ttl);
        }
    }

    /**
     * Return the derived JmsDeliveryDelayTime for a non-JMS/MQ produced message.
     * The value is derived from the Timestamp and DeliveryDelay values.
     * d336582 - now want to return 0 if Timestamp or TimeToLive are 'not set'.
     * 
     * @return long The JmsDeliveryDelayTime value for the JMS message.
     */
    final long getDerivedJmsDeliveryTime() {
        long ts = getTimestamp().longValue();
        long dd = getDeliveryDelay().longValue();
        if ((ts <= 0) || (dd <= 0)) {
            return 0;
        }
        else {
            return (ts + dd);
        }
    }

    /**
     * Return the derived JmsType for a non-JMS/MQ produced message.
     * The value is derived from the Format field of the message.
     * 
     * @return String The JmsType for the JMS message.
     */
    final String getDerivedJmsType() {
        return getFormat();
    }

    /********************************************************************************/
    /* Package methods for getting and setting the CCSID & Encoding for the payload */
    /********************************************************************************/

    /**
     * Get the contents of the ccsid field from the payload part.
     * d395685
     * 
     * @return Integer The value of the CharacterSetID for the message payload.
     */
    final Integer getCcsid() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getCcsid");

        Integer value = (Integer) jmo.getPayloadPart().getField(JsPayloadAccess.CCSID_DATA);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getCcsid", value);
        return value;
    }

    /**
     * Get the contents of the encoding field from the payload part.
     * d395685
     * 
     * @return Integer The value of the Encoding (for MQ) for the message payload.
     */
    final Integer getEncoding() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getEncoding");

        Integer value = (Integer) jmo.getPayloadPart().getField(JsPayloadAccess.ENCODING_DATA);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getEncoding", value);
        return value;
    }

    /**
     * Set the contents of the ccsid field in the message payload part.
     * d395685
     * 
     * @param value the Object value of the CharacterSetID to be set into the ccsid field
     */
    final void setCcsid(Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCcsid", value);

        // If the value is null, then clear the field
        if (value == null) {
            jmo.getPayloadPart().setField(JsPayloadAccess.CCSID, JsPayloadAccess.IS_CCSID_EMPTY);
        }
        // If it is an Integer, then we just store it
        else if (value instanceof Integer) {
            jmo.getPayloadPart().setField(JsPayloadAccess.CCSID_DATA, value);
        }
        // If it is a String (most likely) we have to convert it into an int value first
        /*
         * else if (value instanceof String) {
         * try {
         * int ccsid = CCSID.getCCSID((String)value);
         * jmo.getPayloadPart().setIntField(JsPayloadAccess.CCSID_DATA, ccsid);
         * }
         * catch (UnsupportedEncodingException e) {
         * // FFDC it, then clear the field
         * FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsApiHdrsImpl.setCcsid", "866");
         * jmo.getPayloadPart().setField(JsPayloadAccess.CCSID, JsPayloadAccess.IS_CCSID_EMPTY);
         * }
         * }
         */
        // If it isn't of a suitable type, clear the field
        else {
            jmo.getPayloadPart().setField(JsPayloadAccess.CCSID, JsPayloadAccess.IS_CCSID_EMPTY);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setCcsid");
    }

    /**
     * Set the contents of the encoding field in the message payload part.
     * d395685
     * 
     * @param value the Object value of the Encoding (for MQ) be set into the encoding field
     */
    final void setEncoding(Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setEncoding", value);

        // If the value is null or unsuitable, then clear the field
        if ((value == null) || !(value instanceof Integer)) {
            jmo.getPayloadPart().setField(JsPayloadAccess.ENCODING, JsPayloadAccess.IS_ENCODING_EMPTY);
        }
        // If it does exist and is an Integer, then we just store it
        else {
            jmo.getPayloadPart().setField(JsPayloadAccess.ENCODING_DATA, value);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setEncoding");
    }

    /* ************************************************************************* */
    /* Methods for Message Control Classification */
    /* ************************************************************************* */

    /*
     * Get the Message Control Classification from the message header.
     * 
     * Javadoc description supplied by JsApiMessage interface.
     */
    public String getMessageControlClassification() {
        return (String) getHdr2().getField(JsHdr2Access.MESSAGECONTROLCLASSIFICATION_DATA);
    }

    /*
     * Set the Message Control Classification into the message header.
     * 
     * Javadoc description supplied by JsApiMessage interface.
     */
    public void setMessageControlClassification(String value) {
        if (value != null) {
            getHdr2().setField(JsHdr2Access.MESSAGECONTROLCLASSIFICATION_DATA, value);
        }
        else {
            getHdr2().setChoiceField(JsHdr2Access.MESSAGECONTROLCLASSIFICATION, JsHdr2Access.IS_MESSAGECONTROLCLASSIFICATION_EMPTY);
        }
    }

}
