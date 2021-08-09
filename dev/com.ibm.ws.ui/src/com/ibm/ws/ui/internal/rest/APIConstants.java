/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.rest;

/**
 * Defines the URL resource paths for the core adminCenter REST API.
 */
public interface APIConstants {
    /**
     * The maximum size of the JSON String which we will read in from a POST.
     */
    int POST_MAX_JSON_SIZE = 8192;

    /**
     * The maximum size of the plain text String which we will read in from a POST.
     */
    int POST_MAX_PLAIN_TEXT_SIZE = 8192;

    /**
     * The base path from the IBM API path from which our REST API is rooted.
     */
    String ADMIN_CENTER_ROOT_PATH = "/adminCenter";

    /**
     * The base path of the version 1 REST API.
     */
    String V1_ROOT_PATH = ADMIN_CENTER_ROOT_PATH + "/v1";
}
