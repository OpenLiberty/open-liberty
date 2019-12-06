/*
 * Copyright (c) 2017-2019 Contributors to the Eclipse Foundation
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

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

/**
 * The fallback annotation to define the fallback handler class so that
 * a failure can be handled properly. Below is the criteria:
 * <ol>
 * <li>If value is specified, use {@link FallbackHandler#handle(ExecutionContext)} on the specified handler to execute the fallback.</li>
 * <li>If fallbackMethod is specified, invoke the method specified by the fallbackMethod on the same class.</li>
 * <li>If both are specified, the {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException} must be thrown.</li>
 * </ol>
 * <p>
 * When a method returns and the Fallback policy is present, the following rules are applied:
 * <ol>
 * <li>If the method returns normally (doesn't throw), the result is simply returned.
 * <li>Otherwise, if the thrown object is assignable to any value in the {@link #skipOn()} parameter, the thrown object will be rethrown.
 * <li>Otherwise, if the thrown object is assignable to any value in the {@link #applyOn()} parameter, 
 * the Fallback policy, detailed above, will be applied.
 * <li>Otherwise the thrown object will be rethrown.
 * </ol>
 * If a method throws a {@link Throwable} which is not an {@link Error} or {@link Exception}, non-portable behavior results.

 * 
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Inherited
@InterceptorBinding
public @interface Fallback {

    /**
     * Create a default class so the value is not required to be set all the time.
     */
    class DEFAULT implements FallbackHandler<Void>{
        @Override
        public Void handle(ExecutionContext context) {
            return null;
        }
    }
    /**
     * Specify the fallback class to be used. An new instance of the fallback class
     * is returned. The instance is unmanaged. The type parameter of the fallback class must be assignable to the
     * return type of the annotated method. 
     * Otherwise, {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException} occurs.
     * 
     * @return the fallback class
     */
    @Nonbinding
    Class<? extends FallbackHandler<?>> value() default DEFAULT.class;
    
    /**
    * Specify the method name to be fallbacked to. This method belongs
    * to the same class as the method to fallback.
    * The method must have the exactly same arguments as the method being annotated.
    * The method return type must be assignable to the return type of the method the fallback is for. 
    * Otherwise, {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException} must be thrown.
    * 
    * @return the local method to fallback to
    */
    @Nonbinding
    String fallbackMethod() default "";

    /**
     * The list of exception types which should trigger Fallback
     * <p>
     * Note that if a method throws a {@link Throwable} which is not an {@link Error} or {@link Exception}, 
     * non-portable behavior results.
     *
     * @return the exception types which should trigger Fallback
     */
    @Nonbinding
    Class<? extends Throwable>[] applyOn() default {Throwable.class};
    
    /**
     * The list of exception types which should <i>not</i> trigger Fallback
     * <p>
     * This list takes priority over the types listed in {@link #applyOn}
     * <p>
     * Note that if a method throws a {@link Throwable} which is not an {@link Error} or {@link Exception}, 
     * non-portable behavior results.
     *
     * @return the exception types which should not trigger Fallback
     */
    @Nonbinding
    Class<? extends Throwable>[] skipOn() default {};


}
