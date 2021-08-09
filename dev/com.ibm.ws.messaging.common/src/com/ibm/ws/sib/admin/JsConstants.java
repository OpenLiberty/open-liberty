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

package com.ibm.ws.sib.admin;

import com.ibm.ws.sib.utils.TraceGroups;

public class JsConstants {

    // WCCM configuration document filenames.

    public static final String WCCM_DOC_BUS = "sib-bus.xml";
    public static final String WCCM_DOC_DESTINATIONS = "sib-destinations.xml";
    public static final String WCCM_DOC_MEDIATIONS = "sib-mediations.xml";
    public static final String WCCM_DOC_AUTHORISATIONS = "sib-authorisations.xml";
    public static final String WCCM_DOC_ENGINES = "sib-engines.xml";
    public static final String WCCM_DOC_SERVICE = "sib-service.xml";
    public static final String WCCM_DOC_MQSERVERS = "sib-mqservers.xml";
    public static final String WCCM_DOC_MQSERVER_BUSMEMBERS = "sib-mqserverbusmembers.xml";
    public static final String WCCM_DOC_SECURITY_AUDIT = "sib-security-audit.xml";

    // The name of the default bus.

    //Venu mock mock.. correcting spelling of defaultBus
    public static final String DEFAULT_BUS_NAME = "defaultBus";

    // The name of the default messaging engine.

    public static final String DEFAULT_ME_NAME = "defaultME";

    // Factory and service classes.

    public final static String JS_ADMIN_SERVICE_CLASS = "com.ibm.ws.messaging.service.JsAdminServiceImpl";
    public final static String JS_ADMIN_FACTORY_CLASS = "com.ibm.ws.sib.admin.internal.JsAdminFactoryImpl";
    public final static String JS_GATEWAYLINK_IMPL_CLASS = "com.ibm.ws.sib.admin.internal.JsGatewayLinkImpl";
    public final static String JS_MQCLIENTLINK_IMPL_CLASS = "com.ibm.ws.sib.admin.internal.JsMQClientLinkImpl";
    public final static String JS_MQLINK_IMPL_CLASS = "com.ibm.ws.sib.admin.internal.JsMQLinkImpl";
    final static String JS_STANDALONE_FACTORY_CLASS = "com.ibm.ws.sib.admin.internal.JsStandaloneFactoryImpl";
    public static final String JS_CONFIG_FILTER_CLASS = "com.ibm.ws.sib.admin.config.SIBConfigFilterImpl";
    // Jetstream sub-component class names. These class names are used to instantiate the appropriate
    // sub-components during bootstrap and initialization processing. These constants can also be
    // used as arguments to locator methods which will return an object reference to the instances
    // of such classes.

    public final static String SIB_CLASS_ADMIN_TEST = "com.ibm.ws.sib.admin.tests.TestHarness";
    public final static String SIB_CLASS_CO = "com.ibm.ws.sib.comms.CommsAdminComponent";
    public final static String SIB_CLASS_CO_MQCLIENTLINK = "com.ibm.ws.sib.comms.mq.client.MQClientLink";
    public final static String SIB_CLASS_CO_MQCLIENTLINK_DUMMY = "com.ibm.ws.sib.comms.mq.client.dummy.DummyMQClientLink";
    public final static String SIB_CLASS_CO_MQLINK = "com.ibm.ws.sib.comms.mq.link.MQLink";
    public final static String SIB_CLASS_GATEWAY_LINK = "com.ibm.ws.sib.ibl.InterBusLinkImpl";
    public final static String SIB_CLASS_PSB = "com.ibm.ws.sib.psb.admin.impl.BridgeControllerAdminImpl";
    public final static String SIB_CLASS_MED_START = "com.ibm.ws.sib.mediation.runtime.MediationStartupComponent";
    public final static String SIB_CLASS_MF = "com.ibm.ws.sib.mediation.runtime.MediationFramework";
    public final static String SIB_CLASS_MP = "com.ibm.ws.sib.processor.impl.MessageProcessor";
    public final static String SIB_CLASS_MS = "com.ibm.ws.sib.msgstore.impl.MessageStoreImpl";
    public final static String SIB_CLASS_RA = "com.ibm.ws.sib.ra.SibRaEngineComponent";
    public final static String SIB_CLASS_TO_PROCESS = "com.ibm.ws.sib.trm.TrmMainImpl";
    public final static String SIB_CLASS_TO_ENGINE = "com.ibm.ws.sib.trm.TrmMeMainImpl";
    public final static String SIB_CLASS_WSN = "com.ibm.ws.sib.wsn.admin.impl.WSNEngineComponentImpl";
    public final static String SIB_CLASS_SECURITY = "com.ibm.ws.sib.security.impl.BusSecurityImpl";
    public final static String SIB_CLASS_WSRM = "com.ibm.ws.sib.wsrm.impl.WSRMEngineComponent";

