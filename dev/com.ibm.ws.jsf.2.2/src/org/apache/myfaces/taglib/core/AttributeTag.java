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

import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.JspException;
import javax.faces.webapp.UIComponentClassicTagBase;
import javax.faces.webapp.UIComponentELTag;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.el.ValueExpression;
import javax.el.ELContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspTag;

/**
 * This tag associates an attribute with the nearest parent UIComponent.
 * <p>
 * When the value is not an EL expression, this tag has the same effect as calling component.getAttributes.put(name,
 * value). When the attribute name specified matches a standard property of the component, that property is set. However
 * it is also valid to assign attributes to components using any arbitrary name; the component itself won't make any use
 * of these but other objects such as custom renderers, validators or action listeners can later retrieve the attribute
 * from the component by name.
 * </p>
 * <p>
 * When the value is an EL expression, this tag has the same effect as calling component.setValueBinding. A call to
 * method component.getAttributes().get(name) will then cause that expression to be evaluated and the result of the
 * expression is returned, not the original EL expression string.
 * </p>
 * <p>
 * See the javadoc for UIComponent.getAttributes for more details.
 * </p>
 * <p>
 * Unless otherwise specified, all attributes accept static values or EL expressions.
 * </p>
 * 
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @author Bruno Aranda (JSR-252)
 * @version $Revision: 819754 $ $Date: 2009-09-28 22:27:45 +0000 (Mon, 28 Sep 2009) $
 */
@JSFJspTag(name = "f:attribute", bodyContent = "empty")
public class AttributeTag extends TagSupport
{
    private static final long serialVersionUID = 31476300171678632L;
    private ValueExpression _nameExpression;
    private ValueExpression _valueExpression;

    /**
     * The name of the attribute.
     * 
     * @param nameExpression
     */
    @JSFJspAttribute(className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    public void setName(ValueExpression nameExpression)
    {
        _nameExpression = nameExpression;
    }

    /**
     * The attribute's value.
     * 
     * @param valueExpression
     */
    @JSFJspAttribute(className="javax.el.ValueExpression",
            deferredValueType="java.lang.Object")
    public void setValue(ValueExpression valueExpression)
    {
        _valueExpression = valueExpression;
    }

    @Override
    public int doStartTag() throws JspException
    {
        UIComponentClassicTagBase componentTag = UIComponentELTag.getParentUIComponentClassicTagBase(pageContext);
        if (componentTag == null)
        {
            throw new JspException("no parent UIComponentTag found");
        }
        UIComponent component = componentTag.getComponentInstance();
        if (component == null)
        {
            throw new JspException("parent UIComponentTag has no UIComponent");
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ELContext elContext = facesContext.getELContext();

        String name = null;
        Object value = null;
        boolean isLiteral = false;

        if (_nameExpression != null)
        {
            name = (String)_nameExpression.getValue(elContext);
        }

        if (_valueExpression != null)
        {
            isLiteral = _valueExpression.isLiteralText();
            value = _valueExpression.getValue(elContext);
        }

        if (name != null)
        {
            if (component.getAttributes().get(name) == null)
            {
                if (isLiteral)
                {
                    component.getAttributes().put(name, value);
                }
                else
                {
                    component.setValueExpression(name, _valueExpression);
                }
            }
        }

        return SKIP_BODY;
    }
    
    @Override
    public void release()
    {
        super.release();
        _nameExpression = null;
        _valueExpression = null;
    }
}
