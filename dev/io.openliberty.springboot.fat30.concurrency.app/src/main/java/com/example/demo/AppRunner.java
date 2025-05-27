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

	private final ConcurrencyTasks concurrencyApplicationConfig;

	public AppRunner(ConcurrencyTasks concurrencyApplicationConfig) {
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
	
	//Call a new static method assertAsyncMethod, it will call Task 1 and 2, takes a String message as arg
	//Verifies the result of the CompletableFuture, assert on the string value returned by the tasks
    //Log the message based on success on failure
    public static void assertAsyncMethod(String message) throws Exception {
    	
    	try {	
    		assertNotNull("Async Task 1 failed", concurrencyApplicationConfig.task1("Assert Async Method").get());
    		assertNotNull("Async Task 2 failed", concurrencyApplicationConfig.task2("Assert Async Method").get());
    	}catch (Exception e){
    		logger.error(message + ": ASYNC TASK FAILED", e);
			fail("Async Task failed: " + e.getMessage());
    	}  	
    	logger.info(message + ": ASSERT ASYNC METHOD VERIFICATION PASSED");
    }

}
