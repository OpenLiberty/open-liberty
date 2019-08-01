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
import javax.jms.JMSConnectionFactoryDefinition;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSDestinationDefinitions;
import javax.resource.AdministeredObjectDefinition;
import javax.resource.ConnectionFactoryDefinition;
import javax.resource.ConnectionFactoryDefinitions;

@AdministeredObjectDefinition(name = "java:comp/env/eis/iSpec1",
                              resourceAdapter = "ConfigTestAdapter",
                              className = "org.test.config.adapter.InteractionSpecImpl",
                              properties = "functionName=doSomethingUseful")

@ConnectionFactoryDefinitions({
                                @ConnectionFactoryDefinition(name = "java:module/env/eis/cf1", // same JNDI name is used in WAR module, but okay because different scope
                                                             interfaceName = "javax.sql.DataSource",
                                                             resourceAdapter = "ConfigTestAdapter",
                                                             properties = "purgePolicy=FailingConnectionOnly"),
                                @ConnectionFactoryDefinition(name = "java:comp/env/eis/cf2",
                                                             interfaceName = "javax.sql.DataSource",
                                                             resourceAdapter = "AppDefResourcesApp.EmbTestAdapter",
                                                             maxPoolSize = 2,
                                                             properties = {
                                                                            "escapeChar=^",
                                                                            "userName=euser2",
                                                                            "password=epwd2"
                                                             })
})

@DataSourceDefinition(name = "java:comp/env/jdbc/ds3", // same JNDI name is used in WAR module, but okay because different scope
                      className = "org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource",
                      databaseName = "memory:ejbdb",
                      properties = {
                                     "createDatabase=create"
                      })

@JMSConnectionFactoryDefinition(name = "java:app/env/jms/tcf",
                                interfaceName = "javax.jms.TopicConnectionFactory",
                                resourceAdapter = "ConfigTestAdapter",
                                clientId = "AppDefinedClientId",
                                maxPoolSize = 8,
                                properties = {
                                               "enableBetaContent=true",
                                               "portNumber=8765"
                                })

@JMSDestinationDefinitions({
                             @JMSDestinationDefinition(name = "java:global/env/jms/dest1",
                                                       interfaceName = "javax.jms.Destination",
                                                       resourceAdapter = "ConfigTestAdapter",
                                                       destinationName = "3605 Hwy 52N, Rochester, MN 55901"),
                             @JMSDestinationDefinition(name = "java:comp/env/jms/topic1",
                                                       interfaceName = "javax.jms.Topic",
                                                       destinationName = "MyTopic",
                                                       properties = {
                                                                      "priority=8",
                                                                      "timeToLive=4m6s"
                                                       })
})

@Stateless
@Local
public class AppDefinedResourcesBean implements Executor {
    @Override
    public void execute(Runnable r) {
        r.run();
    }
}
