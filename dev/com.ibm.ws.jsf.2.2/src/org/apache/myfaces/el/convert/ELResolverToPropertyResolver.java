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
package org.apache.myfaces.el.convert;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.PropertyNotFoundException;
import javax.faces.el.PropertyResolver;

/**
 * 
 * @author Stan Silvert
 */
public final class ELResolverToPropertyResolver extends PropertyResolver
{

    private final ELResolver elResolver;

    /**
     * Creates a new instance of ELResolverToPropertyResolver
     */
    public ELResolverToPropertyResolver(final ELResolver elResolver)
    {
        this.elResolver = elResolver;
    }

    @Override
    public boolean isReadOnly(final Object base, final int index)
            throws EvaluationException, PropertyNotFoundException
    {

        try
        {
            return elResolver.isReadOnly(elContext(), base, Integer.valueOf(index));
        }
        catch (javax.el.PropertyNotFoundException e)
        {
            throw new javax.faces.el.PropertyNotFoundException(e);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e);
        }

    }

    @Override
    public boolean isReadOnly(final Object base, final Object property)
            throws EvaluationException, PropertyNotFoundException
    {

        try
        {
            return elResolver.isReadOnly(elContext(), base, property);
        }
        catch (javax.el.PropertyNotFoundException e)
        {
            throw new javax.faces.el.PropertyNotFoundException(e);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e);
        }

    }

    @Override
    public Object getValue(final Object base, final int index) throws EvaluationException, PropertyNotFoundException
    {

        try
        {
            return elResolver.getValue(elContext(), base, Integer.valueOf(index));
        }
        catch (javax.el.PropertyNotFoundException e)
        {
            throw new javax.faces.el.PropertyNotFoundException(e);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e);
        }

    }

    @Override
    public Object getValue(final Object base, final Object property)
            throws EvaluationException, PropertyNotFoundException
    {

        try
        {
            return elResolver.getValue(elContext(), base, property);
        }
        catch (javax.el.PropertyNotFoundException e)
        {
            throw new javax.faces.el.PropertyNotFoundException(e);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e);
        }
    }

    @Override
    public Class getType(final Object base, int index) throws EvaluationException, PropertyNotFoundException
    {

        try
        {
            return elResolver.getType(elContext(), base, Integer.valueOf(index));
        }
        catch (javax.el.PropertyNotFoundException e)
        {
            throw new javax.faces.el.PropertyNotFoundException(e);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e);
        }
    }

    @Override
    public Class getType(final Object base, final Object property)
            throws EvaluationException, PropertyNotFoundException
    {

        try
        {
            return elResolver.getType(elContext(), base, property);
        }
        catch (javax.el.PropertyNotFoundException e)
        {
            throw new javax.faces.el.PropertyNotFoundException(e);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e);
        }
    }

    @Override
    public void setValue(final Object base, final Object property, final Object value)
            throws EvaluationException, PropertyNotFoundException
    {

        try
        {
            elResolver.setValue(elContext(), base, property, value);
        }
        catch (javax.el.PropertyNotFoundException e)
        {
            throw new javax.faces.el.PropertyNotFoundException(e);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e);
        }
    }

    @Override
    public void setValue(final Object base, int index, final Object value)
            throws EvaluationException, PropertyNotFoundException
    {

        try
        {
            elResolver.setValue(elContext(), base, Integer.valueOf(index), value);
        }
        catch (javax.el.PropertyNotFoundException e)
        {
            throw new javax.faces.el.PropertyNotFoundException(e);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e);
        }

    }

    private ELContext elContext()
    {
        return FacesContext.getCurrentInstance().getELContext();
    }

}
