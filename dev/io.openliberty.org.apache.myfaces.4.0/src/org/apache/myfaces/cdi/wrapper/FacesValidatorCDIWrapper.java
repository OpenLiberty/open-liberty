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
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;
import org.apache.myfaces.cdi.util.CDIUtils;

public class FacesValidatorCDIWrapper implements PartialStateHolder, Validator, FacesWrapper<Validator>
{
    private static final Type VALIDATOR_TYPE = new TypeLiteral<Validator<?>>() 
    { 
        private static final long serialVersionUID = 1L; 
    }.getType();

    private transient Validator delegate;
    
    private String validatorId;
    private boolean _transient;
    private boolean _initialStateMarked = false;

    public FacesValidatorCDIWrapper()
    {
    }

    public FacesValidatorCDIWrapper(Class<? extends Validator> validatorClass, String validatorId)
    {
        this.validatorId = validatorId;
    }

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException
    {
        getWrapped().validate(context, component, value);
    }

    @Override
    public Validator getWrapped()
    {
        if (delegate == null)
        {
            BeanManager bm = CDIUtils.getBeanManager(FacesContext.getCurrentInstance());
            FacesValidatorAnnotationLiteral literal = new FacesValidatorAnnotationLiteral(validatorId);
            delegate = (Validator) CDIUtils.get(bm, VALIDATOR_TYPE, true, literal);
            
            if (delegate == null)
            {
                delegate = (Validator) CDIUtils.get(bm, Validator.class, true, literal);
            }
        }
        return delegate;
    }
    
    @Override
    public Object saveState(FacesContext context)
    {
        if (!initialStateMarked())
        {
            Object values[] = new Object[1];
            values[0] = validatorId;
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
            validatorId = (String)values[0];
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
