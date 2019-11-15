/*
 * Copyright (c) 2017-2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.microprofile.faulttolerance;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

/**
 * Wrap the execution and invoke it asynchronously.
 * Any methods marked with this annotation must return one of:
 * <ul>
 * <li>{@link java.util.concurrent.Future}</li>
 * <li>{@link java.util.concurrent.CompletionStage}</li>
 * </ul>
 * 
 * Otherwise, {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException} occurs
 * during deployment. 
 * The return type {@link java.util.concurrent.CompletionStage} is preferred over {@link java.util.concurrent.Future}
 * as a {@link java.util.concurrent.Future} that completes exceptionally will not trigger other Fault Tolerance operations 
 * even if specified (e.g. Retry), while a {@link java.util.concurrent.CompletionStage} that completes exceptionally will trigger other 
 * Fault Tolerance capabilities if specified (e.g. Retry).
 * 
 * <p>
 * When a method marked with this annotation is called from one thread (which we will call Thread A), the method call is
 * intercepted, and execution of the method is submitted to run asynchronously on another thread (which we will call
 * Thread B).
 * <p>
 * On Thread A, a Future or CompletionStage is returned immediately and can be used to get the result of the execution
 * taking place on Thread B, once it is complete.
 * <p>
 * Before the execution on Thread B completes, the Future or CompletionStage returned in Thread A will report itself as
 * incomplete. At this point, {@link java.util.concurrent.Future#cancel(boolean)} can be used to abort the execution.
 * <p>
 * Once the execution on Thread B is complete, the Future or CompletionStage returned in Thread A behaves differently
 * depending on whether the execution in Thread B threw an exception:
 * <ul>
 * <li>If the execution threw an exception, the Future or CompletionStage will be completed with that exception</li>
 * <li>If the execution returned normally, the Future or CompletionStage returned in Thread A will behave in the same
 * way as the Future or CompletionStage returned from the execution in Thread B, i.e. it can be
 * <ul>
 * <li>not complete yet</li>
 * <li>completed successfully with a return value</li>
 * <li>completed exceptionally</li>
 * </ul>
 * <p>
 * At this point, any calls to the Future or CompletionStage returned in Thread A will be delegated to the Future or
 * CompletionStage returned from the execution in Thread B.
 * </li>
 * </ul>
 * 
 * <p>
 * The call made on Thread A will never throw an exception, even if the method declares that it throws checked
 * exceptions, because the execution is going to occur on Thread B and hasn't happened yet.
 * To avoid unnecessary {@code try..catch} blocks around these method calls, it's recommended that methods annotated
 * with {@code @Asynchronous} do not declare that they throw checked exceptions.
 * <p>
 * Any exception thrown from the execution on Thread B, or raised by another Fault Tolerance component such as
 * {@link Bulkhead} or {@link CircuitBreaker}, can be retrieved in the following ways:
 * <ul>
 * <li>If the method declares {@link java.util.concurrent.Future} as the return type,
 * calling {@link java.util.concurrent.Future#get()} on the Future returned in Thread A will throw an
 * {@link java.util.concurrent.ExecutionException} wrapping the original exception</li>
 * <li>If the method declares {@link java.util.concurrent.CompletionStage} as the return type,
 * the CompletionStage returned in Thread A is completed exceptionally with the exception.</li>
 * </ul>
 * 
 * <p>
 * If a class is annotated with this annotation, all class methods are treated as if they were marked
 * with this annotation. If one of the methods doesn't return either Future or CompletionStage,
 * {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException}
 * occurs (at deploy time if the bean is discovered during deployment).
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * <code>@Asynchronous
 * public CompletionStage&lt;String&gt; getString() {
 *  return CompletableFuture.completedFuture("hello");
 * }</code></pre>
 *
 * <p>
 * Example call with exception handling:
 * </p>
 *
 * <pre>
 * <code>CompletionStage stage = getString().exceptionally(e -&gt; {
 *     handleException(e); 
 *     return null;
 * });</code></pre>
 * 
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@InterceptorBinding
@Inherited
public @interface Asynchronous {

}