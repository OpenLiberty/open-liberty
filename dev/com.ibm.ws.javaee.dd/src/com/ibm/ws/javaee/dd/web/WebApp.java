/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
import com.ibm.ws.javaee.dd.common.ModuleDeploymentDescriptor;
import com.ibm.ws.javaee.dd.web.common.AbsoluteOrdering;
import com.ibm.ws.javaee.dd.web.common.WebCommon;

/**
 *
 */
public interface WebApp extends ModuleDeploymentDescriptor, DeploymentDescriptor, WebCommon {

    static final String DD_NAME = "WEB-INF/web.xml";

    static final int VERSION_5_0 = 50;

    static final int VERSION_4_0 = 40;

    static final int VERSION_3_1 = 31;

    static final int VERSION_3_0 = 30;

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
     * @return &lt;module-name>, or null if unspecified
     */
    @Override
    String getModuleName();

    /**
     * @return &lt;absolute-ordering>, or null if unspecified
     */
    AbsoluteOrdering getAbsoluteOrdering();

}
