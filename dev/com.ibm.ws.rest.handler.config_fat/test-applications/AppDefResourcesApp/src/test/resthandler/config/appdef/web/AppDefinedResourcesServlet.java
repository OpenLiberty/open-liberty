/*******************************************************************************
 * Copyright (c) 2019,2026 IBM Corporation and others.
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
package test.resthandler.config.appdef.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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
import javax.sql.DataSource;

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
                             interfaceName = "${env.CONNECTION_FACTORY}",
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
                                               className = "org.h2.jdbcx.JdbcDataSource",
                                               url = "jdbc:h2:mem:firstdb;DB_CLOSE_DELAY=-1",
                                               properties = {
                                                              "agedTimeout=1:05:30",
                                                              "connectionSharing=MatchCurrentState"
                                               }),
                         @DataSourceDefinition(name = "java:module/env/jdbc/ds2",
                                               className = "org.h2.jdbcx.JdbcDataSource",
                                               url = "jdbc:h2:${shared.resource.dir}/data/configRHTestDB;DB_CLOSE_DELAY=-1",
                                               isolationLevel = Connection.TRANSACTION_READ_COMMITTED,
                                               loginTimeout = 220,
                                               maxPoolSize = 2,
                                               maxStatements = 45,
                                               properties = {
                                                              "connectionTimeout=0",
                                                              "containerAuthDataRef=h2Auth1",
                                                              "onConnect=${create.table.temp2}",
                                                              "queryTimeout=1m22s",
                                                              "reapTime=2200s",
                                                              "recoveryAuthDataRef=h2Auth2",
                                                              "syncQueryTimeoutWithTransactionTimeout=true"
                                               }),
                         @DataSourceDefinition(name = "java:comp/env/jdbc/ds3",
                                               className = "org.h2.jdbcx.JdbcDataSource",
                                               url = "jdbc:h2:mem:thirddb;DB_CLOSE_DELAY=-1"),
                         @DataSourceDefinition(name = "java:global/env/jdbc/ds4",
                                               className = "", // infer class name
                                               url = "jdbc:h2:mem:fourthdb;DB_CLOSE_DELAY=-1",
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
                                                                   interfaceName = "${env.QUEUE_FACTORY}",
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
                          interfaceName = "${env.QUEUE_INTERFACE}",
                          resourceAdapter = "wasJms",
                          destinationName = "MyQueue",
                          properties = "readAhead=AlwaysOff")

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/AppDefinedResourcesServlet")
public class AppDefinedResourcesServlet extends FATServlet {
    @EJB
    Executor bean;

    @Resource(lookup = "java:global/env/jdbc/ds4")
    private DataSource ds4;

    /**
     * Initialize H2 database users on servlet startup.
     * H2 in-memory databases require users to be created programmatically.
     */
    @PostConstruct
    public void init() {
        try (Connection conn = ds4.getConnection();
             Statement stmt = conn.createStatement()) {
            // Create dbuser3 with ADMIN privileges for test validation
            stmt.execute("CREATE USER IF NOT EXISTS dbuser3 PASSWORD 'dbpwd3' ADMIN");
            System.out.println("H2 user dbuser3 created successfully");
        } catch (SQLException e) {
            System.err.println("Failed to create H2 user: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * No-op servlet method that the test case uses to ensure the web module is loaded.
     */
    public void doSomething() {
        System.out.println("Servlet is running.");
        bean.execute(() -> System.out.println("EJB is running."));
    }
}
