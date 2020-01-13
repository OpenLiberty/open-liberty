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

package org.apache.myfaces.cdi.converter;

import java.lang.reflect.Type;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.TypeLiteral;
import javax.faces.FacesWrapper;
import javax.faces.component.PartialStateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import org.apache.myfaces.cdi.util.CDIUtils;

/**
 *
 */
public class FacesConverterCDIWrapper implements PartialStateHolder, Converter, FacesWrapper<Converter>
{
    private transient Converter delegate;
    
    //private Class<? extends Converter> converterClass;
    private Class<?> forClass;
    private String converterId;
    private boolean _transient;
    private static final Type CONVERTER_TYPE = new TypeLiteral<Converter<?>>() {
        private static final long serialVersionUID = 1L;
    }.getType();

    public FacesConverterCDIWrapper()
    {
    }

    public FacesConverterCDIWrapper(Class<? extends Converter> converterClass, Class<?> forClass, String converterId)
    {
        //this.converterClass = converterClass;
        this.forClass = forClass;
        this.converterId = converterId;
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException
    {
        return getWrapped().getAsObject(context, component, value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException
    {
        return getWrapped().getAsString(context, component, value);
    }

    @Override
    public Converter getWrapped()
    {
        if (delegate == null)
        {

            BeanManager  beanManager = CDIUtils.getBeanManager(FacesContext.getCurrentInstance().getExternalContext());
            FacesConverterAnnotationLiteral qualifier;

            if (converterId != null)
            {
                qualifier = new FacesConverterAnnotationLiteral(Object.class, converterId, true);
                delegate = (Converter) CDIUtils.getInstance(beanManager, CONVERTER_TYPE, true, qualifier);

                if( delegate == null )
                {
                    delegate = (Converter) CDIUtils.getInstance(beanManager, Converter.class, true, qualifier);
                }
            }
            else if (forClass != null)
            {
                qualifier = new FacesConverterAnnotationLiteral(forClass, "", true);
                delegate = (Converter) CDIUtils.getInstance(beanManager, CONVERTER_TYPE, true, qualifier);

                if( delegate == null )
                {
                    delegate = (Converter) CDIUtils.getInstance(beanManager, Converter.class, true, qualifier);
                }
            }
        }
        return delegate;
    }
    
    @Override
    public Object saveState(FacesContext context)
    {
        if (!initialStateMarked())
        {
            Object values[] = new Object[2];
            //values[0] = converterClass;
            values[0] = forClass;
            values[1] = converterId;
            return values;
        }
        return null;
    }

    @Override
    public void restoreState(FacesContext context, Object state)
    {
        if (state != null)
        {
            Object values[] = (Object[])state;
            //converterClass = (Class)values[0];            
            forClass = (Class)values[0];
            converterId = (String)values[1];
        }
    }

    @Override
    public boolean isTransient()
    {
        return _transient;
    }

    @Override
    public void setTransient(boolean newTransientValue)
    {
        _transient = newTransientValue;
    }

    private boolean _initialStateMarked = false;

    public void clearInitialState()
    {
        _initialStateMarked = false;
    }

    public boolean initialStateMarked()
    {
        return _initialStateMarked;
    }

    public void markInitialState()
    {
        _initialStateMarked = true;
    }
}
