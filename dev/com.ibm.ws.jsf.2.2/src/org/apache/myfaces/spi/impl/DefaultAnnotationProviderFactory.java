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
package org.apache.myfaces.spi.impl;

import org.apache.myfaces.config.annotation.DefaultAnnotationProvider;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.spi.AnnotationProvider;
import org.apache.myfaces.spi.AnnotationProviderFactory;
import org.apache.myfaces.spi.ServiceProviderFinderFactory;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @since 2.0.2
 * @author Leonardo Uribe
 */
public class DefaultAnnotationProviderFactory extends AnnotationProviderFactory
{
    public static final String ANNOTATION_PROVIDER = AnnotationProvider.class.getName();
    
    public static final String ANNOTATION_PROVIDER_LIST = AnnotationProvider.class.getName()+".LIST";
    
    public static final String ANNOTATION_PROVIDER_INSTANCE = AnnotationProvider.class.getName()+".INSTANCE";
    
    private Logger getLogger()
    {
        return Logger.getLogger(DefaultAnnotationProviderFactory.class.getName());
    }
    
    @Override
    public AnnotationProvider getAnnotationProvider(ExternalContext externalContext)
    {
        AnnotationProvider annotationProvider
                = (AnnotationProvider) externalContext.getApplicationMap().get(ANNOTATION_PROVIDER_INSTANCE);
        if (annotationProvider == null)
        {
            annotationProvider = createAnnotationProvider(externalContext);
            externalContext.getApplicationMap().put(ANNOTATION_PROVIDER_INSTANCE, annotationProvider);
        }
        return annotationProvider;
    }
    
    @Override
    public AnnotationProvider createAnnotationProvider(
            ExternalContext externalContext)
    {
        AnnotationProvider returnValue = null;
        final ExternalContext extContext = externalContext;
        try
        {
            if (System.getSecurityManager() != null)
            {
                returnValue = AccessController.doPrivileged(new PrivilegedExceptionAction<AnnotationProvider>()
                        {
                            public AnnotationProvider run() throws ClassNotFoundException,
                                    NoClassDefFoundError,
                                    InstantiationException,
                                    IllegalAccessException,
                                    InvocationTargetException,
                                    PrivilegedActionException
                            {
                                return resolveAnnotationProviderFromService(extContext);
                            }
                        });
            }
            else
            {
                returnValue = resolveAnnotationProviderFromService(extContext);
            }
        }
        catch (ClassNotFoundException e)
        {
            // ignore
        }
        catch (NoClassDefFoundError e)
        {
            // ignore
        }
        catch (InstantiationException e)
        {
            getLogger().log(Level.SEVERE, "", e);
        }
        catch (IllegalAccessException e)
        {
            getLogger().log(Level.SEVERE, "", e);
        }
        catch (InvocationTargetException e)
        {
            getLogger().log(Level.SEVERE, "", e);
        }
        catch (PrivilegedActionException e)
        {
            throw new FacesException(e);
        }
        return returnValue;
    }
    
    private AnnotationProvider resolveAnnotationProviderFromService(
            ExternalContext externalContext) throws ClassNotFoundException,
            NoClassDefFoundError,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException,
            PrivilegedActionException
    {
        List<String> classList = (List<String>) externalContext.getApplicationMap().get(ANNOTATION_PROVIDER_LIST);
        if (classList == null)
        {
            classList = ServiceProviderFinderFactory.getServiceProviderFinder(externalContext).
                    getServiceProviderList(ANNOTATION_PROVIDER);
            externalContext.getApplicationMap().put(ANNOTATION_PROVIDER_LIST, classList);
        }
        return ClassUtils.buildApplicationObject(AnnotationProvider.class, classList, new DefaultAnnotationProvider());
    }
}
