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

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.config.annotation.LifecycleProviderFactory;
import org.apache.myfaces.shared.util.ClassUtils;

import javax.faces.FactoryFinder;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Initialise the MyFaces system.
 * <p/>
 * This context listener is registered by the JSP TLD file for the standard JSF "f" components. Normally, servlet
 * containers will automatically load and process .tld files at startup time, and therefore register and run this class
 * automatically.
 * <p/>
 * Some very old servlet containers do not do this correctly, so in those cases this listener may be registered manually
 * in web.xml. Registering it twice (ie in both .tld and web.xml) will result in a harmless warning message being
 * generated. Very old versions of MyFaces Core do not register the listener in the .tld file, so those also need a
 * manual entry in web.xml. However all versions since at least 1.1.2 have this entry in the tld.
 * <p/>
 * This listener also delegates all session, request and context events to ManagedBeanDestroyer.
 * Because of that we only need to register one listener in the tld.
 *
 * @author Manfred Geiler (latest modification by $Author: werpu $)
 * @version $Revision: 1304305 $ $Date: 2012-03-23 12:17:45 +0000 (Fri, 23 Mar 2012) $
 */
public class StartupServletContextListener implements ServletContextListener,
        HttpSessionAttributeListener, HttpSessionListener,
        ServletRequestListener, ServletRequestAttributeListener,
        ServletContextAttributeListener
{
    static final String FACES_INIT_DONE = "org.apache.myfaces.webapp.StartupServletContextListener.FACES_INIT_DONE";

    /**
     * comma delimited list of plugin classes which can be hooked into myfaces
     */
    @JSFWebConfigParam(since = "2.0")
    static final String FACES_INIT_PLUGINS = "org.apache.myfaces.FACES_INIT_PLUGINS";

    private static final byte FACES_INIT_PHASE_PREINIT = 0;
    private static final byte FACES_INIT_PHASE_POSTINIT = 1;
    private static final byte FACES_INIT_PHASE_PREDESTROY = 2;
    private static final byte FACES_INIT_PHASE_POSTDESTROY = 3;

    //private static final Log log = LogFactory.getLog(StartupServletContextListener.class);
    private static final Logger log = Logger.getLogger(StartupServletContextListener.class.getName());

    private FacesInitializer _facesInitializer;
    private ServletContext _servletContext;
    private ManagedBeanDestroyerListener _detroyerListener = new ManagedBeanDestroyerListener();

    public void contextInitialized(ServletContextEvent event)
    {
        if (_servletContext != null)
        {
            throw new IllegalStateException("context is already initialized");
        }
        _servletContext = event.getServletContext();

        Boolean b = (Boolean) _servletContext.getAttribute(FACES_INIT_DONE);
        if (b == null || b.booleanValue() == false)
        {
            if (_facesInitializer == null)
            {
                _facesInitializer = FacesInitializerFactory.getFacesInitializer(_servletContext);
            }

            // Create startup FacesContext before initializing
            FacesContext facesContext = _facesInitializer.initStartupFacesContext(_servletContext);

            // publish the ManagedBeanDestroyerListener instance in the application map
            _publishManagedBeanDestroyerListener(facesContext);

            dispatchInitializationEvent(event, FACES_INIT_PHASE_PREINIT);
            _facesInitializer.initFaces(_servletContext);
            dispatchInitializationEvent(event, FACES_INIT_PHASE_POSTINIT);
            _servletContext.setAttribute(FACES_INIT_DONE, Boolean.TRUE);

            // call contextInitialized on ManagedBeanDestroyerListener
            _detroyerListener.contextInitialized(event);

            //Destroy startup FacesContext
            _facesInitializer.destroyStartupFacesContext(facesContext);
        }
        else
        {
            log.info("MyFaces already initialized");
        }
    }

    /**
     * Publishes the ManagedBeanDestroyerListener instance in the application map.
     * This allows the FacesConfigurator to access the instance and to set the
     * correct ManagedBeanDestroyer instance on it.
     *
     * @param facesContext
     */
    private void _publishManagedBeanDestroyerListener(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, Object> applicationMap = externalContext.getApplicationMap();

        applicationMap.put(ManagedBeanDestroyerListener.APPLICATION_MAP_KEY, _detroyerListener);
    }

    public void contextDestroyed(ServletContextEvent event)
    {
        if (_facesInitializer != null && _servletContext != null)
        {
            // Create startup FacesContext before start undeploy
            FacesContext facesContext = _facesInitializer.initShutdownFacesContext(_servletContext);

            dispatchInitializationEvent(event, FACES_INIT_PHASE_PREDESTROY);
            // call contextDestroyed on ManagedBeanDestroyerListener to destroy the attributes
            _detroyerListener.contextDestroyed(event);

            _facesInitializer.destroyFaces(_servletContext);

            LifecycleProviderFactory.getLifecycleProviderFactory().release();

            // Destroy startup FacesContext, but note we do before publish postdestroy event on
            // plugins and before release factories.
            if (facesContext != null)
            {
                _facesInitializer.destroyShutdownFacesContext(facesContext);
            }

            FactoryFinder.releaseFactories();

            //DiscoverSingleton.release(); //clears EnvironmentCache and prevents leaking classloader references
            dispatchInitializationEvent(event, FACES_INIT_PHASE_POSTDESTROY);
        }

        _servletContext = null;
    }

    /**
     * configure the faces initializer
     *
     * @param facesInitializer
     */
    public void setFacesInitializer(FacesInitializer facesInitializer) // TODO who uses this method?
    {
        if (_facesInitializer != null && _facesInitializer != facesInitializer && _servletContext != null)
        {
            _facesInitializer.destroyFaces(_servletContext);
        }
        _facesInitializer = facesInitializer;
        if (_servletContext != null)
        {
            facesInitializer.initFaces(_servletContext);
        }
    }

    /**
     * loads the faces init plugins per reflection and Service loader
     * in a jdk6 environment
     *
     * @return false in case of a failed attempt or no listeners found
     *         which then will cause the jdk5 context.xml code to trigger
     */
    private boolean loadFacesInitPluginsJDK6()
    {
        String[] pluginEntries = null;
        try
        {
            Class serviceLoader = ClassUtils.getContextClassLoader().loadClass("java.util.ServiceLoader");
            Method m = serviceLoader.getDeclaredMethod("load", Class.class, ClassLoader.class);
            Object loader = m.invoke(serviceLoader, StartupListener.class, ClassUtils.getContextClassLoader());
            m = loader.getClass().getDeclaredMethod("iterator");
            Iterator<StartupListener> it = (Iterator<StartupListener>) m.invoke(loader);
            List<StartupListener> listeners = new LinkedList<StartupListener>();
            if (!it.hasNext())
            {
                return false;
            }
            while (it.hasNext())
            {
                listeners.add(it.next());
            }
            //StartupListener[] listeners1 = listeners.toArray(new StartupListener[listeners.size()]);
            _servletContext.setAttribute(FACES_INIT_PLUGINS, listeners);
            return true;
        }
        catch (ClassNotFoundException e)
        {

        }
        catch (NoSuchMethodException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        catch (InvocationTargetException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        catch (IllegalAccessException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return false;
    }

    /**
     * loads the faces init plugins per reflection and Service loader
     * in a jdk6 environment
     */
    private void loadFacesInitPluginsJDK5()
    {

        String plugins = (String) _servletContext.getInitParameter(FACES_INIT_PLUGINS);
        if (plugins == null)
        {
            return;
        }
        log.info("MyFaces Plugins found");
        String[] pluginEntries = plugins.split(",");
        List<StartupListener> listeners = new ArrayList<StartupListener>(pluginEntries.length);
        for (String pluginEntry : pluginEntries)
        {
            try
            {
                Class pluginClass = null;
                pluginClass = ClassUtils.getContextClassLoader().loadClass(pluginEntry);
                if (pluginClass == null)
                {
                    pluginClass = this.getClass().getClassLoader().loadClass(pluginEntry);
                }
                listeners.add((StartupListener) pluginClass.newInstance());
            }
            catch (ClassNotFoundException e)
            {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
            catch (InstantiationException e)
            {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
            catch (IllegalAccessException e)
            {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        // StartupListener[] listeners1 = listeners.toArray(new StartupListener[listeners.size()]);
        _servletContext.setAttribute(FACES_INIT_PLUGINS, listeners);

    }

    /**
     * the central initialisation event dispatcher which calls
     * our listeners
     *
     * @param event
     * @param operation
     */
    private void dispatchInitializationEvent(ServletContextEvent event, int operation)
    {

        if (operation == FACES_INIT_PHASE_PREINIT)
        {
            if (!loadFacesInitPluginsJDK6())
            {
                loadFacesInitPluginsJDK5();
            }
        }

        List<StartupListener> pluginEntries = (List<StartupListener>) _servletContext.getAttribute(FACES_INIT_PLUGINS);
        if (pluginEntries == null)
        {
            return;
        }

        //now we process the plugins
        for (StartupListener initializer : pluginEntries)
        {
            log.info("Processing plugin");

            //for now the initializers have to be stateless to
            //so that we do not have to enforce that the initializer
            //must be serializable
            switch (operation)
            {
                case FACES_INIT_PHASE_PREINIT:
                    initializer.preInit(event);
                    break;
                case FACES_INIT_PHASE_POSTINIT:
                    initializer.postInit(event);
                    break;
                case FACES_INIT_PHASE_PREDESTROY:
                    initializer.preDestroy(event);
                    break;
                default:
                    initializer.postDestroy(event);
                    break;
            }
        }
        log.info("Processing MyFaces plugins done");
    }

    /* the following methods are needed to serve ManagedBeanDestroyerListener */
    /* Session related methods ***********************************************/

    public void attributeAdded(HttpSessionBindingEvent event)
    {
        _detroyerListener.attributeAdded(event);
    }

    public void attributeRemoved(HttpSessionBindingEvent event)
    {
        _detroyerListener.attributeRemoved(event);
    }

    public void attributeReplaced(HttpSessionBindingEvent event)
    {
        _detroyerListener.attributeReplaced(event);
    }

    public void sessionCreated(HttpSessionEvent event)
    {
        _detroyerListener.sessionCreated(event);
    }

    public void sessionDestroyed(HttpSessionEvent event)
    {
        _detroyerListener.sessionDestroyed(event);
    }

    /* Context related methods ***********************************************/

    public void attributeAdded(ServletContextAttributeEvent event)
    {
        _detroyerListener.attributeAdded(event);
    }

    public void attributeRemoved(ServletContextAttributeEvent event)
    {
        _detroyerListener.attributeRemoved(event);
    }

    public void attributeReplaced(ServletContextAttributeEvent event)
    {
        _detroyerListener.attributeReplaced(event);
    }

    /* Request related methods ***********************************************/

    public void attributeAdded(ServletRequestAttributeEvent event)
    {
        _detroyerListener.attributeAdded(event);
    }

    public void attributeRemoved(ServletRequestAttributeEvent event)
    {
        _detroyerListener.attributeRemoved(event);
    }

    public void attributeReplaced(ServletRequestAttributeEvent event)
    {
        _detroyerListener.attributeReplaced(event);
    }

    public void requestInitialized(ServletRequestEvent event)
    {
        _detroyerListener.requestInitialized(event);
    }

    public void requestDestroyed(ServletRequestEvent event)
    {
        _detroyerListener.requestDestroyed(event);
    }

}
