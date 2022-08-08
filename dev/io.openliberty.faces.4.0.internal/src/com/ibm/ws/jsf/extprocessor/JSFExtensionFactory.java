/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.extprocessor;

import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.Application;
import javax.servlet.ServletContextListener;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.jsf.shared.JSFConstants;
import com.ibm.ws.jsf.shared.JSFConstants.JSFImplEnabled;
import com.ibm.ws.jsf.shared.cdi.CDIJSFInitializer;
import com.ibm.ws.jsf.shared.extprocessor.JSFExtensionProcessor;
import com.ibm.ws.jsf.shared.util.FacesMessages;
import com.ibm.ws.jsf.shared.util.JspURIMatcher;
import com.ibm.ws.jsf.shared.util.WSFacesUtil;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class JSFExtensionFactory implements ExtensionFactory {
    // Log instance for this class
    protected static final Logger log = Logger.getLogger("com.ibm.ws.jsf");
    private static final String CLASS_NAME = "com.ibm.ws.jsf.extprocessor.JSFExtensionFactory";

    private static final String FACES_SERVLET_RESOURCE = "javax/faces/webapp/FacesServlet.class";
    private static final String SUN_CONFIGURE_LISTENER_CLASSNAME = "com.sun.faces.config.ConfigureListener";
    private static final String MYFACES_LIFECYCLE_LISTENER_CLASSNAME = "org.apache.myfaces.webapp.StartupServletContextListener";
    private static final String sunRIClassToSearch = "com/sun/faces/vendor/WebSphereInjectionProvider.class";
    private URL defaultFacesServlet = null;
    private String applicationName = null;
    private static final String SUN_LISTENER_REGISTERED_STRING = "sunListenerRegistered";
    private static final String MYFACES_LISTENER_REGISTERED_STRING = "myfacesListenerRegistered";
    private static final String NO_LISTENER_REGISTERED_STRING = "noListenerFound";
    private JSFImplEnabled jsfImplEnabled = null;

    /** Reference for delayed activation of ClassLoadingService */
    private final AtomicReference<ClassLoadingService> classLoadingSRRef = new AtomicReference<ClassLoadingService>(null);

    private final static AtomicServiceReference<SerializationService> serializationServiceRef = new AtomicServiceReference<SerializationService>("serializationService");

    protected final AtomicServiceReference<CDIJSFInitializer> cdiJSFInitializerService = new AtomicServiceReference<CDIJSFInitializer>("cdiJSFInitializerService");

    private static final AtomicReference<JSFExtensionFactory> instance = new AtomicReference<JSFExtensionFactory>();

    public JSFExtensionFactory() {
        defaultFacesServlet = WSFacesUtil.getClassLoader(this).getResource(FACES_SERVLET_RESOURCE);
        if (log.isLoggable(Level.FINE)) {
            log.logp(Level.FINE, CLASS_NAME, "<clinit>", "defaultFacesServlet = " + defaultFacesServlet);
        }
    }

    @SuppressWarnings("unchecked")
    public void activate(ComponentContext compcontext, Map<String, Object> properties) {
        instance.set(this);
        this.serializationServiceRef.activate(compcontext);
        this.cdiJSFInitializerService.activate(compcontext);
    }

    public void deactivate(ComponentContext compcontext) {
        this.serializationServiceRef.deactivate(compcontext);
        this.cdiJSFInitializerService.deactivate(compcontext);
        instance.compareAndSet(this, null);
    }

    protected void setCdiJSFInitializerService(ServiceReference<CDIJSFInitializer> cdiJSFInitializerService) {
        this.cdiJSFInitializerService.setReference(cdiJSFInitializerService);
    }

    protected void unsetCdiJSFInitializerService(ServiceReference<CDIJSFInitializer> setCdiJSFInitializerService) {
        this.cdiJSFInitializerService.unsetReference(setCdiJSFInitializerService);
    }

    protected void setClassLoadingService(ClassLoadingService ref) {
        classLoadingSRRef.set(ref);
    }

    protected void unsetClassLoadingService(ClassLoadingService ref) {
        classLoadingSRRef.set(null);
    }

    protected void setSerializationService(ServiceReference<SerializationService> ref) {
        serializationServiceRef.setReference(ref);
    }

    protected void unsetSerializationService(ServiceReference<SerializationService> ref) {
        serializationServiceRef.unsetReference(ref);
    }

    public static SerializationService getSerializationService() {
        return serializationServiceRef.getService();
    }

    public ExtensionProcessor createExtensionProcessor(IServletContext webapp) throws Exception {

        applicationName = webapp.getServletContextName();

        if (log.isLoggable(Level.FINE)) {
            log.logp(Level.FINE, CLASS_NAME, "createExtensionProcessor", "Enter createExtensionProcessor(): webapp =[" + applicationName + "]");
        }

        WebAppConfig config = webapp.getWebAppConfig();

        if (!isFacesApp(config)) {
            webapp.setAttribute(JSFConstants.JSF_IMPL_ENABLED_PARAM, JSFImplEnabled.None);
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, "createExtensionProcessor", "Exit createExtensionProcessor(): JSF is not enabled for webapp =[" + applicationName + "]");
            }
            return null;
        }

        //initialize the JSF runtime based on the FacesServlet loaded in the webapp
        initFaces(webapp, config);

        //set a context attribute for later queries on which JSF impl is in use.
        webapp.setAttribute(JSFConstants.JSF_IMPL_ENABLED_PARAM, jsfImplEnabled);

        if (log.isLoggable(Level.FINE)) {
            log.logp(Level.FINE, CLASS_NAME, "createExtensionProcessor", "Exit createExtensionProcessor(): JSF is enabled for webapp: [" + applicationName
                                                                         + "] using implementation=[" + jsfImplEnabled + "]");
        }

        return new JSFExtensionProcessor(webapp);
    }

    private void initFaces(IServletContext webapp, WebAppConfig config) throws Exception {
        //get the webapp classloader and the URL of the faces servlet that is loaded for the webapp
        ClassLoader appClassLoader = WSFacesUtil.getContextClassLoader(webapp);
        ClassLoader ourClassLoader = JSFExtensionFactory.class.getClassLoader();
        URL webappFacesServlet = appClassLoader.getResource(FACES_SERVLET_RESOURCE);

        if (log.isLoggable(Level.FINE)) {
            log.logp(Level.FINE, CLASS_NAME, "initFaces", "webappFacesServlet equals " + webappFacesServlet.getPath());
            log.logp(Level.FINE, CLASS_NAME, "initFaces", "webappFacesServlet path equals " + webappFacesServlet.getPath());
        }
        boolean usingCustomJar = false;
        boolean usingDefaultSunRIJar = false;
        boolean usingDefaultMyFacesJar = false;
        //check whether a shared library was added.  If not, check for implementation specific classes
        if (!defaultFacesServlet.equals(webappFacesServlet)) {
            usingCustomJar = true;
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, "initFaces", "using a custom jar");
            }
        } else {
            if (appClassLoader.getResource(sunRIClassToSearch) != null) {
                usingDefaultSunRIJar = true;
                if (log.isLoggable(Level.FINE)) {
                    log.logp(Level.FINE, CLASS_NAME, "initFaces", "Using the sunRI runtime jar");
                }
            } else {
                usingDefaultMyFacesJar = true;
                if (log.isLoggable(Level.FINE)) {
                    log.logp(Level.FINE, CLASS_NAME, "initFaces", "Using the myFaces runtime jar");
                }
            }
        }

        //check for listeners registered in the web.xml
        String listenerFound = checkForListeners(config);

        //compare the resource URL for the webapp's facesServlet with that of the default MyFaces bundle from OSGI
        //if they are not equal then we are either using a customer supplied impl or the Sun RI
        if (usingCustomJar) {
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, "initFaces", "Webapp [" + applicationName + "] overrode default Faces Servlet. WebSphere Myfaces JSF config will be ignored.");
            }
            //handleJSPUpdateCheck only done in custom/sunRI case
            handleJSPUpdateCheck(webapp); //233952
            // Webapp is using a third party JSF runtime so do not load either the MyFaces startupServletContextListener or the Sun configureListener.
            //It could still be loaded from the Webapp so just do nothing here.
            jsfImplEnabled = JSFImplEnabled.Custom;
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, "initFaces", "Webapp [" + applicationName + "] is providing a third party JSF runtime.");
                log.logp(Level.FINE, CLASS_NAME, "initFaces", "FacesServlet URL path=[" + webappFacesServlet.getPath() + "]");
            }
        } else if (usingDefaultSunRIJar) {
            handleJSPUpdateCheck(webapp); //233952
            //Webapp is using the IBM-supplied Sun RI 1.2 bundle
            //programmatically add the listener if it is not already registered through the web.xml
            jsfImplEnabled = JSFImplEnabled.SunRI;
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, "initFaces", "Sun RI 1.2 JSF Implementation detected and will be used to configure the webapp for  [" + applicationName + "].");
            }
            if (!listenerFound.equals(SUN_LISTENER_REGISTERED_STRING)) {
                //provide a warning during startup if the MyFaces listener is also registered when using Sun RI
                if (listenerFound.equals(MYFACES_LISTENER_REGISTERED_STRING) && log.isLoggable(Level.WARNING)) {
                    log.logp(Level.WARNING, CLASS_NAME, "initFaces", FacesMessages.getMsg("jsf.warn.myfaces.listener.for.ri.app", new Object[] { applicationName }));
                }

                //initialize Sun RI context listener and register it with the webapp classloader
                boolean sunListenerInitialized = registerListener(webapp, SUN_CONFIGURE_LISTENER_CLASSNAME, appClassLoader);

                if (sunListenerInitialized) {
                    if (log.isLoggable(Level.FINE)) {
                        log.logp(Level.FINE, CLASS_NAME, "initFaces", "Sun RI 1.2 JSF Implementation listener is initialized for [" + applicationName + "].");
                    }
                    jsfImplEnabled = JSFImplEnabled.SunRI;
                } else {
                    //we should never actually get here anymore, assume a user-supplied impl but provide a warning
                    jsfImplEnabled = JSFImplEnabled.Custom;
                    if (log.isLoggable(Level.WARNING)) {
                        log.logp(Level.WARNING, CLASS_NAME, "initFaces", FacesMessages.getMsg("jsf.warn.ri.impl.not.initialized", new Object[] { applicationName }));
                    }
                }

            } else {
                //sun listener is registered via web.xml
                if (log.isLoggable(Level.FINE)) {
                    log.logp(Level.FINE, CLASS_NAME, "initFaces", "Sun RI 1.2 listener already configured, skipping listener registration for  [" + applicationName + "].");
                }
            }
        } else if (usingDefaultMyFacesJar) {
            //application is using the (default) MYFaces implementation
            jsfImplEnabled = JSFImplEnabled.MyFaces;
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, "initFaces", "MyFaces JSF Implementation detected and will be used to configure the webapp for  [" + applicationName + "].");
            }

            if (listenerFound.equals(MYFACES_LISTENER_REGISTERED_STRING)) {
                //myfaces listener already registered, don't need to do it here
                if (log.isLoggable(Level.FINE)) {
                    log.logp(Level.FINE, CLASS_NAME, "initFaces", "MyFaces listener already configured, skipping listener registration for  [" + applicationName + "].");
                }
            } else {
                // Add StartupServletContextListener to the web container so that we can initialize MyFaces.
                // As of WAS 8.0, this is the default behavior.

                //provide a warning during startup if the Sun configureListener is also registered when using MyFaces
                if (listenerFound.equals(SUN_LISTENER_REGISTERED_STRING) && log.isLoggable(Level.WARNING)) {
                    log.logp(Level.WARNING, CLASS_NAME, "initFaces", FacesMessages.getMsg("jsf.warn.ri.listener.for.myfaces.app", new Object[] { applicationName }));
                }

                //initialize MyFaces context listener and register it with the webapp classloader
                boolean myfacesListenerInitialized = registerListener(webapp, MYFACES_LIFECYCLE_LISTENER_CLASSNAME, ourClassLoader);

                if (myfacesListenerInitialized && log.isLoggable(Level.FINE)) {
                    log.logp(Level.FINE, CLASS_NAME, "initFaces", "MyFaces listener is initialized for [" + applicationName + "].");
                }
                if (!myfacesListenerInitialized && log.isLoggable(Level.WARNING)) {
                    log.logp(Level.WARNING, CLASS_NAME, "initFaces", FacesMessages.getMsg("jsf.warn.myfaces.not.initialized", new Object[] { applicationName }));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List getPatternList() {
        return Collections.EMPTY_LIST;
    }

    /*
     * attempts to register a webapp lifecycle listener for the application. returns true if registration successful, false if not
     */
    private boolean registerListener(IServletContext webapp, String listenerClassName, ClassLoader classLoader) {
        try {
            ServletContextListener listener = (ServletContextListener) Class.forName(listenerClassName, true, classLoader).newInstance();
            webapp.addLifecycleListener(listener);
        } catch (Exception e) {
            if (log.isLoggable(Level.WARNING)) {
                log.logp(Level.WARNING, CLASS_NAME, "registerListener", FacesMessages.getMsg("jsf.warn.exception.during.listener", new Object[] { listenerClassName,
                                                                                                                                                 applicationName }), e);
            }
            return false;
        }
        return true;
    }

    /*
     * check for configureListener or startupServletContextListener already configured from web.xml or via tld
     */
    @SuppressWarnings("unchecked")
    private String checkForListeners(WebAppConfig config) {
        String listenerFound = NO_LISTENER_REGISTERED_STRING;
        if (config != null) {
            List listeners = config.getListeners();
            for (Object listener : listeners) {
                String listenerClassname;

                if (listener instanceof String)
                    listenerClassname = (String) listener;
                else
                    continue; //skip this listener
                if (listenerClassname.equals(SUN_CONFIGURE_LISTENER_CLASSNAME)) {
                    listenerFound = SUN_LISTENER_REGISTERED_STRING;
                    break;
                } else if (listenerClassname.equals(MYFACES_LIFECYCLE_LISTENER_CLASSNAME)) {
                    listenerFound = MYFACES_LISTENER_REGISTERED_STRING;
                    break;
                }
            }
        }
        return listenerFound;
    }

    private boolean isFacesApp(WebAppConfig config) {
        // We need to make sure the descriptor actually defines FacesServlet.  Otherwise, JSF will be
        // turned on for every app (since the check used to be whether or not FacesServlet could be
        // loaded by the classloader, which is always true).
        for (Iterator<IServletConfig> servletConfigs = config.getServletInfos(); servletConfigs.hasNext();) {
            String servletClass = servletConfigs.next().getClassName();
            if ("javax.faces.webapp.FacesServlet".equals(servletClass)
                    || "org.apache.myfaces.webapp.MyFacesServlet".equals(servletClass)) {
                return true;
            }
        }
        return false;
    }

    private void handleJSPUpdateCheck(IServletContext webapp) {
        // begin 233952: JSP_UPATE_CHECK fails for non-jsp requests.
        //      1) only create FacesConfig if webmodule did not override FacesServlet.
        //      2) If JSP_UPDATE_CHECK, obtain the URI mappings associated with JSP container.
        String jspUpdateCheckEnabled = webapp.getInitParameter(JSFConstants.JSP_UPDATE_CHECK);
        if (jspUpdateCheckEnabled != null && jspUpdateCheckEnabled.equalsIgnoreCase("TRUE")) {
            JspURIMatcher matcher = new JspURIMatcher(webapp);
            webapp.setAttribute(JSFConstants.JSP_URI_MATCHER, matcher);
        }

        // end 233952: JSP_UPATE_CHECK fails for non-jsp requests.
    }

    public static void initializeCDIJSFELContextListenerAndELResolver(Application application){
        JSFExtensionFactory factory = instance.get();
        if(factory != null){
            if(factory.cdiJSFInitializerService != null){
                CDIJSFInitializer cdiInitializer = factory.cdiJSFInitializerService.getService();
                if(cdiInitializer != null){
                    if (log.isLoggable(Level.FINE)) {
                        log.logp(Level.FINE, CLASS_NAME, "initializeCDIJSFELContextListenerAndELResolver", "Initializing app ELContextListener and ELResolver");
                    }
                    cdiInitializer.initializeCDIJSFELContextListenerAndELResolver(application);
                }
            }
        }
    }

    public static void initializeCDIJSFViewHandler(Application application){
        JSFExtensionFactory factory = instance.get();
        if(factory != null){
            if(factory.cdiJSFInitializerService != null){
                CDIJSFInitializer cdiInitializer = factory.cdiJSFInitializerService.getService();
                if(cdiInitializer != null){
                    if (log.isLoggable(Level.FINE)) {
                        log.logp(Level.FINE, CLASS_NAME, "initializeCDIJSFViewHandler", "Initializing app ViewHandler");
                    }
                    cdiInitializer.initializeCDIJSFViewHandler(application);
                }
            }
        }
    }
}
