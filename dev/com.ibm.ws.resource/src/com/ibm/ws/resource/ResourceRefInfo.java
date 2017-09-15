/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.resource;

import java.util.List;

import com.ibm.wsspi.resource.ResourceInfo;

/**
 * Information about a resource reference.
 * <p>
 * This interface is not intended to be implemented by clients.
 */
public interface ResourceRefInfo
                extends ResourceInfo
{
    /**
     * @return the binding name
     */
    String getJNDIName();

    @Override
    List<? extends Property> getLoginPropertyList();

    interface Property
                    extends ResourceInfo.Property
    {
        // Nothing
    }
}
