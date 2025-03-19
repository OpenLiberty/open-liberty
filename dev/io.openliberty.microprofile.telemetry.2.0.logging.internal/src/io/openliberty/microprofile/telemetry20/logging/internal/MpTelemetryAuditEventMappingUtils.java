/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.logging.internal;

/**
 * This class contains helper methods to correctly format and map the audit event fields to
 * OpenTelemetry, using the OpenTelemetry semantic naming conventions.
 */
public class MpTelemetryAuditEventMappingUtils {

    /*
     * The following are Liberty Audit type tables that have camelCase field names, that need to be
     * formatted into snake_case format. The other audit fields do not need to be formatted,
     * they can just be directly mapped, with the "io.openliberty.audit.*" prefix prepended to it.
     *
     * e.g.
     * target.typeURI => io.openliberty.audit.target.type_uri
     * target.id => io.openliberty.audit.target.id
     *
     */
    private final static String[][] AUDIT_EVENT_TABLE = {
                                                          { MpTelemetryLogFieldConstants.AUDIT_EVENT_NAME, "event_name" },
                                                          { MpTelemetryLogFieldConstants.AUDIT_EVENT_SEQUENCE_NUMBER, "event_sequence_number" } };

    private final static String[][] AUDIT_OBSERVER_TABLE = {
                                                             { MpTelemetryLogFieldConstants.AUDIT_OBSERVER_TYPEURI, "observer.type_uri" }

    };

    private final static String[][] AUDIT_TARGET_TABLE = {
                                                           { MpTelemetryLogFieldConstants.AUDIT_TARGET_TYPEURI, "target.type_uri" },
                                                           { MpTelemetryLogFieldConstants.AUDIT_TARGET_REPOSITORY_ID, "target.repository_id" },
                                                           { MpTelemetryLogFieldConstants.AUDIT_TARGET_UNIQUENAME, "target.unique_name" },
                                                           { MpTelemetryLogFieldConstants.AUDIT_TARGET_ENTITY_TYPE, "target.entity_type" },
                                                           { MpTelemetryLogFieldConstants.AUDIT_TARGET_EXTENDED_PROPERTIES, "target.extended_properties" },
                                                           { MpTelemetryLogFieldConstants.AUDIT_TARGET_APPLICATION_ID, "target.application_id" },
                                                           { MpTelemetryLogFieldConstants.AUDIT_TARGET_TOKEN_ID, "target.token_id" },
                                                           { MpTelemetryLogFieldConstants.AUDIT_TARGET_CLIENT_ID, "target.client_id" },
                                                           { MpTelemetryLogFieldConstants.AUDIT_TARGET_INITIATOR_ROLE, "target.initiator_role" },
                                                           { MpTelemetryLogFieldConstants.AUDIT_TARGET_NUMBER_REVOKED, "target.number_revoked" },
                                                           { MpTelemetryLogFieldConstants.AUDIT_TARGET_USERID, "target.user_id" }

    };

    private final static String[][] AUDIT_TARGET_MESSAGING_TABLE = {
                                                                     { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_LOGIN_TYPE, "target.messaging.login_type" },
                                                                     { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_REMOTE_CHAIN_NAME,
                                                                       "target.messaging.remote.chain_name" },
                                                                     { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_USER_NAME, "target.messaging.user_name" },
                                                                     { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_OPERATIONTYPE, "target.messaging.operation_type" },
                                                                     { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_CALLTYPE, "target.messaging.call_type" },
                                                                     { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_RESOURCE, "target.messaging.jms_resource" },
                                                                     { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_ACTIONS, "target.messaging.jms_actions" },
                                                                     { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_ROLES, "target.messaging.jms_roles" },
                                                                     { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_REQUESTOR_TYPE,
                                                                       "target.messaging.jms_requestor_type" },
                                                                     { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_QUEUE_PERMISSIONS,
                                                                       "target.messaging.queue_permissions" },
                                                                     { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_TOPIC_PERMISSIONS,
                                                                       "target.messaging.topic_permissions" },
                                                                     { MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_JMS_TEMPORARY_DESTINATION_PERMISSIONS,
                                                                       "target.messaging.temp_destination_permissions" }
    };

