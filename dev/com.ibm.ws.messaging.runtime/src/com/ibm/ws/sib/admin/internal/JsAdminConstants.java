/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin.internal;

public interface JsAdminConstants {

	// FFDC probe constants
	public final String PROBE_1 = "1";

	// Constants used for Exception Message resolution
	public final String EXCEPTION_MSG_BUNDLE = "com.ibm.websphere.sib.CWSIKMessages";
	public final String EXCEPTION_MESSAGE_KEY_PREFIX = "DELIVERY_ERROR_SIRC_";

	public final String DISABLE_ME_NAME_LOOKUPS_PROPERTY = "sib.admin.disableMENameLookupsForMsgs";
	public final String DISABLE_ME_NAME_LOOKUPS_PROPERTY_DEFAULT = "false";

	// File store properties
	public final String FILESTORE = "fileStore";
	public final String PATH = "path";
	public final String LOGFILESIZE = "logFileSize";
	public final long LOGFILESIZE_L = 10;
	public final String FILESTORESIZE = "fileStoreSize";	
	public final long FILESTORESIZE_L = 400;
	public final String MINPERMANENTFILESTORESIZE = "minPermanentFileStoreSize";
	public final long MINPERMANENTFILESTORESIZE_L = 20;
	public final String MAXPERMANENTFILESTORESIZE = "maxPermanentFileStoreSize";
	public final long MAXPERMANENTFILESTORESIZE_L = FILESTORESIZE_L/2;
	public final String MINTEMPORARYFILESTORESIZE = "minTemporaryFileStoreSize";
	public final long MINTEMPORARYFILESTORESIZE_L = 20;
	public final String MAXTEMPORARYFILESTORESIZE = "maxTemporaryFileStoreSize";
	public final long MAXTEMPORARYFILESTORESIZE_L = FILESTORESIZE_L/2;
	public final String UNLIMITEDTEMPORARYSTORESIZE = "unlimitedTemporaryStoreSize";
	public final String UNLIMITEDPERMANENTSTORESIZE = "unlimitedPermanentStoreSize";
	public final String DELETEFILESTORE = "deleteFileStore";
	public final long DEFAULT_REDELIVERYINTERVAL_VALUE = 5000;
	

	// Destination Properties
	public final String ID = "id";
	public final String LOCAL = "local";
	public final String ALIAS = "alias";
	public final String TYPE = "type";
	public final String OVERRIDEOFQOSBYPRODUCERALLOWED = "overrideOfQOSByProducerAllowed";
	public final String FORCERELIABILITY = "forceReliability";
	public final String DEFAULTRELIABILITY = "defaultReliability";
	public final String MAXIMUMRELIABILITY = "maximumReliability";
	public final String MESSAGEPRIORITY ="messagePriority";
	public final String DEFAULTPRIORITY = "defaultPriority";
	public final String EXCEPTIONDESTINATION = "exceptionDestination";
	public final String FAILEDDELIVERYPOLICY = "failedDeliveryPolicy";
	public final String REDELIVERYINTERVAL = "redeliveryInterval";
	public final String BLOCKEDRETRYTIMEOUT = "blockedRetryTimeout";
	public final String MAXREDELIVERYCOUNT = "maxRedeliveryCount";
	public final String MAXFAILEDDELIVERIES = "maxFailedDeliveries";
	public final String SENDALLOWED = "sendAllowed";
	public final String RECEIVEALLOWED = "receiveAllowed";
	public final String RECEIVEEXCLUSIVE = "receiveExclusive";
	public final String MAINTAINSTRICTORDER = "maintainStrictOrder";
	public final String PERSISTREDELIVERYCOUNT = "persistRedeliveryCount";
	public final String MAXMESSAGEDEPTH = "maxMessageDepth";
	public final String HIGHMESSAGETHRESHOLD = "highMessageThreshold";
	public final String EXCEPTIONDISCARDRELIABILITY = "exceptionDiscardReliability";
	public final String TOPICACCESSCHECKREQUIRED = "topicAccessCheckRequired";
	public final String DEFAULTQUEUE = "Default.Queue";
	public final String DEFAULTTOPIC = "Default.Topic.Space";
	public final String EXCEPTION_DESTINATION = "_SYSTEM.Exception.Destination";
	public final String QUEUE = "queue";
	public final String TOPICSPACE = "topicSpace";
	
	//destination reliability properties
	public final String BESTEFFORTNONPERSISTENT = "BestEffortNonPersistent";
	public final String EXPRESSNONPERSISTENT = "ExpressNonPersistent";
	public final String RELIABLENONPERSISTENT = "ReliableNonPersistent";
	public final String RELIABLEPERSISTENT = "ReliablePersistent";
	public final String ASSUREDPERSISTENT = "AssuredPersistent";
	
	
	//Alias Destination properties 
	public final String TARGETDESTINATION = "targetDestination";
	public final String DELEGATEAUTHCHECKTOTARGETDESTINATION = "delegateAuthCheckToTargetDestination";
	public final String INHERIT = "Inherit";
	public final String TRUE = "true";
	public final String FALSE = "false";

	// Bus name
	public final String DEFAULTBUS = "defaultBus";
	public final String DEFAULTMENAME = "defaultME";
	
	// Failed Delivery Policy
	public final String KEEP_TRYING = "KEEP_TRYING";
	public final String DISCARD = "DISCARD";

	// ME states
	public enum ME_STATE {
		STARTING, STARTED, STOPPING, STOPPED
	};

	public final int CONFIGADMIN = 0;
	public final int PROCESSOR = 1;

	public final String NONE = "None";
}
