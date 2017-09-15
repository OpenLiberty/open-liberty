/*******************************************************************************
 * Copyright (c) 1999, 2001 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cpi;

import javax.sql.DataSource;

/**
 * Extends PersisterMetaData, and adds methods specific to retrieving
 * metadata for use by JDBC persister. To support persisters tied to
 * non-JDBC back-ends, PersisterMetaData will likely have to be extended
 * in custom fashion.
 * 
 * @see com.ibm.websphere.cpi.PersisterMetaData
 */

public interface JDBCPersisterMetaData extends PersisterMetaData {

    /**
     * getDataSource: returns the DataSource to be used by the JDBC persister.
     * 
     * @return DataSource to be used by the JDBC persister.
     */
    public DataSource getDataSource();

}
