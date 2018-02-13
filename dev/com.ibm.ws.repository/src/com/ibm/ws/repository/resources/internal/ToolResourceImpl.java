/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resources.internal;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.resources.writeable.ToolResourceWritable;
import com.ibm.ws.repository.transport.model.Asset;

public class ToolResourceImpl extends ProductRelatedResourceImpl implements ToolResourceWritable {

    /*
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */
    public ToolResourceImpl(RepositoryConnection repoConnection) {
        this(repoConnection, null);
    }

    public ToolResourceImpl(RepositoryConnection repoConnection, Asset ass) {
        super(repoConnection, ass);
        if (ass == null) {
            setType(ResourceType.TOOL);
        }
    }
}
