/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.couchdb.internal;

import java.beans.IntrospectionException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * A declarative services component can be completely POJO based (no awareness/use of OSGi
 * services).
 * 
 * OSGi methods (activate/deactivate) should be protected.
 */
public class CouchDbService implements ResourceFactory, ApplicationRecycleComponent {
     private static final TraceComponent tc = Tr.register(CouchDbService.class, "couchdb");

     private static final String COUCHDB_CLIENT_OPTIONS_BUILDER_CLS_STR = "org.ektorp.http.StdHttpClient$Builder";
     private static final String COUCHDB_CLIENT_CLS_STR = "org.ektorp.impl.StdCouchDbInstance";

     /**
      * Name of the unique identifier property
      */
     static final String CONFIG_ID = "config.displayId";

     /**
      * Unique identifier for this couchdb instance.
      */
     private String id;

     /**
      * Config element alias: couchdb.
      */
     static final String COUCHDB = "couchdb";

     /**
      * Reference to the shared library that contains the ektorp couchDB driver.
      */
     private final AtomicServiceReference<Library> libraryRef = new AtomicServiceReference<Library>("library");

     /**
      * org.ektorp.CouchDbInstance instance
      */
     private Object couchdb;

     /**
      * Names of applications using this ResourceFactory
      */
     private final Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

     /**
      * Reference to the ApplicationRecycleCoordinator (if any) that is configured for this
      * connection factory.
      */
     private final AtomicServiceReference<ApplicationRecycleCoordinator> appRecycleSvcRef =
          new AtomicServiceReference<ApplicationRecycleCoordinator>(APP_RECYCLE_SERVICE);

     /**
      * Name of reference to the ApplicationRecycleCoordinator
      */
     private static final String APP_RECYCLE_SERVICE = "appRecycleService";

