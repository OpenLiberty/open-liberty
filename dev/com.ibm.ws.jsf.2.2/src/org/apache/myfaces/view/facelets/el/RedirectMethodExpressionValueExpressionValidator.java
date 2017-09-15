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
package org.apache.myfaces.view.facelets.el;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FacesWrapper;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 *
 * @author Leonardo Uribe
 */
public class RedirectMethodExpressionValueExpressionValidator
       implements Validator, FacesWrapper<ValueExpression>, Externalizable
{
    private ValueExpression valueExpression;

    public RedirectMethodExpressionValueExpressionValidator()
    {
    }
    
    public RedirectMethodExpressionValueExpressionValidator(ValueExpression valueExpression)
    {
        this.valueExpression = valueExpression;
    }

    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException
    {
        getMethodExpression().invoke(FacesContext.getCurrentInstance().getELContext(),
                                     new Object[]{context,component,value});
    }

    private MethodExpression getMethodExpression()
    {
        return getMethodExpression(FacesContext.getCurrentInstance().getELContext());
    }
    
    private MethodExpression getMethodExpression(ELContext context)
    {
        Object meOrVe = valueExpression.getValue(context);
        if (meOrVe instanceof MethodExpression)
        {
            return (MethodExpression) meOrVe;
        }
        else if (meOrVe instanceof ValueExpression)
        {
            while (meOrVe != null && meOrVe instanceof ValueExpression)
            {
                meOrVe = ((ValueExpression)meOrVe).getValue(context);
            }
            return (MethodExpression) meOrVe;
        }
        else
        {
            return null;
        }
    }

    public ValueExpression getWrapped()
    {
        return valueExpression;
    }
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.valueExpression = (ValueExpression) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(this.valueExpression);
    }

}
