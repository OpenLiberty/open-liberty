/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.web;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.web.common.Ordering;
import com.ibm.ws.javaee.dd.web.common.WebCommon;

/**
 *
 */
public interface WebFragment
                extends DeploymentDescriptor, WebCommon {
    static final String DD_NAME = "META-INF/web-fragment.xml";

    /**
     * @return version="..." attribute value
     */
    String getVersion();

    /**
     * @return true if metadata-complete="..." attribute is specified
     */
    boolean isSetMetadataComplete();

    /**
     * @return metadata-complete="..." attribute value if specified
     */
    boolean isMetadataComplete();

    /**
     * @return &lt;name>, or null if unspecified
     */
    String getName();

    /**
     * @return &lt;ordering>, or null if unspecified
     */
    Ordering getOrdering();

}
