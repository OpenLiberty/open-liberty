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
package org.apache.myfaces.el.unified;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import jakarta.el.ArrayELResolver;
import jakarta.el.BeanELResolver;
import jakarta.el.CompositeELResolver;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.ListELResolver;
import jakarta.el.MapELResolver;
import jakarta.el.ResourceBundleELResolver;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.context.FacesContext;
import org.apache.myfaces.cdi.util.CDIUtils;

import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.el.FlashELResolver;
import org.apache.myfaces.el.unified.resolver.CompositeComponentELResolver;
import org.apache.myfaces.el.unified.resolver.FacesCompositeELResolver.Scope;
import org.apache.myfaces.el.unified.resolver.ImportConstantsELResolver;
import org.apache.myfaces.el.unified.resolver.ImportHandlerResolver;
import org.apache.myfaces.el.unified.resolver.ManagedBeanResolver;
import org.apache.myfaces.el.unified.resolver.ResourceBundleResolver;
import org.apache.myfaces.el.unified.resolver.ResourceResolver;
import org.apache.myfaces.el.unified.resolver.ScopedAttributeResolver;
import org.apache.myfaces.el.unified.resolver.implicitobject.ImplicitObjectResolver;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ClassUtils;

/**
 * Create the el resolver for faces. see 1.2 spec section 5.6.2
 * 
 * @author Mathias Broekelmann (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
public class ResolverBuilderForFaces extends ResolverBuilderBase implements ELResolverBuilder
{
    private static final Class STATIC_FIELD_EL_RESOLVER_CLASS;
    private static final Method GET_STREAM_EL_RESOLVER_METHOD;
    
    static
    {
        Class staticFieldELResolverClass = null;
        Method getStreamELResolverMethod = null;
        try
        {
            staticFieldELResolverClass = ClassUtils.classForName("jakarta.el.StaticFieldELResolver");
            getStreamELResolverMethod = ExpressionFactory.class.getMethod("getStreamELResolver");
        }
        catch (NoSuchMethodException ex)
        {
            //No op
        }
        catch (SecurityException ex)
        {
            //No op
        }
        catch (ClassNotFoundException ex)
        {
            //No op
        }
        STATIC_FIELD_EL_RESOLVER_CLASS = staticFieldELResolverClass;
        GET_STREAM_EL_RESOLVER_METHOD = getStreamELResolverMethod;
    }
    
    public ResolverBuilderForFaces(RuntimeConfig config)
    {
        super(config);
    }

    public void build(CompositeELResolver compositeElResolver)
    {
        build(FacesContext.getCurrentInstance(), compositeElResolver);
    }
    
    @Override
    public void build(FacesContext facesContext, CompositeELResolver compositeElResolver)
    {
        MyfacesConfig config = MyfacesConfig.getCurrentInstance(
                     FacesContext.getCurrentInstance().getExternalContext());
        
        // add the ELResolvers to a List first to be able to sort them
        List<ELResolver> list = new ArrayList<>();

        // Add CDI ELResolver for JSF 2.3
        if (isReplaceImplicitObjectResolverWithCDIResolver(facesContext))
        {
            list.add(ImplicitObjectResolver.makeResolverForFacesCDI());

            BeanManager beanManager = CDIUtils.getBeanManager(
                    FacesContext.getCurrentInstance().getExternalContext());
            // TODO JakartaFaces
            //list.add(beanManager.getELResolver());
        }
        else
        {
            list.add(ImplicitObjectResolver.makeResolverForFaces());
        }
            
        list.add(new CompositeComponentELResolver());

        addFromRuntimeConfig(list);

        //Flash object is instanceof Map, so it is necessary to resolve
        //before MapELResolver. Better to put this one before
        list.add(new FlashELResolver());
        if (config.isSupportManagedBeans())
        {
            list.add(new ManagedBeanResolver());
        }
        list.add(new ResourceResolver());
        list.add(new ResourceBundleELResolver());
        list.add(new ResourceBundleResolver());
        list.add(new ImportConstantsELResolver());
        
        if (STATIC_FIELD_EL_RESOLVER_CLASS != null &&
            GET_STREAM_EL_RESOLVER_METHOD != null)
        {
            try
            {
                ELResolver streamElResolver = (ELResolver) GET_STREAM_EL_RESOLVER_METHOD.invoke(
                        getRuntimeConfig().getExpressionFactory());
                if (streamElResolver != null)
                {
                    // By default return null, but in a EL 3 implementation it should be there,
                    // this is just to avoid exceptions in junit testing
                    list.add(streamElResolver);
                }
                list.add((ELResolver) STATIC_FIELD_EL_RESOLVER_CLASS.newInstance());
            } 
            catch (IllegalAccessException ex)
            {
            }
            catch (IllegalArgumentException ex)
            {
            }
            catch (InvocationTargetException ex)
            {
            }
            catch (InstantiationException ex)
            {
            }
        }
        
        list.add(new MapELResolver());
        list.add(new ListELResolver());
        list.add(new ArrayELResolver());
        list.add(new BeanELResolver());
        
        // give the user a chance to sort the resolvers
        sortELResolvers(list, Scope.Faces);
        
        // give the user a chance to filter the resolvers
        Iterable<ELResolver> filteredELResolvers = filterELResolvers(list, Scope.Faces);
        
        // add the resolvers from the list to the CompositeELResolver
        for (ELResolver resolver : filteredELResolvers)
        {
            compositeElResolver.add(resolver);
        }
        
        // Only add this resolver if the user wants to use the EL ImportHandler
        if (config.isSupportEL3ImportHandler())
        {
            compositeElResolver.add(new ImportHandlerResolver());
        }
        
        // the ScopedAttributeResolver has to be the last one in every
        // case, because it always sets propertyResolved to true (per the spec)
        compositeElResolver.add(new ScopedAttributeResolver());
    }
    
}
