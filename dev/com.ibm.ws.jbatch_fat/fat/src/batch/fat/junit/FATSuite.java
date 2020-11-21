/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

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
//1
    BatchJobOperatorApiWithAppSecurityTest.class,
//2    BatchSecurityTest.class,
                /*
                 * TESTS 1,2,5 FAILS
                 * Errors/warnings were found in server com.ibm.ws.jbatch.fat logs: <br>Java 2 security issues were found in logs See autoFVT/ACE-report-*.log for details.
                 * <br>[11/23/20 14:17:39:772 CST] 0000002f kernel.launch.internal.MissingDoPrivDetectionSecurityManager W CWWKE0921W: Current Java 2 Security policy reported
                 * a potential violation of Java 2 Security Permission. The application needs to have permissions addedPermission: <br>[11/23/20 14:17:39:777 CST] 0000002f
                 * kernel.launch.internal.MissingDoPrivDetectionSecurityManager W CWWKE0912W: Current Java 2 Security policy reported a potential violation of Java 2 Security
                 * Permission.
                 */
//3    BatchUserTranTest.class,
//4    BatchNoSecurityTest.class,
//5    BatchEveryoneSecurityTest.class,
//6    BatchTransactionalMiscTest.class,
//7    BonusPayoutViaJobOperatorTest.class
//8                BonusPayoutViaJBatchUtilityTest.class,
//9                CDITest.class,
//10               ChunkTest.class,
//11               JdbcConfigTest.class,
//12               LocalServerJobRecoveryAtStartUpTest.class,
//13               MiscTest.class,
//14               ParallelContextPropagationTest.class,
//15               TranTimeoutTest.class,
//16               PartitionMetricsTest.class,
//17               DDLTest.class,
//18               SkipRetryHandlerTest.class,
//19               PartitionReducerTest.class,
//20               JPAPersistenceManagerImplTest.class,
//21               InMemoryPersistenceTest.class,
//22               InMemoryPersistenceBatchJobOperatorApiTest.class,
//23               JPAPersistenceBatchJobOperatorApiTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(new JakartaEE9Action());
}
