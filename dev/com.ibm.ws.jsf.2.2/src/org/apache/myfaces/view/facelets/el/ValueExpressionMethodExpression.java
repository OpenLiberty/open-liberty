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
import javax.el.MethodInfo;
import javax.el.ValueExpression;
import javax.faces.FacesWrapper;
import javax.faces.context.FacesContext;

/**
 * This MethodExpression contains a ValueExpression which resolves to 
 * the "real" MethodExpression that should be invoked. This is needed 
 * when the MethodExpression is on the parent composite component attribute map.
 * See FaceletViewDeclarationLanguage.retargetMethodExpressions() for details.
 * 
 * @author Jakob Korherr (latest modification by $Author: lu4242 $)
 * @version $Revision: 1406265 $ $Date: 2012-11-06 18:33:41 +0000 (Tue, 06 Nov 2012) $
 */
public class ValueExpressionMethodExpression extends MethodExpression 
    implements FacesWrapper<ValueExpression>, Externalizable
{
    
    private static final long serialVersionUID = -2847633717581167765L;
    
    private ValueExpression valueExpression;
    
    public ValueExpressionMethodExpression()
    {
    }
    
    public ValueExpressionMethodExpression(ValueExpression valueExpression)
    {
        this.valueExpression = valueExpression;   
    }

    @Override
    public MethodInfo getMethodInfo(ELContext context)
    {
        MethodExpression me = getMethodExpression(context);
        if (me != null)
        {
            return me.getMethodInfo(context);
        }
        return null;
    }

    @Override
    public Object invoke(ELContext context, Object[] params)
    {
        MethodExpression me = getMethodExpression(context);
        if (me != null)
        {        
            return me.invoke(context, params);
        }
        return null;
    }

    @Override
    public boolean equals(Object obj)
    {
        MethodExpression me = getMethodExpression();
        if (me != null)
        {        
            return me.equals(obj);
        }
        if (!(obj instanceof ValueExpressionMethodExpression))
        {
            return false;
        }
        ValueExpressionMethodExpression other = (ValueExpressionMethodExpression) obj;
        if ((this.valueExpression == null && other.valueExpression != null) || 
             (this.valueExpression != null && !this.valueExpression.equals(other.valueExpression)))
        {
            return false;
        }
        return true;
    }

    @Override
    public String getExpressionString()
    {
        //getMethodExpression().getExpressionString()
        return valueExpression.getExpressionString();
    }

    @Override
    public int hashCode()
    {
        MethodExpression me = getMethodExpression();
        if (me != null)
        {        
            return me.hashCode();
        }
        return valueExpression.hashCode();
    }

    @Override
    public boolean isLiteralText()
    {
        MethodExpression me = getMethodExpression();
        if (me != null)
        {
            return me.isLiteralText();
        }
        return valueExpression.isLiteralText();
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