    // Message resource group constants.

    public final static String MSG_GROUP = TraceGroups.TRGRP_ADMIN;
    public final static String TRGRP_AS = MSG_GROUP;
    public final static String MSG_BUNDLE = "com.ibm.ws.sib.admin.internal.CWSIDMessages";
    public final static String MSG_BUNDLE_COMMANDS = "com.ibm.ws.management.commands.sib.CWSJAMessages";

    // FFDC probe constants.

    public final static String PROBE_10 = "10";
    final static String PROBE_20 = "20";
    final static String PROBE_30 = "30";

    // Messaging Engine start modes (used on JsEngineComponent.start())
    // 250606.3 - adds recovery mode support
    // Note that these values are used to populate mode as a bitfield...

    public final static int ME_START_DEFAULT = 0;
    public final static int ME_START_FLUSH = 1;
    public final static int ME_START_RECOVERY = 2;

    // Messaging Engine start modes (used as arguments to SIBMessagingEngine
    // MBean)

    public final static String ME_START_DEFAULT_STR = "DEFAULT";
    public final static String ME_START_FLUSH_STR = "FLUSH";
    //250606.3 ME_START_RECOVERY_STR not needed - recovery only set during server recovery, not ME 

    // Messaging Engine stop modes (used on JsEngineComponent.stop())

    public final static int ME_STOP_IMMEDIATE = 0;
    public final static int ME_STOP_QUIESCE = ME_STOP_IMMEDIATE;
    public final static int ME_STOP_FORCE = 1;
    public final static int ME_STOP_MIN = ME_STOP_IMMEDIATE;
    public final static int ME_STOP_MAX = ME_STOP_FORCE;
    //Define codes for COMMS.. comms server would call ME stop with these two codes.
    public final static int ME_STOP_COMMS_CONNECTIONS = 2;
    public final static int ME_STOP_COMMS_SSL_CONNECTIONS = 3;

    // Messaging Engine stop modes (used as arguments to SIBMessagingEngine MBean)

    public final static String ME_STOP_IMMEDIATE_STR = "IMMEDIATE";
    public final static String ME_STOP_FORCE_STR = "FORCE";

    // ME state constants.

    public final static String ME_STATE = "SIB_ME_STATE";
    public final static String ME_STATE_UNINITIALIZED_STR = "Uninitialized";
    public final static String ME_STATE_UNINITIALIZED = ME_STATE + "." + ME_STATE_UNINITIALIZED_STR;
    public final static String ME_STATE_INITIALIZING_STR = "Initializing";
    public final static String ME_STATE_INITIALIZING = ME_STATE + "." + ME_STATE_INITIALIZING_STR;
    public final static String ME_STATE_INITIALIZED_STR = "Initialized";
    public final static String ME_STATE_INITIALIZED = ME_STATE + "." + ME_STATE_INITIALIZED_STR;
    public final static String ME_STATE_JOINING_STR = "Joining";
    public final static String ME_STATE_JOINING = ME_STATE + "." + ME_STATE_JOINING_STR;
    public final static String ME_STATE_JOINED_STR = "Joined";
    public final static String ME_STATE_JOINED = ME_STATE + "." + ME_STATE_JOINED_STR;
    public final static String ME_STATE_AUTOSTARTING_STR = "Autostarting";
    public final static String ME_STATE_AUTOSTARTING = ME_STATE + "." + ME_STATE_AUTOSTARTING_STR;
    public final static String ME_STATE_STARTING_STR = "Starting";
    public final static String ME_STATE_STARTING = ME_STATE + "." + ME_STATE_STARTING_STR;
    public final static String ME_STATE_STARTED_STR = "Started";
    public final static String ME_STATE_STARTED = ME_STATE + "." + ME_STATE_STARTED_STR;
    public final static String ME_STATE_STOPPING_STR = "Stopping";
    public final static String ME_STATE_STOPPING = ME_STATE + "." + ME_STATE_STOPPING_STR;
    public final static String ME_STATE_STOPPINGMEMBER_STR = "StoppingMember";
    public final static String ME_STATE_STOPPINGMEMBER = ME_STATE + "." + ME_STATE_STOPPINGMEMBER_STR;
    public final static String ME_STATE_STOPPED_STR = "Stopped";
    public final static String ME_STATE_STOPPED = ME_STATE + "." + ME_STATE_STOPPED_STR;
    public final static String ME_STATE_DESTROYING_STR = "Destroying";
    public final static String ME_STATE_DESTROYING = ME_STATE + "." + ME_STATE_DESTROYING_STR;
    public final static String ME_STATE_DESTROYED_STR = "Destroyed";
    public final static String ME_STATE_DESTROYED = ME_STATE + "." + ME_STATE_DESTROYED_STR;
    public final static String ME_STATE_FAILED_STR = "Failed!";
    public final static String ME_STATE_FAILED = ME_STATE + "." + ME_STATE_FAILED_STR;

