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
import javax.el.ValueExpression;
import javax.faces.FacesWrapper;
import javax.faces.context.FacesContext;
import javax.faces.view.Location;

/**
 * A ValueExpression that contains the original ValueExpression and
 * the Location of the facelet file from which the ValueExpression was
 * created. This is needed when the current composite component (cc) 
 * has to be resolved by the ValueExpression, because #{cc} refers to the
 * composite component which is implemented in the file the ValueExpression
 * comes from and not the one currently on top of the composite component stack.
 * 
 * This ValueExpression implementation passes through all methods to the delegate
 * ValueExpression, but saves the related composite component in a FacesContext attribute 
 * before the invocation of the method on the delegate and removes it afterwards. 
 * 
 * @author Jakob Korherr (latest modification by $Author: lu4242 $)
 * @version $Revision: 1351625 $ $Date: 2012-06-19 09:48:54 +0000 (Tue, 19 Jun 2012) $
 */
public class LocationValueExpression extends ValueExpression
    implements FacesWrapper<ValueExpression>, Externalizable
{
    
    private static final long serialVersionUID = -5636849184764526288L;
    
    // location and delegate need to be available in LocationValueExpressionUEL
    Location location;
    ValueExpression delegate;
    int ccLevel;
    
    public LocationValueExpression()
    {
        super();
    }
    
    public LocationValueExpression(Location location, ValueExpression delegate)
    {
        this.location = location;
        this.delegate = delegate;
        this.ccLevel = 0;
    }

    public LocationValueExpression(Location location, ValueExpression delegate, int ccLevel)
    {
        this.location = location;
        this.delegate = delegate;
        this.ccLevel = ccLevel;
    }
    
    public Location getLocation()
    {
        return location;
    }
    
    public int getCCLevel()
    {
        return ccLevel;
    }
    
    public LocationValueExpression apply(int newCCLevel)
    {
        if(this.ccLevel == newCCLevel)
        {
            return this;
        }
        else
        {
            return new LocationValueExpression(this.location, this.delegate, newCCLevel);
        }
    }
    
    @Override
    public Class<?> getExpectedType()
    {
        return delegate.getExpectedType();
    }

    @Override
    public Class<?> getType(ELContext context)
    {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        CompositeComponentELUtils.saveCompositeComponentForResolver(facesContext, location, ccLevel);
        try
        {
            return delegate.getType(context);
        }
        finally
        {
            CompositeComponentELUtils.removeCompositeComponentForResolver(facesContext);
        }
    }

    @Override
    public Object getValue(ELContext context)
    {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        CompositeComponentELUtils.saveCompositeComponentForResolver(facesContext, location, ccLevel);
        try
        {
            return delegate.getValue(context);
        }
        finally
        {
            CompositeComponentELUtils.removeCompositeComponentForResolver(facesContext);
        }
    }

    @Override
    public boolean isReadOnly(ELContext context)
    {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        CompositeComponentELUtils.saveCompositeComponentForResolver(facesContext, location, ccLevel);
        try
        {
            return delegate.isReadOnly(context);
        }
        finally
        {
            CompositeComponentELUtils.removeCompositeComponentForResolver(facesContext);
        }
    }

    @Override
    public void setValue(ELContext context, Object value)
    {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        CompositeComponentELUtils.saveCompositeComponentForResolver(facesContext, location, ccLevel);
        try
        {
            delegate.setValue(context, value);
        }
        finally
        {
            CompositeComponentELUtils.removeCompositeComponentForResolver(facesContext);
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
    
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.delegate = (ValueExpression) in.readObject();
        this.location = (Location) in.readObject();
        this.ccLevel = in.readInt();
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(this.delegate);
        out.writeObject(this.location);
        out.writeInt(this.ccLevel);
    }
}
