/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.request.interrupt.internal;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import test.common.SharedOutputManager;

public class WeakReferenceThreadsTest {

	private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

	@Rule
	public TestRule rule = outputMgr;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		outputMgr.captureStreams();

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		outputMgr.restoreStreams();
	}

	@After
	public void tearDown() {
		outputMgr.resetStreams();
	}

	@Test
	/*
	 * Tests that the weak reference for the Thread key in the WeakHashMap<Thread,
	 * InterruptibleThreadObject> of InterruptibleThreadInfrastructureImpl is
	 * properly removed when there are no longer any strong references to the
	 * Thread.
	 * 
	 * Create an InterruptibleThreadInfrastructureImpl and initiate a new request in
	 * a new thread. This creates a new entry in the `threadsMap` WeakHashMap with a
	 * weak-reference to the current thread.
	 * 
	 * The InterruptibleThreadInfrastructureImpl would also create an
	 * InterruptibleThreadObject which would create a JVMInterruptObject which holds
	 * on to a reference to the current thread. This reference use to be a strong
	 * reference and would cause a lingering reference preventing the `threadsMap`
	 * from removing entries. For the purpose of the unit test, will create a
	 * JVMInterruptObject separately.
	 * 
	 * This has been fixed to a weak-reference to the thread in JVMInterruptObject.
	 * This test will test that once the strong reference to the running thread
	 * (held by this test) is nullified that the remaining weak-references will
	 * remove themselves.
	 */
	public void testWeakReferenceThread() {

		final String REQUEST_ID = "requestID1";

		/*
		 * Hold on to reference of JVMInterruptObject outside of the separate executing
		 * thread. Need to hold on to the JVMInterruptObject so that the object stays in
		 * the heap and test that the (weak) Reference of the thread held by
		 * JVMInterruptObject does not prevent the threadsMap entry in
		 * InterruptibleThreadInfrastructureImpl from being removed.
		 * 
		 * Note: Previously JVMInterruptObject held a strong reference. This would fail
		 * if there was a strong reference.
		 * 
		 */
		JVMInterruptObject[] jvmInterruptRef = new JVMInterruptObject[1];

		InterruptibleThreadInfrastructureImpl iti = new InterruptibleThreadInfrastructureImpl();

		Runnable r = new Runnable() {

			@Override
			public void run() {

				iti.newRequestEntry(REQUEST_ID);

				JVMInterruptObject j = new JVMInterruptObject(null, null, null, null, null, null);
				// Store ref to keep in heap
				jvmInterruptRef[0] = j;

				iti.completedRequestExit(REQUEST_ID);

			}
		};

		Thread t = new Thread(r);
		t.start();

		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Remove reference to Thread and Invoke a System GC
		t = null;
		doGarbageCollection(iti);

		Assert.assertEquals("The threadMap size is not zero. The value is " + iti.getThreadsMapSize(), 0,
				iti.getThreadsMapSize());

	}

	private void doGarbageCollection(InterruptibleThreadInfrastructureImpl iti) {
		for (int i = 0; i < 10; ++i) {
			System.gc();
			if (iti.getThreadsMapSize() == 0) {
				return;
			}
			try {
				TimeUnit.MILLISECONDS.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
