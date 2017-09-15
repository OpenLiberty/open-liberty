/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.comms.client.util;


/**
 * This file contains constants which are used in XCT annotations and associations
 * 
 */
public interface XctJmsConstants
{
	
    // ***********************XCT CONSTANTS ****************************************
	
    public static final String XCT_JMS ="JMS";
    public static final String XCT_SIBUS ="SIBus";
    public static final String XCT_DEST_NAME ="DestinationName";
    public static final String XCT_DEST_TYPE ="DestinationType";
    public static final String XCT_DEST_TYPE_QUEUE ="Queue";
    public static final String XCT_DEST_TYPE_TOPICSPACE ="TopicSpace";
    public static final String XCT_TRANSACTED ="Transacted";
    public static final String XCT_TRANSACTED_TRUE ="True";
    public static final String XCT_TRANSACTED_FALSE ="False";
    public static final String XCT_MESSAGE_ID ="MessageID";
    public static final String XCT_SYSTEM_MESSAGE_ID ="SystemMessageID";
    public static final String XCT_FAILED ="Failed";
    public static final String XCT_ERROR_MSG_01 ="NoLocalisation";
    public static final String XCT_ERROR_MSG_02 ="SessionNotAvailable";
    public static final String XCT_ERROR_MSG_03 ="NotAuthorized";
    public static final String XCT_ERROR_MSG_04 ="ConfigurationError";
    public static final String XCT_ERROR_MSG_05 ="TemporaryDestinationNotFound";
    public static final String XCT_ERROR_MSG_06 ="Exception";
    public static final String XCT_NO_MESSAGE ="NoMessage";    
    public static final String XCT_RELIABILITY ="Reliability";
    public static final String XCT_ME_UUID ="MessagingEngineUuid";
    public static final String XCT_SOURCE_ME_UUID ="SourceMessagingEngineUuid";
    public static final String XCT_TARGET_ME_UUID ="TargetMessagingEngineUuid";
    public static final String XCT_JMS_SEND ="JMS_SEND";
    public static final String XCT_JMS_RECV ="JMS_RECV";
    public static final String XCT_JMS_TEXT_MSG_SUFFIX = "txt";
    public static final String XCT_JMS_MAP_MSG_SUFFIX = "map";
    

    // *********XCT SEND ANNOTATION/ASSOCIATION CONSTANTS *****************************
	
    public static final String XCT_SEND ="Send";
    public static final String XCT_PROXY_SEND ="ProxySend";
    public static final String XCT_SEND_MESSAGE ="SendMessage";
    public static final String XCT_ACK_MODE ="AcknowledgeMode";
    public static final String XCT_ACK_MODE_TRANSACTED ="SESSION_TRANSACTED";
    public static final String XCT_ACK_MODE_CLIENT ="CLIENT_ACKNOWLEDGE";
    public static final String XCT_ACK_MODE_DUPS_OK ="DUPS_OK_ACKNOWLEDGE";
    public static final String XCT_ACK_MODE_AUTO ="AUTO_ACKNOWLEDGE";
    public static final String XCT_ACK_MODE_NONE ="NONE";
      

    // *********XCT RECEIVE ANNOTATION/ASSOCIATION CONSTANTS *****************************
	 
    public static final String XCT_RECEIVE ="Receive";
    public static final String XCT_CONSUME_SEND ="ConsumeSend";
    public static final String XCT_RECEIVE_NO_WAIT ="ReceiveNoWait";
    public static final String XCT_RECEIVE_WITH_WAIT ="ReceiveWithWait";
    public static final String XCT_PROXY_RECEIVE_NO_WAIT ="ProxyReceiveNoWait";
    public static final String XCT_PROXY_RECEIVE_WITH_WAIT ="ProxyReceiveWithWait";
    public static final String XCT_RECEIVE_INBOUND ="ReceiveInBound";
    public static final String XCT_CONSUME_MESSAGE ="ConsumeMessage";
    public static final String XCT_PROCESS_MESSAGE ="ProcessMessage";
    public static final String XCT_ID ="XctId";
    public static final String XCT_ROOT_ID ="XctRootId";	
    public static final String XCT_CLIENT_ID ="ClientID";
    public static final String XCT_SUBSCRIPTION_ID ="SubscriptionID";
    public static final String XCT_SUBSCRIPTION ="Subscription";
    public static final String XCT_SUBSCRIPTION_DURABLE ="Durable";
    public static final String XCT_SUBSCRIPTION_NONDURABLE ="NonDurable";
      
                
}
