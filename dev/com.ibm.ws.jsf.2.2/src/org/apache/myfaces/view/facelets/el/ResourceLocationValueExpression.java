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

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.FacesWrapper;
import javax.faces.context.FacesContext;
import javax.faces.view.Location;

/**
 * 
 * @author Leonardo Uribe 
 */
public class ResourceLocationValueExpression extends LocationValueExpression implements FacesWrapper<ValueExpression>
{
    
    private static final long serialVersionUID = -5636849184764526288L;
    
    public ResourceLocationValueExpression()
    {
        super();
    }
    
    public ResourceLocationValueExpression(Location location, ValueExpression delegate)
    {
        super(location, delegate);
    }
    
    @Override
    public Class<?> getType(ELContext context)
    {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        ResourceELUtils.saveResourceLocationForResolver(facesContext, location);
        try
        {
            return delegate.getType(context);
        }
        finally
        {
            ResourceELUtils.removeResourceLocationForResolver(facesContext);
        }
    }

    @Override
    public Object getValue(ELContext context)
    {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        ResourceELUtils.saveResourceLocationForResolver(facesContext, location);
        try
        {
            return delegate.getValue(context);
        }
        finally
        {
            ResourceELUtils.removeResourceLocationForResolver(facesContext);
        }
    }

    @Override
    public boolean isReadOnly(ELContext context)
    {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        ResourceELUtils.saveResourceLocationForResolver(facesContext, location);
        try
        {
            return delegate.isReadOnly(context);
        }
        finally
        {
            ResourceELUtils.removeResourceLocationForResolver(facesContext);
        }
    }

    @Override
    public void setValue(ELContext context, Object value)
    {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        ResourceELUtils.saveResourceLocationForResolver(facesContext, location);
        try
        {
            delegate.setValue(context, value);
        }
        finally
        {
            ResourceELUtils.removeResourceLocationForResolver(facesContext);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        return delegate.equals(obj);
    }

    @Override
    public String getExpressionString()
    {
        return delegate.getExpressionString();
    }

    @Override
    public int hashCode()
    {
        return delegate.hashCode();
    }

    @Override
    public boolean isLiteralText()
    {
        return delegate.isLiteralText();
    }

    public ValueExpression getWrapped()
    {
        return delegate;
    }
}
