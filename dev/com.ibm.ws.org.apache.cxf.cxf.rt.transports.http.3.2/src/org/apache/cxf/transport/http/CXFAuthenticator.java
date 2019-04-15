/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.transport.http;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.Conduit;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
public class CXFAuthenticator extends Authenticator {
    static CXFAuthenticator instance;


    public CXFAuthenticator() {
    }

    @FFDCIgnore({Exception.class, Exception.class, InvocationTargetException.class, Throwable.class, Throwable.class})
    public static synchronized void addAuthenticator() {
        if (instance == null) {
            instance = new CXFAuthenticator();
            Authenticator wrapped = null;
            if (JavaUtils.isJava9Compatible()) {
                try {
                    Method m = ReflectionUtil.getMethod(Authenticator.class, "getDefault");
                    // Liberty change added doPriv (should be in next CXF release)
                    wrapped = AccessController.doPrivileged((PrivilegedExceptionAction<Authenticator>) () -> {
                        return (Authenticator)m.invoke(null);
                        });
                } catch (Exception e) {
                    // ignore
                }
                

            } else {
                for (final Field f : ReflectionUtil.getDeclaredFields(Authenticator.class)) {
                    if (f.getType().equals(Authenticator.class)) {
                        ReflectionUtil.setAccessible(f);
                        try {
                            wrapped = (Authenticator)f.get(null);
                            if (wrapped != null && wrapped.getClass().getName()
                                .equals(ReferencingAuthenticator.class.getName())) {
                                Method m = wrapped.getClass().getMethod("check");
                                m.setAccessible(true);
                                m.invoke(wrapped);
                            }
                            wrapped = (Authenticator)f.get(null);
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }

            try {
                Class<?> cls = null;
                InputStream ins = ReferencingAuthenticator.class
                    .getResourceAsStream("ReferencingAuthenticator.class");
                byte[] b = IOUtils.readBytesFromStream(ins);
                if (JavaUtils.isJava9Compatible()) {
                    Class<?> methodHandles = Class.forName("java.lang.invoke.MethodHandles");
                    Method m = ReflectionUtil.getMethod(methodHandles, "lookup");
                    Object lookup = m.invoke(null);
                    m = ReflectionUtil.getMethod(lookup.getClass(), "findClass", String.class);
                    try {
                        cls = (Class<?>)m.invoke(lookup, "org.apache.cxf.transport.http.ReferencingAuthenticator");
                    } catch (InvocationTargetException e) {
                        //use defineClass as fallback
                        m = ReflectionUtil.getMethod(lookup.getClass(), "defineClass", byte[].class);
                        cls = (Class<?>)m.invoke(lookup, b);
                    }
                } else {
                    ClassLoader loader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                        public ClassLoader run() {
                            return new URLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
                        }
                    }, null);
                    Method m = ReflectionUtil.getDeclaredMethod(ClassLoader.class, "defineClass",
                                                                String.class, byte[].class, Integer.TYPE,
                                                                Integer.TYPE);

                    

                    ReflectionUtil.setAccessible(m).invoke(loader, ReferencingAuthenticator.class.getName(),
                                                           b, 0, b.length);
                    cls = loader.loadClass(ReferencingAuthenticator.class.getName());
                    try {
                        //clear the acc field that can hold onto the webapp classloader
                        Field f = ReflectionUtil.getDeclaredField(loader.getClass(), "acc");
                        ReflectionUtil.setAccessible(f).set(loader, null);
                    } catch (Throwable t) {
                        //ignore
                    }
                }
                final Authenticator auth = (Authenticator)cls.getConstructor(Authenticator.class, Authenticator.class)
                    .newInstance(instance, wrapped);

                if (System.getSecurityManager() == null) {
                    Authenticator.setDefault(auth);
                } else {
                    AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                        public Boolean run() {
                            Authenticator.setDefault(auth);
                            return true;
                        }
                    });

                }
                
            } catch (Throwable t) {
                //ignore
            }
        }
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        PasswordAuthentication auth = null;
        Message m = PhaseInterceptorChain.getCurrentMessage();
        if (m != null) {
            Exchange exchange = m.getExchange();
            Conduit conduit = exchange.getConduit(m);
            if (conduit instanceof HTTPConduit) {
                HTTPConduit httpConduit = (HTTPConduit)conduit;
                if (getRequestorType() == RequestorType.PROXY
                    && httpConduit.getProxyAuthorization() != null) {
                    String un = httpConduit.getProxyAuthorization().getUserName();
                    String pwd = httpConduit.getProxyAuthorization().getPassword();
                    if (un != null && pwd != null) {
                        auth = new PasswordAuthentication(un, pwd.toCharArray());
                    }
                } else if (getRequestorType() == RequestorType.SERVER
                    && httpConduit.getAuthorization() != null) {

                    if ("basic".equals(getRequestingScheme()) || "digest".equals(getRequestingScheme())) {
                        return null;
                    }

                    String un = httpConduit.getAuthorization().getUserName();
                    String pwd = httpConduit.getAuthorization().getPassword();
                    if (un != null && pwd != null) {
                        auth = new PasswordAuthentication(un, pwd.toCharArray());
                    }
                }
            }
        }
        // else PhaseInterceptorChain.getCurrentMessage() is null,
        // this HTTP call has therefore not been generated by CXF
        return auth;
    }
}
