/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.web;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.web.common.Ordering;
import com.ibm.ws.javaee.dd.web.common.WebCommon;

public interface WebFragment extends DeploymentDescriptor, WebCommon {
    String DD_SHORT_NAME = "web-fragment.xml";
    String DD_NAME = "META-INF/web-fragment.xml";

    int[] VERSIONS = {
                       WebApp.VERSION_3_0, WebApp.VERSION_3_1, WebApp.VERSION_4_0,
                       WebApp.VERSION_5_0, WebApp.VERSION_6_0, WebApp.VERSION_6_1
    };

    String getVersion();

    boolean isSetMetadataComplete();

    boolean isMetadataComplete();

    String getName();

    Ordering getOrdering();
}
