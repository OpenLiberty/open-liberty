/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
 * Any methods marked with this annotation must return {@link java.util.concurrent.Future}. 
 * Otherwise, {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException} occurs.
 * Example usage:
 * 
 * <pre>
 * <code>@Asynchronous</code>
 * public Future&lt;String&gt; getString() {
 *  return CompletableFuture.completedFuture("hello");
 * }
 * </pre>
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@InterceptorBinding
@Inherited
public @interface Asynchronous {
}
