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
package org.apache.myfaces.view.facelets.el;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;
import javax.faces.FacesWrapper;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;
import org.apache.myfaces.view.facelets.tag.jsf.FaceletState;

/**
 *
 */
public class FaceletStateValueExpression extends ValueExpression 
    implements Externalizable, FacesWrapper<ValueExpression>
{
    private String uniqueId;
    private String key;

    public FaceletStateValueExpression()
    {
    }
    
    public FaceletStateValueExpression(String uniqueId, String key)
    {
        this.uniqueId = uniqueId;
        this.key = key;
    }
    
    @Override
    public ValueExpression getWrapped()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        UIViewRoot root = facesContext.getViewRoot();
        FaceletState map = (FaceletState) root.getAttributes().get(
            ComponentSupport.FACELET_STATE_INSTANCE);
        return map.getBinding(uniqueId, key);
    }
    
    public ValueExpression getWrapped(ELContext context)
    {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        if (facesContext == null)
        {
            facesContext = FacesContext.getCurrentInstance();
        }
        UIViewRoot root = facesContext.getViewRoot();
        FaceletState map = (FaceletState) root.getAttributes().get(
            ComponentSupport.FACELET_STATE_INSTANCE);
        return map.getBinding(uniqueId, key);
    }


    @Override
    public Class<?> getExpectedType()
    {
        return getWrapped().getExpectedType();
    }

    @Override
    public Class<?> getType(ELContext context) throws NullPointerException, PropertyNotFoundException, ELException
    {
        return getWrapped(context).getType(context);
    }

    @Override
    public boolean isReadOnly(ELContext context) throws NullPointerException, PropertyNotFoundException, ELException
    {
        return getWrapped(context).isReadOnly(context);
    }

    @Override
    public void setValue(ELContext context, Object value) 
        throws NullPointerException, PropertyNotFoundException, PropertyNotWritableException, ELException
    {
        getWrapped(context).setValue(context, value);
    }

    @Override
    public Object getValue(ELContext context) throws NullPointerException, PropertyNotFoundException, ELException
    {
        return getWrapped(context).getValue(context);
    }

    @Override
    public String getExpressionString()
    {
        return getWrapped().getExpressionString();
    }

    @Override
    public boolean isLiteralText()
    {
        return getWrapped().isLiteralText();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeUTF(uniqueId);
        out.writeUTF(key);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        uniqueId = (String) in.readUTF();
        key = (String) in.readUTF();
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 37 * hash + (this.uniqueId != null ? this.uniqueId.hashCode() : 0);
        hash = 37 * hash + (this.key != null ? this.key.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final FaceletStateValueExpression other = (FaceletStateValueExpression) obj;
        if ((this.uniqueId == null) ? (other.uniqueId != null) : !this.uniqueId.equals(other.uniqueId))
        {
            return false;
        }
        if ((this.key == null) ? (other.key != null) : !this.key.equals(other.key))
        {
            return false;
        }
        return true;
    }

}
