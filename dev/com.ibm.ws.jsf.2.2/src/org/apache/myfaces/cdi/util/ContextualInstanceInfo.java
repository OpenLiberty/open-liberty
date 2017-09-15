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

package org.apache.myfaces.cdi.util;

import javax.enterprise.context.spi.CreationalContext;
import java.io.Serializable;

/**
 * This data holder contains all necessary data you need to
 * store a Contextual Instance in a CDI Context.
 * 
 * NOTE: Taken from Apache DeltaSpike
 */
public class ContextualInstanceInfo<T> implements Serializable
{
    /**
     * The actual Contextual Instance in the context
     */
    private T contextualInstance;

    /**
     * We need to store the CreationalContext as we need it for
     * properly destroying the contextual instance via
     * {@link javax.enterprise.context.spi.Contextual#destroy(Object, javax.enterprise.context.spi.CreationalContext)}
     */
    private CreationalContext<T> creationalContext;

    /**
     * @return the CreationalContext of the bean
     */
    public CreationalContext<T> getCreationalContext()
    {
        return creationalContext;
    }

    /**
     * @param creationalContext the CreationalContext of the bean
     */
    public void setCreationalContext(CreationalContext<T> creationalContext)
    {
        this.creationalContext = creationalContext;
    }

    /**
     * @return the contextual instance itself
     */
    public T getContextualInstance()
    {
        return contextualInstance;
    }

    /**
     * @param contextualInstance the contextual instance itself
     */
    public void setContextualInstance(T contextualInstance)
    {
        this.contextualInstance = contextualInstance;
    }

}
