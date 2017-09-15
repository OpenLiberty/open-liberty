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

package com.ibm.websphere.sib;

/**
 * This class contains constants for the System and JMS property names for
 * SIBus messages.
 * 
 * @ibm-was-base
 * @ibm-api
 */
public final class SIProperties {

    /**
     * Constant for the SI Next Destination property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     */
    public final static String SI_NextDestination = "SI_NextDestination";

    /**
     * Constant for the SI Reliability property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried and set using the methods provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getReliability()
     * @see SIMessage#setReliability(Reliability)
     */
    public final static String SI_Reliability = "SI_Reliability";

    /**
     * Constant for the SI Priority property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried and set using the methods provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getPriority()
     * @see SIMessage#setPriority(int)
     */
    public final static String SI_Priority = "SI_Priority";

    /**
     * Constant for the SI Time To Live property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried and set using the methods provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getTimeToLive()
     * @see SIMessage#setTimeToLive(long)
     */
    public final static String SI_TimeToLive = "SI_TimeToLive";

    /**
     * Constant for the SI Delivery Delay property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried and set using the methods provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getDeliveryDelay()
     * @see SIMessage#setDeliveryDelay(long)
     */
    public final static String SI_DeliveryDelay = "SI_DeliveryDelay";

    /**
     * Constant for the SI Discriminator property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried and set using the methods provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getDiscriminator()
     * @see SIMessage#setDiscriminator(String)
     */
    public final static String SI_Discriminator = "SI_Discriminator";

    /**
     * Constant for the SI Reply Reliability property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried and set using the methods provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getReplyReliability()
     * @see SIMessage#setReplyReliability(Reliability)
     */
    public final static String SI_ReplyReliability = "SI_ReplyReliability";

    /**
     * Constant for the SI Reply Priority property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried and set using the methods provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getReplyPriority()
     * @see SIMessage#setReplyPriority(int)
     */
    public final static String SI_ReplyPriority = "SI_ReplyPriority";

    /**
     * Constant for the SI Reply Time To Live property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried and set using the methods provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getReplyTimeToLive()
     * @see SIMessage#setReplyTimeToLive(long)
     */
    public final static String SI_ReplyTimeToLive = "SI_ReplyTimeToLive";

    /**
     * Constant for the SI Reply Descriminator property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried and set using the methods provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getReplyDiscriminator()
     * @see SIMessage#setReplyDiscriminator(String)
     */
    public final static String SI_ReplyDiscriminator = "SI_ReplyDiscriminator";

    /**
     * Constant for the SI Redelivered Count property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried using the method provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getRedeliveredCount()
     */
    public final static String SI_RedeliveredCount = "SI_RedeliveredCount";

    /**
     * Constant for the SI Message Identifier property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried and set using the methods provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getApiMessageId()
     * @see SIMessage#setApiMessageId(String)
     */
    public final static String SI_MessageID = "SI_MessageID";

    /**
     * Constant for the SI Correlation Identifier property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried and set using the methods provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getCorrelationId()
     * @see SIMessage#setCorrelationId(String)
     */
    public final static String SI_CorrelationID = "SI_CorrelationID";

    /**
     * Constant for the SI User Identifier property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried and set using the methods provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getUserId()
     * @see SIMessage#setUserId(String)
     */
    public final static String SI_UserID = "SI_UserID";

    /**
     * Constant for the SI Format property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried using the method provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getFormat()
     */
    public final static String SI_Format = "SI_Format";

    /**
     * Constant for the SI System Message Identifier property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     * Instead, the property should be queried using the method provided
     * by the SIMessage interface.
     * 
     * @see SIMessage#getSystemMessageId()
     */
    public final static String SI_SystemMessageID = "SI_SystemMessageID";

