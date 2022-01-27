/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.custom.jdbc.impl;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 */
public interface SQLRetriableLog {
    public Connection getConnection() throws Exception;

    public int prepareConnectionForBatch(Connection conn) throws SQLException;

    public void closeConnectionAfterBatch(Connection conn, int initialIsolation) throws SQLException;
}
