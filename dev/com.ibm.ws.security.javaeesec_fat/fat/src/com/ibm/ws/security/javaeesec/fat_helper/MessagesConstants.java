/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat_helper;

/**
 *
 */
public class MessagesConstants {

    // Values to be verified in messages
    protected static final String MSG_JASPI_AUTHENTICATION_FAILED = "CWWKS1652A:.*";
    protected static final String PROVIDER_AUTHENTICATION_FAILED = "Invalid user or password";
    protected static final String MSG_AUTHORIZATION_FAILED = "CWWKS9104A:.*";
    protected static final String MSG_JACC_AUTHORIZATION_FAILED = "CWWKS9124A:.*";

    protected static final String MSG_JASPI_PROVIDER_ACTIVATED = "CWWKS1653I";
    protected static final String MSG_JASPI_PROVIDER_DEACTIVATED = "CWWKS1654I";

    protected static final String MSG_JACC_SERVICE_STARTING = "CWWKS2850I";
    protected static final String MSG_JACC_SERVICE_STARTED = "CWWKS2851I";
}
