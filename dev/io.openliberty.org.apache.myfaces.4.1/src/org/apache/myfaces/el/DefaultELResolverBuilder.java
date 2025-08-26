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
package org.apache.myfaces.el;

import org.apache.myfaces.el.resolver.FlashELResolver;
import java.util.ArrayList;
import java.util.List;

import jakarta.el.ArrayELResolver;
import jakarta.el.BeanELResolver;
import jakarta.el.CompositeELResolver;
import jakarta.el.ELResolver;
import jakarta.el.ListELResolver;
import jakarta.el.MapELResolver;
import jakarta.el.ResourceBundleELResolver;
import jakarta.el.StaticFieldELResolver;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.myfaces.cdi.util.CDIUtils;

import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.config.webparameters.MyfacesConfig;
import org.apache.myfaces.el.resolver.CompositeComponentELResolver;
import org.apache.myfaces.el.resolver.ImportConstantsELResolver;
import org.apache.myfaces.el.resolver.ImportHandlerResolver;
import org.apache.myfaces.el.resolver.ResourceBundleResolver;
import org.apache.myfaces.el.resolver.ResourceResolver;
import org.apache.myfaces.el.resolver.ScopedAttributeResolver;
import org.apache.myfaces.el.resolver.implicitobject.ImplicitObjectResolver;
import org.apache.myfaces.core.api.shared.lang.PropertyDescriptorUtils;
import org.apache.myfaces.el.resolver.EmptyStringToNullELResolver;
import org.apache.myfaces.el.resolver.LambdaBeanELResolver;

/**
 * Create the el resolver for faces. see 1.2 spec section 5.6.2
 * 
 * @author Mathias Broekelmann (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
public class DefaultELResolverBuilder extends ELResolverBuilder
{
    private static final Logger LOG = Logger.getLogger(DefaultELResolverBuilder.class.getName());
    public static final List<String> CDI_EL_RESOLVERS = Arrays.asList(
            "org.apache.webbeans.el22.WebBeansELResolver",
            "org.apache.webbeans.el.WebBeansELResolver",
            "org.jboss.weld.module.web.el.WeldELResolver",
            "org.jboss.weld.environment.servlet.jsf.WeldApplication$LazyBeanManagerIntegrationELResolver");

    public DefaultELResolverBuilder(RuntimeConfig runtimeConfig, MyfacesConfig myfacesConfig)
    {
        super(runtimeConfig, myfacesConfig);
        

    }

    @Override
    public void build(FacesContext facesContext, CompositeELResolver compositeElResolver)
    {
        MyfacesConfig config = MyfacesConfig.getCurrentInstance(FacesContext.getCurrentInstance());
        
        // add the ELResolvers to a List first to be able to sort them
        List<ELResolver> list = new ArrayList<>();

        boolean replaceImplicitObjectResolverWithCDIResolver =
                isReplaceImplicitObjectResolverWithCDIResolver(facesContext);
        if (replaceImplicitObjectResolverWithCDIResolver)
        {
            list.add(ImplicitObjectResolver.makeResolverForCDI());
            list.add(getCDIELResolver());
        }
        else
        {
            list.add(ImplicitObjectResolver.makeResolver());
        }
            
        list.add(new CompositeComponentELResolver(config));

        // the spec forces us to add BeanManager#getELResolver manually
        // but both CDI impls also add this ELResolver... remove it here
        // see https://github.com/jakartaee/faces/issues/1798
        if (replaceImplicitObjectResolverWithCDIResolver)
        {
            List<ELResolver> temp = new ArrayList<>();
            addFromRuntimeConfig(temp);
            temp.removeIf(resolver -> CDI_EL_RESOLVERS.contains(resolver.getClass().getName()));
            list.addAll(temp);
        }
        else
        {
            addFromRuntimeConfig(list);
        }

        if ("true".equalsIgnoreCase(
                facesContext.getExternalContext().getInitParameter(UIInput.EMPTY_STRING_AS_NULL_PARAM_NAME)))
        {
            list.add(new EmptyStringToNullELResolver());
        }

        //Flash object is instanceof Map, so it is necessary to resolve
        //before MapELResolver. Better to put this one before
        list.add(new FlashELResolver());
        list.add(new ResourceResolver());
        list.add(new ResourceBundleELResolver());
        list.add(new ResourceBundleResolver());
        list.add(new ImportConstantsELResolver());
 
        try
        {
            ELResolver streamElResolver = (ELResolver) runtimeConfig.getExpressionFactory().getStreamELResolver();
            if (streamElResolver != null)
            {
                list.add(streamElResolver);
            }
        } 
        catch (Throwable ex)
        {
            LOG.log(Level.WARNING, "Could not add ExpressionFactory#getStreamELResolver!", ex);
        }

        try
        {
            list.add(new StaticFieldELResolver());
        } 
        catch (Throwable ex)
        {
            LOG.log(Level.WARNING, "Could not add StaticFieldELResolver!", ex);
        }

        list.add(new MapELResolver());
        list.add(new ListELResolver());
        list.add(new ArrayELResolver());
        if (PropertyDescriptorUtils.isUseLambdaMetafactory(facesContext.getExternalContext()))
        {
            list.add(new LambdaBeanELResolver());
        }
        else
        {
            list.add(new BeanELResolver());
        }

        // give the user a chance to sort the resolvers
        sortELResolvers(list);
        
        list = wrapELResolvers(list);

        // give the user a chance to filter the resolvers
        Iterable<ELResolver> filteredELResolvers = filterELResolvers(list);

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
    
    protected ELResolver getCDIELResolver()
    {
        BeanManager beanManager = CDIUtils.getBeanManager(FacesContext.getCurrentInstance());
        return beanManager.getELResolver();
    }
}
