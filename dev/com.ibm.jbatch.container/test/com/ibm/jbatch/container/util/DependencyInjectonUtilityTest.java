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
package com.ibm.jbatch.container.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.execution.impl.RuntimeStepExecution;
import com.ibm.jbatch.container.util.artifacts.SubClass;
import com.ibm.jbatch.jsl.model.v1.Property;

public class DependencyInjectonUtilityTest {

	/**
	 * Mock environment.
	 */
	private Mockery mockery = null;
	private InjectionReferences mockInjectionRefs = null;
	JobContext jobCtx = null;
	RuntimeStepExecution runtimeStepCtx = null;
	SubClass subClass = null;
	StepContext stepCtx = null;

	/**
	 * Create the mockery environment for each test. Setting up a new mockery
	 * environment for each test helps isolate Expectation sets, making it easier to
	 * debug when some Expectation fails and all the Expectations are dumped
	 * to the error log.
	 */
	
	@Before
	public void before() {
		mockery = new JUnit4Mockery() {
			{
				setImposteriser(ClassImposteriser.INSTANCE);
			}
		};

		stepCtx = new StepContextImpl(null);
		runtimeStepCtx = mockery.mock(RuntimeStepExecution.class);
		jobCtx = new JobContextImpl(null);

		Property ibmBatch = new Property();
		ibmBatch.setName("ibmBatch");
		ibmBatch.setValue("injected_string_ibmBatch");

		Property numRecords = new Property();
		numRecords.setName("numRecords");
		numRecords.setValue("injected_string_numRecords");
		
		Property privateSuper = new Property();
		privateSuper.setName("privateSuper");
		privateSuper.setValue("injected_string_privateSuper");

		List<com.ibm.jbatch.jsl.model.Property> properties = new ArrayList<com.ibm.jbatch.jsl.model.Property>();
		properties.add(numRecords);
		properties.add(ibmBatch);
		properties.add(privateSuper);
		mockInjectionRefs = new InjectionReferences(jobCtx, runtimeStepCtx,
				properties);
		mockery.checking(new Expectations() {
			{
				allowing(runtimeStepCtx).getStepContext();
				will(returnValue(stepCtx));
			}
		});
		this.subClass = new SubClass();
		DependencyInjectionUtility.injectReferences(subClass, mockInjectionRefs);

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

	//Tests the value of field that does not have a reference property defined
	@Test
	public void testNoReference() throws Exception {

		assertEquals(subClass.getNoReference(), "default");
	}

	//Tests that stepContext and jobContext are injected properly
	@Test
	public void testingContextInjection() throws Exception {

		assertEquals(subClass.getStepCtx(), stepCtx);
		assertEquals(subClass.getJobCtx(), jobCtx);
	}

	//Tests the injected properties 
	@Test
	public void testingBatchPropertyInjection() throws Exception {

		assertEquals(subClass.getIbmBatch(), "injected_string_ibmBatch");
		assertEquals(subClass.getNumRecords(), "injected_string_numRecords");
		assertEquals(subClass.getBaseString(), null);
		assertEquals(subClass.getPrivateSuper(),"injected_string_privateSuper");
	}

}