    /**
     * Constant for the SI Report Exception property.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String SI_ReportException = "SI_ReportException";

    /**
     * Constant for the SI Report Expiration property.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String SI_ReportExpiration = "SI_ReportExpiration";

    /**
     * Constant for the SI Report Confirm On Arrival property.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String SI_ReportCOA = "SI_ReportCOA";

    /**
     * Constant for the SI Report Confirm On Delivery property.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String SI_ReportCOD = "SI_ReportCOD";

    /**
     * Constant for the SI Positive Action Notification property.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String SI_ReportPAN = "SI_ReportPAN";

    /**
     * Constant for the SI Negative Action Notification property.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String SI_ReportNAN = "SI_ReportNAN";

    /**
     * Constant for the SI Report Pass Message Identifier property.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String SI_ReportPassMsgID = "SI_ReportPassMsgID";

    /**
     * Constant for the SI Report Pass Correlation Identifier property.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String SI_ReportPassCorrelID = "SI_ReportPassCorrelID";

    /**
     * Constant for the SI Report Discard Message property.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String SI_ReportDiscardMsg = "SI_ReportDiscardMsg";

    /**
     * Constant for the SI Report Feedback property.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String SI_ReportFeedback = "SI_ReportFeedback";

    /**
     * Constant for the SI Exception Reason property.
     * 
     * <p>This property may be used for message selection and may be accessed
     * using getMessageProperty. It may not be modified using setMessageProprerty.
     */
    public final static String SI_ExceptionReason = "SI_ExceptionReason";

    /**
     * Constant for the SI Exception Inserts property.
     * 
     * <p>This property may be used for message selection and may be accessed
     * using getMessageProperty. It may not be modified using setMessageProprerty.
     */
    public final static String SI_ExceptionInserts = "SI_ExceptionInserts";

    /**
     * Constant for the SI Exception Timestamp property.
     * 
     * <p>This property may be used for message selection and may be accessed
     * using getMessageProperty. It may not be modified using setMessageProprerty.
     */
    public final static String SI_ExceptionTimestamp = "SI_ExceptionTimestamp";

    /**
     * Constant for the SI Exception Problem Destination property.
     * 
     * <p>This property may be used for message selection and may be accessed
     * using getMessageProperty. It may not be modified using setMessageProprerty.
     */
    public final static String SI_ExceptionProblemDestination = "SI_ExceptionProblemDestination";

    /**
     * Constant for the SI Exception Problem Subscription property.
     * 
     * <p>This property may be used for message selection and may be accessed
     * using getMessageProperty. It may not be modified using setMessageProprerty.
     */
    public final static String SI_ExceptionProblemSubscription = "SI_ExceptionProblemSubscription";

    /**
     * Constant for the JMS Destination Message Header property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSDestination = "JMSDestination";

    /**
     * Constant for the JMS Delivery Mode Message Header property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSDeliveryMode = "JMSDeliveryMode";

    /**
     * Constant for the JMS Message Identifier Message Header property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSMessageID = "JMSMessageID";

    /**
     * Constant for the JMS Timestamp Message Header property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSTimestamp = "JMSTimestamp";

    /**
     * Constant for the JMS Expiration Message Header property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSExpiration = "JMSExpiration";

    /**
     * Constant for the JMS DeliveryTime Message Header property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSDeliveryTime = "JMSDeliveryTime";

    /**
     * Constant for the JMS Redelivered Message Header property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSRedelivered = "JMSRedelivered";

    /**
     * Constant for the JMS Priority Message Header property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSPriority = "JMSPriority";

    /**
     * Constant for the JMS Reply To Message Header property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSReplyTo = "JMSReplyTo";

    /**
     * Constant for the JMS Correlation Identifier Message Header property.
     * 
     * <p>This may be treated as a property for message selection but may not be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSCorrelationID = "JMSCorrelationID";

    /**
     * Constant for the JMS Type Message Header property.
     * 
     * <p>This may be treated as a property for message selection and may be
     * accessed or modified using getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSType = "JMSType";

    /**
     * Constant for the JMS Application Identifier Property.
     * This is an optional JMS-defined property.
     * This property is fully supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSXAppID = "JMSXAppID";

    /**
     * Constant for the JMS Delivery Count Property.
     * This is an optional JMS-defined property.
     * This property is fully supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed using
     * the getMessageProperty method. It may not be modified using the
     * setMessageProperty method.
     */
    public final static String JMSXDeliveryCount = "JMSXDeliveryCount";

    /**
     * Constant for the JMS User Identifier Property.
     * This is an optional JMS-defined property.
     * This property is fully supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSXUserID = "JMSXUserID";

    /**
     * Constant for the JMS Group Identifier Property.
     * This is an optional JMS-defined property.
     * This property is fully supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSXGroupID = "JMSXGroupID";

    /**
     * Constant for the JMS Group Sequence Property.
     * This is an optional JMS-defined property.
     * This property is fully supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMSXGroupSeq = "JMSXGroupSeq";

    /**
     * Constant for the JMS Producer Transaction Identifier Property.
     * This is an optional JMS-defined property.
     * This property is not supported by the SIBus.
     * 
     * <p>This property always has a value of null when used in message selection
     * or accessed using the getMessageProperty method. It may not be modified
     * using the setMessageProperty method.
     */
    public final static String JMSXProducerTXID = "JMSXProducerTXID";

