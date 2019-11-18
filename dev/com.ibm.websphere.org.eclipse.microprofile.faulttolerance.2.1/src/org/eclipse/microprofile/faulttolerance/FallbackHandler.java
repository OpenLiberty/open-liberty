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
/**
 * The handler instance used by the container to service a fallback invocation is a non-contextual
 * instance created using the CDI SPI. The instance exists to service a single invocation only.
 * The type parameter of the handler instance must be assignable to the return type of the method, 
 * where the {@link Fallback} is specified. The container must ensure this type safety. 
 * Otherwise, {@link IllegalArgumentException} should be thrown. 
 * <h3>Usage</h3>
 * <pre>
 * public class MyService {
 *  &#064;Inject OtherService otherService;
 * 
 *  &#064;Timeout(3000)
 *  &#064;Fallback(MyFallback.class) 
 *  Long getAmount() {
 *      return otherService.getAmount() * 2;
 *  }
 *}
 *</pre>
 *
 *The fallback handler implementation is shown below. The type parameter must be assignable to 
 *{@link Long}.
 *
 *<pre>
 * public class MyFallback implements FallbackHandler&lt;Long&gt; {
 * Long handle(ExecutionContext context) {
 *   return 42;
 * }
*}
</pre>
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 * @author Ken Finnigan
 *
 */
public interface FallbackHandler<T>{
    /**
     * Handle the previous calling failure and then call alternative methods
     * or perform any alternative operations.
     * 
     * @param context the execution context
     * @return the result of the fallback 
     */
    T handle(ExecutionContext context);

}
