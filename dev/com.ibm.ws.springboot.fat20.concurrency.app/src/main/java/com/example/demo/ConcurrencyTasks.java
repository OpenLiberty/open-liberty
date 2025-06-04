package com.example.demo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
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
}
