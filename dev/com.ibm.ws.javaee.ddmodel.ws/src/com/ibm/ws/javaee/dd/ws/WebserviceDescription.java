/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ws;

import java.util.List;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;

public interface WebserviceDescription {

    /**
     * @return &lt;description>, or null if unspecified
     */
    public Description getDescription();

    /**
     * @return &lt;display-name>, or null if unspecified
     */
    public DisplayName getDisplayName();

    /**
     * @return &lt;icon>, or null if unspecified
     */
    public Icon getIcon();

    public String getWebserviceDescriptionName();

    public String getWSDLFile();

    public List<PortComponent> getPortComponents();

    public String getJAXRPCMappingFile();
}
