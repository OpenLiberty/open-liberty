/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.helloworldra;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.spi.ManagedConnectionMetaData;

public class HelloWorldManagedConnectionMetaDataImpl implements ManagedConnectionMetaData {

    private static final int MAX_CONNECTIONS = 1;

    private ConnectionMetaData cxMetaData;

    /**
     * Constructor for HelloWorldManagedConnectionMetaDataImpl
     */
    public HelloWorldManagedConnectionMetaDataImpl(ConnectionMetaData cxMetaData) {

        super();
        this.cxMetaData = cxMetaData;
    }

    /**
     * @see ManagedConnectionMetaData#getEISProductName()
     */
    @Override
    public String getEISProductName() throws ResourceException {

        return cxMetaData.getEISProductName();
    }

    /**
     * @see ManagedConnectionMetaData#getEISProductVersion()
     */
    @Override
    public String getEISProductVersion() throws ResourceException {

        return cxMetaData.getEISProductVersion();
    }

    /**
     * @see ManagedConnectionMetaData#getMaxConnections()
     */
    @Override
    public int getMaxConnections() throws ResourceException {

        return MAX_CONNECTIONS;
    }

    /**
     * @see ManagedConnectionMetaData#getUserName()
     */
    @Override
    public String getUserName() throws ResourceException {

        return cxMetaData.getUserName();
    }

}