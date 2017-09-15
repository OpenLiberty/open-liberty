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
import javax.faces.FacesWrapper;
import javax.faces.view.Location;
import javax.faces.view.facelets.TagAttribute;

/**
 * 
 * 
 * @author Jacob Hookom
 * @version $Id: ContextAwareTagMethodExpression.java 1194849 2011-10-29 09:28:45Z struberg $
 */
public final class ContextAwareTagMethodExpression
        extends MethodExpression
        implements Externalizable, FacesWrapper<MethodExpression>, ContextAware
{

    private static final long serialVersionUID = 1L;

    private MethodExpression _wrapped;

    private Location _location;
    
    private String _qName;

    public ContextAwareTagMethodExpression()
    {
        super();
    }

    public ContextAwareTagMethodExpression(TagAttribute tagAttribute, MethodExpression methodExpression)
    {
        _location = tagAttribute.getLocation();
        _qName = tagAttribute.getQName();
        _wrapped = methodExpression;
    }

    public MethodInfo getMethodInfo(ELContext context)
    {
        try
        {
            return _wrapped.getMethodInfo(context);
        }
        catch (PropertyNotFoundException pnfe)
        {
            throw new ContextAwarePropertyNotFoundException(getLocation(), getLocalExpressionString(), getQName(),pnfe);
        }
        catch (MethodNotFoundException mnfe)
        {
            throw new ContextAwareMethodNotFoundException(getLocation(), getLocalExpressionString(), getQName(), mnfe);
        }
        catch (ELException e)
        {
            throw new ContextAwareELException(getLocation(), getLocalExpressionString(), getQName(), e);
        } 
        //Not necessary because NullPointerException by null context never occur and should not be wrapped
        //catch (Exception e)
        //{
        //    throw new ContextAwareException(getLocation(), getLocalExpressionString(), getQName(), e);
        //}
    }

    public Object invoke(ELContext context, Object[] params)
    {
        try
        {
            return _wrapped.invoke(context, params);
        }
        catch (PropertyNotFoundException pnfe)
        {
            throw new ContextAwarePropertyNotFoundException(getLocation(), getLocalExpressionString(), getQName(),pnfe);
        }
        catch (MethodNotFoundException mnfe)
        {
            throw new ContextAwareMethodNotFoundException(getLocation(), getLocalExpressionString(), getQName(), mnfe);
        }
        catch (ELException e)
        {
            throw new ContextAwareELException(getLocation(), getLocalExpressionString(), getQName(), e);
        }
        //Not necessary because NullPointerException by null context never occur and should not be wrapped
        //catch (Exception e)
        //{
        //    throw new ContextAwareException(getLocation(), getLocalExpressionString(), getQName(), e);
        //}
    }
    
        
    private String getLocalExpressionString()
    {
        String expressionString = null;
        try
        {
            expressionString = getExpressionString();
        }
        catch (Throwable t)
        {
            //swallo it because it is not important
        }
        return expressionString;
    }

    public String getExpressionString()
    {
        return _wrapped.getExpressionString();
    }

    public boolean equals(Object obj)
    {
        return _wrapped.equals(obj);
    }

    public int hashCode()
    {
        return _wrapped.hashCode();
    }

    public boolean isLiteralText()
    {
        return _wrapped.isLiteralText();
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(_wrapped);
        out.writeObject(_location);
        out.writeUTF(_qName);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        _wrapped = (MethodExpression) in.readObject();
        _location = (Location) in.readObject();
        _qName = in.readUTF();
    }

    public String toString()
    {
        return _location + ": " + _wrapped;
    }
    
    public Location getLocation()
    {
        return _location;
    }
    
    public String getQName()
    {
        return _qName;
    }
    
    public MethodExpression getWrapped()
    {
        return _wrapped;
    }

}
