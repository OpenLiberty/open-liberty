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

/**
 * Extension point for compatibility with data store helpers.
 */
public interface DataStoreHelperMetaData {
    /**
     * Indicates whether the JDBC driver supports <code>java.sql.Connection.getCatalog</code>.
     *
     * @return true if the operation is supported, otherwise false.
     */
    boolean supportsGetCatalog();
}