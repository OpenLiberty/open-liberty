/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package com.ibm.ws.jsp.taglib;

import java.util.List;

import com.ibm.ws.container.service.app.deploy.ContainerInfo;

/**
 *
 */
public interface SharedLibClassesContainerInfo {
    public List<ContainerInfo> getSharedLibraryClassesContainerInfo();
    public List<ContainerInfo> getCommonLibraryClassesContainerInfo();
}
