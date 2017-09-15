/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.wab.helper;

import java.util.List;

import org.osgi.framework.Bundle;

import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.wsspi.adaptable.module.Container;

/**
 * Internal helper that allows class ContainerInfos to be added for the Container
 * of a WAB. Helpers are registered as OSGi services and called for each WAB
 * that is deployed to the web container
 */
public interface WABClassInfoHelper {
    /**
     * Returns addition ContainerInfo objects that will be associated with the
     * Container of the specified WAB.
     * 
     * @param wabContainer The Container
     * @param wab the WAB
     * @return the ContainerInfo objects for the WAB
     */
    List<ContainerInfo> getContainerInfos(Container wabContainer, Bundle wab);
}
