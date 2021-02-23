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
package test.jdbc.heritage.driver.helper;

import java.sql.Connection;

import com.ibm.ws.jdbc.heritage.AccessIntent;
import com.ibm.ws.jdbc.heritage.DataStoreHelper;
import com.ibm.ws.jdbc.heritage.DataStoreHelperMetaData;

/**
 * Data store helper for the test JDBC driver.
 */
public class HDDataStoreHelper implements DataStoreHelper {
    private final HDDataStoreHelperMetaData metadata = new HDDataStoreHelperMetaData();

    @Override
    public int getIsolationLevel(AccessIntent unused) {
        return Connection.TRANSACTION_SERIALIZABLE;
    }

    @Override
    public DataStoreHelperMetaData getMetaData() {
        return metadata;
    }
}