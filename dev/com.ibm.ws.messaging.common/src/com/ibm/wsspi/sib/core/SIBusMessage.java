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
package com.ibm.wsspi.sib.core;

import java.io.Serializable;
import java.util.List;

import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;

/**
 * SIBusMessage is the basic interface for accessing and processing any
 * SIBus message visible to the Core SPI.
 * <p>
 * All SIBus message types (e.g. JMS, WDO, etc) are specializations of
 * an SIBusMessage.
 * 
 */
public interface SIBusMessage extends Serializable {

    /* ************************************************************************* */
    /* Get Methods */
    /* ************************************************************************* */

    /**
     * Get the contents of the ForwardRoutingPath field from the message header.
     * The List returned is a copy of the header field, so no updates to it
     * affect the Message header itself.
     * 
     * @return A List containing the ForwardRoutingPath which is a set of
     *         SIDestinationAddress instances.
     *         Null is returned if the field was not set.
     */
    public List<SIDestinationAddress> getForwardRoutingPath();

    /**
     * Get the contents of the ReverseRoutingPath field from the message header.
     * The List returned is a copy of the header field, so no updates to it
     * affect the Message header itself.
     * 
     * @return A List containing the ReverseRoutingPath which is a set of
     *         SIDestinationAddress instances.
     *         Null is returned if the field was not set.
     */
    public List<SIDestinationAddress> getReverseRoutingPath();

    /**
     * Get the contents of the topic Discriminator field from the message header.
     * 
     * @return A String containing the Discriminator for Pub/Sub
     */
    public String getDiscriminator();

    /**
     * Get the value of the Priority field from the message header.
     * 
     * @return An Integer containing the Priority of the message.
     *         Null is returned if the field was not set.
     */
    public Integer getPriority();

    /**
     * Get the value of the Reliability field from the message header.
     * 
     * @return The Reliability instance representing the Reliability of the
     *         message (i.e. Express, Reliable or Assured).
     *         Reliability.UNKNOWN is returned if the field is not set.
     */
    public Reliability getReliability();

    /**
     * Get the value of the TimeToLive field from the message header.
     * 
     * @return A Long containing the TimeToLive of the message.
     *         A value of 0 indicates that the message will never expire.
     *         Null is returned if the field was not set.
     */
    public Long getTimeToLive();

    /**
     * Get the value of the DeliveryDelay field from the message header.
     * 
     * @return A Long containing the DeliveryDelay of the message.
     *         A value of 0 indicates that there is no delivery delay set.
     *         Null is returned if the field was not set.
     */
    public Long getDeliveryDelay();

    /**
     * Get the remaining time in milliseconds before the message expires.
     * 
     * @return A long containing the remaining time, in milliseconds, before the
     *         message expires. A negative number indicates that the message
     *         will never expire.
     */
    public long getRemainingTimeToLive();

    /**
     * Get the contents of the ReplyDiscriminator field from the message header.
     * 
     * @return A String containing the Discriminator for any reply.
     *         Null is returned if the field was not set.
     */
    public String getReplyDiscriminator();

    /**
     * Get the value of the ReplyPriority field from the message header.
     * 
     * @return An Integer containing the Priority for any reply.
     *         Null is returned if the field was not set.
     */
    public Integer getReplyPriority();

    /**
     * Get the value of the ReplyReliability field from the message header.
     * 
     * @return The Reliability instance representing the Reliability for any reply
     *         message (i.e. Express, Reliable or Assured).
     *         Null is returned if the field was not set.
     */
    public Reliability getReplyReliability();

    /**
     * Get the value of the ReplyTimeToLive field from the message header.
     * 
     * @return A Long containing the TimeToLive for any reply.
     *         A value of 0 indicates that the reply message will never expire.
     *         Null is returned if the field was not set.
     */
    public Long getReplyTimeToLive();

    /**
     * Get the unique system message id from the message header.
     * 
     * @return A String containing the system message id.
     *         Null is returned if the field has not yet been set.
     */
    public String getSystemMessageId();

