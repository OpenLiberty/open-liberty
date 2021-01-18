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
package jakarta.faces.webapp;

import jakarta.faces.application.Application;
import jakarta.faces.el.ValueBinding;
import jakarta.faces.validator.Validator;
import jakarta.faces.component.EditableValueHolder;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.Tag;
import jakarta.servlet.jsp.tagext.TagSupport;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 * 
 * @deprecated replaced by {@link ValidatorELTag}
 */
@Deprecated
public class ValidatorTag extends TagSupport
{
    private static final long serialVersionUID = 8794036166323016663L;
    private String _validatorId;
    private String _binding;

    public void setValidatorId(String validatorId)
    {
        _validatorId = validatorId;
    }

    @Override
    public int doStartTag() throws JspException
    {
        UIComponentTag componentTag = UIComponentTag.getParentUIComponentTag(pageContext);
        if (componentTag == null)
        {
            throw new JspException("no parent UIComponentTag found");
        }
        if (!componentTag.getCreated())
        {
            return Tag.SKIP_BODY;
        }

        Validator validator = createValidator();

        UIComponent component = componentTag.getComponentInstance();
        if (component == null)
        {
            throw new JspException("parent UIComponentTag has no UIComponent");
        }
        if (!(component instanceof EditableValueHolder))
        {
            throw new JspException("UIComponent is no ValueHolder");
        }
        ((EditableValueHolder)component).addValidator(validator);

        return Tag.SKIP_BODY;
    }

    @Override
    public void release()
    {
        super.release();
        _validatorId = null;
        _binding = null;
    }

    /**
     * @throws JspException  
     */
    protected Validator createValidator() throws JspException
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Application application = facesContext.getApplication();

        if (_binding != null)
        {
            ValueBinding vb = application.createValueBinding(_binding);
            if (vb != null)
            {
                Validator validator = (Validator)vb.getValue(facesContext);
                if (validator != null)
                {
                    return validator;
                }
            }
        }

        if (UIComponentTag.isValueReference(_validatorId))
        {
            ValueBinding vb = facesContext.getApplication().createValueBinding(_validatorId);
            return application.createValidator((String)vb.getValue(facesContext));
        }

        return application.createValidator(_validatorId);

    }

    /**
     * 
     * @param binding
     * @throws jakarta.servlet.jsp.JspException
     * 
     * @deprecated
     */
    public void setBinding(java.lang.String binding) throws jakarta.servlet.jsp.JspException
    {
        if (binding != null && !UIComponentTag.isValueReference(binding))
        {
            throw new IllegalArgumentException("not a valid binding: " + binding);
        }
        _binding = binding;
    }
}
