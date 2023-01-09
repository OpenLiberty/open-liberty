/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jdbc.heritage.driver.helper;

/**
 * Data store helper metadata for the test JDBC driver.
 */
public class HDDataStoreHelperMetaData {

    public boolean doesStatementCacheIsoLevel() {
        return true;
    }

    public boolean supportsGetCatalog() {
        return false;
    }

    public boolean supportsGetNetworkTimeout() {
        return false;
    }

    public boolean supportsGetSchema() {
        return false;
    }

    public boolean supportsGetTypeMap() {
        return false;
    }

    public boolean supportsIsReadOnly() {
        return false;
    }

    public boolean supportsUOWDetection() {
        return false;
    }
}