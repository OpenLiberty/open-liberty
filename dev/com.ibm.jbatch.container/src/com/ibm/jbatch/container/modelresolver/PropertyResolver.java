/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ibm.jbatch.container.modelresolver;

import java.util.Properties;

public interface PropertyResolver<B> {

    /**
     * Convenience method that is the same as calling substituteProperties(batchElement,
     * null, null)
     * 
     * @param b
     * @return
     */
    public B substituteProperties(final B b);

    /**
     * Convenience method that is the same as calling substituteProperties(batchElement,
     * submittedProps, null)
     * 
     * @param job
     * @param submittedProps
     */
    public B substituteProperties(final B b, final Properties submittedProps);

    /**
     * Performs property substitution on a given batch element b and all nested
     * sub elements. The given batch element is directly modified by this
     * method.
     * 
     * @param b
     * @param submittedProps Properties submitted as job parameters 
     * @param parentProps Properties inherited from parent elements
     * @return
     */
    public B substituteProperties(final B b, final Properties submittedProps, final Properties parentProps);


}
