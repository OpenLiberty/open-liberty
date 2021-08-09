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
import javax.el.ELException;
import javax.el.MethodExpression;
import javax.el.MethodInfo;
import javax.el.MethodNotFoundException;
import javax.el.PropertyNotFoundException;

/**
 *
 * @author Leonardo Uribe
 */
public class MethodExpressionMethodExpression extends MethodExpression implements Externalizable
{
    
    private static final Object[] EMPTY_PARAMS = new Object[0];
    
    private MethodExpression methodExpressionOneArg;
    private MethodExpression methodExpressionZeroArg;

    public MethodExpressionMethodExpression()
    {
    }
    
    public MethodExpressionMethodExpression(MethodExpression methodExpressionOneArg,
                                            MethodExpression methodExpressionZeroArg)
    {
        this.methodExpressionOneArg = methodExpressionOneArg;
        this.methodExpressionZeroArg = methodExpressionZeroArg;
    }

    @Override
    public MethodInfo getMethodInfo(ELContext context)
            throws NullPointerException, PropertyNotFoundException, MethodNotFoundException, ELException
    {
        try
        {
            // call to the one argument MethodExpression
            return methodExpressionOneArg.getMethodInfo(context);
        }
        catch (MethodNotFoundException mnfe)
        {
            // call to the zero argument MethodExpression
            return methodExpressionZeroArg.getMethodInfo(context);
        }
    }

    @Override
    public Object invoke(ELContext context, Object[] params)
            throws NullPointerException, PropertyNotFoundException, MethodNotFoundException, ELException
    {
        try
        {
            // call to the one argument MethodExpression
            return methodExpressionOneArg.invoke(context, params);
        }
        catch (MethodNotFoundException mnfe)
        {
            // call to the zero argument MethodExpression
            return methodExpressionZeroArg.invoke(context, EMPTY_PARAMS);
        }
    }

    @Override
    public String getExpressionString()
    {
        try
        {
            // call to the one argument MethodExpression
            return methodExpressionOneArg.getExpressionString();
        }
        catch (MethodNotFoundException mnfe)
        {
            // call to the zero argument MethodExpression
            return methodExpressionZeroArg.getExpressionString();
        }
    }

    @Override
    public boolean isLiteralText()
    {
        return false;
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(methodExpressionOneArg);
        out.writeObject(methodExpressionZeroArg);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        methodExpressionOneArg = (MethodExpression) in.readObject();
        methodExpressionZeroArg = (MethodExpression) in.readObject();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof MethodExpressionMethodExpression)
        {
            MethodExpressionMethodExpression me = (MethodExpressionMethodExpression) obj;
            return methodExpressionOneArg.equals(me.methodExpressionOneArg) &&
                   methodExpressionZeroArg.equals(me.methodExpressionZeroArg);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 31;
        hash = 19 * hash + (this.methodExpressionOneArg != null ? this.methodExpressionOneArg.hashCode() : 0);
        hash = 19 * hash + (this.methodExpressionZeroArg != null ? this.methodExpressionZeroArg.hashCode() : 0);
        return hash;
    }
}
