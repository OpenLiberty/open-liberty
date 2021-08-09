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

package com.ibm.ws.sib.processor.impl;

import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPNoLocalisationsException;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

/**
 * ReportHandler class
 * <p>This class handles the generation and sending of report messages. Report
 * messages are generated on certain events. E.g. when a message expires, we may wish
 * to send a report message back to the producer to notify them. At the various point
 * in the message processor where these reports may be generated, we use a ReportHandler
 * object to deal with details of generation and routing.
 * 
 */
public class ReportHandler
{

    /**
     * Trace for the component
     */
    private static final TraceComponent tc =
                    SibTr.register(
                                   ReportHandler.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    /**
     * The MessageProcessor reference used to lookup destinations
     */

    private final MessageProcessor _messageProcessor;

    /**
     * Constructor. Takes the messageprocessor as a parameter.
     * Gets the system connection on which to create future
     * producerSessions.
     * 
     * @param MessageProcessor - The mp to set
     */
    public ReportHandler(MessageProcessor messageProcessor)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "ReportHandler", messageProcessor);

        // Get the message processor reference
        _messageProcessor = messageProcessor;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "ReportHandler", this);
    }

    /**
     * This method contains the routine used to generate and handle a report message.
     * The method examines the attributes of a message to determine which report(s)
     * to generate.
     * 
     * @param msg - The message to generate reports on.
     * @param tran - The transaction that the message was delivered under
     * @return The report message
     */

    public void handleMessage(
                              SIMPMessage msg,
                              TransactionCommon tran,
                              Integer reportType) throws SIIncorrectCallException, SITemporaryDestinationNotFoundException, SIResourceException, SINotPossibleInCurrentConfigurationException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleMessage",
                        new Object[] { msg,
                                      tran,
                                      reportType });

        // Get copy of message
        JsMessage message = null;
        try
        {
            message = msg.getMessage().getReceived();
        } catch (MessageCopyFailedException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ReportHandler.handleMessage",
                                        "1:139:1.40",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "handleMessage", "SIErrorException");

            throw new SIErrorException(e);
        }

        // Check the body content requested for the report
        Byte reportContent = null;
        if (reportType == SIApiConstants.REPORT_COA)
            reportContent = message.getReportCOA();
        else if (reportType == SIApiConstants.REPORT_COD)
            reportContent = msg.getReportCOD();
        else if (reportType == SIApiConstants.REPORT_EXCEPTION)
            reportContent = message.getReportException();
        else if (reportType == SIApiConstants.REPORT_EXPIRY)
            reportContent = message.getReportExpiry();

        // reportContent will not be null, need to use equals() comparison
        // since the Byte has been flowed in the message
        if (reportContent.equals(SIApiConstants.REPORT_NO_DATA))
        {
            message.clearMessagePayload();
            message.clearMessageProperties();
        }

        // Set the feedback field of the report message
        message.setReportFeedback(reportType);

        // Nullify remaining report options so the report itself does not request a report
        message.setReportCOA(null);
        message.setReportCOD(null);
        message.setReportException(null);
        message.setReportExpiry(null);
        message.setReportNAN(null);
        message.setReportPAN(null);

        // The original message's discard option does not apply to its report message
        message.setReportDiscardMsg(Boolean.FALSE);

        // Neither does its expiry
        message.setTimeToLive(0);
        message.setRemainingTimeToLive(-1);
        message.setMessageWaitTime(0);

        // Set the time that this message was created
        message.setTimestamp(System.currentTimeMillis());

        // Reset the redelivery count
        message.setRedeliveredCount(0);

        message.setMediated(false);

        message.setDeliveryDelay(0);

        // Copy reply fields to reply message
        if (msg.getMessage().getReplyPriority() != null)
            message.setPriority(msg.getMessage().getReplyPriority().intValue());
        if (msg.getMessage().getReplyReliability() != null)
            message.setReliability(msg.getMessage().getReplyReliability());
        if (msg.getMessage().getReplyTimeToLive() != null)
            message.setTimeToLive(msg.getMessage().getReplyTimeToLive().longValue());
        else
            message.setTimeToLive(0);
        if (msg.getMessage().getReplyDiscriminator() != null)
            message.setDiscriminator(msg.getMessage().getReplyDiscriminator());
        else
            message.setDiscriminator(null);

        // Set msgId/correlId fields
        if (Boolean.FALSE.equals(msg.getMessage().getReportPassCorrelId()))
            message.setCorrelationIdAsBytes(message.getApiMessageIdAsBytes());

        if (Boolean.FALSE.equals(msg.getMessage().getReportPassMsgId()))
        {
            // Set the new msgId to be the old one plus the report type byte
            byte[] oldId = message.getApiMessageIdAsBytes();
            byte[] newId;
            if (oldId != null)
            {
                newId = new byte[oldId.length + 1];
                for (int i = 0; i < oldId.length; i++)
                    newId[i] = oldId[i];
                newId[oldId.length] = reportType.byteValue();
            }
            else
                newId = new byte[] { reportType.byteValue() };

            message.setApiMessageIdAsBytes(newId);
        }

        // Get the reverse routing path
        if (!msg.getMessage().isReverseRoutingPathEmpty())
        {
            List<SIDestinationAddress> rrp = msg.getMessage().getReverseRoutingPath();
            JsDestinationAddress destinationAddress = null;

            Iterator it = rrp.iterator();
            destinationAddress = (JsDestinationAddress) it.next();

            // Remove first element of RRP
            rrp.remove(destinationAddress);
            message.setForwardRoutingPath(rrp);
            message.setRoutingDestination(destinationAddress);

            //Get the named destination from the destination manager
            DestinationHandler destination =
                            _messageProcessor.getDestinationManager().
                                            getDestination(destinationAddress, true);

            // Get the inputhandler associated with it        
            InputHandler inputHandler = destination.getInputHandler();

            // Send the report message
            try
            {
                inputHandler.handleMessage(
                                           new MessageItem(message),
                                           tran,
                                           _messageProcessor.getMessagingEngineUuid());
            } catch (SIMPNoLocalisationsException e)
            {
                // No FFDC code needed

                // We can't find a suitable localisation.
                // Although a queue must have at least one localisation this is
                // possible if the sender restricted the potential localisations
                // using a fixed ME or a scoping alias (to an out-of-date set of
                // localisation)

                //Put the message to the exception destination
                destination.handleUndeliverableMessage(msg
                                                       , SIRCConstants.SIRC0026_NO_LOCALISATIONS_FOUND_ERROR
                                                       , new String[] { destination.getName() },
                                                       null);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleMessage");
    }
}
