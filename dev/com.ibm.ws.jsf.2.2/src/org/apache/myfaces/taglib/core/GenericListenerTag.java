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

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.webapp.UIComponentClassicTagBase;
import javax.faces.webapp.UIComponentELTag;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.myfaces.shared.util.ClassUtils;

/**
 * @author Andreas Berger (latest modification by $Author: lu4242 $)
 * @version $Revision: 1151650 $ $Date: 2011-07-27 22:14:17 +0000 (Wed, 27 Jul 2011) $
 * @since 1.2
 */
public abstract class GenericListenerTag<_Holder, _Listener> extends TagSupport
{
    private ValueExpression _type = null;
    private ValueExpression _binding = null;
    private Class<_Holder> _holderClazz;

    protected GenericListenerTag(Class<_Holder> holderClazz)
    {
        super();
        _holderClazz = holderClazz;
    }

    public void setType(ValueExpression type)
    {
        _type = type;
    }

    public void setBinding(ValueExpression binding)
    {
        _binding = binding;
    }

    @Override
    public void release()
    {
        super.release();
        _type = null;
        _binding = null;
    }

    protected abstract void addListener(_Holder holder, _Listener listener);

    protected abstract _Listener createDelegateListener(ValueExpression type, ValueExpression binding);

    @Override
    @SuppressWarnings("unchecked")
    public int doStartTag() throws JspException
    {
        UIComponentClassicTagBase componentTag = UIComponentELTag.getParentUIComponentClassicTagBase(pageContext);
        if (componentTag == null)
        {
            throw new JspException("no parent UIComponentTag found");
        }

        if (_type == null && _binding == null)
        {
            throw new JspException("\"actionListener\" must have binding and/or type attribute.");
        }

        if (!componentTag.getCreated())
        {
            return Tag.SKIP_BODY;
        }

        _Holder holder = null;
        UIComponent component = componentTag.getComponentInstance();
        try
        {
            holder = (_Holder)component;
        }
        catch (ClassCastException e)
        {
            throw new JspException("Component " + ((UIComponent)holder).getId() + " is not instance of "
                    + _holderClazz.getName());
        }

        if (_type != null && _type.isLiteralText())
        {
            createListener(holder, component);
        }
        else
        {
            addListener(holder, createDelegateListener(_type, _binding));
        }

        return Tag.SKIP_BODY;
    }

    @SuppressWarnings("unchecked")
    protected void createListener(_Holder holder, UIComponent component) throws JspException
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        _Listener listener;
        // type and/or binding must be specified
        try
        {
            if (null != _binding)
            {
                try
                {
                    listener = (_Listener)_binding.getValue(facesContext.getELContext());
                    if (null != listener)
                    {
                        addListener(holder, listener);
                        // no need for further processing
                        return;
                    }
                }
                catch (ELException e)
                {
                    throw new JspException("Exception while evaluating the binding attribute of Component "
                            + component.getId(), e);
                }
            }
            if (null != _type)
            {
                String className;
                if (_type.isLiteralText())
                {
                    className = _type.getExpressionString();
                    // If type is literal text we should create
                    // a new instance
                    listener = (_Listener)ClassUtils.newInstance(className);
                }
                else
                {
                    className = (String)_type.getValue(facesContext.getELContext());
                    listener = null;
                }

                if (null != _binding)
                {
                    try
                    {
                        _binding.setValue(facesContext.getELContext(), listener);
                    }
                    catch (ELException e)
                    {
                        throw new JspException("Exception while evaluating the binding attribute of Component "
                                + component.getId(), e);
                    }
                }
                else
                {
                    // Type is a EL expression, and there is
                    // no binding property so we should create
                    // a new instance
                    listener = (_Listener)ClassUtils.newInstance(className);
                }
                addListener(holder, listener);
            }
        }
        catch (ClassCastException e)
        {
            throw new JspException(e);
        }
    }

}