    /**
     * Get the message handle which uniquely identifies this message.
     * 
     * @return An SIMessageHandle which identifies this message.
     */
    public SIMessageHandle getMessageHandle();

    /* ------------------------------------------------------------------------ */
    /* Optional Exception information */
    /* ------------------------------------------------------------------------ */

    /**
     * Get the value of the Exception Reason from the message header.
     * 
     * @return An Integer representing the reason the Message was written to
     *         the Exception Destination.
     *         The field will not be set if the message was not written to an
     *         Exception Destination.
     */
    public Integer getExceptionReason();

    /**
     * Get the value of the Exception Inserts from the message header.
     * 
     * @return An array containing the String 'inserts' for the Error Message identified
     *         by the Exception Reason.
     *         The field will not be set if the message was not written to an
     *         Exception Destination.
     */
    public String[] getExceptionInserts();

    /**
     * Get the value of the Exception Timestamp from the message header.
     * 
     * @return A Long representing the time, in milliseconds after epoch, when
     *         the Message was written to the Exception Destination.
     *         The field will not be set if the message was not written to an
     *         Exception Destination.
     */
    public Long getExceptionTimestamp();

    /**
     * Get the value of the Exception Problem Destination from the message header.
     * 
     * @return A String indicating the Destination the message was intended for
     *         when a problem occurred which caused it to be routed to the
     *         Exception Destination.
     *         The field will not be set if the message was not written to an
     *         Exception Destination, or if no destination was known.
     */
    public String getExceptionProblemDestination();

    /**
     * Get the value of the Exception Problem Subscription from the message header.
     * 
     * @return A String indicating the Subscription the message was intended for
     *         when a problem occurred which caused it to be routed to the
     *         Exception Destination.
     *         The field will not be set if the message was not written to an
     *         Exception Destination, or if no subscription was known.
     */
    public String getExceptionProblemSubscription();

    /* ------------------------------------------------------------------------ */
    /* Optional Report Message information */
    /* ------------------------------------------------------------------------ */

    /**
     * Get the Report Expiry field from the message header.
     * 
     * @return A Byte representing the type of Expiry Report required for
     *         this message.
     *         The field will not be set if no Expiry Report is required.
     */
    public Byte getReportExpiry();

    /**
     * Get the Report Exception field from the message header.
     * 
     * @return A Byte representing the type of Exception Report required for
     *         this message.
     *         The field will not be set if no Exception Report is required.
     */
    public Byte getReportException();

    /**
     * Get the Report COD field from the message header.
     * 
     * @return A Byte representing the type of COD Report required for
     *         this message.
     *         The field will not be set if no COD Report is required.
     */
    public Byte getReportCOD();

    /**
     * Get the Report COA field from the message header.
     * 
     * @return A Byte representing the type of COA Report required for
     *         this message.
     *         The field will not be set if no COA Report is required.
     */
    public Byte getReportCOA();

    /**
     * Get the Report PAN field from the message header.
     * 
     * @return A Boolean indicating whether PAN Reports are required for
     *         this message.
     */
    public Boolean getReportPAN();

    /**
     * Get the Report NAN field from the message header.
     * 
     * @return A Boolean indicating whether NAN Reports are required for
     *         this message.
     */
    public Boolean getReportNAN();

    /**
     * Get the Report PassMsgId field from the message header.
     * 
     * @return A Boolean indicating whether the existing ApiMessageId should be
     *         passed into any report message. False indicates that a new
     *         ApiMessageId will be generated for any report message.
     */
    public Boolean getReportPassMsgId();

    /**
     * Get the Report PassCorrelId field from the message header.
     * 
     * @return A Boolean indicating whether the existing CorrelationId should be
     *         passed into any report message. False indicates that the existing
     *         ApiMessageId will be used as the CorrelationId for any report message.
     */
    public Boolean getReportPassCorrelId();

    /**
     * Get the Report DiscardMsg field from the message header.
     * 
     * @return A Boolean indicating whether report messages should be discarded
     *         if delivery exceptions occur. False indicates that they will be
     *         sent to the Exception Destination.
     */
    public Boolean getReportDiscardMsg();

