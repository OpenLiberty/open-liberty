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
package org.apache.myfaces.webapp;

import java.util.Enumeration;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.myfaces.config.ManagedBeanDestroyer;
import org.apache.myfaces.context.ReleaseableExternalContext;
import org.apache.myfaces.context.servlet.StartupFacesContextImpl;
import org.apache.myfaces.context.servlet.StartupServletExternalContextImpl;
import org.apache.myfaces.shared.context.ExceptionHandlerImpl;
import org.apache.myfaces.spi.ViewScopeProvider;

/**
 * Listens to
 *   - removing, replacing of attributes in context, session and request
 *   - destroying of context, session and request
 * for the ManagedBeanDestroyer to assure right destruction of managed beans in those scopes.
 * 
 * This listener is not registered in a tld or web.xml, but will be called by StartupServletContextListener.
 * 
 * @author Jakob Korherr (latest modification by $Author: lu4242 $)
 * @version $Revision: 1533310 $ $Date: 2013-10-18 02:01:42 +0000 (Fri, 18 Oct 2013) $
 * @since 2.0
 */
public class ManagedBeanDestroyerListener implements 
        HttpSessionAttributeListener, HttpSessionListener,
        ServletContextListener, ServletContextAttributeListener,
        ServletRequestListener, ServletRequestAttributeListener
{

    /**
     * The instance of the ManagedBeanDestroyerListener created by
     * StartupServletContextListener is stored under this key in the
     * ApplicationMap.
     */
    public static final String APPLICATION_MAP_KEY = "org.apache.myfaces.ManagedBeanDestroyerListener";

    private ManagedBeanDestroyer _destroyer = null;
    
    private ViewScopeProvider _viewScopeHandler = null;

    /**
     * Sets the ManagedBeanDestroyer instance to use.
     *  
     * @param destroyer
     */
    public void setManagedBeanDestroyer(ManagedBeanDestroyer destroyer)
    {
        _destroyer = destroyer;
    }
    
    public void setViewScopeHandler(ViewScopeProvider listener)
    {
        _viewScopeHandler = listener;
    }

    /* Session related methods ***********************************************/
    
    public void attributeAdded(HttpSessionBindingEvent event)
    {
        // noop
    }

    public void attributeRemoved(HttpSessionBindingEvent event)
    {
        if (_destroyer != null)
        {
            _destroyer.destroy(event.getName(), event.getValue());
        }
    }

    public void attributeReplaced(HttpSessionBindingEvent event)
    {
        if (_destroyer != null)
        {
            _destroyer.destroy(event.getName(), event.getValue());
        }
    }

    public void sessionCreated(HttpSessionEvent event)
    {
        // noop
    }

    @SuppressWarnings("unchecked")
    public void sessionDestroyed(HttpSessionEvent event)
    {
        // MYFACES-3040 @PreDestroy Has Called 2 times
        // attributeRemoved receives the event too, so it does not 
        // have sense to handle it here. Unfortunately, it is not possible to 
        // handle it first and then on attributeRemoved, so the best bet is
        // let the code in just one place.
        /*
        if (_destroyer != null)
        {
            HttpSession session = event.getSession();
            Enumeration<String> attributes = session.getAttributeNames();
            if (!attributes.hasMoreElements())
            {
                // nothing to do
                return;
            }

            while (attributes.hasMoreElements())
            {
                String name = attributes.nextElement();
                Object value = session.getAttribute(name);
                _destroyer.destroy(name, value);
            }
        }*/
        
        // If we don't propagate this event, CDI will do for us but outside JSF control
        // so when @PreDestroy methods are called there will not be an active FacesContext.
        // The trick here is ensure clean the affected scopes to avoid duplicates.
        // Remember cdi session scope is different from jsf session scope, because in
        // jsf case the beans are stored under a session attribute, so it has the problem
        // with attributeRemoved, but on cdi a wrapper is used instead, avoiding the problem.
        if (_viewScopeHandler != null)
        {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null)
            {
                _viewScopeHandler.onSessionDestroyed();
            }
            else
            {
                // In case no FacesContext is available, we are on session invalidation
                // through timeout. In that case, create a dummy FacesContext for this one
                // like the one used in startup or shutdown and invoke the destroy method.
                try
                {
                    ServletContext servletContext = event.getSession().getServletContext();
                    ExternalContext externalContext = new StartupServletExternalContextImpl(servletContext, false);
                    ExceptionHandler exceptionHandler = new ExceptionHandlerImpl();
                    facesContext = new StartupFacesContextImpl(externalContext, 
                            (ReleaseableExternalContext) externalContext, exceptionHandler, false);
                    _viewScopeHandler.onSessionDestroyed();
                }
                finally
                {
                    facesContext.release();
                }
            }
        }
    }
    
    /* Context related methods ***********************************************/
    
    public void attributeAdded(ServletContextAttributeEvent event)
    {
        // noop
    }

    public void attributeRemoved(ServletContextAttributeEvent event)
    {
        if (_destroyer != null)
        {
            _destroyer.destroy(event.getName(), event.getValue());
        }
    }

    public void attributeReplaced(ServletContextAttributeEvent event)
    {
        if (_destroyer != null)
        {
            _destroyer.destroy(event.getName(), event.getValue());
        }
    }

    public void contextInitialized(ServletContextEvent event)
    {
        // noop
    }
    
    @SuppressWarnings("unchecked")
    public void contextDestroyed(ServletContextEvent event)
    {
        if (_destroyer != null)
        {
            ServletContext ctx = event.getServletContext();
            Enumeration<String> attributes = ctx.getAttributeNames();
            if (!attributes.hasMoreElements())
            {
                // nothing to do
                return;
            }

            while (attributes.hasMoreElements())
            {
                String name = attributes.nextElement();
                Object value = ctx.getAttribute(name);
                _destroyer.destroy(name, value);
            }
        }
    }
    
    /* Request related methods ***********************************************/
    
    public void attributeAdded(ServletRequestAttributeEvent event)
    {
        // noop
    }

    public void attributeRemoved(ServletRequestAttributeEvent event)
    {
        if (_destroyer != null)
        {
            _destroyer.destroy(event.getName(), event.getValue());
        }
    }

    public void attributeReplaced(ServletRequestAttributeEvent event)
    {
        if (_destroyer != null)
        {
            _destroyer.destroy(event.getName(), event.getValue());
        }
    }

    public void requestInitialized(ServletRequestEvent event)
    {
        // noop
    }
    
    @SuppressWarnings("unchecked")
    public void requestDestroyed(ServletRequestEvent event)
    {
        if (_destroyer != null)
        {
            ServletRequest request = event.getServletRequest();
            Enumeration<String> attributes = request.getAttributeNames();
            if (!attributes.hasMoreElements())
            {
                // nothing to do
                return;
            }
            
            while (attributes.hasMoreElements())
            {
                String name = attributes.nextElement();
                Object value = request.getAttribute(name);
                _destroyer.destroy(name, value);
            }
        }
    }

}
