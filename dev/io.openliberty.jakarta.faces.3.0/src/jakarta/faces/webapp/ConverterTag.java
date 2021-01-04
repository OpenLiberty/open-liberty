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
import jakarta.faces.context.FacesContext;
import jakarta.faces.el.ValueBinding;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.ValueHolder;
import jakarta.faces.convert.Converter;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.Tag;
import jakarta.servlet.jsp.tagext.TagSupport;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 * 
 * @deprecated replaced by {@link ConverterELTag}
 */
@Deprecated
public class ConverterTag extends TagSupport
{
    private static final long serialVersionUID = -6168345066829108081L;
    private String _converterId;
    private String _binding;

    public ConverterTag()
    {
        super();
    }

    public void setConverterId(String converterId)
    {
        _converterId = converterId;
    }

    @Override
    public int doStartTag() throws JspException
    {

        UIComponentClassicTagBase componentTag =
                UIComponentClassicTagBase.getParentUIComponentClassicTagBase(pageContext);

        if (componentTag == null)
        {
            throw new JspException("no parent UIComponentTag found");
        }
        if (!componentTag.getCreated())
        {
            return Tag.SKIP_BODY;
        }

        Converter converter = createConverter();

        UIComponent component = componentTag.getComponentInstance();
        if (component == null)
        {
            throw new JspException("parent UIComponentTag has no UIComponent");
        }
        if (!(component instanceof ValueHolder))
        {
            throw new JspException("UIComponent is no ValueHolder");
        }
        ((ValueHolder)component).setConverter(converter);

        return Tag.SKIP_BODY;
    }

    @Override
    public void release()
    {
        super.release();
        _converterId = null;
        _binding = null;
    }

    /**
     * @throws JspException  
     */
    protected Converter createConverter() throws JspException
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Application application = facesContext.getApplication();

        if (_binding != null)
        {
            ValueBinding vb = application.createValueBinding(_binding);
            if (vb != null)
            {
                Converter converter = (Converter)vb.getValue(facesContext);
                if (converter != null)
                {
                    return converter;
                }
            }
        }

        if (UIComponentTag.isValueReference(_converterId))
        {
            ValueBinding vb = facesContext.getApplication().createValueBinding(_converterId);
            return application.createConverter((String)vb.getValue(facesContext));
        }

        return application.createConverter(_converterId);

    }

    /**
     * @throws JspException  
     */
    public void setBinding(String binding) throws JspException
    {
        _binding = binding;
    }
}
