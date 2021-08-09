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

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspTag;

/**
 * Creates a validator and associates it with the nearest parent UIComponent.
 * <p>
 * During the validation phase (or the apply-request-values phase for immediate components), if the associated component
 * has any submitted value and the conversion of that value to the required type has succeeded then the specified
 * validator type is invoked to test the validity of the converted value.
 * </p>
 * <p>
 * Commonly associated with an h:inputText entity, but may be applied to any input component.
 * </p>
 * <p>
 * Some validators may allow the component to use attributes to define component-specific validation constraints; see
 * the f:attribute tag. See also the "validator" attribute of all input components, which allows a component to specify
 * an arbitrary validation &lt;i&gt;method&lt;/i&gt; (rather than a registered validation type, as this tag does).
 * </p>
 * <p>
 * Unless otherwise specified, all attributes accept static values or EL expressions.
 * </p>
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 819754 $ $Date: 2009-09-28 22:27:45 +0000 (Mon, 28 Sep 2009) $
 */
@JSFJspTag(name = "f:validator", bodyContent = "empty")
public class ValidatorImplTag extends ValidatorELTag
{
    private ValueExpression _validatorId;
    private ValueExpression _binding;
    private String _validatorIdString = null;

    /**
     * The registered ID of the desired Validator.
     */
    @JSFJspAttribute(className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    public void setValidatorId(ValueExpression validatorId)
    {
        _validatorId = validatorId;
    }

    /**
     * A ValueExpression that evaluates to an implementation of the javax.faces.validator.Validator interface.
     */
    @JSFJspAttribute(className="javax.el.ValueExpression",
            deferredValueType="javax.faces.validator.Validator")
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

        if (_validatorIdString != null)
        {
            return this.createClassicValidator();
        }
        if (_validatorId != null && _validatorId.isLiteralText())
        {
            return this.createClassicValidator();
        }

        return new DelegateValidator(_validatorId, _binding, _validatorIdString);
    }

    protected Validator createClassicValidator() throws javax.servlet.jsp.JspException
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
