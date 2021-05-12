/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.spi;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;

/**
 * Implementation class for ManagedConnectionMetaData.
 */
public class ManagedConnectionMetaDataImpl implements ManagedConnectionMetaData {

    /**
     * @see javax.resource.spi.ManagedConnectionMetaData#getEISProductName()
     */
    @Override
    public String getEISProductName() throws ResourceException {
        return null;
    }

    /**
     * @see javax.resource.spi.ManagedConnectionMetaData#getEISProductVersion()
     */
    @Override
    public String getEISProductVersion() throws ResourceException {
        return null;
    }

    /**
     * @see javax.resource.spi.ManagedConnectionMetaData#getMaxConnections()
     */
    @Override
    public int getMaxConnections() throws ResourceException {
        return 0;
    }

    /**
     * @see javax.resource.spi.ManagedConnectionMetaData#getUserName()
     */
    @Override
    public String getUserName() throws ResourceException {
        return null;
    }

}