    /**
     * Get the Report Feedback field from the message header.
     * 
     * @return An Integer representing the type of Feedback Report this message
     *         represents.
     *         The field will not be set if the message is not a report message.
     */
    public Integer getReportFeedback();

    /**
     * Gets the XCT Correlation ID from the message header.
     * 
     * @return A String containing the XCT Correlation ID
     *         Null is returned if the field was not set.
     */
    public String getXctCorrelationID();

    /* ************************************************************************* */
    /* Set Methods */
    /* ************************************************************************* */

    /**
     * Set the contents of the ForwardRoutingPath field in the message header.
     * 
     * @param value A List containing the ForwardRoutingPath which is a set of
     *            SIDestinationAddress instances.
     * 
     * @exception NullPointerException The List contains one or more null entries.
     */
    public void setForwardRoutingPath(List<SIDestinationAddress> value);

    /**
     * Set the contents of the ReverseRoutingPath field in the message header.
     * 
     * @param value A List containing the ReverseRoutingPath which is a set of
     *            SIDestinationAddress instances.
     * 
     * @exception NullPointerException The List contains one or more null entries.
     */
    public void setReverseRoutingPath(List<SIDestinationAddress> value);

    /**
     * Set the contents of the topic Discriminator field in the message header.
     * 
     * @param value A String containing the Discriminator for Pub/Sub
     *            The Discriminator is a simplified XPATH expression without wild cards.
     *            It is considered valid if the following expression is true:
     *            <code>
     *            java.util.regex.Pattern.matches("([^:./*][^:/*]*)(/[^:./*][^:/*]*)*",value);
     *            </code>
     * 
     * @exception IllegalArgumentException The value given contains wild cards
     *                or is otherwise invalid.
     */
    public void setDiscriminator(String value);

    /**
     * Set the value of the Priority field in the message header.
     * 
     * @param value An int containing the Priority of the message.
     * 
     * @exception IllegalArgumentException The value given is outside the
     *                permitted range.
     */
    public void setPriority(int value);

    /**
     * Set the value of the Reliability field in the message header.
     * 
     * @return The Reliability instance representing the reliability of the
     *         message (i.e. Express, Reliable or Assured).
     * 
     * @exception NullPointerException Null is not a valid Reliability.
     */
    public void setReliability(Reliability value);

    /**
     * Set the value of the TimeToLive field in the message header.
     * 
     * @param value A long containing the TimeToLive of the message.
     *            A value of 0 indicates that the message should never expire.
     * 
     * @exception IllegalArgumentException The value is
     *                given is less than 0 or greater than 290,000,000 years.
     */
    public void setTimeToLive(long value);

    /**
     * Set the value of the DeliveryDelay field in the message header.
     * 
     * @param value A long containing the DeliveryDelay of the message.
     *            A value of 0 indicates that the delivery delay is not set.
     * 
     * @exception IllegalArgumentException The value is
     *                given is less than 0 or greater than 290,000,000 years.
     */
    public void setDeliveryDelay(long value);

    /**
     * Set the remaining time in milliseconds before the message should expire.
     * 
     * @param value A long containing the remaining time, in milliseconds, before the
     *            message should expire. A negative number indicates that the message
     *            should never expire.
     * 
     * @exception IllegalArgumentException The resulting
     *                Time To Live is less than 0 or greater than 290,000,000 years.
     */
    public void setRemainingTimeToLive(long value);

    /**
     * Set the contents of the ReplyDiscriminator field in the message header.
     * 
     * @param value A String containing the Discriminator for any reply.
     *            The Discriminator is a simplified XPATH expression without wild cards.
     *            It is considered valid if the following expression is true:
     *            <code>
     *            java.util.regex.Pattern.matches("([^:./*][^:/*]*)(/[^:./*][^:/*]*)*",value);
     *            </code>
     * 
     * @exception IllegalArgumentException The value given contains wild cards
     *                or is otherwise invalid.
     */
    public void setReplyDiscriminator(String value);

