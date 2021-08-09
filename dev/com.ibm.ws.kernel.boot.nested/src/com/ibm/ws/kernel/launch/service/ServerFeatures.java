/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.service;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

public interface ServerFeatures {

    public final static String REQUEST_SERVER_FEATURES_PROPERTY = BootstrapConstants.REQUEST_SERVER_FEATURES_PROPERTY;

    /**
     * Get an array of feature names for the features required by the server.
     */
    String[] getServerFeatureNames();
}
