/*******************************************************************************
 * Copyright (c) 2016,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cloudant.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.auth.data.AuthData;
import com.ibm.websphere.security.auth.data.AuthDataProvider;
import com.ibm.ws.classloading.ClassLoaderIdentifierService;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.library.LibraryChangeListener;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

public class CloudantService implements ApplicationRecycleComponent, LibraryChangeListener, ResourceFactory 
{
    private static final String URL = "url"; // cloudant client property

    private static final String ACCOUNT = "account"; // cloudant client property

    private static final String PROXY_PASSWORD = "proxyPassword"; // cloudant client property

    private static final String USERNAME = "username"; // cloudant client property

    private static final String PROXY_USER = "proxyUser"; // cloudant client property

    private static final String PROXY_URL = "proxyURL"; // cloudant client property

    private static final String READ_TIMEOUT = "readTimeout"; // cloudant client property

    private static final String PASSWORD = "password"; // property name for auth data password attribute and a cloudant client property

    private static final String MAX_CONNECTIONS = "maxConnections"; // cloudant client property

    private static final String CONNECT_TIMEOUT = "connectTimeout"; // cloudant client property

    private static final String SSL_SOCKET_FACTORY = "customSSLSocketFactory"; // cloudant client property
    
    private static final String DISABLE_SSL_AUTHENTICATION = "disableSSLAuthentication"; // cloudant client property

    private static final TraceComponent tc = Tr.register(CloudantService.class, "cloudant", "com.ibm.ws.cloudant.internal.resources.Messages");

    private static final String CLOUDANT_CLIENT_OPTIONS_BUILDER_CLS_STR = "com.cloudant.client.api.ClientBuilder";

    private static final String AUTHENTICATION_ALIAS_LOGIN_NAME = "DefaultPrincipalMapping";

    /**
     * Clears out Cloudant Client caches when applications are stopped in order to avoid memory leaks.
     */
    private CloudantApplicationListener appListener;

    /**
     * Class loader identifier service.
     */
    private ClassLoaderIdentifierService classLoaderIdSvc;

    /**
     * Name of the unique identifier property
     */
    private static final String CONFIG_ID = "config.displayId";

    /**
     * Cached CloudantClient instances.
     */
    private final ConcurrentMap<ClientKey, Object> clients = new ConcurrentHashMap<ClientKey, Object>();

    /**
     * Component context for this OSGi service component. 
     */
    private ComponentContext componentContext;

    /**
     * Service reference to the default container managed auth alias (if any).
     */
    private final AtomicServiceReference<AuthData> containerAuthDataRef = new AtomicServiceReference<AuthData>("containerAuthData");

    /**
     * Shared library that contains the cloudant driver (if a library is configured for it).
     */
    private Library library;

    /**
     * Indicates if Cloudant library should be loaded from the application's thread context class loader.
     */
    private boolean loadFromApp;

    /**
     * Reference to the SSL configuration (if any)
     */
    private final AtomicServiceReference<Object> sslConfig = new AtomicServiceReference<Object>("ssl");

    /**
     * Names of applications using this ResourceFactory
     */
    private final Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Config properties.
     */
    private Map<String, Object> props;
    
    /**
     * String identifier, either jndi name or config id, for display to user in err messages to make it easier
     * to find the cloudant config that is in error
     */
    private String cloudantConfigIdentifier;

    /**
     * Reusable helper class for invoking new URL as a privileged action. 
     */
    @Trivial
    private static class ConstructURLAction implements PrivilegedExceptionAction<URL> {
        private final String url;

        private ConstructURLAction(String url) {
            this.url = url;
        }

        @Override
        public URL run() throws MalformedURLException {
            return new URL(url);
        }
    }
    
    /**
     * DS method to activate this component. Best practice: this should be a protected method, not
     * public or private
     * 
     * @param context
     *             DeclarativeService defined/populated component context
     * @param props
     *             DeclarativeService defined/populated map of service properties
     */
    protected void activate(ComponentContext context, Map<String, Object> props) {
        componentContext = context;
        containerAuthDataRef.activate(context);
        sslConfig.activate(context);
        this.props = props;

        cloudantConfigIdentifier = (String) props.get(JNDI_NAME);
        if (cloudantConfigIdentifier == null || "".equals(cloudantConfigIdentifier)) {
            cloudantConfigIdentifier = (String) props.get(CONFIG_ID);
        }

        // TODO when libraryRef is made optional, switch to library == null and get rid of this field
        loadFromApp = Boolean.parseBoolean((String) props.get("ibm.internal.nonship.function"))
                && "ibm.internal.simulate.no.library.do.not.ship".equals(library.id());
    }

    /**
     * DS method to deactivate this component. Best practice: this should be a protected method,
     * not public or private
     * 
     * @param context
     *             DeclarativeService defined/populated component context
     */
    protected void deactivate(ComponentContext context) {
        appListener.unregister(this);

        // shut down instances that were created
        if (!clients.isEmpty())
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    Method shutdown = null;
                    for (Object client : clients.values())
                        try {
                            if (shutdown == null)
                                shutdown = client.getClass().getMethod("shutdown");
                            shutdown.invoke(client);
                        } catch (Throwable x) {
                            // FFDC will be logged, otherwise ignore
                        }
                    return null;
                }
            });

        containerAuthDataRef.deactivate(context);
        sslConfig.deactivate(context);
    }

    /** {@inheritDoc} */
    @Override
    public Object createResource(ResourceInfo info) throws Exception {
        if (info == null)
            throw new UnsupportedOperationException(Tr.formatMessage(tc, "direct.lookup.CWWKD0301E", cloudantConfigIdentifier));

        return createResource(
                null,
                false,
                info.getAuth(),
                info.getLoginPropertyList());
    }

    /**
     * Create an instance of Cloudant ClientBuilder or Database.
     * 
     * @param databaseName name of the Cloudant database. Null if the type is ClientBuilder.
     * @param createDatabase true if the type is Database and Cloudant should autocreate the database.
     * @param resAuth resource reference authentication type (ResourceInfo.CONTAINER or ResourceInfo.APPLICATION)
     * @param loginPropertyList custom login properties
     * @return com.cloudant.client.api.ClientBuilder or com.cloudant.client.api.Database
     * @throws Exception
     */
    @FFDCIgnore(value = {InvocationTargetException.class, PrivilegedActionException.class })
    Object createResource(String databaseName, boolean createDatabase, int resAuth, List<? extends ResourceInfo.Property> loginPropertyList) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        try {
            ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            if (cData != null)
                applications.add(cData.getJ2EEName().getApplication());
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, cloudantConfigIdentifier + " accessed by " + cData);

            AuthData containerAuthData = null;
            if (resAuth == ResourceInfo.AUTH_CONTAINER) {
                // look at resource info to see if auth alias specified in bindings via the login configuration properties
                if (!loginPropertyList.isEmpty()) {
                    for (ResourceInfo.Property property : loginPropertyList) {
                        if (property.getName().equals(AUTHENTICATION_ALIAS_LOGIN_NAME)) {
                            String authAliasName = property.getValue();
                            containerAuthData = AuthDataProvider.getAuthData(authAliasName);
                            if (trace && tc.isDebugEnabled())
                                Tr.debug(this, tc, "resource ref container auth alias " + authAliasName, containerAuthData);
                            break;
                        }
                    }
                }
                // fall back to default container auth alias, if any
                if (containerAuthData == null) {
                    containerAuthData = containerAuthDataRef.getService();
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "default container auth " + containerAuthData);
                }
            }

            String userName = containerAuthData == null ? (String) props.get(USERNAME) : containerAuthData.getUserName();
            String password = null;
            if (containerAuthData == null) {
                SerializableProtectedString protectedPwd = (SerializableProtectedString) props.get(PASSWORD);
                if (protectedPwd != null) {
                    password = String.valueOf(protectedPwd.getChars());
                    password = PasswordUtil.getCryptoAlgorithm(password) == null ? password : PasswordUtil.decode(password);
                }
            } else
                password = String.valueOf(containerAuthData.getPassword());

            ClassLoader appClassLoader;
            String appClassLoaderId;
            if (loadFromApp) {
                if (System.getSecurityManager() == null) {
                    appClassLoader = Thread.currentThread().getContextClassLoader();
                } else {
                    appClassLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                        @Override
                        public ClassLoader run() {
                            return Thread.currentThread().getContextClassLoader();
                        }
                    });
                }
                if (appClassLoader == null)
                    throw new ClassNotFoundException(Tr.formatMessage(tc, "class.not.found.CWWKD0302E", CLOUDANT_CLIENT_OPTIONS_BUILDER_CLS_STR, cloudantConfigIdentifier));
                else {
                    appClassLoaderId = classLoaderIdSvc.getClassLoaderIdentifier(appClassLoader);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "classloader identifier", appClassLoaderId, appClassLoader);
                    if (appClassLoaderId == null)
                        throw new ClassNotFoundException(Tr.formatMessage(tc, "class.not.found.CWWKD0302E", CLOUDANT_CLIENT_OPTIONS_BUILDER_CLS_STR, cloudantConfigIdentifier));
                }
            } else {
                appClassLoader = null;
                appClassLoaderId = null;
            }

            if (databaseName == null)
                return createClientBuilder(appClassLoader, userName, password);

            final ClientKey key = new ClientKey(appClassLoaderId, userName, password);
            Object client = clients.get(key);
            if (client == null) {
                final Object builder = createClientBuilder(appClassLoader, userName, password);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "creating Cloudant client with " + builder);

                // Invoke the build method of the builder class to create a CloudantClient.
                // which is then used to create the cloudant db
                client = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    @Trivial
                    public Object run() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
                        Object newClient = builder.getClass().getMethod("build").invoke(builder);
                        Object existingClient = clients.putIfAbsent(key, newClient);
                        if (existingClient == null) {
                            // Not found in map, which means the new instance was added
                            if (key.getApplicationClassLoaderIdentifier() != null)
                                appListener.register(CloudantService.this, clients); // to avoid leaking the classloader when app stops
                            return newClient;
                        } else {
                            // Found existing instance, so discard and shut down the instance we optimistically created
                            newClient.getClass().getMethod("shutdown").invoke(newClient);
                            return existingClient;
                        }
                    }
                });
            }

            return client.getClass()
                    .getMethod("database", String.class, boolean.class)
                    .invoke(client, databaseName, createDatabase);
        } catch (InvocationTargetException x) {
            Throwable cause = x.getCause();
            FFDCFilter.processException(cause, getClass().getName(), "224", this, new Object[] { createDatabase, databaseName, resAuth, loginPropertyList });
            if (cause instanceof Exception)
                throw (Exception) cause;
            else if (cause instanceof Error)
                throw (Error) cause;
            else
                throw x;
        } catch (PrivilegedActionException x) {
            Throwable cause = x.getCause();
            if (cause instanceof InvocationTargetException)
                cause = cause.getCause();
            FFDCFilter.processException(cause, getClass().getName(), "228", this, new Object[] { createDatabase, databaseName, resAuth, loginPropertyList });
            if (cause instanceof Exception)
                throw (Exception) cause;
            else if (cause instanceof Error)
                throw (Error) cause;
            else
                throw x;
        } catch (Exception x) {
            // rethrowing the exception allows it to be captured in FFDC
            throw x;
        } catch (Error x) {
            // rethrowing the exception allows it to be captured in FFDC
            throw x;
        }
    }

    /**
     * Get a Cloudant builder with provided user/pass and other configuration options
     * From the javadoc: Instances of CloudantClient are created using a ClientBuilder. Once
     * created a CloudantClient is immutable and safe to access from multiple threads.
     * @param loader thread context class loader of application, if driver is to be loaded this way. Otherwise null.
     * @param userName user name, possibly null
     * @param password password, possibly null
     * @return com.cloudant.client.api.CloudantClient instance, or com.cloudant.client.api.ClientBuilder, as determined by the ResourceInfo type
     */
    private Object createClientBuilder(ClassLoader loader, String userName, @Sensitive String password) throws Exception {
        if (loader == null)
            loader = library.getClassLoader();

        Class<?> cloudantClientOptionsBuilderCls = loader.loadClass(CLOUDANT_CLIENT_OPTIONS_BUILDER_CLS_STR);
        // create the builder
        Object builderInstance = newInstance(cloudantClientOptionsBuilderCls);

        if (userName != null)
            set(cloudantClientOptionsBuilderCls, builderInstance, USERNAME, String.class, userName);
        if (password != null)
            set(cloudantClientOptionsBuilderCls, builderInstance, PASSWORD, String.class, password);

        Integer maxConn = (Integer)props.get(MAX_CONNECTIONS);
        set(cloudantClientOptionsBuilderCls, builderInstance, MAX_CONNECTIONS, int.class, maxConn);

        Long connectTimeout = (Long)props.get(CONNECT_TIMEOUT);
        set(cloudantClientOptionsBuilderCls, builderInstance, CONNECT_TIMEOUT, long.class, connectTimeout);

        Long readTimeout = (Long)props.get(READ_TIMEOUT);
        set(cloudantClientOptionsBuilderCls, builderInstance, READ_TIMEOUT, long.class, readTimeout);

        String proxyUrl = (String)props.get(PROXY_URL);
        if (proxyUrl != null) {
            set(cloudantClientOptionsBuilderCls, builderInstance, PROXY_URL, URL.class, AccessController.doPrivileged(new ConstructURLAction(proxyUrl)));
        }

        String proxyUser = (String)props.get(PROXY_USER);
        if (proxyUser != null) {
            set(cloudantClientOptionsBuilderCls, builderInstance, PROXY_USER, String.class, proxyUser);
        }

        SerializableProtectedString proxyPass = (SerializableProtectedString)props.get(PROXY_PASSWORD);
        if (proxyPass != null) {
            String pwdStr = String.valueOf(proxyPass.getChars());
            pwdStr = PasswordUtil.getCryptoAlgorithm(pwdStr) == null ? pwdStr : PasswordUtil.decode(pwdStr);
            set(cloudantClientOptionsBuilderCls, builderInstance, PROXY_PASSWORD, String.class, pwdStr);
        }

        Boolean disableSSLAuthentication = (Boolean)props.get(DISABLE_SSL_AUTHENTICATION);
        if (disableSSLAuthentication) {
            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "disableSSLAuthentication()");
            cloudantClientOptionsBuilderCls.getMethod(DISABLE_SSL_AUTHENTICATION).invoke(builderInstance); 
        } else {
            SSLSocketFactory sslSF = getSSLSocketFactory();
            if (sslSF != null) {
                set(cloudantClientOptionsBuilderCls, builderInstance, SSL_SOCKET_FACTORY, SSLSocketFactory.class, sslSF);
            }
        }

        return builderInstance;
    }

    @Override
    public ApplicationRecycleContext getContext() {
        return null;
    }

    /**
     * Returns the list of applications that have used this resource so that the applications can be
     * stopped by the application recycle code in response to a configuration change.
     */
    @Override
    public Set<String> getDependentApplications() {
        Set<String> appsToStop = new HashSet<String>(applications);
        applications.clear();
        return appsToStop;
    }

    private Object newInstance(Class<?> clazz) throws Exception {
        // based on which properties are set, decide which method to use to create a ClientBuilder
        // as there is no public default constructor, you must decide up front whether to create a ClientBuilder
        // based on a url or account.  If both are specified, account will take precedence
        Object clientBuilder = null;
        String account = (String)props.get(ACCOUNT);
        if (account != null && account.length() > 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "creating Cloudant client based on account:" + account);
            }
            Method m = clazz.getMethod(ACCOUNT, String.class);
            clientBuilder = m.invoke(null, account);
        } else {
            String url = (String)props.get(URL);
            if (url != null && url.length() > 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "creating Cloudant ClientBuilder based on url:" + url);
                }
                Method m = clazz.getMethod(URL, URL.class);
                clientBuilder = m.invoke(null, AccessController.doPrivileged(new ConstructURLAction(url)));
            } else // one of either an account or url must be specified
                throw new IllegalArgumentException(Tr.formatMessage(tc, "error.cloudant.config.CWWKD0300E", new Object[] {cloudantConfigIdentifier}));
        }
        return clientBuilder;
    }

    /**
     * Method to reflectively invoke setters on the cloudant client builder 
     * @param clazz client builder class
     * @param clientBuilder builder instance
     * @param name method to invoke
     * @param type argument type with which to invoke method
     * @param value argument value with which to invoke method, sensitive as password will be passed
     * @throws Exception if reflection methods fail
     */
    @Trivial // do our own trace in order to selectively include the value
    private <T extends Object> void set(Class<?> clazz, Object clientBuilder, String name, Class<T> type, T value) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc,  name + '(' +  (name.endsWith("ssword") ? "***" : value) + ')');
        Method m;
        // timeout properties take an additional parm for time unit
        if ((type == long.class) && ((READ_TIMEOUT.equals(name) || CONNECT_TIMEOUT.equals(name)))) {
            m = clazz.getMethod(name, type, TimeUnit.class);
            m.invoke(clientBuilder, value, TimeUnit.MILLISECONDS);
        } else {
            m = clazz.getMethod(name, type);
            m.invoke(clientBuilder, value);
        }
    }

    /**
     * Received when library is changed, for example by altering the files in the library.
     */
    @Override
    public void libraryNotification() {
        // Notify the application recycle coordinator of an incompatible change that requires restarting the application
        if (!applications.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "recycle applications", applications);
            ApplicationRecycleCoordinator appRecycleCoord = (ApplicationRecycleCoordinator) componentContext.locateService("appRecycleCoordinator");
            Set<String> members = new HashSet<String>(applications);
            applications.removeAll(members);
            appRecycleCoord.recycleApplications(members);
        }
    }
    
    /**
     * Get an ssl socket factory, if configured
     */
    private SSLSocketFactory getSSLSocketFactory() throws Exception {
        SSLSocketFactory sslSF = null;
        Object sslConfig = this.sslConfig.getService();
        if(sslConfig != null) {
            // Reflectively invoke this operation to a helper class because the classes needed to perform
            // this operation are dynamically imported, depending on if the ssl-1.0 feature is enabled.
            Class<?> SSLHelper = Class.forName("com.ibm.ws.cloudant.internal.SSLHelper");
            sslSF = (SSLSocketFactory) SSLHelper.getMethod("getSSLSocketFactory", Object.class).invoke(null, sslConfig);
        }
        return sslSF;
    }

    // Declare dependency on application recycle coordinator so that it is available to our component context,
    // but don't do anything with it until we actually need it
    protected void setAppRecycleCoordinator(ServiceReference<ApplicationRecycleCoordinator> ref) {
    }

    protected void unsetAppRecycleCoordinator(ServiceReference<ApplicationRecycleCoordinator> ref) {
    }

    protected void setAppListener(ApplicationStateListener svc) {
        appListener = (CloudantApplicationListener) svc;
    }

    protected void unsetAppListener(ApplicationStateListener svc) {
        appListener = null;
    }

    protected void setClassLoaderIdentifier(ClassLoaderIdentifierService svc) {
        classLoaderIdSvc = svc;
    }

    protected void unsetClassLoaderIdentifier(ClassLoaderIdentifierService svc) {
        classLoaderIdSvc = null;
    }

    /**
     * Declarative Services method for setting the service reference for the default container auth data
     * 
     * @param ref reference to the service
     */
    protected void setContainerAuthData(ServiceReference<AuthData> ref) {
        containerAuthDataRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the service reference for the default container auth data
     * 
     * @param ref reference to the service
     */
    protected void unsetContainerAuthData(ServiceReference<AuthData> ref) {
        containerAuthDataRef.unsetReference(ref);
    }

    protected void setLibrary(Library svc) {
        library = svc;
    }

    protected void unsetLibrary(Library svc) {
        library = null;
    }

    protected void setSsl(ServiceReference<Object> ref) { // com.ibm.wsspi.ssl.SSLConfiguration
        sslConfig.setReference(ref);
    }
    
    protected void unsetSsl(ServiceReference<Object> ref) {
        sslConfig.unsetReference(ref);
    }
}
