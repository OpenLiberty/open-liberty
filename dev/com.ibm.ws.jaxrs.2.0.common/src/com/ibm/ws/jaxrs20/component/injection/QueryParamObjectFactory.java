/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs20.component.injection;

import static com.ibm.ws.jaxrs20.internal.JaxRsCommonConstants.TR_GROUP;
import static com.ibm.ws.jaxrs20.internal.JaxRsCommonConstants.TR_RESOURCE_BUNDLE;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.ws.rs.core.Application;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.injection.ApplicationInjectionProxy;
import com.ibm.ws.jaxrs20.injection.InjectionRuntimeContextHelper;
import com.ibm.ws.jaxrs20.injection.metadata.InjectionRuntimeContext;

/**
 * Object factory to inject a proxy instance for fields annotated with @Context.
 * The proxy will get the actual Object off the RuntimeContext.
 */
@Component(name = "com.ibm.ws.jaxrs20.component.injection.QueryParamObjectFactory", service = javax.naming.spi.ObjectFactory.class, property = { "service.vendor=IBM" })
public class QueryParamObjectFactory implements ObjectFactory {

    private static final TraceComponent tc = Tr.register(QueryParamObjectFactory.class, TR_GROUP, TR_RESOURCE_BUNDLE);

    private Dictionary<String, Object> props = null;

    /*
     * (non-Javadoc)
     *
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context, java.util.Hashtable)
     *
     * Create the proxy instance.
     */
    @Override
    public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> envmt) throws Exception {

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "getObjectInstance: o=" + o + ",name=" + n + ",context=" + c + ",evnmt=" + envmt);
        }

        if (!(o instanceof Reference)) {
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "Object " + o + " is not an instance of javax.naming.Reference. Returning null.");
            }
            return null;
        }
        Reference ref = (Reference) o;
        final Class<?> contextClass = Class.forName(ref.getClassName());
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Creating a proxy for type {0}", contextClass);
        Object proxy = null;

        if (Application.class.equals(contextClass)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "This is an Application sub-class being injected. Injecting an instance of ApplicationInjectionProxy.");
            }
            proxy = new ApplicationInjectionProxy();
        } else {

            proxy = Proxy.newProxyInstance(ContextObjectFactory.priv.getClassLoader(contextClass),
                                           new Class[] { contextClass },
                                           new InvocationHandler() {
                                               @Override
                                               public Object invoke(Object proxy,
                                                                    Method method,
                                                                    Object[] args) throws Throwable {
                                                   // Without these entry and exit calls RAS uses bytecode injection
                                                   // to insert Tr.entry on first line of this method and then Tr.exit
                                                   // on last lie. Tr.entry attempts to call proxy.toString(), which
                                                   // loops back to the invoke method again--hence resulting in a
                                                   // StackOverflowError
                                                   if (tc.isEntryEnabled()) {
                                                       Tr.entry(tc, "invoke");
                                                   }

                                                   Object result = null;
                                                   // use runtimeContext from TLS
                                                   InjectionRuntimeContext runtimeContext = InjectionRuntimeContextHelper.getRuntimeContext();

                                                   if ("toString".equals(method.getName()) && (method.getParameterTypes().length == 0)) {
                                                       result = "Proxy for " + contextClass.getName();
                                                       if (tc.isEntryEnabled()) {
                                                           Tr.exit(tc, "invoke", result);
                                                       }
                                                       return result;
                                                   }

                                                   // get the real context from the RuntimeContext
                                                   Object context = runtimeContext.getRuntimeCtxObject(contextClass.getName());
                                                   if (context != null) {
                                                       result = method.invoke(context, args);
                                                   }

                                                   if (tc.isEntryEnabled()) {
                                                       Tr.exit(tc, "invoke", result);
                                                   }
                                                   // invoke the method on the real context
                                                   return result;
                                               }
                                           });
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "getObjectInstance", proxy);
        }

        return proxy;

    }

    /*
     * Called by DS to activate service
     */
//    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext cc) {
        props = cc.getProperties();
    }

    /*
     * Called by DS to modify service config properties
     */
//    @SuppressWarnings("unchecked")
    @Modified
    protected void modified(Map<?, ?> newProperties) {
        if (newProperties instanceof Dictionary) {
            props = (Dictionary<String, Object>) newProperties;
        } else {
            props = new Hashtable(newProperties);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.injectionengine.factory.InjectionObjectFactory#getInjectionObjectInstance(javax.naming.Reference, java.lang.Object,
     * com.ibm.wsspi.injectionengine.InjectionTargetContext)
     */
//    @Override
//    public Object getInjectionObjectInstance(Reference o, Object targetInstance, InjectionTargetContext targetContext) throws Exception {
//        InjectionRuntimeContext irc = InjectionRuntimeContextHelper.getRuntimeContext();
//
//        final String methodName = "getInjectionObjectInstance";
//
//        if (!(o instanceof Reference)) {
//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, "Object " + o + " is not an instance of javax.naming.Reference. Returning null.");
//            }
//
//            return null;
//        }
//        Reference ref = o;
//        final Class<?> contextClass = Class.forName(ref.getClassName());
//        String typeName = targetInstance.getClass().getName() + "-" + contextClass.getName();
//        Object v = irc.getRuntimeCtxObject(typeName);
//
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "the injected value  is: " + v);
//        }
//        //Leave the remove work in the ending of invoking
//        //should not remove if it is a threadlocal proxy
//        //irc.removetRuntimeCtxObject(typeName);
//        return v;
//    }
}
