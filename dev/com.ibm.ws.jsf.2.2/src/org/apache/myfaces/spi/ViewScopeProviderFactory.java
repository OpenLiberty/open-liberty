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
package org.apache.myfaces.spi;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.faces.FacesException;
import javax.faces.FacesWrapper;
import javax.faces.context.ExternalContext;
import org.apache.myfaces.spi.impl.SpiUtils;

/**
 *
 * @author Leonardo Uribe
 */
public abstract class ViewScopeProviderFactory implements FacesWrapper<ViewScopeProviderFactory>
{
    protected static final String FACTORY_DEFAULT = 
        "org.apache.myfaces.spi.impl.DefaultViewScopeProviderFactory";
    
    private static final String FACTORY_KEY = ViewScopeProviderFactory.class.getName();
    
    public static ViewScopeProviderFactory getViewScopeHandlerFactory(ExternalContext ctx)
    {
        ViewScopeProviderFactory instance
                = (ViewScopeProviderFactory) ctx.getApplicationMap().get(FACTORY_KEY);
        if (instance != null)
        {
            return instance;
        }
        ViewScopeProviderFactory lpf = null;
        try
        {

            if (System.getSecurityManager() != null)
            {
                final ExternalContext ectx = ctx;
                lpf = (ViewScopeProviderFactory)
                        AccessController.doPrivileged(new PrivilegedExceptionAction<Object>()
                        {
                            public Object run() throws PrivilegedActionException
                            {
                                return SpiUtils.build(ectx,
                                        ViewScopeProviderFactory.class,
                                        FACTORY_DEFAULT);
                            }
                        });
            }
            else
            {
                lpf = (ViewScopeProviderFactory)
                        SpiUtils.build(ctx, ViewScopeProviderFactory.class, FACTORY_DEFAULT);
            }
        }
        catch (PrivilegedActionException pae)
        {
            throw new FacesException(pae);
        }
        if (lpf != null)
        {
            setViewScopeHandlerFactory(ctx, lpf);
        }
        return lpf;
    }

    public static void setViewScopeHandlerFactory(ExternalContext ctx,
                                                             ViewScopeProviderFactory instance)
    {
        ctx.getApplicationMap().put(FACTORY_KEY, instance);
    }    
    
    public abstract ViewScopeProvider getViewScopeHandler(ExternalContext ctx);
    
    public ViewScopeProviderFactory getWrapped()
    {
        return null;
    }
    
    public abstract void setViewScopeHandler(ExternalContext ctx, ViewScopeProvider viewScopeHandler);
}
