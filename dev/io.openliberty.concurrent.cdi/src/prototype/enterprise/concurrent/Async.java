/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package prototype.enterprise.concurrent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

// For experimentation with a possible Async or Asynchronous annotation in Jakarta Concurrency.
// TODO Delete this class if it goes into the spec. Delete this class if it doesn't.
/**
 * Annotates a CDI managed bean method (or CDI managed bean class containing suitable methods)
 * to run asynchronously.
 * <p>
 * The Jakarta EE Product Provider runs the method on a {@link ManagedExecutorService}
 * and returns to the caller a {@link java.util.concurrent.CompletableFuture CompletableFuture}
 * that is backed by the same <code>ManagedExecutorService</code>
 * to represent the execution of the method.
 * The Jakarta EE Product Provider makes this <code>CompletableFuture</code> available
 * to the asynchronous method implementation via the
 * {@link Result#getFuture Async.Result.getFuture} and
 * {@link Result#complete Async.Result.complete} methods.
 * <p>
 * For example,
 *
 * <pre>
 * {@literal @}Async
 * CompletableFuture{@literal <Double>} totalSales(LocalDate from, LocalDate to) {
 *     // Application component's context is available to the async method,
 *     try (Connection con = ((DataSource) InitialContext.doLookup(
 *         "java:comp/env/jdbc/salesDB")).getConnection() {
 *         ...
 *         double total = rs.next() ? rs.getDouble() : 0.0;
 *         return Async.Result.complete(total);
 *     } catch (NamingException | SQLException x) {
 *         throw new CompletionException(x);
 *     }
 * }
 * </pre>
 *
 * with usage,
 *
 * <pre>
 * totalSales(mon, fri).thenAccept(total -> {
 *     // Application component's context is available to dependent stage actions,
 *     DataSource ds = InitialContext.doLookup(
 *         "java:comp/env/jdbc/reportsDB");
 *     ...
 * });
 * </pre>
 *
 * When the asynchronous method implementation returns a different
 * <code>CompletableFuture</code> instance, the Jakarta EE Product Provider
 * uses the completion of that instance to complete the <code>CompletableFuture</code>
 * that the Jakarta EE Product Provider returns to the caller,
 * completing it with the same result or exception.
 * <p>
 * For example,
 *
 * <pre>
 * {@literal @}Async
 * CompletableFuture{@literal <List<Itinerary>>} findSingleLayoverFlights(Location source, Location dest) {
 *     try {
 *         ManagedExecutorService executor = InitialContext.doLookup(
 *             "java:comp/DefaultManagedExecutorService");
 *
 *         return executor.supplyAsync(source::flightsFrom)
 *                        .thenCombine(executor.completedFuture(dest.flightsTo),
 *                                     Itinerary::destMatchingSource);
 *     } catch (NamingException) {
 *         throw new CompletionException(x);
 *     }
 * }
 * </pre>
 *
 * with usage,
 *
 * <pre>
 * findSingleLayoverFlights(RST, DEN).thenApply(Itinerary::sortByPrice);
 * </pre>
 *
 * <p>
 * When annotating asynchronous methods at the method level, methods
 * can have any of the following return types, with all other return types resulting in
 * {@link java.lang.UnsupportedOperationException UnsupportedOperationException}:
 * <ul>
 * <li>{@link java.util.concurrent.CompletableFuture CompletableFuture}</li>
 * <li>{@link java.util.concurrent.CompletionStage CompletionStage}</li>
 * <li><code>void</code></li>
 * </ul>
 * <p>
 * When annotating asynchronous methods at the class level, methods with
 * any of the following return types are considered asynchronous methods,
 * whereas methods with other return types are treated as normal
 * (non-asynchronous) methods:
 * <ul>
 * <li>{@link java.util.concurrent.CompletableFuture CompletableFuture}</li>
 * <li>{@link java.util.concurrent.CompletionStage CompletionStage}</li>
 * </ul>
 * <p>
 * If the <code>Async</code> annotation is present at both the method and class
 * level, the annotation that is specified at the method level takes precedence.
 * <p>
 * Exceptions that are raised by asynchronous methods are not raised directly
 * to the caller because the method runs asynchronously to the caller.
 * Instead, the <code>CompletableFuture</code> that represents the result
 * is completed with the raised exception. Asynchronous methods are
 * discouraged from raising checked exceptions because checked exceptions
 * force the caller to write exception handling code that is unreachable.
 * When a checked occurs and the asynchronous method implementation wishes
 * to flow the exception back to the resulting <code>CompletableFuture</code>,
 * the asynchronous method implementation should either raise a
 * {@link java.util.concurrent.CompletionException CompletionException}
 * with the original exception as the cause, or it can take the equivalent
 * approach of returning the exceptionally completed future using
 * {@link java.util.concurrent.CompletableFuture#completeExceptionally completeExceptionally}
 * to supply the original exception as the cause.
 * <p>
 * Except where otherwise stated, the Jakarta EE Product Provider raises
 * {@link java.util.concurrent.RejectedExecutionException RejectedExecutionException}
 * upon invocation of the asynchronous method if evident upfront that it cannot
 * be accepted, for example if the JNDI name is not valid or points to something
 * other than a managed executor resource. If determined at a later point that the
 * asynchronous method cannot run (for example, if unable to establish thread context),
 * then the Jakarta EE Product Provider completes the <code>CompletableFuture</code>
 * exceptionally with {@link java.util.concurrent.CancellationException CancellationException},
 * and chains a cause exception if there is any.
 * <p>
 * The Jakarta EE Product Provider must assign the interceptor for asynchronous methods
 * to have priority of <code>Interceptor.Priority.PLATFORM_BEFORE + 5</code>,
 * so as to enable most other platform interceptors, such as <code>Transactional</code>,
 * to be applied to the thread upon which the asynchronous method executes.
 * When an asynchronous method is annotated as <code>Transactional</code>,
 * the transactional types <code>TxType.REQUIRES_NEW</code> and
 * <code>TxType.NOT_SUPPORTED</code> can be used. All other transaction attributes must
 * result in {@link java.lang.UnsupportedOperationException UnsupportedOperationException}
 * upon invocation of the asynchronous method.
 */
