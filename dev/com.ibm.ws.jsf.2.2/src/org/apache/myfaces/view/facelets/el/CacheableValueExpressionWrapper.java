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

/**
 *
 */
public class CacheableValueExpressionWrapper extends ValueExpression
    implements FacesWrapper<ValueExpression>, Externalizable, CacheableValueExpression
{
    private static final long serialVersionUID = -5636849184764526288L;
    
    ValueExpression delegate;

    public CacheableValueExpressionWrapper()
    {
    }

    public CacheableValueExpressionWrapper(ValueExpression delegate)
    {
        this.delegate = delegate;
    }
    
    @Override
    public Class<?> getExpectedType()
    {
        return delegate.getExpectedType();
    }

    @Override
    public Class<?> getType(ELContext context)
    {
        return delegate.getType(context);
    }

    @Override
    public Object getValue(ELContext context)
    {
        return delegate.getValue(context);
    }

    @Override
    public boolean isReadOnly(ELContext context)
    {
        return delegate.isReadOnly(context);
    }

    @Override
    public void setValue(ELContext context, Object value)
    {
        delegate.setValue(context, value);
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
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(this.delegate);
    }
}
