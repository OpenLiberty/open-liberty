/*
 * Copyright 2026 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.ws.kernel.service.util;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.util.stream.IntStream.range;
import static org.junit.Assert.*;

public class LazyReferenceTest {
    private static final int INITIALIZATION_DELAY_MS = 50;
    private static final int EXPENSIVE_INITIALIZATION_DELAY_MS = 10;
    private static final int CONCURRENT_THREAD_COUNT = 10;
    private static final int HIGH_CONTENTION_THREAD_COUNT = 50;
    private static final int TEST_TIMEOUT_SECONDS = 10;
    private static final int EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 5;
    private static final int SEQUENTIAL_ACCESS_COUNT = 100;

    /**
     * Tests basic lazy initialization behavior.
     * Verifies that the initializer is called exactly once on first access
     * and subsequent calls return the cached value.
     */
    @Test
    public void testBasicInitialization() {
        AtomicInteger initCount = new AtomicInteger(0);
        LazyReference<String> ref = LazyReference.of(() -> {
            initCount.incrementAndGet();
            return "initialized";
        });

        assertFalse("Reference should not be initialized initially", ref.isCompleted());

        String value = ref.get();
        assertEquals("Should return initialized value", "initialized", value);
        assertEquals("Initializer should be called exactly once", 1, initCount.get());
        assertTrue("Reference should be initialized after first get", ref.isCompleted());

        // Second call should return cached value without re-initialization
        String value2 = ref.get();
        assertEquals("Should return same value", "initialized", value2);
        assertEquals("Initializer should still be called only once", 1, initCount.get());
    }

    /**
     * Tests thread-safe initialization under concurrent access.
     * Verifies that only one thread performs initialization even when
     * multiple threads attempt to access the value simultaneously.
     */
    @Test
    public void testConcurrentInitialization() throws InterruptedException {
        AtomicInteger initCount = new AtomicInteger(0);
        AtomicInteger maxConcurrentInits = new AtomicInteger(0);
        AtomicInteger currentConcurrentInits = new AtomicInteger(0);

        LazyReference<String> ref = LazyReference.of(() -> {
            initCount.incrementAndGet();
            int concurrent = currentConcurrentInits.incrementAndGet();
            maxConcurrentInits.updateAndGet(max -> Math.max(max, concurrent));

            // Simulate some work
            try {
                Thread.sleep(INITIALIZATION_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            currentConcurrentInits.decrementAndGet();
            return "initialized";
        });

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_THREAD_COUNT);
        final AtomicReference<Throwable> error = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREAD_COUNT);

        range(0, CONCURRENT_THREAD_COUNT).forEach(i -> {
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // All threads try to get the value simultaneously
                    String value = ref.get();
                    assertEquals("All threads should get the same value", "initialized", value);
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        });

        // Release all threads at once
        startLatch.countDown();

        // Wait for all threads to complete
        assertTrue("All threads should complete", doneLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        executor.shutdown();
        assertTrue("Executor should terminate", executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        assertNull("No errors should occur", error.get());
        assertEquals("Initializer should be called exactly once despite concurrent access", 1, initCount.get());
        assertEquals("Only one thread should be initializing at a time", 1, maxConcurrentInits.get());
    }

    /**
     * Tests initialization under high contention with many concurrent threads.
     * Repeated 10 times to catch potential race conditions.
     * Verifies that initialization happens exactly once despite high contention.
     */
    @Test
    public void testHighContentionInitialization() throws InterruptedException {
        for (int repeat = 0; repeat < 10; repeat++) {
            AtomicInteger initCount = new AtomicInteger(0);

            LazyReference<Integer> ref = LazyReference.of(() -> {
                int count = initCount.incrementAndGet();
                // Simulate expensive initialization
                try {
                    Thread.sleep(EXPENSIVE_INITIALIZATION_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return count;
            });

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(HIGH_CONTENTION_THREAD_COUNT);
            AtomicInteger successCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(HIGH_CONTENTION_THREAD_COUNT);

            for (int i = 0; i < HIGH_CONTENTION_THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        Integer value = ref.get();
                        assertEquals("All threads should get value 1", 1, value.intValue());
                        successCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue("All threads should complete", doneLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS));

            executor.shutdown();
            assertTrue("Executor should terminate", executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS));

            assertEquals("All threads should succeed (repeat " + repeat + ")", HIGH_CONTENTION_THREAD_COUNT, successCount.get());
            assertEquals("Initializer should be called exactly once (repeat " + repeat + ")", 1, initCount.get());
        }
    }

    /**
     * Tests that initialization exceptions are propagated to the caller.
     * Verifies that exceptions thrown during initialization are not swallowed.
     */
    @Test(expected = RuntimeException.class)
    public void testInitializationWithException() {
        LazyReference<String> ref = LazyReference.of(() -> {
            throw new RuntimeException("Initialization failed");
        });
        ref.get();
    }

    /**
     * Tests retry behavior when initialization fails.
     * Verifies that with retry enabled, failed initialization can be retried
     * on subsequent calls, and successful initialization is cached.
     */
    @Test
    public void testInitializationWithExceptionRetry() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        LazyReference<String> ref = LazyReference.ofRetryable(() -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt == 1) {
                throw new RuntimeException("First attempt failed");
            }
            return "success-" + attempt;
        });

        // First attempt should fail
        try {
            ref.get();
            fail("First attempt should throw exception");
        } catch (RuntimeException e) {
            // expected
        }
        assertFalse("Reference should not be initialized after exception", ref.isCompleted());

        // Second attempt should succeed
        String value = ref.get();
        assertEquals("Second attempt should succeed", "success-2", value);
        assertTrue("Reference should be initialized after successful retry", ref.isCompleted());
        assertEquals("Should have attempted twice", 2, attemptCount.get());

        // Subsequent calls should return cached value
        assertEquals("Should return cached value", "success-2", ref.get());
        assertEquals("Should not retry after success", 2, attemptCount.get());
    }

    /**
     * Tests permanent error state when retry is disabled.
     * Verifies that when initialization fails and retry is disabled,
     * all subsequent calls throw InitializationException without retrying,
     * and each exception wraps the same original cause.
     */
    @Test
    public void testInitializationWithExceptionNoRetry() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RuntimeException originalException = new RuntimeException("Initialization failed");
        LazyReference<String> ref = LazyReference.of(() -> {
            attemptCount.incrementAndGet();
            throw originalException;
        });

        // First attempt should fail with wrapped exception
        try {
            ref.get();
            fail("First attempt should throw InitializationException");
        } catch (LazyReference.InitializationException e) {
            assertSame("Should wrap original exception", originalException, e.getCause());
        }
        assertTrue("Reference should be in error state after exception", ref.isCompleted());
        assertEquals("Should have attempted once", 1, attemptCount.get());

        // Second attempt should throw new wrapped exception with same cause, without retrying
        try {
            ref.get();
            fail("Second attempt should throw wrapped exception");
        } catch (LazyReference.InitializationException e) {
            assertSame("Should wrap same original exception", originalException, e.getCause());
        }
        assertEquals("Should not retry initialization", 1, attemptCount.get());

        // Subsequent calls should continue throwing new wrapped exceptions with same cause
        try {
            ref.get();
            fail("Third attempt should throw wrapped exception");
        } catch (LazyReference.InitializationException e) {
            assertSame("Should still wrap same original exception", originalException, e.getCause());
        }
        assertEquals("Should still not retry", 1, attemptCount.get());
    }

    /**
     * Tests that Error instances are handled the same as exceptions.
     * Verifies that even Error subclasses (like OutOfMemoryError) are wrapped
     * in InitializationException and result in permanent error state when retry is disabled.
     */
    @Test
    public void testInitializationWithErrorNoRetry() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        OutOfMemoryError originalError = new OutOfMemoryError("Simulated OOM");
        LazyReference<String> ref = LazyReference.of(() -> {
            attemptCount.incrementAndGet();
            throw originalError;
        });

        // First attempt should fail with wrapped exception (even for Error)
        try {
            ref.get();
            fail("First attempt should throw InitializationException");
        } catch (LazyReference.InitializationException e) {
            assertSame("Should wrap original Error", originalError, e.getCause());
        }
        assertTrue("Reference should be in error state after Error", ref.isCompleted());
        assertEquals("Should have attempted once", 1, attemptCount.get());

        // Second attempt should throw new wrapped exception with same cause, without retrying
        try {
            ref.get();
            fail("Second attempt should throw wrapped exception");
        } catch (LazyReference.InitializationException e) {
            assertSame("Should wrap same original Error", originalError, e.getCause());
        }
        assertEquals("Should not retry initialization", 1, attemptCount.get());
    }

    /**
     * Tests that multiple LazyReference instances are independent.
     * Verifies that each instance maintains its own initialization state
     * and cached value.
     */
    @Test
    public void testMultipleInstances() {
        AtomicInteger initCount = new AtomicInteger(0);
        LazyReference.Init<String> supplier = () -> {
            int count = initCount.incrementAndGet();
            return "initialized-" + count;
        };

        LazyReference<String> ref1 = LazyReference.of(supplier);
        assertEquals("initialized-1", ref1.get());
        assertTrue(ref1.isCompleted());

        LazyReference<String> ref2 = LazyReference.of(supplier);
        assertEquals("initialized-2", ref2.get());
        assertTrue(ref2.isCompleted());

        // Verify each reference maintains its own value
        assertEquals("initialized-1", ref1.get());
        assertEquals("initialized-2", ref2.get());
        assertEquals("Initializer should be called once per reference", 2, initCount.get());
    }

    /**
     * Tests the {@code of(Init, StandIn)} factory method with recursive initialization.
     * Verifies that the {@link LazyReference.Init}-based overload supports placeholders
     * without requiring the {@link LazyReference.InitWith}-based initializer signature.
     */
    @Test
    public void testInitFactoryWithPlaceholder() {
        @SuppressWarnings("unchecked")
        LazyReference<String>[] refHolder = new LazyReference[1];
        AtomicInteger placeholderCallCount = new AtomicInteger(0);

        refHolder[0] = LazyReference.of(
            () -> "final-" + refHolder[0].get(),
            () -> {
                placeholderCallCount.incrementAndGet();
                return "placeholder";
            }
        );

        assertEquals("Supplier/Supplier factory should use placeholder during recursion",
            "final-placeholder", refHolder[0].get());
        assertEquals("Placeholder generator should be called exactly once",
            1, placeholderCallCount.get());
        assertTrue("Reference should be initialized", refHolder[0].isCompleted());
    }

    /**
     * Tests the {@code ofRetryable(Init, StandIn)} factory method.
     * Verifies that the retryable {@link LazyReference.Init}-based overload supports
     * placeholders and succeeds after an initial failure.
     */
    @Test
    public void testInitFactoryWithPlaceholderAndRetry() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        AtomicInteger placeholderCallCount = new AtomicInteger(0);
        @SuppressWarnings("unchecked")
        LazyReference<String>[] refHolder = new LazyReference[1];

        refHolder[0] = LazyReference.ofRetryable(
            () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt == 1) throw new RuntimeException("First attempt failed");
                return "final-" + refHolder[0].get();
            },
            () -> {
                placeholderCallCount.incrementAndGet();
                return "placeholder";
            }
        );

        try {
            refHolder[0].get();
            fail("First attempt should fail");
        } catch (RuntimeException e) {
            // expected
        }
        assertFalse("Reference should not be completed after retryable failure", refHolder[0].isCompleted());

        assertEquals("Second attempt should succeed using the placeholder",
            "final-placeholder", refHolder[0].get());
        assertEquals("Initializer should be attempted twice", 2, attemptCount.get());
        assertEquals("Placeholder generator should be called only during successful recursive initialization",
            1, placeholderCallCount.get());
        assertTrue("Reference should be initialized after retry", refHolder[0].isCompleted());
    }

    /**
     * Tests that null values are properly supported.
     * Verifies that null can be returned from initialization and is cached
     * like any other value.
     */
    @Test
    public void testNullValue() {
        LazyReference<String> ref = LazyReference.of(() -> null);

        assertNull("Should support null values", ref.get());
        assertTrue("Reference should be initialized even with null value", ref.isCompleted());

        // Second call should still return null without re-initialization
        assertNull("Should return null on subsequent calls", ref.get());
    }

    /**
     * Tests lazy initialization with complex objects.
     * Verifies that the same object instance is returned on all calls
     * and initialization happens only once.
     */
    @Test
    public void testComplexObject() {
        class ComplexObject {
            final String name;
            final int value;

            ComplexObject(String name, int value) {
                this.name = name;
                this.value = value;
            }
        }

        AtomicInteger counter = new AtomicInteger(0);
        LazyReference<ComplexObject> ref = LazyReference.of(() -> {
            counter.incrementAndGet();
            return new ComplexObject("test", 42);
        });

        ComplexObject obj1 = ref.get();
        ComplexObject obj2 = ref.get();

        assertSame("Should return the same instance", obj1, obj2);
        assertEquals("test", obj1.name);
        assertEquals(42, obj1.value);
        assertEquals("Should initialize only once", 1, counter.get());
    }

    /**
     * Tests that sequential access doesn't trigger re-initialization.
     * Verifies that many sequential calls to get() only initialize once.
     */
    @Test
    public void testSequentialAccess() {
        AtomicInteger initCount = new AtomicInteger(0);
        LazyReference<String> ref = LazyReference.of(() -> {
            initCount.incrementAndGet();
            return "value";
        });

        // Multiple sequential accesses
        for (int i = 0; i < SEQUENTIAL_ACCESS_COUNT; i++) {
            assertEquals("value", ref.get());
        }

        assertEquals("Should initialize only once despite many accesses", 1, initCount.get());
    }

    /**
     * Tests detection of recursive initialization without placeholder support.
     * Verifies that when no placeholder generator is provided, recursive calls
     * during initialization throw IllegalStateException with RecursiveInitializationException
     * as the cause, and the reference enters a permanent error state.
     */
    @Test
    public void testRecursiveInitializationDetection() {
        @SuppressWarnings("unchecked")
        LazyReference<String>[] refHolder = new LazyReference[1];

        // Create a LazyReference that tries to call get() during initialization
        refHolder[0] = LazyReference.of(() -> {
            // This should throw IllegalStateException due to recursive call
            return refHolder[0].get();
        });

        try {
            refHolder[0].get();
            fail("Should detect recursive initialization");
        } catch (IllegalStateException e) {
            assertEquals("Exception message should indicate recursive initialization failure",
                "Initialization failed due to recursive call", e.getMessage());
            assertNotNull("Should have a cause", e.getCause());
            assertTrue("Cause should be RecursiveInitializationException",
                e.getCause() instanceof LazyReference.RecursiveInitializationException);
            assertTrue("Cause message should indicate recursive initialization",
                e.getCause().getMessage().contains("Recursive initialization detected"));
            assertTrue("Cause message should include thread name",
                e.getCause().getMessage().contains(Thread.currentThread().getName()));
        }
    }

    /**
     * Tests recursive initialization with placeholder support.
     * Verifies that when a placeholder generator is provided, recursive calls
     * during initialization return the placeholder value instead of throwing an exception.
     * The placeholder is generated once and the final value incorporates it.
     */
    @Test
    public void testRecursiveInitializationWithPlaceholder() {
        @SuppressWarnings("unchecked")
        LazyReference<String>[] refHolder = new LazyReference[1];
        AtomicInteger placeholderCallCount = new AtomicInteger(0);

        // Create a LazyReference with placeholder support
        refHolder[0] = LazyReference.of(
            placeholderSupplier -> {
                // Recursive call should return placeholder instead of throwing
                String placeholder = refHolder[0].get();
                return "final-value-with-" + placeholder;
            },
            () -> {
                placeholderCallCount.incrementAndGet();
                return "placeholder";
            }
        );

        String result = refHolder[0].get();
        assertEquals("Should use placeholder during recursive initialization",
            "final-value-with-placeholder", result);
        assertEquals("Placeholder generator should be called exactly once",
            1, placeholderCallCount.get());
        assertTrue("Reference should be initialized", refHolder[0].isCompleted());

        // Subsequent calls should return the final value
        assertEquals("Should return cached final value",
            "final-value-with-placeholder", refHolder[0].get());
        assertEquals("Placeholder generator should not be called again",
            1, placeholderCallCount.get());
    }

    /**
     * Tests recursive initialization with a custom placeholder value.
     * Verifies that custom placeholder values (non-String types) work correctly
     * and can be used in calculations during initialization.
     */
    @Test
    public void testRecursiveInitializationWithCustomPlaceholder() {
        @SuppressWarnings("unchecked")
        LazyReference<Integer>[] refHolder = new LazyReference[1];

        refHolder[0] = LazyReference.of(
            placeholderSupplier -> {
                Integer placeholder = refHolder[0].get();
                // Use placeholder in calculation
                return placeholder * 10;
            },
            () -> 42  // Custom placeholder value
        );

        assertEquals("Should use custom placeholder value", Integer.valueOf(420), refHolder[0].get());
    }

    /**
     * Tests that multiple recursive calls reuse the same placeholder.
     * Verifies that the placeholder generator is called only once even when
     * the initializer makes multiple recursive get() calls, and all recursive
     * calls return the same placeholder instance.
     */
    @Test
    public void testMultipleRecursiveCallsReusePlaceholder() {
        @SuppressWarnings("unchecked")
        LazyReference<String>[] refHolder = new LazyReference[1];
        AtomicInteger placeholderCallCount = new AtomicInteger(0);

        refHolder[0] = LazyReference.of(
            placeholderSupplier -> {
                // Multiple recursive calls should reuse the same placeholder
                String first = refHolder[0].get();
                String second = refHolder[0].get();
                String third = refHolder[0].get();

                assertEquals("Multiple recursive calls should return same placeholder", first, second);
                assertEquals("Multiple recursive calls should return same placeholder", second, third);

                return "final-" + first;
            },
            () -> {
                placeholderCallCount.incrementAndGet();
                return "placeholder";
            }
        );

        String result = refHolder[0].get();
        assertEquals("final-placeholder", result);
        assertEquals("Placeholder should be generated only once despite multiple recursive calls",
            1, placeholderCallCount.get());
    }

    /**
     * Tests that null placeholder values are handled correctly.
     * Verifies that a placeholder generator can return null and the
     * initializer can distinguish between null and non-null placeholders.
     */
    @Test
    public void testPlaceholderWithNullValue() {
        @SuppressWarnings("unchecked")
        LazyReference<String>[] refHolder = new LazyReference[1];

        refHolder[0] = LazyReference.of(
            placeholderSupplier -> {
                String placeholder = refHolder[0].get();
                return placeholder == null ? "null-placeholder" : "non-null-placeholder";
            },
            () -> null  // Null placeholder
        );

        assertEquals("Should handle null placeholder correctly", "null-placeholder", refHolder[0].get());
    }

    /**
     * Tests that placeholder generator is not called without recursion.
     * Verifies that when initialization completes without any recursive calls,
     * the placeholder generator is never invoked, avoiding unnecessary work.
     */
    @Test
    public void testPlaceholderNotCalledWithoutRecursion() {
        AtomicInteger placeholderCallCount = new AtomicInteger(0);
        AtomicInteger initCallCount = new AtomicInteger(0);

        LazyReference<String> ref = LazyReference.of(
            placeholderSupplier -> {
                initCallCount.incrementAndGet();
                // No recursive call, so placeholder should not be generated
                return "normal-value";
            },
            () -> {
                placeholderCallCount.incrementAndGet();
                return "placeholder";
            }
        );

        String result = ref.get();
        assertEquals("normal-value", result);
        assertEquals("Initializer should be called once", 1, initCallCount.get());
        assertEquals("Placeholder generator should not be called without recursion",
            0, placeholderCallCount.get());
    }

    /**
     * Tests thread safety when using placeholders with concurrent access.
     * Verifies that when one thread is performing initialization with recursive calls,
     * other threads correctly wait and receive the final value (not the placeholder).
     * Only the initializing thread should see the placeholder.
     */
    @Test
    public void testConcurrentAccessWithPlaceholder() throws InterruptedException {
        @SuppressWarnings("unchecked")
        LazyReference<String>[] refHolder = new LazyReference[1];
        AtomicInteger placeholderCallCount = new AtomicInteger(0);
        CountDownLatch recursiveLatch = new CountDownLatch(1);
        CountDownLatch waitLatch = new CountDownLatch(1);

        refHolder[0] = LazyReference.of(
            placeholderSupplier -> {
                // Signal that we're in initialization
                recursiveLatch.countDown();

                // Make recursive call
                String placeholder = refHolder[0].get();

                // Wait a bit to allow other threads to attempt access
                try {
                    waitLatch.await(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                return "final-" + placeholder;
            },
            () -> {
                placeholderCallCount.incrementAndGet();
                return "placeholder";
            }
        );

        // Start initialization in background
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicReference<String> result1 = new AtomicReference<>();
        AtomicReference<String> result2 = new AtomicReference<>();

        executor.submit(() -> {
            result1.set(refHolder[0].get());
        });

        // Wait for initialization to start
        assertTrue("Initialization should start", recursiveLatch.await(1, TimeUnit.SECONDS));

        // Try to access from another thread while initialization is in progress
        executor.submit(() -> {
            result2.set(refHolder[0].get());
        });

        // Release the initialization
        waitLatch.countDown();

        executor.shutdown();
        assertTrue("Executor should terminate", executor.awaitTermination(5, TimeUnit.SECONDS));

        // Both threads should get the final value
        assertEquals("final-placeholder", result1.get());
        assertEquals("final-placeholder", result2.get());
        assertEquals("Placeholder should be generated only once", 1, placeholderCallCount.get());
    }

    /**
     * Tests that the placeholder supplier is passed to the initializer function.
     * Verifies that the initializer receives a supplier that can provide placeholders,
     * even if it chooses not to use it. Outside of recursive context, the supplier
     * returns null since no placeholder was generated.
     */
    @Test
    public void testPlaceholderSupplierAccessibleToInitializer() {
        AtomicReference<Supplier<String>> capturedSupplier = new AtomicReference<>();

        LazyReference<String> ref = LazyReference.of(
            placeholderSupplier -> {
                // Capture the supplier for verification
                capturedSupplier.set(placeholderSupplier);
                // Don't actually call it to avoid recursion
                return "value";
            },
            () -> "placeholder"
        );

        ref.get();

        assertNotNull("Placeholder supplier should be passed to initializer", capturedSupplier.get());

        // Verify the supplier works (outside of initialization context)
        // Note: This will return null since we're not in a recursive context
        assertNull("Placeholder supplier should return null outside recursive context",
            capturedSupplier.get().get());
    }

    /**
     * Tests that placeholder is cleared after initialization completes.
     * Verifies that once initialization finishes, the placeholder is no longer
     * retrievable, even if a reference to the placeholder supplier was captured
     * during initialization. This ensures the placeholder's role ends with initialization.
     */
    @Test
    public void testPlaceholderClearedAfterInitialization() {
        @SuppressWarnings("unchecked")
        LazyReference<String>[] refHolder = new LazyReference[1];
        AtomicReference<Supplier<String>> capturedSupplier = new AtomicReference<>();

        refHolder[0] = LazyReference.of(
            placeholderSupplier -> {
                // Capture the supplier for later verification
                capturedSupplier.set(placeholderSupplier);
                // Make recursive call to generate placeholder
                String placeholder = refHolder[0].get();
                return "final-" + placeholder;
            },
            () -> "placeholder"
        );

        // Initialize the reference
        String result = refHolder[0].get();
        assertEquals("Should use placeholder during initialization", "final-placeholder", result);

        // After initialization completes, the placeholder should be cleared
        // The captured supplier should now return null
        assertNull("Placeholder should be cleared after initialization completes",
            capturedSupplier.get().get());
    }
}
