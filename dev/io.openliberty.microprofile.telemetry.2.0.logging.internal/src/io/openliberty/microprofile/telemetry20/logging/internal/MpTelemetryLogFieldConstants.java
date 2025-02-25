/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.logging.internal;

/**
 * This class contains the transformed Liberty event field names to match OpenTelemetry Log Attributes naming convention.
 */
public class MpTelemetryLogFieldConstants {

    /*
     * Common Mapped OTel Attribute Log fields
     */
    public static final String LIBERTY_TYPE = "io.openliberty.type";
    public static final String LIBERTY_SEQUENCE = "io.openliberty.sequence";
    public static final String LIBERTY_CLASSNAME = "io.openliberty.class_name";

    /*
     * Mapped OTel Attribute Liberty message and trace log fields
     */
    public static final String LIBERTY_MESSAGEID = "io.openliberty.message_id";
    public static final String LIBERTY_MODULE = "io.openliberty.module";
    public static final String LIBERTY_METHODNAME = "io.openliberty.method_name";

    /*
     * Mapped OTel Attribute Liberty FFDC log fields
     */
    public static final String LIBERTY_PROBEID = "io.openliberty.probe_id";
    public static final String LIBERTY_OBJECTDETAILS = "io.openliberty.object_details";

    /*
     * Mapped OTel Attribute Liberty LogRecordContext Extension fields
     */
    public static final String LIBERTY_EXT_APP_NAME = "io.openliberty.ext.app_name";

    /*
     * Miscellaneous
     */
    public static final String EXT_APPNAME = "ext_appName";
    public static final String EXT_THREAD = "ext_thread";
    public static final String IO_OPENLIBERTY_TAG = "io.openliberty.";
    public static final String IO_OPENLIBERTY_EXT_TAG = "io.openliberty.ext.";
    public static final String LOGGING_EVENT_TIME = "loggingEventTime";
    public static final String LOGGING_SEQUENCE_NUMBER = "loggingSequenceNumber";

    /*
     * OpenTelemetry Scope Info field
     */
    public static final String OTEL_SCOPE_INFO = "scopeInfo:";

    /*
     * Audit type prefix
     */
    public static final String AUDIT_TYPE_PREFIX_TAG = "audit.";

    /**
     * Audit Events
     */
    public final static String AUDIT_EVENT_PREFIX = "event";
    public static final String AUDIT_EVENT_NAME = "eventName";
    public static final String AUDIT_EVENT_SEQUENCE_NUMBER = "eventSequenceNumber";
    public static final String AUDIT_EVENT_TIME = "eventTime";

    /**
     * Audit Observers
     */
    public final static String AUDIT_OBSERVER_PREFIX = "observer";
    public final static String AUDIT_OBSERVER_TYPEURI = "observer.typeURI";

    /**
     * Audit Targets
     */
    public final static String AUDIT_TARGET_PREFIX = "target";
    public final static String AUDIT_TARGET_TYPEURI = "target.typeURI";
    public final static String AUDIT_TARGET_REPOSITORY_ID = "target.repositoryId";
    public final static String AUDIT_TARGET_UNIQUENAME = "target.uniqueName";
    public final static String AUDIT_TARGET_ENTITY_TYPE = "target.entityType";
    public final static String AUDIT_TARGET_EXTENDED_PROPERTIES = "target.extendedProperties";
    public final static String AUDIT_TARGET_APPLICATION_ID = "target.applicationId";
    public final static String AUDIT_TARGET_TOKEN_ID = "target.tokenId";
    public final static String AUDIT_TARGET_CLIENT_ID = "target.clientId";
    public final static String AUDIT_TARGET_INITIATOR_ROLE = "target.initiatorRole";
    public final static String AUDIT_TARGET_NUMBER_REVOKED = "target.numberRevoked";
    public final static String AUDIT_TARGET_USERID = "target.userId";

