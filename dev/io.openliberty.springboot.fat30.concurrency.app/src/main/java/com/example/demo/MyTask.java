package com.example.demo;


import static org.junit.Assert.fail;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.naming.Context;
import javax.naming.InitialContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jndi.JndiTemplate;
import org.springframework.stereotype.Component;
import javax.naming.NamingException;

 @Component
 public class MyTask{
	 
	private final static Logger logger = LoggerFactory.getLogger(MyTask.class);
	
	private ConcurrencyApplicationConfig concurrencyApplicationConfig = new ConcurrencyApplicationConfig();
	
	public MyTask(ConcurrencyApplicationConfig concurrencyApplicationConfig) {
		this.concurrencyApplicationConfig = concurrencyApplicationConfig;
	}

	 
    //Scheduled method for the Async tasks
    @Scheduled(fixedDelay = 5000)
    public void scheduledTask() throws InterruptedException, Throwable {
    	
    	AppRunner.assertManagedThread("ScheduledTask");
		concurrencyApplicationConfig.assertAsyncMethod("ScheduledTask");
    }
	
	//Utility method that verifies if an application is running on a managed thread or not
	public static boolean verifyManagedThread() {
        Thread currentThread = Thread.currentThread();
        return currentThread != null && currentThread.getName() != null && currentThread.getName().startsWith("app-");
    }

	public void run(String... args) throws Exception {
		try {
			testJNDI("MyTask");
		} catch (Exception e) {
			logger.error("Exception on testJNDI.", e);
		}
	}
    
	public static void testJNDI(String val) {
		try {
			printJavaJNDI();
		} catch (NamingException e) {			
			logger.error("Error on printJavaJNDI.", e);
			}
		try {
			InitialContext ic = new InitialContext();
			Object tm = ic.lookup("java:comp/TransactionManager");
			Object dmses = ic.lookup("java:comp/DefaultManagedScheduledExecutorService");
			if (tm == null || dmses == null) {
				logger.error(val + ": JNDI TESTS FAILED : tm=" + tm + " dmses=" + dmses);
			} else {
				logger.info(val + ": JNDI TESTS PASSED");
			}
		} catch (NamingException e) {
			logger.error(val + ": JNDI TESTS FAILED", e);
		}
	}

	private static void printJavaJNDI() throws NamingException {
		logger.info('\n' + "java:comp\n" + print(toMap((Context) new InitialContext().lookup("java:comp")), new StringBuilder(), "").toString());
		logger.info('\n' + "java:global\n" + print(toMap((Context) new InitialContext().lookup("java:global")), new StringBuilder(), "").toString());
		logger.info('\n' + "java:app\n" + print(toMap((Context) new InitialContext().lookup("java:app")), new StringBuilder(), "").toString());
		logger.info('\n' + "java:module\n" + print(toMap((Context) new InitialContext().lookup("java:module")), new StringBuilder(), "").toString());
	}
	
	@SuppressWarnings("unchecked")
	private static StringBuilder print(Map<String, Object> contextMap, StringBuilder builder, String indent) {
		builder.append('{').append('\n');
		for (Map.Entry<String, Object> entry : contextMap.entrySet()) {
			builder.append(indent).append("    \"").append(entry.getKey()).append("\": ");
			if (entry.getValue() instanceof Map) {
				print((Map<String, Object>) entry.getValue(), builder, indent + "    ");
			} else {
				builder.append('"').append(entry.getValue()).append('"').append('\n');
			}
		}
		builder.append(indent).append('}').append('\n');
		return builder;
	}
	
	public static Map<String, Object> toMap(Context ctx) throws NamingException {
		Map<String, Object> map = new LinkedHashMap<>();
		String namespace = ctx instanceof InitialContext ? ctx.getNameInNamespace() : "";
		try {
			NamingEnumeration<NameClassPair> list = ctx.list(namespace);
			while (list.hasMoreElements()) {
				NameClassPair next = list.next();
				String name = next.getName();
				if (name.isEmpty()) {
					continue;
				}
				String jndiPath = namespace + name;
				Object lookup;
	
				try {
					Object tmp = ctx.lookup(jndiPath);
					if (tmp instanceof Context) {
						lookup = toMap((Context) tmp);
					} else {
						lookup = next.getClassName() + " - " + tmp.toString();
					}
				} catch (Throwable t) {
					lookup = t.getMessage();
				}
				map.put(name, lookup);
			}
		} catch (NamingException e) {
			// ignore
		}
		return map;
	}
 }