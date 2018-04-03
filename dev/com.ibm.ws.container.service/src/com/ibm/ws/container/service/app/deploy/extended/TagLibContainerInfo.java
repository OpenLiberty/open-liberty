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
package com.ibm.ws.container.service.app.deploy.extended;

import java.util.List;

import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;

/**
 * Optional information that can be provided to the {@link NonPersistentCache}
 * for a module container to provide container information about where to look
 * for JSP tag libraries
 */
public interface TagLibContainerInfo {
    public List<ContainerInfo> getTagLibContainers();
}
