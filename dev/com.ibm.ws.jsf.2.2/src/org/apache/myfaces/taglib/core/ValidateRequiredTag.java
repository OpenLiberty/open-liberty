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
package org.apache.myfaces.taglib.core;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.validator.RequiredValidator;
import javax.faces.validator.Validator;
import javax.faces.webapp.ValidatorELTag;
import javax.servlet.jsp.JspException;

/**
 * JSP Tag class for {@link javax.faces.validator.RequiredValidator}.
 *
 * @author Leonardo Uribe
 * @since 2.0
 */
public class ValidateRequiredTag extends ValidatorELTag
{
    /**
     * 
     */
    private static final long serialVersionUID = 6569308536432242026L;
        
    private ValueExpression _binding;
    
    public void setBinding(ValueExpression binding)
    {
        _binding = binding;
    }

    @Override
    public void release()
    {
        _binding = null;
    }
    
    @Override
    protected Validator createValidator() throws JspException
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ELContext elContext = facesContext.getELContext();
        if (null != _binding)
        {
            Object validator;
            try
            {
                validator = _binding.getValue(elContext);
            }
            catch (Exception e)
            {
                throw new JspException("Error while creating the Validator", e);
            }
            if (validator instanceof RequiredValidator)
            {
                return (Validator)validator;
            }
        }
        Application application = facesContext.getApplication();
        RequiredValidator validator = null;
        try
        {
            validator = (RequiredValidator) application.createValidator(RequiredValidator.VALIDATOR_ID);
        }
        catch (Exception e)
        {
            throw new JspException("Error while creating the Validator", e);
        }

        if (null != validator)
        {
            if (null != _binding)
            {
                _binding.setValue(elContext, validator);
            }            
        }
        return validator;
    }
}
