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
package com.ibm.ws.javaee.dd.jsf;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;

/**
 * Represents &lt;faces-config>.
 */
public interface FacesConfig extends DeploymentDescriptor {

    static final String DD_NAME = "WEB-INF/faces-config.xml";

    static final int VERSION_2_0 = 20;

    static final int VERSION_2_2 = 22;

    /**
     * @return version="..." attribute value
     */
    String getVersion();

    List<FacesConfigManagedBean> getManagedBeans();

    // Added for CDI 1.2 support
    List<String> getManagedObjects();

}
