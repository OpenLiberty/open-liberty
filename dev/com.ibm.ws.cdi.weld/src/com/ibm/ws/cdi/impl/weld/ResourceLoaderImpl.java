/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.weld;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;

import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.resources.spi.ResourceLoadingException;
import org.jboss.weld.util.collections.EnumerationList;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * This is a ResouceLoader implementation to load resources, which might be application classes
 * or Weld classes
 */
public class ResourceLoaderImpl implements ResourceLoader
{

    private static final TraceComponent tc = Tr.register(ResourceLoaderImpl.class);
    private final ClassLoader delegate;

    public ResourceLoaderImpl(ClassLoader classloader)
    {
        delegate = classloader;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.weld.bootstrap.api.Service#cleanup()
     */
    @Override
    public void cleanup()
    {
        //noop
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.weld.resources.spi.ResourceLoader#classForName(java.lang.String)
     */
    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    public Class<?> classForName(String className)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, "Loading class " + className + " using classloader " + delegate);
        }
        try
        {
            return delegate.loadClass(className);
        } catch (ClassNotFoundException e)
        {
            return loadFromWeldBundleCL(className);
        }
    }

    @FFDCIgnore(ClassNotFoundException.class)
    private Class<?> loadFromWeldBundleCL(String className)
    {
        //use weld bundle classloader to load the class
        ClassLoader bundleCL = (ClassLoader) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return BeanManagerImpl.class.getClassLoader();
					}
				});
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, "Unable to load class " + className + " using classloader " + delegate + " Try to use the weld bundle classloader "
                         + bundleCL);

        }
        try
        {
            return bundleCL.loadClass(className);
        } catch (ClassNotFoundException e1)
        {
            throw new ResourceLoadingException(Tr.formatMessage(tc,
                                                                "error.loading.class.CWOWB1003E",
                                                                className,
                                                                bundleCL), e1);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.weld.resources.spi.ResourceLoader#getResource(java.lang.String)
     */
    @Override
    public URL getResource(String resName)
    {
        return delegate.getResource(resName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.weld.resources.spi.ResourceLoader#getResources(java.lang.String)
     */
    @Override
    public Collection<URL> getResources(String resName)
    {
        try
        {
            return new EnumerationList<URL>(delegate.getResources(resName));
        } catch (IOException e)
        {
            throw new ResourceLoadingException(Tr.formatMessage(tc,
                                                                "error.loading.resource.CWOWB1005E",
                                                                resName
                            ), e);

        }
    }

}
