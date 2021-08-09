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
package com.ibm.ws.javaee.dd;

/**
 *
 */
public interface DeploymentDescriptor {
    /**
     * @return the path for the deployment descriptor.
     */
    String getDeploymentDescriptorPath();

    /**
     * @return the deployment descriptor component with the matching id="..." attribute, or null if not present
     */
    Object getComponentForId(String id);

    /**
     * @return the id="..." attribute for the deployment descriptor component, or null if unspecified
     */
    String getIdForComponent(Object ddComponent);

}
