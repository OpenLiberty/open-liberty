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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ValueExpression;
import javax.faces.component.ActionSource;
import javax.faces.component.UIComponent;
import javax.faces.event.ActionListener;
import javax.faces.webapp.UIComponentClassicTagBase;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspTag;
import org.apache.myfaces.event.SetPropertyActionListener;

/**
 * @author Dennis Byrne
 * @since 1.2
 */
@JSFJspTag(name = "f:setPropertyActionListener", bodyContent = "empty")
public class SetPropertyActionListenerTag extends TagSupport
{

    //private static final Log log = LogFactory.getLog(SetPropertyActionListenerTag.class);
    private static final Logger log = Logger.getLogger(SetPropertyActionListenerTag.class.getName());

    private ValueExpression target;

    private ValueExpression value;

    @Override
    public int doStartTag() throws JspException
    {

        if (log.isLoggable(Level.FINE))
        {
            log.fine("JSF 1.2 Spec : Create a new instance of the ActionListener");
        }

        ActionListener actionListener = new SetPropertyActionListener(target, value);

        UIComponentClassicTagBase tag = UIComponentClassicTagBase.getParentUIComponentClassicTagBase(pageContext);

        if (tag == null)
        {
            throw new JspException("Could not find a " + "parent UIComponentClassicTagBase ... is this "
                    + "tag in a child of a UIComponentClassicTagBase?");
        }

        if (tag.getCreated())
        {

            UIComponent component = tag.getComponentInstance();

            if (component == null)
            {
                throw new JspException(" Could not locate a UIComponent " + "for a UIComponentClassicTagBase w/ a "
                        + "JSP id of " + tag.getJspId());
            }

            if (!(component instanceof ActionSource))
            {
                throw new JspException("Component w/ id of " + component.getId()
                        + " is associated w/ a tag w/ JSP id of " + tag.getJspId() + ". This component is of type "
                        + component.getClass() + ", which is not an " + ActionSource.class);
            }

            if (log.isLoggable(Level.FINE))
            {
                log.fine(" ... register it with the UIComponent " + "instance associated with our most immediately "
                        + "surrounding UIComponentTagBase");
            }

            ((ActionSource)component).addActionListener(actionListener);

        }

        return SKIP_BODY;
    }

    /**
     * ValueExpression for the destination of the value attribute.
     */
    @JSFJspAttribute(required = true,
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.Object")
    public ValueExpression getTarget()
    {
        return target;
    }

    public void setTarget(ValueExpression target)
    {
        this.target = target;
    }

    /**
     * ValueExpression for the value of the target attribute.
     * 
     * @return
     */
    @JSFJspAttribute(required = true,
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.Object")
    public ValueExpression getValue()
    {
        return value;
    }

    public void setValue(ValueExpression value)
    {
        this.value = value;
    }

    @Override
    public void release()
    {
        target = null;
        value = null;
    }

}
