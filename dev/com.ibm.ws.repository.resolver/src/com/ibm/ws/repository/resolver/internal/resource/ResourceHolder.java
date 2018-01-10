/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.resolver.internal.resource;

import org.osgi.resource.Resource;

/**
 * Implementors of this interface are capable of holding a link to a {@link Resource}
 */
public interface ResourceHolder {

    /**
     * Set the resources that this object is holding a reference to.
     * 
     * @param resource
     */
    public void setResource(Resource resource);

}
