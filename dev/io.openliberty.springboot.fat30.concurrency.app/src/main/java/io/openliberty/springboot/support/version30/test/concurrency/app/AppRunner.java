/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.springboot.support.version30.test.concurrency.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
class AppRunner implements CommandLineRunner {

	private final static Logger logger = LoggerFactory.getLogger(AppRunner.class);

	private final MyScheduledTask myScheduledTask;
	public AppRunner(MyScheduledTask myScheduledTask) {
		this.myScheduledTask = myScheduledTask;
	}
	@Override
	public void run(String... args) throws Exception {
		myScheduledTask.verifyScheduledTaskRepetition("AppRunner");
	}

	public static void assertManagedThread(String message) throws Exception {
//		assert not null of the JNDIlookups for TransactionManager and DefaultManagedScheduledExecutorService
		try {
			InitialContext ic = new InitialContext();
			Object tm = ic.lookup("java:comp/TransactionManager");
			Object dmses = ic.lookup("java:comp/DefaultManagedScheduledExecutorService");
			assertNotNull("Transaction manager lookup failed", tm);
			assertNotNull("DefaultManagedScheduledExecutorService", dmses);
		} catch (NamingException e) {
			logger.error(message + ": JNDI LOOKUP FAILED", e);
			fail("Transaction manager lookup failed: " + e.getMessage());
		}
		logger.info(message + ": MANAGED THREAD VERIFICATION PASSED");
	}
}
