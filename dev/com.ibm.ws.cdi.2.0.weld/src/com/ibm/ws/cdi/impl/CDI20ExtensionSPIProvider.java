/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.cdi.impl;

import java.util.Collection;
import java.util.Map;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchive;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchiveProvider;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.framework.ServiceReference;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class CDI20ExtensionSPIProvider implements ExtensionArchiveProvider {

    @Override
    public ExtensionArchive newSPIExtensionArchive(CDIRuntime cdiRuntime, ServiceReference<CDIExtensionMetadata> sr,
                                                    CDIExtensionMetadata webSphereCDIExtensionMetaData, WebSphereCDIDeployment applicationContext) throws CDIException {
        return CDIUtils.newSPIExtensionArchive(cdiRuntime, sr, webSphereCDIExtensionMetaData, applicationContext);
    }
}
