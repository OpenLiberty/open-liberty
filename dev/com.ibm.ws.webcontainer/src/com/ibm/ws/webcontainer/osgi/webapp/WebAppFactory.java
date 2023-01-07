/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.osgi.webapp;

import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 *
 */
public interface WebAppFactory {

    WebApp createWebApp(WebAppConfiguration webAppConfig,
                  ClassLoader moduleLoader,
                  ReferenceContext referenceContext,
                  MetaDataService metaDataService,
                  J2EENameFactory j2eeNameFactory,
                  ManagedObjectService managedObjectService);
}
