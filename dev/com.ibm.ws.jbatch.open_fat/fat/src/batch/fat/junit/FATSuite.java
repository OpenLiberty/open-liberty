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
        BatchJobOperatorApiWithAppSecurityTest.class,
        BatchSecurityTest.class,
        BatchUserTranTest.class,
        BatchNoSecurityTest.class,
        BatchEveryoneSecurityTest.class,
        BatchTransactionalMiscTest.class,
        BonusPayoutViaJobOperatorTest.class,
        BonusPayoutViaJBatchUtilityTest.class,
        CDITest.class,
        ChunkTest.class,
        JdbcConfigTest.class,
        LocalServerJobRecoveryAtStartUpTest.class,
        MiscTest.class,
        ParallelContextPropagationTest.class,
        TranTimeoutTest.class,
        PartitionMetricsTest.class,
        DDLTest.class,
        SkipRetryHandlerTest.class,
        PartitionReducerTest.class,
        JPAPersistenceManagerImplTest.class,
        InMemoryPersistenceTest.class,
        InMemoryPersistenceBatchJobOperatorApiTest.class,
        JPAPersistenceBatchJobOperatorApiTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.with(new JakartaEE9Action());
    // TODO: Use this configuration to enable tests for javaee8 batch features
    //public static RepeatTests r = RepeatTests.withoutModification().andWith(new JakartaEE9Action());
}
