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

package com.ibm.ws.jmx.connector.client.rest.internal;

class ClientConstants {

    enum HttpMethod {
        GET, POST, PUT, DELETE,
        HEAD, OPTIONS, TRACE
    };

    static final String JSON_MIME_TYPE = "application/json";

    static final boolean DISABLE_HOSTNAME_VERIFICATION_DEFAULT = false;

    static final int NOTIFICATION_DELIVERY_INTERVAL_DEFAULT = 0;

    static final int NOTIFICATION_FETCH_INTERVAL_DEFAULT = 1000;

    static final int NOTIFICATION_INBOX_EXPIRY_DEFAULT = 5 * 60 * 1000;

    static final int SERVER_FAILOVER_INTERVAL_DEFAULT = 30 * 1000;

    static final int READ_TIMEOUT_DEFAULT = 60 * 1000;

    static final int MAX_SERVER_WAIT_TIME_DEFAULT = 120 * 1000;

    static final int SERVER_STATUS_POLLING_INTERVAL_DEFAULT = 4 * 1000;

    static final String CONNECTOR_URI = "IBMJMXConnectorREST";

    static final String ROUTER_URI = CONNECTOR_URI + "/router";
}
