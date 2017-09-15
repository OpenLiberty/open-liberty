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
import javax.faces.validator.RegexValidator;
import javax.faces.validator.Validator;
import javax.faces.webapp.ValidatorELTag;
import javax.servlet.jsp.JspException;

/**
 * JSP Tag class for {@link javax.faces.validator.RegexValidator}.
 *
 * @author Jan-Kees van Andel
 * @since 2.0
 */
public class ValidateRegexTag extends ValidatorELTag
{
    private static final long serialVersionUID = 8363913774859484811L;

    private ValueExpression _pattern;

    private ValueExpression _binding;

    @Override
    protected Validator createValidator() throws JspException
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        ELContext elc = fc.getELContext();
        if (_binding != null)
        {
            Object validator;
            try
            {
                validator = _binding.getValue(elc);
            }
            catch (Exception e)
            {
                throw new JspException("Error while creating the Validator", e);
            }
            if (validator instanceof RegexValidator)
            {
                return (Validator)validator;
            }
        }
        if (null != _pattern)
        {
            Application appl = fc.getApplication();
            RegexValidator validator = (RegexValidator) appl.createValidator(RegexValidator.VALIDATOR_ID);
            String pattern = (String)_pattern.getValue(elc);
            validator.setPattern(pattern);

            if (_binding != null)
            {
                _binding.setValue(elc, validator);
            }

            return validator;
        }
        else
        {
            throw new AssertionError("pattern may not be null");
        }
    }

    public ValueExpression getBinding()
    {
        return _binding;
    }

    public void setBinding(ValueExpression binding)
    {
        _binding = binding;
    }

    public ValueExpression getPattern()
    {
        return _pattern;
    }

    public void setPattern(ValueExpression pattern)
    {
        _pattern = pattern;
    }

    @Override
    public void release()
    {
        _pattern = null;
        _binding = null;
    }
}