    // Defined JMX MBean types supported by WPM.

/*    public final static String MBEAN_TYPE_JMSRESOURCE = "SIBJMSResource";
    public final static String MBEAN_TYPE_MAIN = "SIBMain";*/
    public final static String MBEAN_TYPE_BUS = "WEMBus";
    public final static String MBEAN_TYPE_ME = "WEMMessagingEngine";
    public final static String MBEAN_TYPE_QP = "WEMQueue";
    public final static String MBEAN_TYPE_PP = "WEMTopic";
/*    public final static String MBEAN_TYPE_MP = "SIBMediationPoint";
    public final static String MBEAN_TYPE_MEP = "SIBMediationExecutionPoint";
    public final static String MBEAN_TYPE_MMP = "SIBMediationMessagePoint";*/
    public final static String MBEAN_TYPE_SP = "WEMSubscriber";
/*    public final static String MBEAN_TYPE_RQP = "SIBRemoteQueuePoint";
    public final static String MBEAN_TYPE_RPP = "SIBRemotePublicationPoint";
    public final static String MBEAN_TYPE_RMP = "SIBRemoteMediationPoint";
    public final static String MBEAN_TYPE_RSP = "SIBRemoteSubscriptionPoint";
    public final static String MBEAN_TYPE_SIB_LINK = "SIBGatewayLink";
    public final static String MBEAN_TYPE_MQ_LINK = "SIBMQLink";
    public final static String MBEAN_TYPE_LINK_TRANSMITTER = "SIBLinkTransmitter";
    public final static String MBEAN_TYPE_MQ_LINK_SENDER_CHANNEL = "SIBMQLinkSenderChannel";
    public final static String MBEAN_TYPE_MQ_LINK_RECEIVER_CHANNEL = "SIBMQLinkReceiverChannel";
    public final static String MBEAN_TYPE_MQ_LINK_SENDER_CHANNEL_TRANSMITTER = "SIBMQLinkSenderChannelTransmitter";
    public final static String MBEAN_TYPE_MQ_PSB_BROKER_PROFILE = "SIBPSBBrokerProfile";*/

    // Prefix for the default exception destination. This will be suffixed with
    // the ME name.

    public static final String EXCEPTION_DESTINATION_PREFIX = "_SYSTEM.Exception.Destination.";

    // Runtime substitution strings used in WCCM attribute string values

    public static final String DEFAULT_EXCEPTION_DESTINATION = "$DEFAULT_EXCEPTION_DESTINATION";

    // The destination name for the "default topic space".

    public static final String DEFAULT_TOPIC_SPACE_NAME = "Default.Topic.Space";

    // Admin PMI constants.

    /** The root of all the XML files */

    public static final String ADMIN_PMI_XML_DIRNAME = "/com/ibm/ws/sib/admin/pmi/xml/";

    /** SIBService */

    public static final String SIBSERVICE_GROUP_NLS = "StatGroup.SIBService";
    public static final String SIBSERVICE_GROUP_XML = ADMIN_PMI_XML_DIRNAME + "SIBService.xml";

    /** SIBMessagingEngines */

    public static final String SIBME_GROUP_NLS = "StatGroup.SIBMessagingEngines";
    public static final String SIBME_GROUP_XML = ADMIN_PMI_XML_DIRNAME + "SIBMessagingEngines.xml";

    /** SIBMessagingEngine */

    public static final String SIBME_XML = ADMIN_PMI_XML_DIRNAME + "SIBMessagingEngine.xml";

    // Custom property for enablement of Notification Eventing.

    public static final String SIB_EVENT_NOTIFICATION_KEY = "sib.event.notification";
    // Value "enabled"
    public static final String SIB_EVENT_NOTIFICATION_VALUE_ENABLED = "enabled";
    // Value "disabled"
    public static final String SIB_EVENT_NOTIFICATION_VALUE_DISABLED = "disabled";

    public static final String CREATE_SIB_DESTINATION = "_CREATE_SIB_DESTINATION";

    // duplicated all over SERV1 but no single appropriate place to get them from
    // for SIB.  Should be in a console interface.
    public static final String SORT_ORDER_ASCENDING = "ASC";
    public static final String SORT_ORDER_DESCENDING = "DESC";
    

}
