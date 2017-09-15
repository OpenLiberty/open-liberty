/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.faces.component.visit;

/**
 *
 * <p>An enum that specifies hints that impact
 * the behavior of a component tree visit.</p>
 *
 * @see VisitContext#getHints VisitContext.getHints()
 *
 * @since 2.0
 */
public enum VisitHint
{
    /** 
     * Hint that indicates that only the rendered subtree should be visited.
     */
    SKIP_UNRENDERED,

    /** 
     * Hint that indicates that only non-transient subtrees should be visited.
     */
    SKIP_TRANSIENT,
  
    /**
     * Hint that indicates that the visit is being performed as part of
     * lifecycle phase execution and as such phase-specific actions
     * (initialization) may be taken.
     */
    EXECUTE_LIFECYCLE,
    
    /**
     * Hint that indicates that the visit should traverse only full component
     * instances and not "virtual" ones like UIData rows and so on.
     * 
     * @since 2.1
     */
    SKIP_ITERATION,
}