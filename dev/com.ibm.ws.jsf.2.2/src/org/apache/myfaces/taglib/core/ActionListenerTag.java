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

import javax.el.ValueExpression;
import javax.faces.component.ActionSource;
import javax.faces.event.ActionListener;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspTag;

/**
 * This tag creates an instance of the specified ActionListener, and associates it with the nearest parent UIComponent.
 * <p>
 * Unless otherwise specified, all attributes accept static values or EL expressions.
 * </p>
 * 
 * @author Manfred Geiler (latest modification by $Author: bommel $)
 * @version $Revision: 1187701 $ $Date: 2011-10-22 12:21:54 +0000 (Sat, 22 Oct 2011) $
 */
@JSFJspTag(name = "f:actionListener", bodyContent = "empty")
public class ActionListenerTag extends GenericListenerTag<ActionSource, ActionListener>
{
    private static final long serialVersionUID = -2021978765020549175L;

    public ActionListenerTag()
    {
        super(ActionSource.class);
    }

    @Override
    protected void addListener(ActionSource actionSource, ActionListener actionListener)
    {
        actionSource.addActionListener(actionListener);
    }

    @Override
    protected ActionListener createDelegateListener(ValueExpression type, ValueExpression binding)
    {
        return new DelegateActionListener(type, binding);
    }

    /**
     * The fully qualified class name of the ActionListener class.
     */
    @Override
    @JSFJspAttribute(className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    public void setType(ValueExpression type)
    {
        super.setType(type);
    }

    /**
     * Value binding expression that evaluates to an object that implements javax.faces.event.ActionListener.
     */
    @Override
    @JSFJspAttribute(className="javax.el.ValueExpression",
            deferredValueType="javax.faces.event.ActionListener")
    public void setBinding(ValueExpression binding)
    {
        super.setBinding(binding);
    }
}
