/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package dsdfat_global_lib;

import java.sql.Connection;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.sql.DataSource;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@DataSourceDefinition(name = "java:comp/env/jdbc/annoDS",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                      databaseName = "memory:dsdfat_global_lib",
                      properties = { "createDatabase=create" })
public class DSDGlobalLibTestServlet extends FATServlet {

    @Resource(lookup = "java:comp/env/jdbc/annoDS")
    DataSource annoDS;

    @Resource(lookup = "java:app/jdbc/xmlDS")
    DataSource xmlDS;

    public void testDataSourceDefGlobalLib() throws Exception {
        try (Connection con = annoDS.getConnection()) {
        }
        try (Connection con = xmlDS.getConnection()) {
        }
    }
}
