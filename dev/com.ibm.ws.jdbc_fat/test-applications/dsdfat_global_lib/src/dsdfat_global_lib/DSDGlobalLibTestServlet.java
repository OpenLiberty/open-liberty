/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package dsdfat_global_lib;

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
        annoDS.getConnection().close();
        xmlDS.getConnection().close();
    }
}
