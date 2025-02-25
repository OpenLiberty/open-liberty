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

import io.opentelemetry.semconv.SemanticAttributes;

/**
 * This class contains helper methods to correctly format and map the access event fields to
 * OpenTelemetry, using the OpenTelemetry semantic naming conventions.
 */
public class MpTelemetryAccessEventMappingUtils {

    private final static String[][] ACCESS_ATTRIBUTE_TABLE = {
                                                               { MpTelemetryLogFieldConstants.ACCESS_REMOTE_HOST, SemanticAttributes.CLIENT_ADDRESS.toString() },
                                                               { MpTelemetryLogFieldConstants.ACCESS_REQUEST_METHOD, SemanticAttributes.HTTP_REQUEST_METHOD.toString() },
                                                               { MpTelemetryLogFieldConstants.ACCESS_REQUEST_PORT, SemanticAttributes.NETWORK_LOCAL_PORT.toString() },
                                                               { MpTelemetryLogFieldConstants.ACCESS_REQUEST_FIRST_LINE,
                                                                 MpTelemetryLogFieldConstants.OPENLIBERTY_ACCESS_PREFIX
                                                                                                                         + "request_first_line" },
                                                               { MpTelemetryLogFieldConstants.ACCESS_RESPONSE_CODE, SemanticAttributes.HTTP_RESPONSE_STATUS_CODE.toString() },
                                                               { MpTelemetryLogFieldConstants.ACCESS_REQUEST_START_TIME,
                                                                 MpTelemetryLogFieldConstants.OPENLIBERTY_ACCESS_PREFIX
                                                                                                                         + "request_start_time" },
                                                               { MpTelemetryLogFieldConstants.ACCESS_REMOTE_USER_ID,
                                                                 MpTelemetryLogFieldConstants.OPENLIBERTY_ACCESS_PREFIX
                                                                                                                     + "remote_user_id" },
                                                               { MpTelemetryLogFieldConstants.ACCESS_URI_PATH,
                                                                 MpTelemetryLogFieldConstants.OPENLIBERTY_ACCESS_PREFIX
                                                                                                               + "url.path" },
                                                               { MpTelemetryLogFieldConstants.ACCESS_ELAPSED_TIME,
                                                                 MpTelemetryLogFieldConstants.OPENLIBERTY_ACCESS_PREFIX
                                                                                                                   + "elapsed_time" },
                                                               { MpTelemetryLogFieldConstants.ACCESS_REMOTE_IP, SemanticAttributes.NETWORK_PEER_ADDRESS.toString() },
                                                               { MpTelemetryLogFieldConstants.ACCESS_REQUEST_HOST, SemanticAttributes.SERVER_ADDRESS.toString() },
                                                               { MpTelemetryLogFieldConstants.ACCESS_REQUEST_ELAPSED_TIME,
                                                                 MpTelemetryLogFieldConstants.OPENLIBERTY_ACCESS_PREFIX
                                                                                                                           + "request_elapsed_time" },
                                                               { MpTelemetryLogFieldConstants.ACCESS_SEQUENCE,
                                                                 MpTelemetryLogFieldConstants.LIBERTY_SEQUENCE },
                                                               { MpTelemetryLogFieldConstants.ACCESS_BYTES_SENT,
                                                                 MpTelemetryLogFieldConstants.OPENLIBERTY_ACCESS_PREFIX
                                                                                                                 + "bytes_sent" },
                                                               { MpTelemetryLogFieldConstants.ACCESS_USER_AGENT, SemanticAttributes.USER_AGENT_ORIGINAL.toString() },
                                                               { MpTelemetryLogFieldConstants.ACCESS_BYTES_RECEIVED,
                                                                 MpTelemetryLogFieldConstants.OPENLIBERTY_ACCESS_PREFIX
                                                                                                                     + "bytes_received" },

    };

    private final static String[][] ACCESS_SPECIAL_KEY_ATTRIBUTE_TABLE = {
                                                                           { MpTelemetryLogFieldConstants.ACCESS_RESPONSE_HEADER_PREFIX, "http.response.header." },
                                                                           { MpTelemetryLogFieldConstants.ACCESS_REQUEST_HEADER_PREFIX, "http.request.header." }

    };

    private final static String[][] ACCESS_COOKIE_KEY_ATTRIBUTE_TABLE = {
                                                                          { MpTelemetryLogFieldConstants.ACCESS_COOKIE_PREFIX,
                                                                            MpTelemetryLogFieldConstants.OPENLIBERTY_ACCESS_PREFIX
                                                                                                                               + "cookie_" }

    };

    // Helper variables
    private final static int ACCESS_TYPE_FIELD_INDEX = 0; // Access CADF JSON field name index
    private final static int OTEL_ACCESS_TYPE_FIELD_INDEX = 1; // OpenTelemetry formatted Access field name index

    /**
     * Gets the formatted access event key from the Access table.
     *
     * @param accessKey
     * @return
     */
    public static String getOTelMappedAccessEventKeyName(String accessKey) {
        String[][] accessTable = getSpecialAccessTypes(accessKey);
        String formattedKey = null;

        if (accessTable != null) {
            // Finds the OTel formatted access field key with the provided access key, from the respective access type table.
            for (String[] field : accessTable) {
                if (accessTable == ACCESS_SPECIAL_KEY_ATTRIBUTE_TABLE) {
                    if (accessKey.contains(field[ACCESS_TYPE_FIELD_INDEX])) {
                        formattedKey = accessKey.replace(field[ACCESS_TYPE_FIELD_INDEX].toString(), field[OTEL_ACCESS_TYPE_FIELD_INDEX].toString());
                    }
                } else if (accessTable == ACCESS_COOKIE_KEY_ATTRIBUTE_TABLE) {
                    formattedKey = accessKey.replace(field[ACCESS_TYPE_FIELD_INDEX].toString(), field[OTEL_ACCESS_TYPE_FIELD_INDEX].toString());
                } else {
                    if (field[ACCESS_TYPE_FIELD_INDEX].equals(accessKey)) {
                        // Retrieves the OTel formatted mapped key, for the corresponding accessKey
                        formattedKey = field[OTEL_ACCESS_TYPE_FIELD_INDEX];

                        if (formattedKey == null) {
                            // No match was found from the access table, which means the access field is not in camelCase format for this type,
                            // thus, does not need to be formatted, can be directly mapped.
                        }

                        break;
                    }
                }

            }
        }

        return formattedKey;
    }

    private static String[][] getSpecialAccessTypes(String accessKey) {
        if (accessKey.startsWith(MpTelemetryLogFieldConstants.ACCESS_REQUEST_HEADER_PREFIX) || accessKey.startsWith(MpTelemetryLogFieldConstants.ACCESS_RESPONSE_HEADER_PREFIX)) {
            return ACCESS_SPECIAL_KEY_ATTRIBUTE_TABLE;
        } else if (accessKey.startsWith(MpTelemetryLogFieldConstants.ACCESS_COOKIE_PREFIX)) {
            return ACCESS_COOKIE_KEY_ATTRIBUTE_TABLE;
        } else
            return ACCESS_ATTRIBUTE_TABLE;
    }

}