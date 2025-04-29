package com.example.demo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class AppRunner implements CommandLineRunner {

	private final static Logger logger = LoggerFactory.getLogger(AppRunner.class);

	private final ConcurrencyApplicationConfig concurrencyApplicationConfig;

	public AppRunner(ConcurrencyApplicationConfig concurrencyApplicationConfig) {
		this.concurrencyApplicationConfig = concurrencyApplicationConfig;
	}

	@Override
	public void run(String... args) throws Exception {
		try {		
			assertManagedThread("AppRunner");
		} catch (Exception e) {
			logger.error("Exception on assertManagedThread.", e);
		}
	}

	public static void assertManagedThread(String message) throws Exception {
		//assert not null of the JNDIlookups for TransactionManager and DefaultManagedScheduledExecutorService
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