    /**
     * Audit Target Messaging
     */
    public final static String AUDIT_TARGET_MESSAGING_PREFIX = "target.messaging";
    public final static String AUDIT_TARGET_MESSAGING_LOGIN_TYPE = "target.messaging.loginType";
    public final static String AUDIT_TARGET_MESSAGING_REMOTE_CHAIN_NAME = "target.messaging.remote.chainName";
    public final static String AUDIT_TARGET_MESSAGING_USER_NAME = "target.messaging.userName";
    public final static String AUDIT_TARGET_MESSAGING_OPERATIONTYPE = "target.messaging.operationType";
    public final static String AUDIT_TARGET_MESSAGING_CALLTYPE = "target.messaging.callType";
    public final static String AUDIT_TARGET_MESSAGING_JMS_RESOURCE = "target.messaging.jmsResource";
    public final static String AUDIT_TARGET_MESSAGING_JMS_ACTIONS = "target.messaging.jmsActions";
    public final static String AUDIT_TARGET_MESSAGING_JMS_ROLES = "target.messaging.jmsRoles";
    public final static String AUDIT_TARGET_MESSAGING_JMS_REQUESTOR_TYPE = "target.messaging.jmsRequestorType";
    public final static String AUDIT_TARGET_MESSAGING_JMS_QUEUE_PERMISSIONS = "target.messaging.queuePermissions";
    public final static String AUDIT_TARGET_MESSAGING_JMS_TOPIC_PERMISSIONS = "target.messaging.topicPermissions";
    public final static String AUDIT_TARGET_MESSAGING_JMS_TEMPORARY_DESTINATION_PERMISSIONS = "target.messaging.tempDestinationPermissions";

    /**
     * Audit Target JMX
     */
    public final static String AUDIT_TARGET_JMX_PREFIX = "target.jmx";
    public final static String AUDIT_TARGET_JMX_MBEAN_QUERYEXP = "target.jmx.mbean.queryExp";

    /**
     * Audit Initiator
     */
    public final static String AUDIT_INITIATOR_PREFIX = "initiator";
    public final static String AUDIT_INITIATOR_TYPEURI = "initiator.typeURI";

    /**
     * Audit Reason
     */
    public final static String AUDIT_REASON_PREFIX = "reason";
    public final static String AUDIT_REASON_CODE = "reason.reasonCode";
    public final static String AUDIT_REASON_TYPE = "reason.reasonType";

    public static final String ACCESS_TYPE_PREFIX_TAG = "access_log.";

    public static final String OPENLIBERTY_ACCESS_PREFIX = IO_OPENLIBERTY_TAG + ACCESS_TYPE_PREFIX_TAG;

    public static final String ACCESS_COOKIE_PREFIX = "cookie_";
    public static final String ACCESS_REQUEST_HEADER_PREFIX = "requestHeader_";
    public static final String ACCESS_RESPONSE_HEADER_PREFIX = "responseHeader_";

    /**
     * Access logs attribute names
     */
    public static final String ACCESS_REMOTE_HOST = "remoteHost";
    public static final String ACCESS_REQUEST_METHOD = "requestMethod";
    public static final String ACCESS_REQUEST_PORT = "requestPort";
    public static final String ACCESS_REQUEST_FIRST_LINE = "requestFirstLine";
    public static final String ACCESS_RESPONSE_CODE = "responseCode";
    public static final String ACCESS_REQUEST_START_TIME = "requestStartTime";
    public static final String ACCESS_REMOTE_USER_ID = "remoteUserID";
    public static final String ACCESS_URI_PATH = "uriPath";
    public static final String ACCESS_ELAPSED_TIME = "elapsedTime";
    public static final String ACCESS_REMOTE_IP = "remoteIP";
    public static final String ACCESS_REQUEST_HOST = "requestHost";
    public static final String ACCESS_REQUEST_ELAPSED_TIME = "requestElapsedTime";
    public static final String ACCESS_SEQUENCE = "sequence";
    public static final String ACCESS_BYTES_SENT = "bytesSent";
    public static final String ACCESS_USER_AGENT = "userAgent";
    public static final String ACCESS_BYTES_RECEIVED = "bytesReceived";

}