    /**
     * Constant for the JMS Consumer Transaction Identifier Property.
     * This is an optional JMS-defined property.
     * This property is not supported by the SIBus.
     * 
     * <p>This property always has a value of null when used in message selection
     * or accessed using the getMessageProperty method. It may not be modified
     * using the setMessageProperty method.
     */
    public final static String JMSXConsumerTXID = "JMSXConsumerTXID";

    /**
     * Constant for the JMS Receiver Timestamp Property.
     * This is an optional JMS-defined property.
     * This property is not supported by the SIBus.
     * 
     * <p>This property always has a value of null when used in message selection
     * or accessed using the getMessageProperty method. It may not be modified
     * using the setMessageProperty method.
     */
    public final static String JMSXRcvTimestamp = "JMSXRcvTimestamp";

    /**
     * Constant for the JMS State Property.
     * This is an optional JMS-defined property.
     * This property is not supported by the SIBus.
     * 
     * <p>This property always has a value of null when used in message selection
     * or accessed using the getMessageProperty method. It may not be modified
     * using the setMessageProperty method.
     */
    public final static String JMSXState = "JMSXState";

    /**
     * Constant for the JMS IBM Report Exception property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Report_Exception = "JMS_IBM_Report_Exception";

    /**
     * Constant for the JMS IBM Report Expiration property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Report_Expiration = "JMS_IBM_Report_Expiration";

    /**
     * Constant for the JMS IBM Report Confirm On Arrival property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Report_COA = "JMS_IBM_Report_COA";

    /**
     * Constant for the JMS IBM Report Confirm On Delivery property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Report_COD = "JMS_IBM_Report_COD";

    /**
     * Constant for the JMS IBM Positive Action Notification property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Report_PAN = "JMS_IBM_Report_PAN";

    /**
     * Constant for the JMS IBM Negative Action Notification property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Report_NAN = "JMS_IBM_Report_NAN";

    /**
     * Constant for the JMS IBM Report Pass Message Identifier property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Report_Pass_Msg_ID = "JMS_IBM_Report_Pass_Msg_ID";

    /**
     * Constant for the JMS IBM Report Pass Correlation Identifier property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Report_Pass_Correl_ID = "JMS_IBM_Report_Pass_Correl_ID";

    /**
     * Constant for the JMS IBM Report Discard Message property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Report_Discard_Msg = "JMS_IBM_Report_Discard_Msg";

    /**
     * Constant for the JMS IBM Report Feedback property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed and
     * modified using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Feedback = "JMS_IBM_Feedback";

    /**
     * Constant for the JMS IBM Exception Message property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed
     * using the getMessageProperty method. It may not be modified using the
     * setMessageProperty method.
     */
    public final static String JMS_IBM_ExceptionMessage = "JMS_IBM_ExceptionMessage";

    /**
     * Constant for the JMS IBM Exception Timestamp property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed
     * using the getMessageProperty method. It may not be modified using the
     * setMessageProperty method.
     */
    public final static String JMS_IBM_ExceptionTimestamp = "JMS_IBM_ExceptionTimestamp";

    /**
     * Constant for the JMS IBM Exception Reason property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed
     * using the getMessageProperty method. It may not be modified using the
     * setMessageProperty method.
     */
    public final static String JMS_IBM_ExceptionReason = "JMS_IBM_ExceptionReason";

    /**
     * Constant for the JMS IBM Exception Problem Destination property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed
     * using the getMessageProperty method. It may not be modified using the
     * setMessageProperty method.
     */
    public final static String JMS_IBM_ExceptionProblemDestination = "JMS_IBM_ExceptionProblemDestination";

    /**
     * Constant for the JMS IBM Exception Problem Subscription property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed
     * using the getMessageProperty method. It may not be modified using the
     * setMessageProperty method.
     */
    public final static String JMS_IBM_ExceptionProblemSubscription = "JMS_IBM_ExceptionProblemSubscription";

