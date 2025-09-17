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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration(proxyBeanMethods = false)
@EnableAsync
public class ConcurrencyTasks {

	@Async
	public CompletableFuture<String> task1(String message) throws Exception {
		System.out.println("Async Task 1: " + Thread.currentThread().getName());
		TimeUnit.SECONDS.sleep(5);
		AppRunner.assertManagedThread(message + ": Async Task 1");
		return CompletableFuture.completedFuture("Async Task 1 passed");
	}
	
	@Async
	public CompletableFuture<String> task2(String message) throws Exception {
		System.out.println("Async Task 2: " + Thread.currentThread().getName());
		TimeUnit.SECONDS.sleep(3);
		AppRunner.assertManagedThread(message + ": Async Task 2");
		return CompletableFuture.completedFuture("Async Task 2 passed");
	}
	
	@Async("taskExecutor1")
	public CompletableFuture<String> task3(String message) throws Exception {
		System.out.println("Async Task 3: " + Thread.currentThread().getName());
		TimeUnit.SECONDS.sleep(4);
		AppRunner.assertManagedThread(message + ": Async Task 3");
		return CompletableFuture.completedFuture("Async Task 3 passed");
	}
}
