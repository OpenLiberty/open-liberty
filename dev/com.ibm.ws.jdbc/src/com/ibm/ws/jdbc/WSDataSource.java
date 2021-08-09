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
package com.ibm.ws.jdbc;

import javax.sql.DataSource;

/**
 * Extensions for use by internal components. Internal components may cast a DataSource to WSDataSource
 * in order to use these methods.
 */
public interface WSDataSource extends DataSource {
    /**
     * <p>Returns the cached value of <code>DatabaseMetaData.getDatabaseProductName</code> from the first connection
     * that was obtained via this data source.
     * 
     * <p>This method is useful as a performance optimization for obtaining the database product name in the typical
     * case where a connection has been previously obtained from the data source. If a connection hasn't been obtained
     * yet, it will just return a null value, in which case it will be the user's responsibility to obtain a connection. 
     *  
     * @return the database product name. Null if a connection has not yet been obtained.
     */
    String getDatabaseProductName();
}
