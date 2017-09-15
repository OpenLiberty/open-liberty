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

import java.lang.reflect.Method;

/**
 * The execution context for the method being executed.
 * 
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 */

public interface ExecutionContext {

     /**
     * Returns the method being executed
     * 
     * @return the method
     */
    public Method getMethod();

    /**
     * Returns the parameter values being passed to the method 
     * 
     * @return the parameter values, as an array
     * 
     * 
     */
    public Object[] getParameters();

}
