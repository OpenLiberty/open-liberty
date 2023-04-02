/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package jdbc.fat.driver.derby.xa;

import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

import jdbc.fat.driver.derby.FATDataSource;

/**
 * This data source class name will be difficult to guess because it doesn't match the pattern
 * of the Driver class name. It also intentionally inherits from a javax.sql.DataSource implementation
 * so as to be an implementation of multiple data source types, which is valid for a JDBC driver to do.
 */
public class FAT2PCDataSource extends FATDataSource implements XADataSource {
    public FAT2PCDataSource() throws Exception {
        super((XADataSource) Class.forName("org.apache.derby.jdbc.EmbeddedXADataSource").newInstance());
    }

    @Override
    public XAConnection getXAConnection() throws SQLException {
        return ((XADataSource) derbyds).getXAConnection();
    }

    @Override
    public XAConnection getXAConnection(String user, String password) throws SQLException {
        return ((XADataSource) derbyds).getXAConnection(user, password);
    }
}