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
package org.apache.myfaces.el.convert;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.VariableResolver;
import java.beans.FeatureDescriptor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.myfaces.el.VariableResolverImpl;

/**
 * Wrapper that converts a VariableResolver into an ELResolver. See JSF 1.2 spec section 5.6.1.5
 * 
 * @author Stan Silvert (latest modification by $Author: lu4242 $)
 * @author Mathias Broekelmann
 * @version $Revision: 1215316 $ $Date: 2011-12-16 22:07:07 +0000 (Fri, 16 Dec 2011) $
 */
@SuppressWarnings("deprecation")
public final class VariableResolverToELResolver extends ELResolver
{

    // holds a flag to check if this instance is already called in current thread 
    private static final ThreadLocal<Collection<String>> propertyGuardThreadLocal
            = new ThreadLocal<Collection<String>>();

    /**
     * Gets the Collection<String> value of the propertyGuardThreadLocal.
     * If the value from the ThreadLocal ist null, a new Collection<String>
     * will be created.
     *
     * NOTE that we should not accomplish this by setting an initialValue on the
     * ThreadLocal, because this will automatically be set on the ThreadLocalMap
     * and thus can propably cause a memory leak.
     *
     * @return
     */
    private static Collection<String> getPropertyGuard()
    {
        Collection<String> propertyGuard = propertyGuardThreadLocal.get();

        if (propertyGuard == null)
        {
            propertyGuard = new HashSet<String>();
            propertyGuardThreadLocal.set(propertyGuard);
        }

        return propertyGuard;
    }
    
    private VariableResolver variableResolver;
    private boolean isDefaultLegacyVariableResolver;

    /**
     * Creates a new instance of VariableResolverToELResolver
     */
    public VariableResolverToELResolver(final VariableResolver variableResolver)
    {
        this.variableResolver = variableResolver;
        this.isDefaultLegacyVariableResolver = (this.variableResolver instanceof VariableResolverImpl);
    }
    
    /**
     * @return the variableResolver
     */
    public VariableResolver getVariableResolver()
    {
        return variableResolver;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) throws NullPointerException,
            PropertyNotFoundException, ELException
    {
        if (isDefaultLegacyVariableResolver)
        {
            // Skip this one because it causes previous EL Resolvers already processed
            // before this one to be called again. This behavior is only valid if it 
            // is installed a custom VariableResolver.
            return null;
        }
        if (base != null)
        {
            return null;
        }
        if (property == null)
        {
            throw new PropertyNotFoundException();
        }

        context.setPropertyResolved(true);

        if (!(property instanceof String))
        {
            return null;
        }

        final String strProperty = (String) property;

        Collection<String> propertyGuard = getPropertyGuard();

        Object result = null;
        try
        {
            // only call the resolver if we haven't done it in current stack
            if(!propertyGuard.contains(strProperty))
            {
                propertyGuard.add(strProperty);
                result = variableResolver.resolveVariable(facesContext(context), strProperty);
            }
        }
        catch (javax.faces.el.PropertyNotFoundException e)
        {
            context.setPropertyResolved(false);
            throw new PropertyNotFoundException(e.getMessage(), e);
        }
        catch (EvaluationException e)
        {
            context.setPropertyResolved(false);
            throw new ELException(e.getMessage(), e);
        }
        catch (RuntimeException e)
        {
            context.setPropertyResolved(false);
            throw e;
        }
        finally
        {
            propertyGuard.remove(strProperty);

            // if the propertyGuard is empty, remove the ThreadLocal
            // in order to prevent a memory leak
            if (propertyGuard.isEmpty())
            {
                propertyGuardThreadLocal.remove();
            }

            // set property resolved to false in any case if result is null
            context.setPropertyResolved(result != null);
        }

        return result;
    }

    // get the FacesContext from the ELContext
    private static FacesContext facesContext(ELContext context)
    {
        return (FacesContext) context.getContext(FacesContext.class);
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base)
    {
        if (isDefaultLegacyVariableResolver)
        {
            return null;
        }
        if (base != null)
        {
            return null;
        }

        return String.class;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) throws NullPointerException,
            PropertyNotFoundException, PropertyNotWritableException, ELException
    {
        if (isDefaultLegacyVariableResolver)
        {
            return;
        }
        if ((base == null) && (property == null))
        {
            throw new PropertyNotFoundException();
        }
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) throws NullPointerException,
            PropertyNotFoundException, ELException
    {
        if (isDefaultLegacyVariableResolver)
        {
            return false;
        }
        if ((base == null) && (property == null))
        {
            throw new PropertyNotFoundException();
        }

        return false;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) throws NullPointerException,
            PropertyNotFoundException, ELException
    {
        if (isDefaultLegacyVariableResolver)
        {
            return null;
        }
        if ((base == null) && (property == null))
        {
            throw new PropertyNotFoundException();
        }

        return null;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base)
    {
        if (isDefaultLegacyVariableResolver)
        {
            return null;
        }
        return null;
    }

}