    /**
     * Constant for the JMS IBM Format property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Format = "JMS_IBM_Format";

    /**
     * Constant for the JMS IBM Message Type property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_MsgType = "JMS_IBM_MsgType";

    /**
     * Constant for the JMS IBM Put Application Type property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_PutApplType = "JMS_IBM_PutApplType";

    /**
     * Constant for the JMS IBM Last Message In Group property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Last_Msg_In_Group = "JMS_IBM_Last_Msg_In_Group";

    /**
     * Constant for the JMS IBM Put Date property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_PutDate = "JMS_IBM_PutDate";

    /**
     * Constant for the JMS IBM Put Time property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_PutTime = "JMS_IBM_PutTime";

    /**
     * Constant for the JMS IBM Encoding property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Encoding = "JMS_IBM_Encoding";

    /**
     * Constant for the JMS IBM Character Set property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_Character_Set = "JMS_IBM_Character_Set";

    /**
     * Constant for the JMS IBM System Message Identifier property.
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This property may be used for message selection and may be accessed
     * using the getMessageProperty method. It may not be modified using the
     * setMessageProperty method.
     */
    public final static String JMS_IBM_System_MessageID = "JMS_IBM_System_MessageID";

    /**
     * Constant for the JMS IBM Arm Correlator property.
     * This is an IBM-defined property and is a synonym of JMS_TOG_ARM_Correlator.
     * This property is supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_IBM_ArmCorrelator = "JMS_IBM_ArmCorrelator";

    /**
     * Constant for The Open Group JMS ARM Correlator property.
     * This is a JMS property defined by The Open Group and is a synonym of JMS_IBM_ArmCorrelator.
     * This property is supported by the SIBus.
     * 
     * <p>This may be used for message selection and may be accessed or modified
     * using the getMessageProperty/setMessageProperty methods.
     */
    public final static String JMS_TOG_ARM_Correlator = "JMS_TOG_ARM_Correlator";

    /**
     * Constant for the JMS_IBM_MQMD_MsgId property which contains the MsgId
     * value from a WebSphere MQ Message Descriptor (MQMD).
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This may be accessed or modified using the getMessageProperty/setMessageProperty
     * methods. It always has a value of null when used in message selection.
     * The property only exists if the message originated in MQ, or it has been
     * explicitly set.
     */
    public final static String JMS_IBM_MQMD_MsgId = "JMS_IBM_MQMD_MsgId";

    /**
     * Constant for the JMS_IBM_MQMD_CorrelId property which contains the CorrelId
     * value from a WebSphere MQ Message Descriptor (MQMD).
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This may be accessed or modified using the getMessageProperty/setMessageProperty
     * methods. It always has a value of null when used in message selection.
     * The property only exists if the message originated in MQ, or it has been
     * explicitly set.
     */
    public final static String JMS_IBM_MQMD_CorrelId = "JMS_IBM_MQMD_CorrelId";

    /**
     * Constant for the JMS_IBM_MQMD_Persistence property which contains the Persistence
     * value from a WebSphere MQ Message Descriptor (MQMD).
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This may be accessed or modified using the getMessageProperty/setMessageProperty
     * methods. It always has a value of null when used in message selection.
     * The property only exists if the message originated in MQ, or it has been
     * explicitly set.
     */
    public final static String JMS_IBM_MQMD_Persistence = "JMS_IBM_MQMD_Persistence";

    /**
     * Constant for the JMS_IBM_MQMD_ReplyToQ property which contains the ReplyToQ
     * value from a WebSphere MQ Message Descriptor (MQMD).
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This may be accessed or modified using the getMessageProperty/setMessageProperty
     * methods. It always has a value of null when used in message selection.
     * The property only exists if the message originated in MQ, or it has been
     * explicitly set.
     */
    public final static String JMS_IBM_MQMD_ReplyToQ = "JMS_IBM_MQMD_ReplyToQ";

    /**
     * Constant for the JMS_IBM_MQMD_ReplyToQMgr property which contains the ReplyToQMgr
     * value from a WebSphere MQ Message Descriptor (MQMD).
     * This is an IBM-defined property.
     * This property is supported by the SIBus.
     * 
     * <p>This may be accessed or modified using the getMessageProperty/setMessageProperty
     * methods. It always has a value of null when used in message selection.
     * The property only exists if the message originated in MQ, or it has been
     * explicitly set.
     */
    public final static String JMS_IBM_MQMD_ReplyToQMgr = "JMS_IBM_MQMD_ReplyToQMgr";

}
