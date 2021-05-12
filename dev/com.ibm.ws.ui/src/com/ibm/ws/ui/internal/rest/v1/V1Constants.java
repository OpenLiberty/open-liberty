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
package com.ibm.ws.ui.internal.rest.v1;

import com.ibm.ws.ui.internal.rest.APIConstants;

/**
 * Defines the URL resource paths for the version 1 adminCenter REST API.
 */
public interface V1Constants {
    /**
     * The tool data resource path.
     */
    String TOOLDATA_PATH = APIConstants.V1_ROOT_PATH + "/tooldata";
    /**
     * The catalog resource path.
     */
    String CATALOG_PATH = APIConstants.V1_ROOT_PATH + "/catalog";

    /**
     * The toolbox resource path.
     */
    String TOOLBOX_PATH = APIConstants.V1_ROOT_PATH + "/toolbox";

    /**
     * The utility resource path.
     */
    String UTILS_PATH = APIConstants.V1_ROOT_PATH + "/utils";

    /**
     * The icons resource path.
     */
    String ICON_PATH = APIConstants.V1_ROOT_PATH + "/icons";

    /**
     * The deployTool validation path
     */
    String DEPLOY_VALIDATION_PATH = APIConstants.V1_ROOT_PATH + "/deployValidation";
}
