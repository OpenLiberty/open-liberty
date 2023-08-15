/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.internal.interfaces;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchive;;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

import org.osgi.framework.ServiceReference;

/**
 * Service interface for components containing version specific CDI code to implement so they can provide SPI extension arhives
 */
public interface ExtensionArchiveFactory {

    /**
     * Creates and returns an extension archive based on a CDIExtensionMetadata
     *
     * @param cdiRuntime the CDI runtime
     * @param sr the service reference to the CDIExtensionMetadata with the classes that shall be included in this ExtensionArchive
     * @param webSphereCDIExtensionMetaData the actual CDIExtensionMetadata object with the classes that shall be included in this ExtensionArchive
     * @param applicationContext the applicationContext for the application that this extension will modifiy the CDI environment of
     * @return A newly created ExtensionArchive with whatever is defined in webSphereCDIExtensionMetaData
     * @throws CDIException if an unexpected exception is encountered
     */
    ExtensionArchive newSPIExtensionArchive(CDIRuntime cdiRuntime, ServiceReference<CDIExtensionMetadata> sr,
                                                    CDIExtensionMetadata webSphereCDIExtensionMetaData, WebSphereCDIDeployment applicationContext) throws CDIException;
}
