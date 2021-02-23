/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.rest.v1.utils;

import com.ibm.ws.ui.internal.rest.v1.V1Constants;

/**
 * Defines the URL resource paths for the version 1 utilities of the adminCenter
 * REST API.
 */
public interface V1UtilsConstants {

    /**
     * The feature utils path.
     */
    String FEATURE_UTILS_PATH = V1Constants.UTILS_PATH + "/feature";

    /**
     * The URL utils path.
     */
    String URL_UTILS_PATH = V1Constants.UTILS_PATH + "/url";
}
