/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.heritage;

import javax.resource.ResourceException;

/**
 * Extension point for compatibility with data store helpers.
 */
public interface DataStoreHelper {
    /**
     * Returns the default to use for transaction isolation level when not specified another way.
     *
     * @param unused always null. This is only here for compatibility.
     * @return transaction isolation level constant from java.sql.Connection
     * @throws ResourceException never. This is only here for compatibility.
     */
    int getIsolationLevel(AccessIntent unused) throws ResourceException;

    /**
     * Returns metadata for the data store helper.
     *
     * @return metadata.
     */
    DataStoreHelperMetaData getMetaData();
}