     /**
      * Mapping of property name to value value type. This is used to find the correct 'setter'
      * method on org.ektorp.http.StdHttpClient$Builder.
      */
     private static final Map<String, Class<?>> COUCHDB_CLIENT_OPTIONS_TYPES;
     static {
          COUCHDB_CLIENT_OPTIONS_TYPES = new HashMap<String, Class<?>>();
          COUCHDB_CLIENT_OPTIONS_TYPES.put("url", String.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("host", String.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("port", int.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("username", String.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("password", String.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("maxConnections", int.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("connectionTimeout", int.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("socketTimeout", int.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("enableSSL", boolean.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("relaxedSSLSettings", boolean.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("caching", boolean.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("maxCacheEntries", int.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("maxObjectSizeBytes", int.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("useExpectContinue", boolean.class);
          COUCHDB_CLIENT_OPTIONS_TYPES.put("cleanupIdleConnections", boolean.class);
     }

     /**
      * Config properties.
      */
     private Map<String, Object> props;

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
          libraryRef.activate(context);
          this.props = props;
          id = (String) props.get(CONFIG_ID);
          appRecycleSvcRef.activate(context);
     }

     /**
      * DS method to deactivate this component. Best practice: this should be a protected method,
      * not public or private
      * 
      * @param context
      *             DeclarativeService defined/populated component context
      */
     protected void deactivate(ComponentContext context) {
          libraryRef.deactivate(context);
          appRecycleSvcRef.deactivate(context);

     }

     /** {@inheritDoc} */
     @Override
     public Object createResource(ResourceInfo info) throws Exception {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
               Tr.debug(this, tc, "createResource");

          Object dbInstance = getDBInstance();

          ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
          if (cData != null)
               applications.add(cData.getJ2EEName().getApplication());

          return dbInstance;
     }

     /**
      * Get a CouchDbInstance instance built with provided user/pass and other configuration options
      * 
      * @return org.ektorp.impl.StdCouchDbInstance instance
      * @throws Exception
      *              if an error occurs.
      */
     Object getDBInstance() throws Exception {
          try {
               // Get classloader from ektorp lib -- should we ensure it is the right version here?
               ClassLoader loader = libraryRef.getServiceWithException().getClassLoader();
               Class<?> couchDbClientOptionsBuilderCls = loader.loadClass(COUCHDB_CLIENT_OPTIONS_BUILDER_CLS_STR);
               Object builderInstance = couchDbClientOptionsBuilderCls.newInstance();
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc,
                         "creating a org.ektorp.impl.StdCouchDbInstance instance with " + props.toString());
               }
               // Parse through properties in server.xml and set them to builderInstance
               for (Map.Entry<String, Object> prop : props.entrySet()) {
                    String name = prop.getKey();
                    Object value = prop.getValue();
                    if (value != null && name.indexOf('.') < 0 && COUCHDB_CLIENT_OPTIONS_TYPES.containsKey(name))
                         set(couchDbClientOptionsBuilderCls, builderInstance, name, value);
               }

               Class<?> couchDbClientCls = loader.loadClass(COUCHDB_CLIENT_CLS_STR);
               Class<?> httpClientCls = loader.loadClass("org.ektorp.http.HttpClient");
               Constructor<?> constructor = couchDbClientCls.getConstructor(httpClientCls);
               Method builderBuildMethod = couchDbClientOptionsBuilderCls.getMethod("build");
               // Invoke the build method of the builder class to create a HttpClient.
               // This HttpClient is then used as a parameter for creating the CouchDbInstance
               couchdb = constructor.newInstance(builderBuildMethod.invoke(builderInstance));
               return couchdb;
          } catch (Throwable x) {
               // rethrowing the exception allows it to be captured in FFDC and traced automatically
               x = x instanceof InvocationTargetException ? x.getCause() : x;
               if (x instanceof Exception)
                    throw (Exception) x;
               else if (x instanceof Error)
                    throw (Error) x;
               else
                    throw new RuntimeException(x);
          }
     }

     /**
      * Configure a couchdb option.
      * 
      * @param builder
      *             class org.ektorp.http.StdHttpClient$Builder
      * @param builder
      *             instance org.ektorp.http.StdHttpClient$Builder
      * @param propName
      *             name of the config property
      * @param value
      *             value of the config property.
      */
     @Trivial
     private void set(Class<?> builderCls, Object builder, String propName, Object value)
          throws IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
          try {
               Class<?> cls = COUCHDB_CLIENT_OPTIONS_TYPES.get(propName);
               // setter methods are just propName, not setPropName.
               Method method = builderCls.getMethod(propName, cls);
               // even though we told the config service that some of these props are Integers, they
               // get converted to longs. Need
               // to convert them back to int so that our .invoke(..) method doesn't blow up.
               if (cls.equals(int.class) && value instanceof Long) {
                    value = ((Long) value).intValue();
               }
               if (propName.equals("password")) {
                    SerializableProtectedString password = (SerializableProtectedString) value;
                    String pwdStr = password == null ? null : String.valueOf(password.getChars());
                    value = PasswordUtil.getCryptoAlgorithm(pwdStr) == null ? pwdStr : PasswordUtil.decode(pwdStr);
               }
               method.invoke(builder, value);
               return;

          } catch (Throwable x) {
               if (x instanceof InvocationTargetException)
                    x = x.getCause();
               IllegalArgumentException failure =
                    ignoreWarnOrFail(x, IllegalArgumentException.class, "CWKKD0010.prop.error", propName, COUCHDB, id,
                         x);
               if (failure != null) {
                    FFDCFilter.processException(failure, getClass().getName(), "394", this, new Object[] {
                         value == null ? null : value.getClass(), value });
                    throw failure;
               }
          }
     }

     /**
      * Declarative Services method for setting the shared library service reference
      * 
      * @param ref
      *             reference to the service
      */
     protected void setLibrary(ServiceReference<Library> ref) {
          libraryRef.setReference(ref);
     }

     /**
      * Declarative Services method for unsetting the shared library service reference
      * 
      * @param ref
      *             reference to the service
      */
     protected void unsetLibrary(ServiceReference<Library> ref) {
          libraryRef.unsetReference(ref);
     }

     /**
      * Ignore, warn, or fail when a configuration error occurs. This is copied from Tim's code in
      * tWAS and updated slightly to override with the Liberty ignore/warn/fail setting.
      * 
      * @param throwable
      *             an already created Throwable object, which can be used if the desired action is
      *             fail.
      * @param exceptionClassToRaise
      *             the class of the Throwable object to return
      * @param msgKey
      *             the NLS message key
      * @param objs
      *             list of objects to substitute in the NLS message
      * @return either null or the Throwable object
      */
     private <T extends Throwable> T ignoreWarnOrFail(Throwable throwable, Class<T> exceptionClassToRaise,
          String msgKey, Object... objs) {

          // Read the value each time in order to allow for changes to the onError setting
          switch ((OnError) props.get(OnErrorUtil.CFG_KEY_ON_ERROR)) {
          case IGNORE:
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "ignoring error: " + msgKey, objs);
               return null;
          case WARN:
               Tr.warning(tc, msgKey, objs);
               return null;
          case FAIL:
               try {
                    if (throwable != null && exceptionClassToRaise.isInstance(throwable))
                         return exceptionClassToRaise.cast(throwable);

                    Constructor<T> con = exceptionClassToRaise.getConstructor(String.class);
                    String message = msgKey == null ? throwable.getMessage() : Utils.getMessage(msgKey, objs);
                    T failure = con.newInstance(message);
                    failure.initCause(throwable);
                    return failure;
               } catch (RuntimeException e) {
                    throw e;
               } catch (Exception e) {
                    throw new RuntimeException(e);
               }
          }

          return null;
     }

     @Override
     public ApplicationRecycleContext getContext() {
          return null;
     }

     @Override
     public Set<String> getDependentApplications() {
          Set<String> members = new HashSet<String>(applications);
          applications.removeAll(members);
          return members;
     }

     /**
      * Declarative Services method for setting the ApplicationRecycleCoordinator service
      * 
      * @param the
      *             service
      */
     protected void setAppRecycleService(ServiceReference<ApplicationRecycleCoordinator> svc) {
          appRecycleSvcRef.setReference(svc);
     }

     /**
      * Declarative Services method for unsetting the ApplicationRecycleCoordinator service
      * reference
      * 
      * @param ref
      *             reference to the service
      */
     protected void unsetAppRecycleService(ServiceReference<ApplicationRecycleCoordinator> ref) {
          appRecycleSvcRef.unsetReference(ref);
     }

}
