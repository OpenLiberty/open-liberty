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
package test.resthandler.config.appdef.web;

import java.sql.Connection;
import java.util.concurrent.Executor;

import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.ejb.EJB;
import javax.jms.JMSConnectionFactoryDefinition;
import javax.jms.JMSConnectionFactoryDefinitions;
import javax.jms.JMSDestinationDefinition;
import javax.resource.AdministeredObjectDefinition;
import javax.resource.ConnectionFactoryDefinition;
import javax.resource.spi.TransactionSupport.TransactionSupportLevel;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

@AdministeredObjectDefinition(name = "java:global/env/eis/conSpec1",
                              resourceAdapter = "ConfigTestAdapter",
                              className = "org.test.config.adapter.ConnectionSpecImpl",
                              properties = { "connectionTimeout=10203",
                                             "userName=aouser1",
                                             "password=aopwd1"
                              })

@ConnectionFactoryDefinition(name = "java:module/env/eis/cf1",
                             description = "It is Test ConnectionFactory",
                             interfaceName = "javax.resource.cci.ConnectionFactory",
                             resourceAdapter = "ConfigTestAdapter",
                             transactionSupport = TransactionSupportLevel.NoTransaction,
                             maxPoolSize = 101,
                             properties = { "enableBetaContent=true",
                                            "escapeChar=`",
                                            "portNumber=1515",
                                            "reapTime=1m1s",
                                            "userName=cfuser1",
                                            "password=cfpwd1"
                             })

@DataSourceDefinitions({
                         @DataSourceDefinition(name = "java:app/env/jdbc/ds1",
                                               className = "org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource",
                                               databaseName = "memory:firstdb",
                                               properties = {
                                                              "agedTimeout=1:05:30",
                                                              "connectionSharing=MatchCurrentState",
                                                              "createDatabase=create"
                                               }),
                         @DataSourceDefinition(name = "java:module/env/jdbc/ds2",
                                               className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                                               databaseName = "${shared.resource.dir}/data/configRHTestDB",
                                               isolationLevel = Connection.TRANSACTION_READ_COMMITTED,
                                               loginTimeout = 220,
                                               maxPoolSize = 2,
                                               maxStatements = 45,
                                               properties = {
                                                              "connectionTimeout=0",
                                                              "containerAuthDataRef=derbyAuth1",
                                                              "createDatabase=create",
                                                              "onConnect=DECLARE GLOBAL TEMPORARY TABLE TEMP2 (COL1 VARCHAR(80)) ON COMMIT PRESERVE ROWS NOT LOGGED",
                                                              "queryTimeout=1m22s",
                                                              "reapTime=2200s",
                                                              "recoveryAuthDataRef=derbyAuth2",
                                                              "syncQueryTimeoutWithTransactionTimeout=true"
                                               }),
                         @DataSourceDefinition(name = "java:comp/env/jdbc/ds3",
                                               className = "org.apache.derby.jdbc.EmbeddedDataSource",
                                               databaseName = "memory:thirddb;create=true"),
                         @DataSourceDefinition(name = "java:global/env/jdbc/ds4",
                                               className = "", // infer class name
                                               databaseName = "memory:fourthdb",
                                               properties = "createDatabase=create",
                                               user = "dbuser4",
                                               password = "dbpwd4")
})

@JMSConnectionFactoryDefinitions({
                                   @JMSConnectionFactoryDefinition(name = "java:comp/env/jms/cf",
                                                                   // interfaceName = "javax.jms.ConnectionFactory", // already the default value
                                                                   resourceAdapter = "wasJms",
                                                                   clientId = "JMSClientID6",
                                                                   maxPoolSize = 6,
                                                                   user = "jmsuser",
                                                                   password = "jmspwd",
                                                                   properties = {
                                                                                  "busName=cfBus",
                                                                                  "readAhead=AlwaysOff",
                                                                                  "shareDurableSubscription=NeverShared",
                                                                                  "temporaryQueueNamePrefix=cfq"
                                                                   }),
                                   @JMSConnectionFactoryDefinition(name = "java:module/env/jms/qcf",
                                                                   interfaceName = "javax.jms.QueueConnectionFactory",
                                                                   resourceAdapter = "wasJms",
                                                                   maxPoolSize = 7,
                                                                   minPoolSize = 3,
                                                                   properties = {
                                                                                  "agedTimeout=7h9m50s",
                                                                                  "busName=qcfBus",
                                                                                  "connectionTimeout=70s",
                                                                                  "enableSharingForDirectLookups=false",
                                                                                  "maxIdleTime=7m30s",
                                                                                  "purgePolicy=ValidateAllConnections",
                                                                                  "reapTime=72",
                                                                                  "temporaryQueueNamePrefix=tempq"
                                                                   })
})

@JMSDestinationDefinition(name = "java:app/env/jms/queue1",
                          interfaceName = "javax.jms.Queue",
                          resourceAdapter = "wasJms",
                          destinationName = "MyQueue",
                          properties = "readAhead=AlwaysOff")

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/AppDefinedResourcesServlet")
public class AppDefinedResourcesServlet extends FATServlet {
    @EJB
    Executor bean;

    /**
     * No-op servlet method that the test case uses to ensure the web module is loaded.
     */
    public void doSomething() {
        System.out.println("Servlet is running.");
        bean.execute(() -> System.out.println("EJB is running."));
    }
}
