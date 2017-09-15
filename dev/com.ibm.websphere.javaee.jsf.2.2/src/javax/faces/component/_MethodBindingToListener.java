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

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.FacesEvent;

/**
 * Common base class for converting a MethodBinding to a FacesListener
 */
abstract class _MethodBindingToListener implements StateHolder
{

    protected MethodBinding methodBinding;

    public _MethodBindingToListener()
    {
    }

    /**
     * Creates a new instance of MethodBindingToListener
     */
    public _MethodBindingToListener(MethodBinding methodBinding)
    {
        if (methodBinding == null)
        {
            throw new NullPointerException("methodBinding can not be null");
        }
        if (!(methodBinding instanceof StateHolder))
        {
            throw new IllegalArgumentException("methodBinding must implement the StateHolder interface");
        }

        this.methodBinding = methodBinding;
    }

    private FacesContext getFacesContext()
    {
        return FacesContext.getCurrentInstance();
    }

    protected void invokeMethodBinding(FacesEvent event) throws AbortProcessingException
    {
        try
        {
            methodBinding.invoke(getFacesContext(), new Object[]{event});
        }
        catch (EvaluationException e)
        {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof AbortProcessingException)
            {
                throw (AbortProcessingException) cause;
            }

            throw e;
        }
    }

    public MethodBinding getMethodBinding()
    {
        return methodBinding;
    }

    public void restoreState(FacesContext context, Object state)
    {
        Object[] stateArray = (Object[]) state;
        try
        {
            methodBinding = (MethodBinding) _ClassUtils.getContextClassLoader()
                    .loadClass((String) stateArray[0])
                    .newInstance();
        }
        catch (Exception e)
        {
            throw new FacesException(e);
        }

        ((StateHolder) methodBinding).restoreState(context, stateArray[1]);
    }

    public Object saveState(FacesContext context)
    {
        Object[] stateArray = new Object[2];
        stateArray[0] = methodBinding.getClass().getName();
        stateArray[1] = ((StateHolder) methodBinding).saveState(context);
        return stateArray;
    }

    public void setTransient(boolean newTransientValue)
    {
        ((StateHolder) methodBinding).setTransient(newTransientValue);
    }

    public boolean isTransient()
    {
        return ((StateHolder) methodBinding).isTransient();
    }

}
