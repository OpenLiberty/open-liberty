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

	private ConcurrencyTasks concurrencyApplicationConfig = new ConcurrencyTasks();

	public MyTask(ConcurrencyTasks concurrencyApplicationConfig) {
		this.concurrencyApplicationConfig = concurrencyApplicationConfig;
	}

    //Scheduled method for the Async tasks
    @Scheduled(fixedDelay = 5000)
    public void scheduledTask() throws InterruptedException, Throwable {

    	AppRunner.assertManagedThread("ScheduledTask");
		AppRunner.assertAsyncMethod("ScheduledTask");
    }
 }