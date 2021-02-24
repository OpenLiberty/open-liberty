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

import com.ibm.ws.jdbc.heritage.DataStoreHelperMetaData;

/**
 * Data store helper metadata for the test JDBC driver.
 */
public class HDDataStoreHelperMetaData implements DataStoreHelperMetaData {

    @Override
    public boolean doesStatementCacheIsoLevel() {
        return true;
    }

    @Override
    public boolean supportsGetCatalog() {
        return false;
    }

    @Override
    public boolean supportsGetNetworkTimeout() {
        return false;
    }

    @Override
    public boolean supportsGetSchema() {
        return false;
    }

    @Override
    public boolean supportsGetTypeMap() {
        return false;
    }

    @Override
    public boolean supportsIsReadOnly() {
        return false;
    }

    @Override
    public boolean supportsUOWDetection() {
        return false;
    }
}