    private final static String[][] AUDIT_TARGET_JMX_TABLE = {
                                                               { MpTelemetryLogFieldConstants.AUDIT_TARGET_JMX_MBEAN_QUERYEXP, "target.jmx.mbean.query_exp" }
    };

    private final static String[][] AUDIT_INITIATOR_TABLE = {
                                                              { MpTelemetryLogFieldConstants.AUDIT_INITIATOR_TYPEURI, "initiator.type_uri" }
    };

    private final static String[][] AUDIT_REASON_TABLE = {
                                                           { MpTelemetryLogFieldConstants.AUDIT_REASON_CODE, "reason.reason_code" },
                                                           { MpTelemetryLogFieldConstants.AUDIT_REASON_TYPE, "reason.reason_type" }
    };

    // Helper variables
    private final static int AUDIT_TYPE_FIELD_INDEX = 0; // Audit CADF JSON field name index
    private final static int OTEL_AUDIT_TYPE_FIELD_INDEX = 1; // OpenTelemetry formatted Audit field name index

    /**
     * Gets the OpenTelemetry formatted audit event key name.
     *
     * @param auditKey
     * @return
     */
    public static String getOTelMappedAuditEventKeyName(String auditKey) {
        String formattedAuditKey = getFormattedAuditEventKey(auditKey);

        // Prepends the "io.openliberty.audit." prefix to the formatted key name.
        StringBuffer sb = new StringBuffer();
        sb.append(MpTelemetryLogFieldConstants.IO_OPENLIBERTY_TAG);
        sb.append(MpTelemetryLogFieldConstants.AUDIT_TYPE_PREFIX_TAG);
        sb.append(formattedAuditKey);

        return sb.toString();
    }

    /**
     * Gets the formatted audit event key from the audit table.
     *
     * @param auditKey
     * @return
     */
    private static String getFormattedAuditEventKey(String auditKey) {
        String[][] auditTable = getAuditTypeTableFromKey(auditKey);
        String formattedKey = null;

        if (auditTable != null) {
            // Finds the OTel formatted audit field key with the provided audit key, from the respective audit type table.
            for (String[] field : auditTable) {
                if (field[AUDIT_TYPE_FIELD_INDEX].equals(auditKey)) {
                    // Retrieves the OTel formatted mapped key, for the corresponding auditKey
                    formattedKey = field[OTEL_AUDIT_TYPE_FIELD_INDEX];
                    break;
                }
            }
        }

        if (formattedKey == null) {
            // No match was found from the audit table, which means the audit field is not in camelCase format for this type,
            // thus, does not need to be formatted, can be directly mapped.
            formattedKey = auditKey;
        }

        return formattedKey;
    }

    /**
     * Get the corresponding audit type table, given the audit key.
     *
     * @param auditKey
     * @return
     */
    private static String[][] getAuditTypeTableFromKey(String auditKey) {
        if (auditKey.startsWith(MpTelemetryLogFieldConstants.AUDIT_EVENT_PREFIX)) {
            return AUDIT_EVENT_TABLE;
        } else if (auditKey.startsWith(MpTelemetryLogFieldConstants.AUDIT_OBSERVER_PREFIX)) {
            return AUDIT_OBSERVER_TABLE;
        } else if (auditKey.startsWith(MpTelemetryLogFieldConstants.AUDIT_TARGET_PREFIX)) {
            if (auditKey.contains(MpTelemetryLogFieldConstants.AUDIT_TARGET_MESSAGING_PREFIX)) {
                // audit key = target.messaging.*
                return AUDIT_TARGET_MESSAGING_TABLE;
            }
            if (auditKey.contains(MpTelemetryLogFieldConstants.AUDIT_TARGET_JMX_PREFIX)) {
                // audit key = target.jmx.*
                return AUDIT_TARGET_JMX_TABLE;
            }
            // audit key = target.*
            return AUDIT_TARGET_TABLE;
        } else if (auditKey.startsWith(MpTelemetryLogFieldConstants.AUDIT_INITIATOR_PREFIX)) {
            return AUDIT_INITIATOR_TABLE;
        } else if (auditKey.startsWith(MpTelemetryLogFieldConstants.AUDIT_REASON_PREFIX)) {
            return AUDIT_REASON_TABLE;
        } else {
            return null;
        }
    }

}
