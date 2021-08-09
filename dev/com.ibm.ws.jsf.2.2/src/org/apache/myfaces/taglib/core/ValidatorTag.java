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
import javax.faces.validator.Validator;
import javax.faces.webapp.ValidatorELTag;
import javax.servlet.jsp.JspException;

/**
 * Basic Validator implementation.
 * 
 * @author Andreas Berger (latest modification by $Author: slessard $)
 * @version $Revision: 701829 $ $Date: 2008-10-05 17:06:02 +0000 (Sun, 05 Oct 2008) $
 * @since 1.2
 */
public class ValidatorTag extends ValidatorELTag
{
    private ValueExpression _validatorId;
    private ValueExpression _binding;
    private String _validatorIdString = null;

    public void setValidatorId(ValueExpression validatorId)
    {
        _validatorId = validatorId;
    }

    public void setBinding(ValueExpression binding)
    {
        _binding = binding;
    }

    /**
     * Use this method to specify the validatorId programmatically.
     * 
     * @param validatorIdString
     */
    public void setValidatorIdString(String validatorIdString)
    {
        _validatorIdString = validatorIdString;
    }

    @Override
    public void release()
    {
        super.release();
        _validatorId = null;
        _binding = null;
        _validatorIdString = null;
    }

    @Override
    protected Validator createValidator() throws javax.servlet.jsp.JspException
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
            if (validator instanceof Validator)
            {
                return (Validator)validator;
            }
        }
        Application application = facesContext.getApplication();
        Validator validator = null;
        try
        {
            // first check if an ValidatorId was set by a method
            if (null != _validatorIdString)
            {
                validator = application.createValidator(_validatorIdString);
            }
            else if (null != _validatorId)
            {
                String validatorId = (String)_validatorId.getValue(elContext);
                validator = application.createValidator(validatorId);
            }
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
            return validator;
        }
        throw new JspException("validatorId and/or binding must be specified");
    }

}
