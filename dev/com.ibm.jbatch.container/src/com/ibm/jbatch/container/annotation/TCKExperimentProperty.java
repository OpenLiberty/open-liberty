/*
 * Copyright 2014 International Business Machines Corp.
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
package com.ibm.jbatch.container.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 
 * Used to mark boolean flags, typically set by a boolean System property,
 * to toggle the behavior of the runtime.   This was of value in exploring
 * TCK ambiguities or assumptions that the RI and/or TCK may have been making
 * that were questioned or challenged.
 *  
 * This annotation allows us for easy searching of the codebase.
 * @author skurz
 *
 */
@Retention(RetentionPolicy.SOURCE)
public @interface TCKExperimentProperty {
	  public abstract java.lang.String value() default "";
}
