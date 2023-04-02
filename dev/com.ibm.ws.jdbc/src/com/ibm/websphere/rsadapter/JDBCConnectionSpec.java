/*******************************************************************************
 * Copyright (c) 2001, 2021 IBM Corporation and others.
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
package com.ibm.websphere.rsadapter;

// This is provided as legacy function. Do not use.
/**
* <p>An interface provided for JDBC users to specify additional Connection
* properties on getConnection.</p>
*
* <p>To make use of this functionality, the JDBC application must cast to WSDataSource as
* follows,</p>
*
* <p><code>Connection conn = ((WSDataSource) ds).getConnection(jdbcConnectionSpec);</code></p>
*/
public interface JDBCConnectionSpec extends WSConnectionSpec {
    /**
     * Get the transaction isolation level.
     *
     * @return the java.sql.Connection transaction isolation constant for the isolation level.
     */
    int getTransactionIsolation();

    /**
     * <p>Set the transaction isolation level. Any isolation level constant from the
     * java.sql.Connection interface can be used, provided the backend supports it.</p>
     *
     * <p>A value of TRANSACTION_NONE indicates unspecified, in which case the
     * isolation level is determined as usual.</p>
     *
     * @param isolationLevel the isolation level.
     */
    void setTransactionIsolation(int isolationLevel);
}
