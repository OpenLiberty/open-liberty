/*******************************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * Collection of all example tests
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should represent all of the test cases for this FAT.
 *
 *
 * NOTES on commented-out classes:
 *
 * BasicJDBCPersistenceTest.class,
 *
 * Useful when starting, or perhaps if you needed to run
 * one quick test with FAT. At this point this isn't adding
 * much in regression on top of what's already there in a real
 * lite or full bucket run, so we'll remove.
 *
 * OracleJDBCPersistenceTest.class
 *
 * Was useful before building on Cathy's DB swap framework,
 * which will be the "real" way to execute cross DB going forward.
 * Left in here commented out in case it helps someone running manually.
 */
@SuiteClasses({
        BatchUserTranTest.class,
        BatchNoSecurityTest.class,
        BatchTransactionalMiscTest.class,
        CDITest.class,
        CDITestCheckpoint.class,
        ChunkTest.class,
        JdbcConfigTest.class,
        JdbcConfigTestCheckpoint.class,
        LocalServerJobRecoveryAtStartUpTest.class,
        MiscTest.class,
        TranTimeoutTest.class,
        DDLTest.class,
        SkipRetryHandlerTest.class,
        JPAPersistenceManagerImplTest.class,
        InMemoryPersistenceTest.class,
        InMemoryPersistenceBatchJobOperatorApiTest.class,
        JPAPersistenceBatchJobOperatorApiTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly())
        .andWith(new JakartaEE9Action().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
        .andWith(new JakartaEE10Action()); 
    
    static public void configureBootStrapProperties(LibertyServer server, Map<String, String> properties) throws Exception, IOException, FileNotFoundException {
        Properties bootStrapProperties = new Properties();
        File bootStrapPropertiesFile = new File(server.getFileFromLibertyServerRoot("bootstrap.properties").getAbsolutePath());
        if (bootStrapPropertiesFile.isFile()) {
            try (InputStream in = new FileInputStream(bootStrapPropertiesFile)) {
                bootStrapProperties.load(in);
            }
        }
        bootStrapProperties.putAll(properties);
        try (OutputStream out = new FileOutputStream(bootStrapPropertiesFile)) {
            bootStrapProperties.store(out, "");
        }
    } 
    
}
