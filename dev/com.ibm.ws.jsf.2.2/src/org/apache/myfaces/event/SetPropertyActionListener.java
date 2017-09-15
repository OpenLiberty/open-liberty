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
package org.apache.myfaces.event;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.component.StateHolder;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

/**
 * The MyFaces implementation of the <code>SetPropertyActionListener</code>.
 * 
 * @author Dennis Byrne
 * @since 1.2
 */
public class SetPropertyActionListener implements ActionListener, StateHolder
{

    private ValueExpression target;
    
    private ValueExpression value;
    
    private boolean _transient ;
    
    public SetPropertyActionListener(){}
    
    public SetPropertyActionListener(ValueExpression target, ValueExpression value)
    {
        this.target = target;
        this.value = value;
    }
    
    public void processAction(ActionEvent actionEvent) throws AbortProcessingException
    {
        
        if( target == null )
        {
            throw new AbortProcessingException("@target has not been set");
        }

        if( value == null )
        {
            throw new AbortProcessingException("@value has not been set");
        }
        
        FacesContext ctx = FacesContext.getCurrentInstance();
        
        if( ctx == null )
        {
            throw new AbortProcessingException("FacesContext ctx is null");
        }
        
        ELContext ectx = ctx.getELContext();
        
        if( ectx == null )
        {
            throw new AbortProcessingException("ELContext ectx is null");
        }
        
        // TODO use a Converter before calling setValue 
        
        target.setValue(ectx, value.getValue(ectx));
        
    }

    public Object saveState(FacesContext context)
    {
        Object[] state = new Object[2];
        state[0] = target;
        state[1] = value;
        return state;
    }

    public void restoreState(FacesContext context, Object state)
    {
        Object[] values = (Object[]) state;
        target = (ValueExpression) values[0];
        value = (ValueExpression) values[1];
    }

    public boolean isTransient()
    {
        return _transient;
    }

    public void setTransient(boolean trans)
    {
        this._transient = trans;
    }

    public ValueExpression getTarget()
    {
        return target;
    }

    public void setTarget(ValueExpression target)
    {
        this.target = target;
    }

    public ValueExpression getValue()
    {
        return value;
    }

    public void setValue(ValueExpression value)
    {
        this.value = value;
    }

}
