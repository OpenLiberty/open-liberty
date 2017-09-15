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
import javax.faces.component.EditableValueHolder;
import javax.faces.event.ValueChangeListener;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspTag;

/**
 * Adds the specified ValueChangeListener to the nearest parent UIComponent (which is expected to be a UIInput
 * component).
 * <p>
 * Whenever the form containing the parent UIComponent is submitted, an instance of the specified type is created. If
 * the submitted value from the component is different from the component's current value then a ValueChangeEvent is
 * queued. When the ValueChangeEvent is processed (at end of the validate phase for non-immediate components, or at end
 * of the apply-request-values phase for immediate components) the object's processValueChange method is invoked.
 * </p>
 * <p>
 * Unless otherwise specified, all attributes accept static values or EL expressions.
 * </p>
 * 
 * @author Manfred Geiler (latest modification by $Author: bommel $)
 * @version $Revision: 1187701 $ $Date: 2011-10-22 12:21:54 +0000 (Sat, 22 Oct 2011) $
 */
@JSFJspTag(name = "f:valueChangeListener", bodyContent = "empty")
public class ValueChangeListenerTag extends GenericListenerTag<EditableValueHolder, ValueChangeListener>
{
    private static final long serialVersionUID = 2155190261951046892L;

    public ValueChangeListenerTag()
    {
        super(EditableValueHolder.class);
    }

    @Override
    protected void addListener(EditableValueHolder editableValueHolder, ValueChangeListener valueChangeListener)
    {
        editableValueHolder.addValueChangeListener(valueChangeListener);
    }

    @Override
    protected ValueChangeListener createDelegateListener(ValueExpression type, ValueExpression binding)
    {
        return new DelegateValueChangeListener(type, binding);
    }

    /**
     * The name of a Java class that implements ValueChangeListener.
     */
    @Override
    @JSFJspAttribute(className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    public void setType(ValueExpression type)
    {
        super.setType(type);
    }

    /**
     * Value binding expression that evaluates to an implementation of the javax.faces.event.ValueChangeListener
     * interface.
     */
    @Override
    @JSFJspAttribute(className="javax.el.ValueExpression",
            deferredValueType="javax.faces.event.ValueChangeListener")
    public void setBinding(ValueExpression binding)
    {
        super.setBinding(binding);
    }
}
