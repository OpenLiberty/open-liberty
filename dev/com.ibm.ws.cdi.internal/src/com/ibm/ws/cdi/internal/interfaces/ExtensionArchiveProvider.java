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

import java.util.Collection;

import com.ibm.ws.cdi.CDIException;

/**
 * Service interface for components which may want to contribute additional extension archives to the deployment
 */
public interface ExtensionArchiveProvider {

    /**
     * Return the archives that should be added for this deployment
     *
     * @param cdiRuntime the CDI runtime
     * @param deployment the application deployment
     * @return a collection of additional extension archives to add
     * @throws CDIException if an unexpected exception is encountered
     */
    Collection<ExtensionArchive> getArchives(CDIRuntime cdiRuntime, WebSphereCDIDeployment deployment) throws CDIException;

}
