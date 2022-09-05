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

package org.apache.myfaces.cdi.wrapper;

import java.lang.reflect.Type;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.faces.FacesWrapper;
import jakarta.faces.component.PartialStateHolder;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import org.apache.myfaces.cdi.util.CDIUtils;

/**
 *
 */
public class FacesConverterCDIWrapper implements PartialStateHolder, Converter, FacesWrapper<Converter>
{
    private static final Type CONVERTER_TYPE = new TypeLiteral<Converter<?>>()
    { 
        private static final long serialVersionUID = 1L; 
    }.getType(); 

    private transient Converter delegate;

    private Class<?> forClass;
    private String converterId;
    private boolean _transient;

    public FacesConverterCDIWrapper()
    {
    }

    public FacesConverterCDIWrapper(Class<? extends Converter> converterClass, Class<?> forClass, String converterId)
    {
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
            BeanManager bm = CDIUtils.getBeanManager(FacesContext.getCurrentInstance());

            if (converterId != null)
            {
                FacesConverterAnnotationLiteral literal =
                        new FacesConverterAnnotationLiteral(Object.class, converterId);
                delegate = (Converter) CDIUtils.get(bm, CONVERTER_TYPE, true, literal);

                if (delegate == null)
                {
                    delegate = (Converter) CDIUtils.get(bm, Converter.class, true, literal);
                }
            }
            else if (forClass != null)
            {
                FacesConverterAnnotationLiteral literal = new FacesConverterAnnotationLiteral(forClass, "");
                delegate = (Converter) CDIUtils.get(bm, CONVERTER_TYPE, true, literal);

                if (delegate == null)
                {
                    delegate = (Converter) CDIUtils.get(bm, Converter.class, true, literal);
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

    @Override
    public void clearInitialState()
    {
        _initialStateMarked = false;
    }

    @Override
    public boolean initialStateMarked()
    {
        return _initialStateMarked;
    }

    @Override
    public void markInitialState()
    {
        _initialStateMarked = true;
    }
}
