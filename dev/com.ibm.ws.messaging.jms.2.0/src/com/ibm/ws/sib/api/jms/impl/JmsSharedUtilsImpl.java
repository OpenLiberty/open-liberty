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
package com.ibm.ws.sib.api.jms.impl;

import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.api.jms.JmsSession;
import com.ibm.ws.sib.api.jms.JmsSharedUtils;
import com.ibm.ws.sib.mfp.IncorrectMessageTypeException;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIBusMessage;

/**
 * Implementation of the JmsSharedUtils interface.
 */
public class JmsSharedUtilsImpl implements JmsSharedUtils
{

    // *************************** TRACE INITIALIZATION **************************
    private static TraceComponent tc = SibTr.register(JmsSharedUtilsImpl.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

    /**
     * Characters that are used to join the jms clientID and subName
     * for use in the core API.
     */
    private static final String SUB_CONCATENATOR = "##";

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jms.JmsSharedUtils#getCoreDurableSubName(java.lang.String, java.lang.String)
     */
    @Override
    public String getCoreDurableSubName(String jmsClientID, String jmsSubName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getCoreDurableSubName", new Object[] { jmsClientID, jmsSubName });

        String retSubName;

        if ((jmsClientID == null) || ("".equals(jmsClientID))) {
            retSubName = jmsSubName;
        } else {
            retSubName = jmsClientID + SUB_CONCATENATOR + jmsSubName;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getCoreDurableSubName", retSubName);

        return retSubName;
    }

    /**
     * @see com.ibm.ws.sib.api.jms.JmsSharedUtils#inboundMessagePath(com.ibm.ws.sib.common.SIBusMessage, com.ibm.websphere.sib.api.jms.JmsSession)
     * @throws JMSException if
     *             the SIBusMessage is not an instance of JsMessage - FFDC generated,<p>
     *             the message can't be represented as a JMS message - FFDC generated.
     */
    @Override
    public Message inboundMessagePath(SIBusMessage sibMsg, JmsSession theSession, Map passThruProps) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "inboundMessagePath", new Object[] { sibMsg, theSession, passThruProps });
        Message msg = null;

        try {
            // Check the requirements for the first parameter to be an instance of
            // JsMessage, and the second to be an instance of JmsSessionImpl if not null.

            // Having spoken with the MFP team (SP) I have been been assured that this
            // cast is a valid and reasonable thing to do, and that the SIBusMessage and
            // JsMessage interfaces are effectively equivalent.
            JsMessage jsMsg = (JsMessage) sibMsg;

            JmsSessionImpl sessionImpl = null;
            if (theSession != null)
                sessionImpl = (JmsSessionImpl) theSession;

            if (sibMsg != null) {
                // Convert it to an MFP JMS message object
                JsJmsMessage jsJmsMsg = jsMsg.makeInboundJmsMessage();
                // Get a JMS message reference to it. Pass on the 'pass thru props' param to
                // associate properties with the new message (SIB0121)
                msg = JmsMessageImpl.inboundJmsInstance(jsJmsMsg, sessionImpl, passThruProps);
            }

        } catch (ClassCastException cce) {
            // No FFDC code needed
            // d222942 review. Comments above suggest this would be an internal error case,
            // so exception received, though unusual, is probably ok.
            // d238447 FFDC review. Internal error so generate FFDC.
            JMSException jmse = (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                          "EXCEPTION_RECEIVED_CWSIA0022",
                                                                          new Object[] { cce, "JmsSharedUtilsImpl.inboundMessagePath" },
                                                                          cce, "JmsSharedUtilsImpl#1", this, tc);
            throw jmse;
        } catch (IncorrectMessageTypeException imte) {
            // No FFDC code needed
            // d222942 review. Javadoc says that IMT is thrown from makeInboundJmsMessage if the jsMsg
            // can't be represented as a JMS message. I'd expect everything to be representable as at
            // least a BytesMessage, so this seems to be another internal error case where the default
            // message is ok.
            // d238447 FFDC review. Internal error, so generate FFDC.
            JMSException jmse = (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                          "EXCEPTION_RECEIVED_CWSIA0022",
                                                                          new Object[] { imte, "JsMessage.makeInboundJmsMessage" },
                                                                          imte, "JmsSharedUtilsImpl#2", this, tc);
            throw jmse;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "inboundMessagePath", msg);
        return msg;
    }

    /**
     * Convert from Jetstream feedback values to MQ values.
     * 
     * @param fb A Jetstream feedback value
     * @return The equivalent MQ value
     */
    @Override
    public Integer convertJSFeedbackToMQ(int fb) {
        return convertJSFeedbackToMQ(Integer.valueOf(fb));
    }

    /**
     * Convert from Jetstream feedback values to MQ values.
     * 
     * @param fb A Jetstream feedback value
     * @return The equivalent MQ value
     */
    @Override
    public Integer convertJSFeedbackToMQ(Integer fb) {
        Integer result = null;

        if (SIApiConstants.REPORT_COA.equals(fb)) {
            result = Integer.valueOf(ApiJmsConstants.MQFB_COA);
        }
        else if (SIApiConstants.REPORT_COD.equals(fb)) {
            result = Integer.valueOf(ApiJmsConstants.MQFB_COD);
        }
        else if (SIApiConstants.REPORT_EXPIRY.equals(fb)) {
            result = Integer.valueOf(ApiJmsConstants.MQFB_EXPIRATION);
        }
        else if (SIApiConstants.REPORT_PAN.equals(fb)) {
            result = Integer.valueOf(ApiJmsConstants.MQFB_PAN);
        }
        else if (SIApiConstants.REPORT_NAN.equals(fb)) {
            result = Integer.valueOf(ApiJmsConstants.MQFB_NAN);
        }
        else {
            // All other values passed unchanged
            result = fb;
        }

        return result;
    }

    /**
     * Convert from MQ feedback values to Jetstream values.
     * 
     * @param fb An MQ feedback value
     * @return The equivalent Jetstream value
     */
    @Override
    public Integer convertMQFeedbackToJS(int fb) {
        Integer result;
        switch (fb) {
            case ApiJmsConstants.MQFB_COA:
                result = SIApiConstants.REPORT_COA;
                break;
            case ApiJmsConstants.MQFB_COD:
                result = SIApiConstants.REPORT_COD;
                break;
            case ApiJmsConstants.MQFB_EXPIRATION:
                result = SIApiConstants.REPORT_EXPIRY;
                break;
            case ApiJmsConstants.MQFB_PAN:
                result = SIApiConstants.REPORT_PAN;
                break;
            case ApiJmsConstants.MQFB_NAN:
                result = SIApiConstants.REPORT_NAN;
                break;
            default:
                result = Integer.valueOf(fb); // all other values are passed unchanged.
        }
        return result;
    }

    /**
     * Convert from MQ feedback values to Jetstream values.
     * 
     * @param fb An MQ feedback value
     * @return The equivalent Jetstream value
     */
    @Override
    public Integer convertMQFeedbackToJS(Integer fb) {
        Integer result = null;

        // if fb is null, then the default result value of null is ok.
        if (fb != null)
            result = convertMQFeedbackToJS(fb.intValue());

        return result;
    }
}
