/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.api.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class JobOperatorImplSuspendTranTest {

    /**
     * Mock environment.
     */
    private Mockery mockery = null;

    /**
     * Create the mockery environment for each test. Setting up a new mockery
     * environment for each test helps isolate Expectation sets, making it easier to
     * debug when some Expectation fails and all the Expectations are dumped
     * to the error log.
     */
    @Before
    public void before() {
        mockery = new JUnit4Mockery() ;
    }

    /**
     * There are alternative ways to do this.
     * 1) Use @RunWith(JMock.class) (deprecated)
     * 2) Declare the field: @Rule public final JUnitRuleMockery context = new JUnitRuleMockery();
     * (this version of Junit is not in our codebase).
     * 
     * Doing it the manual way for now.
     */
    @After
    public void after() {
        mockery.assertIsSatisfied();
    }
    
    /**
     * Convenience method.
     */
    private JobOperatorImplSuspendTran buildJobOperator(JobOperator mockJobOperator, TransactionManager mockTranMgr) {
        JobOperatorImplSuspendTran retMe = new JobOperatorImplSuspendTran();
        retMe.setTransactionManager(mockTranMgr);
        retMe.setJobOperator( mockJobOperator );
        return retMe;
    }
    
    /**
     * 
     */
    @Test
    public void testGetJobNames() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);
        final Set<String> retMe = new HashSet<String>();

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getJobNames();
                will(returnValue(retMe)); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        assertEquals( retMe, buildJobOperator(mockJobOperator, mockTranMgr).getJobNames() );
    }
    
    /**
     * 
     */
    @Test
    public void testGetJobNamesNoTran() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Set<String> retMe = new HashSet<String>();

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(null));
                
                oneOf(mockJobOperator).getJobNames();
                will(returnValue(retMe)); 
            }
        });
        
        assertEquals( retMe, buildJobOperator(mockJobOperator, mockTranMgr).getJobNames() );
    }

    /**
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetJobNamesFail() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getJobNames();
                will(throwException(new IllegalArgumentException())); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).getJobNames();
    }

    /**
     * 
     */
    @Test
    public void testGetJobInstanceCount() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);
        final int retMe = 1;

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getJobInstanceCount(null);
                will(returnValue(retMe)); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getJobInstanceCount(null) );
    }
    
    /**
     * 
     */
    @Test
    public void testGetJobInstanceCountNoTran() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final int retMe = 1;

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(null));
                
                oneOf(mockJobOperator).getJobInstanceCount(null);
                will(returnValue(retMe)); 
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getJobInstanceCount(null) );
    }

    /**
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetJobInstanceCountFail() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getJobInstanceCount(null);
                will(throwException(new IllegalArgumentException())); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).getJobInstanceCount(null);
    }

    /**
     * 
     */
    @Test
    public void testGetJobInstances() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);
        final List<JobInstance> retMe = new ArrayList<JobInstance>();

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getJobInstances(null, 0, 0);
                will(returnValue(retMe)); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getJobInstances(null, 0, 0) );
    }
    
    /**
     * 
     */
    @Test
    public void testGetJobInstancesNoTran() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final List<JobInstance> retMe = new ArrayList<JobInstance>();

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(null));
                
                oneOf(mockJobOperator).getJobInstances(null, 0, 0);
                will(returnValue(retMe)); 
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getJobInstances(null, 0, 0) );
    }

    /**
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetJobInstancesFail() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getJobInstances(null, 0, 0);
                will(throwException(new IllegalArgumentException())); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).getJobInstances(null, 0 ,0);
    }

    /**
     * 
     */
    @Test
    public void testGetRunningExecutions() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);
        final List<Long> retMe = new ArrayList<Long>();

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getRunningExecutions(null);
                will(returnValue(retMe)); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getRunningExecutions(null) );
    }
    
    /**
     * 
     */
    @Test
    public void testGetRunningExecutionsNoTran() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final List<Long> retMe = new ArrayList<Long>();

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(null));
                
                oneOf(mockJobOperator).getRunningExecutions(null);
                will(returnValue(retMe)); 
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getRunningExecutions(null) );
    }

    /**
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetRunningExecutionsFail() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getRunningExecutions(null);
                will(throwException(new IllegalArgumentException())); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).getRunningExecutions(null);
    }
  
    /**
     * 
     */
    @Test
    public void testGetParameters() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);
        final Properties retMe = new Properties();

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getParameters(0);
                will(returnValue(retMe)); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getParameters(0) );
    }
    
    /**
     * 
     */
    @Test
    public void testGetParametersNoTran() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Properties retMe = new Properties();

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(null));
                
                oneOf(mockJobOperator).getParameters(0);
                will(returnValue(retMe)); 
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getParameters(0) );
    }

    /**
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetParametersFail() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getParameters(0);
                will(throwException(new IllegalArgumentException())); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).getParameters(0);
    }

    /**
     * 
     */
    @Test
    public void testGetJobExecutions() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);
        final List<JobExecution> retMe = new ArrayList<JobExecution>();

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getJobExecutions(null);
                will(returnValue(retMe)); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getJobExecutions(null) );
    }
    
    /**
     * 
     */
    @Test
    public void testGetJobExecutionsNoTran() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final List<JobExecution> retMe = new ArrayList<JobExecution>();

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(null));
                
                oneOf(mockJobOperator).getJobExecutions(null);
                will(returnValue(retMe)); 
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getJobExecutions(null) );
    }

    /**
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetJobExecutionsFail() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getJobExecutions(null);
                will(throwException(new IllegalArgumentException())); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).getJobExecutions(null);
    }

    /**
     * 
     */
    @Test
    public void testGetJobInstance() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);
        final JobInstance retMe = mockery.mock(JobInstance.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getJobInstance(0);
                will(returnValue(retMe)); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getJobInstance(0) );
    }
    
    /**
     * 
     */
    @Test
    public void testGetJobInstanceNoTran() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final JobInstance retMe = mockery.mock(JobInstance.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(null));
                
                oneOf(mockJobOperator).getJobInstance(0);
                will(returnValue(retMe)); 
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getJobInstance(0) );
    }

    /**
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetJobInstanceFail() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getJobInstance(0);
                will(throwException(new IllegalArgumentException())); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).getJobInstance(0);
    }

    /**
     * 
     */
    @Test
    public void testGetJobExecution() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);
        final JobExecution retMe = mockery.mock(JobExecution.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getJobExecution(0);
                will(returnValue(retMe)); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getJobExecution(0) );
    }
    
    /**
     * 
     */
    @Test
    public void testGetJobExecutionNoTran() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final JobExecution retMe = mockery.mock(JobExecution.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(null));
                
                oneOf(mockJobOperator).getJobExecution(0);
                will(returnValue(retMe)); 
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getJobExecution(0) );
    }

    /**
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetJobExecutionFail() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getJobExecution(0);
                will(throwException(new IllegalArgumentException())); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).getJobExecution(0);
    }

    /**
     * 
     */
    @Test
    public void testGetStepExecutions() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);
        final List<StepExecution> retMe = new ArrayList<StepExecution>();

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getStepExecutions(0);
                will(returnValue(retMe)); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getStepExecutions(0) );
    }
    
    /**
     * 
     */
    @Test
    public void testGetStepExecutionsNoTran() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final List<StepExecution> retMe = new ArrayList<StepExecution>();

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(null));
                
                oneOf(mockJobOperator).getStepExecutions(0);
                will(returnValue(retMe)); 
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).getStepExecutions(0) );
    }

    /**
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetStepExecutionsFail() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).getStepExecutions(0);
                will(throwException(new IllegalArgumentException())); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).getStepExecutions(0);
    }

    /**
     * 
     */
    @Test
    public void testStart() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);
        final long retMe = 1;

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).start(null, null);
                will(returnValue(retMe)); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).start(null, null) );
    }
    
    /**
     * 
     */
    @Test
    public void testStartNoTran() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final long retMe = 1;

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(null));
                
                oneOf(mockJobOperator).start(null, null);
                will(returnValue(retMe)); 
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).start(null, null) );
    }

    /**
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testStartFail() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).start(null, null);
                will(throwException(new IllegalArgumentException())); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).start(null, null);
    }

    /**
     * 
     */
    @Test
    public void testRestart() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);
        final long retMe = 1;

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).restart(0, null);
                will(returnValue(retMe)); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).restart(0, null) );
    }
    
    /**
     * 
     */
    @Test
    public void testRestartNoTran() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final long retMe = 1;

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(null));
                
                oneOf(mockJobOperator).restart(0, null);
                will(returnValue(retMe)); 
            }
        });
        
        assertEquals(retMe, buildJobOperator(mockJobOperator, mockTranMgr).restart(0, null) );
    }

    /**
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRestartFail() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).restart(0, null);
                will(throwException(new IllegalArgumentException())); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).restart(0, null);
    }

    /**
     * 
     */
    @Test
    public void testStop() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).stop(0);
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).stop(0);
    }
    
    /**
     * 
     */
    @Test
    public void testStopNoTran() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(null));
                
                oneOf(mockJobOperator).stop(0);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).stop(0);
    }

    /**
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testStopFail() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).stop(0);
                will(throwException(new IllegalArgumentException())); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).stop(0);
    }

    /**
     * 
     */
    @Test
    public void testAbandon() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).abandon(0);
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).abandon(0);
    }
    
    /**
     * 
     */
    @Test
    public void testAbandonNoTran() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(null));
                
                oneOf(mockJobOperator).abandon(0);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).abandon(0);
    }

    /**
     * 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAbandonFail() throws Exception {
        
        final JobOperator mockJobOperator = mockery.mock(JobOperator.class);
        final TransactionManager mockTranMgr = mockery.mock(TransactionManager.class);
        final Transaction mockTran = mockery.mock(Transaction.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mockTranMgr).suspend();
                will(returnValue(mockTran));
                
                oneOf(mockJobOperator).abandon(0);
                will(throwException(new IllegalArgumentException())); 
                
                oneOf(mockTranMgr).resume(mockTran);
            }
        });
        
        buildJobOperator(mockJobOperator, mockTranMgr).abandon(0);
    }


}
