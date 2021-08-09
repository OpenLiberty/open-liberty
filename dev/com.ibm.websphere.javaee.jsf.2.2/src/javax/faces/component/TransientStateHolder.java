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
package javax.faces.component;

/**
 * <p>This interface is implemented by classes 
 * that need to save state that are expected to be available only on the 
 * current request.</p>
 * 
 * <p>An implementor <strong>must</strong> implement both {@link
 * #saveTransientState} and {@link #restoreTransientState} methods in this class, since
 * these two methods have a tightly coupled contract between themselves.
 * In other words, if there is an inheritance hierarchy, it is not
 * permissible to have the {@link #saveTransientState} and {@link #restoreTransientState}
 * methods reside at different levels of the hierarchy.</p>
 * 
 * @since 2.1
 */
public interface TransientStateHolder
{
    /**
     * <p>Return the object containing related "transient states".
     * that could be used later to restore the "transient state".<p>
     * 
     * @param context
     * @return object containing transient values
     * @since 2.1
     */
    public java.lang.Object saveTransientState(javax.faces.context.FacesContext context);

    /**
     * <p>Restore the "transient state" using the object passed as
     * state.</p>
     * 
     * <p>If the <code>state</code>
     * argument is <code>null</code> clear any previous transient
     * state if any and return.</p>
     * 
     * @param context
     * @param state the object containing transient values
     * @since 2.1
     */
    public void restoreTransientState(javax.faces.context.FacesContext context,
                             java.lang.Object state);

}
