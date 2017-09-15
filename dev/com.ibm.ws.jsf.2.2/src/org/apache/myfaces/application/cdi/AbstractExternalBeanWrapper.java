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
package org.apache.myfaces.application.cdi;

import javax.faces.FacesWrapper;
import javax.faces.component.PartialStateHolder;
import javax.faces.component.StateHolder;
import javax.faces.context.FacesContext;

abstract class AbstractExternalBeanWrapper<T> implements PartialStateHolder, FacesWrapper<T>
{
    private T wrapped;

    protected AbstractExternalBeanWrapper(T wrapped)
    {
        this.wrapped = wrapped;
    }

    @Override
    public void markInitialState()
    {
        if (this.wrapped instanceof PartialStateHolder)
        {
            ((PartialStateHolder) this.wrapped).markInitialState();
        }
    }

    @Override
    public void clearInitialState()
    {
        if (this.wrapped instanceof PartialStateHolder)
        {
            ((PartialStateHolder) this.wrapped).clearInitialState();
        }
    }

    @Override
    public boolean initialStateMarked()
    {
        return this.wrapped instanceof PartialStateHolder && ((PartialStateHolder) this.wrapped).initialStateMarked();
    }

    @Override
    public Object saveState(FacesContext context)
    {
        if (this.wrapped instanceof StateHolder)
        {
            return ((StateHolder)wrapped).saveState(context);
        }

        return null;
    }

    @Override
    public void restoreState(FacesContext context, Object state)
    {
        if (this.wrapped instanceof StateHolder)
        {
            ((StateHolder)this.wrapped).restoreState(context, state);
        }
    }

    @Override
    public boolean isTransient()
    {
        return this.wrapped instanceof StateHolder && ((StateHolder) this.wrapped).isTransient();
    }

    @Override
    public void setTransient(boolean newTransientValue)
    {
        if (this.wrapped instanceof StateHolder)
        {
            ((StateHolder) this.wrapped).setTransient(newTransientValue);
        }
    }

    @Override
    public T getWrapped()
    {
        return this.wrapped;
    }
}
