/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.interfaces;

import java.net.URL;

import org.jboss.weld.bootstrap.spi.BeansXml;

/**
 *
 */
public interface BeanParser {

    /**
     * Parse a BeansXml from the given URL resource
     *
     * @param cdiDeployment The CDI Deployment that the beans.xml is part of
     * @param beansXmlUrl   A URL where the beans.xml file can be found
     * @return A BeansXml instance
     */
    BeansXml parse(WebSphereCDIDeployment cdiDeployment, URL beansXmlUrl);

}
