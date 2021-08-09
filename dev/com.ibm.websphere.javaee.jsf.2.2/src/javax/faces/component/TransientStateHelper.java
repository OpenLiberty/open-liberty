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
 * <p>Define a <code>Map</code>-like contract
 * that makes it easier for components to implement {@link
 * TransientStateHolder}.  Each {@link UIComponent} in the view will
 * return an implementation of this interface from its {@link
 * UIComponent#getTransientStateHelper} method.</p>
 * 
 * <p>The values retrieved or saved through 
 * {@link #getTransient(java.io.Serializable)} } or 
 * {@link #putTransient(java.io.Serializable, Object value)} } 
 * will not be preserved between requests.</p>
 * 
 * @since 2.1
 */
public interface TransientStateHelper extends TransientStateHolder
{
    /**
     * <p>Return the value currently
     * associated with the specified <code>key</code> if any.</p>
     * 
     * @param key the key for which the value should be returned.
     * @since 2.1
     */
    public Object getTransient(Object key);
    
    /**
     * <p>Performs the same logic as {@link
     * #getTransient(java.io.Serializable)} } but if no value is found, this
     * will return the specified <code>defaultValue</code></p>

     * @param key the key for which the value should be returned.
     * @param defaultValue the value to return if no value is found in
     * the call to <code>get()</code>.
     * @since 2.1
     */
    public Object getTransient(Object key, Object defaultValue);
    
    /**
     * <p>Return the previously stored value
     * and store the specified key/value pair.  This is intended to
     * store data that would otherwise reside in an instance variable on
     * the component.</p>
     * 
     * @param key the key for the value
     * @param value the value
     * @since 2.1
     */
    public Object putTransient(Object key, Object value);
    
}
