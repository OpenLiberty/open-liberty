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
package org.apache.myfaces.shared.el;

import javax.faces.component.StateHolder;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodBinding;
import javax.faces.el.MethodNotFoundException;

/**
 * Convenient method binding that does nothing other than returning a fixed outcome String when invoked.
 */
public class SimpleActionMethodBinding
        extends MethodBinding
        implements StateHolder
{
    private String _outcome;

    public SimpleActionMethodBinding(String outcome)
    {
        _outcome = outcome;
    }

    public Object invoke(FacesContext facescontext, Object aobj[]) throws EvaluationException, MethodNotFoundException
    {
        return _outcome;
    }

    public Class getType(FacesContext facescontext) throws MethodNotFoundException
    {
        return String.class;
    }


    //~ StateHolder support ----------------------------------------------------------------------------

    private boolean _transient = false;

    /**
     * Empty constructor, so that new instances can be created when restoring state.
     */
    public SimpleActionMethodBinding()
    {
        _outcome = null;
    }

    public Object saveState(FacesContext facescontext)
    {
        return _outcome;
    }

    public void restoreState(FacesContext facescontext, Object obj)
    {
        _outcome = (String)obj;
    }

    public boolean isTransient()
    {
        return _transient;
    }

    public void setTransient(boolean flag)
    {
        _transient = flag;
    }

    public String toString()
    {
        return _outcome;
    }
}