// TODO the above restrictions on Transactional interceptors could be eliminated
// if transaction context propagation is later added to the spec.
@Documented
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Async {
    /**
     * JNDI name of a {@link ManagedExecutorService} or {@link ManagedScheduledExecutorService}
     * upon which to run the asynchronous method.
     * <p>
     * The default value is the JNDI name of the built-in <code>ManagedExecutorService</code>
     * that is provided by the Jakarta EE platform provider:<br>
     * <code>java:comp/DefaultManagedExecutorService</code>
     *
     * @return managed executor service JNDI name.
     */
    @Nonbinding
    String executor() default "java:comp/DefaultManagedExecutorService";

    /**
     * Mechanism by which the Jakarta EE Product Provider makes available
     * to the asynchronous method implementation the same
     * {@link java.util.concurrent.CompletableFuture CompletableFuture}
     * instance that the Jakarta EE Product Provider supplies to the caller
     * of the asynchronous method.
     * <p>
     * Before invoking the asynchronous method implementation on a thread,
     * the Jakarta EE Product Provider invokes the {@link #setFuture} method
     * which makes available to the asynchronous method implementation
     * the same <code>CompletableFuture</code> that the Jakarta EE Product Provider
     * returns to the caller.
     * <p>
     * The asynchronous method implementation invokes the {@link #getFuture} method
     * to obtain the same <code>CompletableFuture</code> that the
     * Jakarta EE Product Provider returns to the caller.
     * The asynchronous method implementation can choose to complete
     * this future (normally or exceptionally) or otherwise arrange for its
     * completion, for example upon completion of a pipeline of completion stages.
     * Having this same <code>CompletableFuture</code> also enables the asynchronous
     * method implementation to determine if the caller has forcibly completed
     * (such as by cancellation or any other means) the <code>CompletableFuture</code>,
     * in which case the asynchronous method implementation could decide to end
     * immediately rather than continue processing.
     * <p>
     * For example,
     *
     * <pre>
     * {@literal @}Async
     * CompletableFuture{@literal <Double>} totalSales(LocalDate from, LocalDate to) {
     *     CompletableFuture<Double> future = Async.Result.getFuture(Double.class);
     *     if (future.isDone())
     *         return future;
     *
     *     try (Connection con = ((DataSource) InitialContext.doLookup(
     *         "java:comp/env/jdbc/salesDB")).getConnection() {
     *         ...
     *         for (ResultSet result = stmt.executeQuery(); result.next() && !future.isDone(); )
     *             ...
     *         future.complete(total);
     *     } catch (NamingException | SQLException x) {
     *         future.completeExceptionally(x);
     *     }
     *     return future;
     * }
     * </pre>
     *
     * After the asynchronous method completes, the Jakarta EE Product Provider
     * invokes the {@link #setFuture} method with a <code>null</code> value
     * to clear it from the thread.
     */
    public static class Result {
        private static final ThreadLocal<CompletableFuture<?>> futures = new ThreadLocal<CompletableFuture<?>>();

        /**
         * Completes the {@link java.util.concurrent.CompletableFuture CompletableFuture}
         * instance that the Jakarta EE Product Provider supplies to the caller of the
         * asynchronous method.
         * <p>
         * This method must only be invoked by the asynchronous method implementation.
         *
         * @param <T>  type of result returned by the asynchronous method's <code>CompletableFuture</code>.
         * @param type type of result returned by the asynchronous method's <code>CompletableFuture</code>.
         * @return the same <code>CompletableFuture</code> that the container returns to the caller.
         * @throws IllegalStateException if the <code>CompletableFuture</code> for an asynchronous
         *                                   method is not present on the thread.
         */
        public static <T> CompletableFuture<T> complete(T result) {
            @SuppressWarnings("unchecked")
            CompletableFuture<T> future = (CompletableFuture<T>) futures.get();
            if (future == null)
                throw new IllegalStateException();
            future.complete(result);
            return future;
        }

        /**
         * Obtains the same {@link java.util.concurrent.CompletableFuture CompletableFuture}
         * instance that the Jakarta EE Product Provider supplies to the caller of the
         * asynchronous method.
         * <p>
         * This method must only be invoked by the asynchronous method implementation.
         *
         * @param <T> type of result returned by the asynchronous method's <code>CompletableFuture</code>.
         * @return the same <code>CompletableFuture</code> that the container returns to the caller.
         * @throws IllegalStateException if the <code>CompletableFuture</code> for an asynchronous
         *                                   method is not present on the thread.
         */
        public static <T> CompletableFuture<T> getFuture() {
            @SuppressWarnings("unchecked")
            CompletableFuture<T> future = (CompletableFuture<T>) futures.get();
            if (future == null)
                throw new IllegalStateException();
            return future;
        }

        /**
         * Before invoking the asynchronous method implementation on a thread,
         * the Jakarta EE Product Provider invokes this method to make available
         * to the asynchronous method implementation the same <code>CompletableFuture</code>
         * that the Jakarta EE Product Provider returns to the caller.
         * <p>
         * After the asynchronous method completes, the Jakarta EE Product Provider
         * invokes this method with a <code>null</code> value
         * to clear it from the thread.
         * <p>
         * This method must only be invoked by the Jakarta EE Product Provider.
         *
         * @param <T>    type of result returned by the asynchronous method's <code>CompletableFuture</code>.
         * @param future
         */
        public static <T> void setFuture(CompletableFuture<T> future) {
            if (future == null)
                futures.remove();
            else
                futures.set(future);
        }
    }
}