    /**
     * Set the value of the ReplyPriority field in the message header.
     * 
     * @param value An int containing the Priority for any reply.
     * 
     * @exception IllegalArgumentException The value given is outside the
     *                permitted range.
     */
    public void setReplyPriority(int value);

    /**
     * Set the value of the ReplyReliability field in the message header.
     * 
     * @return The Reliability instance representing the reliability for any reply
     *         message (i.e. Express, Reliable or Assured).
     * 
     * @exception NullPointerException Null is not a valid Reliability.
     */
    public void setReplyReliability(Reliability value);

    /**
     * Set the value of the ReplyTimeToLive field in the message header.
     * 
     * @param value A long containing the TimeToLive for any reply.
     *            A value of 0 indicates that the reply message should never expire.
     * 
     * @exception IllegalArgumentException The value
     *                given is less than 0 or greater than 290,000,000 years.
     */
    public void setReplyTimeToLive(long value);

    /**
     * Clear the four ReplyXxxx fields in the message header.
     */
    public void clearReplyFields();

    /* ------------------------------------------------------------------------ */
    /* Optional Exception information */
    /* ------------------------------------------------------------------------ */

    /**
     * Clear all of the Exception fields from the message header.
     * The fields cleared are ExceptionReason, ExceptionTimestamp, ExceptionInserts
     * and ExceptionProblemDestination.
     */
    public void clearExceptionData();

    /* ------------------------------------------------------------------------ */
    /* Optional Report Message information */
    /* ------------------------------------------------------------------------ */

    /**
     * Set the Report Expiry field in the message header.
     * 
     * @param value A Byte representing the type of Expiry Report required for
     *            this message.
     */
    public void setReportExpiry(Byte value);

    /**
     * Set the Report Exception field in the message header.
     * 
     * @param value A Byte representing the type of Exception Report required for
     *            this message.
     */
    public void setReportException(Byte value);

    /**
     * Set the Report COD field in the message header.
     * 
     * @param value A Byte representing the type of COD Report required for
     *            this message.
     */
    public void setReportCOD(Byte value);

    /**
     * Set the Report COA field in the message header.
     * 
     * @param value A Byte representing the type of COA Report required for
     *            this message.
     */
    public void setReportCOA(Byte value);

    /**
     * Set the Report PAN field in the message header.
     * 
     * @param value A Boolean indicating whether PAN Reports are required for
     *            this message.
     */
    public void setReportPAN(Boolean value);

    /**
     * Set the Report NAN field in the message header.
     * 
     * @param value A Boolean indicating whether NAN Reports are required for
     *            this message.
     */
    public void setReportNAN(Boolean value);

    /**
     * Set the Report PassMsgId field in the message header.
     * 
     * @param value A Boolean indicating whether the existing ApiMessageId should be
     *            passed into any report message. False indicates that a new
     *            ApiMessageId will be generated for any report message.
     */
    public void setReportPassMsgId(Boolean value);

    /**
     * Set the Report PassCorrelId field in the message header.
     * 
     * @param value A Boolean indicating whether the existing CorrelationId should be
     *            passed into any report message. False indicates that the existing
     *            ApiMessageId will be used as the CorrelationId for any report message.
     */
    public void setReportPassCorrelId(Boolean value);

    /**
     * Set the Report DiscardMsg field in the message header.
     * 
     * @param value A Boolean indicating whether report messages should be discarded
     *            if delivery exceptions occur. False indicates that they will be
     *            sent to the Exception Destination.
     */
    public void setReportDiscardMsg(Boolean value);

    /**
     * Set the Report Feedback field in the message header.
     * 
     * @param value An Integer representing the type of Feedback Report this message
     *            represents.
     */
    public void setReportFeedback(Integer value);

    /**
     * Sets the XCT Correlation ID into the message header.
     * 
     * @param value A String containing the XCT Correlation ID
     */
    public void setXctCorrelationID(String value);

}
