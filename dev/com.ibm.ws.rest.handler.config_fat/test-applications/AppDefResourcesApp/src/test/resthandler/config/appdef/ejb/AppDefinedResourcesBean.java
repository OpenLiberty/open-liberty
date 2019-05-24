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
package test.resthandler.config.appdef.ejb;

import java.util.concurrent.Executor;

import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.resource.ConnectionFactoryDefinition;
import javax.resource.ConnectionFactoryDefinitions;

@ConnectionFactoryDefinitions({
                                @ConnectionFactoryDefinition(name = "java:module/env/eis/cf1", // same JNDI name is used in WAR module, but okay because different scope
                                                             interfaceName = "javax.sql.DataSource",
                                                             resourceAdapter = "ConfigTestAdapter",
                                                             properties = "purgePolicy=FailingConnectionOnly")
})

@DataSourceDefinition(name = "java:comp/env/jdbc/ds3", // same JNDI name is used in WAR module, but okay because different scope
                      className = "org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource",
                      databaseName = "memory:ejbdb",
                      properties = {
                                     "createDatabase=create"
                      })
@Stateless
@Local
public class AppDefinedResourcesBean implements Executor {
    @Override
    public void execute(Runnable r) {
        r.run();
    }
}
