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
package com.ibm.ws.ui.internal.v1.utils;

import java.net.URL;
import java.util.Map;

import com.ibm.ws.ui.internal.rest.exceptions.RESTException;

/**
 *
 */
public interface URLUtility {

    /**
     * Logic used to grab the HTTP GET status of a URL.
     * 
     * @param url The URL to inspect. Must not be {@code null}.
     * @return A JSON object with the URL and the HTTP status code which represents the status of a GET for the URL
     * @throws RESTException If the reaching the URL encountered an error
     */
    Map<String, Object> getStatus(final URL url) throws RESTException;

    /**
     * Attempts to construct a Tool object from the given URL by attempting
     * to find the name, description and other related metadata from the URL.
     * 
     * @param url The URL to inspect. Must not be {@code null}.
     * @return A JSON object with the URL being reachable and a Tool object with as many of the fields prepopulated as is possible.
     * @throws RESTException If the reaching the URL encountered an error
     */
    Map<String, Object> analyzeURL(final URL url) throws RESTException;

}
