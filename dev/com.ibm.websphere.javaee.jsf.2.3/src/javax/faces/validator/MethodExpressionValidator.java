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

package javax.faces.validator;

import javax.el.ELException;
import javax.el.MethodExpression;
import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * see Javadoc of <a href="http://java.sun.com/j2ee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public class MethodExpressionValidator implements Validator, StateHolder
{

    private MethodExpression methodExpression;

    private boolean isTransient = false;

    /** Creates a new instance of MethodExpressionValidator */
    public MethodExpressionValidator()
    {
    }

    public MethodExpressionValidator(MethodExpression methodExpression)
    {
        if (methodExpression == null)
        {
            throw new NullPointerException("methodExpression can not be null.");
        }

        this.methodExpression = methodExpression;
    }

    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException
    {
        Object[] params = new Object[3];
        params[0] = context;
        params[1] = component;
        params[2] = value;

        try
        {
            methodExpression.invoke(context.getELContext(), params);
        }
        catch (ELException e)
        {
            Throwable cause = e.getCause();
            ValidatorException vex = null;
            if (cause != null)
            {
                do
                {
                    if (cause != null && cause instanceof ValidatorException)
                    {
                        vex = (ValidatorException) cause;
                        break;
                    }
                    cause = cause.getCause();
                }
                while (cause != null);
            }
            if (vex != null)
            {
                throw vex;
            }
            else
            {
                throw e;
            }
            //Throwable cause = e.getCause();
            //if (cause instanceof ValidatorException)
            //{
            //    throw (ValidatorException)cause;
            //}
            //else
            //{
            //    throw e;
            //}
        }
    }

    public void restoreState(FacesContext context, Object state)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        methodExpression = (MethodExpression)state;
    }

    public Object saveState(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        return methodExpression;
    }

    public void setTransient(boolean newTransientValue)
    {
        isTransient = newTransientValue;
    }

    public boolean isTransient()
    {
        return isTransient;
    }

}
