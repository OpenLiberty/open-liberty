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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class ReferencingAuthenticator extends Authenticator {
    private static final boolean SKIPCHECK = System.getSecurityManager() == null;
    final Reference<Authenticator> auth;
    final Authenticator wrapped;

    public ReferencingAuthenticator(Authenticator cxfauth, Authenticator wrapped) {
        this.auth = new WeakReference<>(cxfauth);
        this.wrapped = wrapped;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        Authenticator cxfauth = auth.get();
        if (cxfauth == null) {
            remove();
        }
        PasswordAuthentication pauth = null;
        if (wrapped != null) {
            try {
                pauth = tryWith(wrapped);
                if (pauth != null) {
                    return pauth;
                }
            } catch (Exception e) {
                pauth = null;
            }
        }
        if (cxfauth != null) {
            try {
                pauth = tryWith(cxfauth);
            } catch (Exception e1) {
                pauth = null;
            }
        }
        return pauth;
    }

    public final void check() {
        Authenticator cxfauth = auth.get();
        if (cxfauth == null) {
            remove();
        }
        if (wrapped != null && wrapped.getClass().getName().equals(ReferencingAuthenticator.class.getName())) {
            try {
                Method m = wrapped.getClass().getMethod("check");
                m.setAccessible(true);
                m.invoke(wrapped);
            } catch (Throwable t) {
                //ignore
            }
        }
    }

    private void remove() {
        try { 
            // Try Authenticator.getDefault() first, JDK9+
            final MethodHandle mt = MethodHandles
               .lookup()
               .findStatic(Authenticator.class, "getDefault", MethodType.methodType(Authenticator.class));
            removeInternal((Authenticator)mt.invoke());
        } catch (final NoSuchMethodException | IllegalAccessException ex) {
            removeInternal();
        } catch (Throwable e) {
            //ignore
        }
    }
    
    private void removeInternal(final Authenticator def) {
        try {
            if (def == this) {
                //this is at the root of any chain of authenticators
                Authenticator.setDefault(wrapped);
            } else {
                removeFromChain(def);
            }
        } catch (Throwable t) {
            //ignore
        }
    }
    
    private void removeInternal() {
        try {
            for (final Field f : Authenticator.class.getDeclaredFields()) {
                if (f.getType().equals(Authenticator.class)) {
                    try {
                        f.setAccessible(true);
                        Authenticator o = (Authenticator) f.get(null);
                        if (o == this) {
                            //this is at the root of any chain of authenticators
                            Authenticator.setDefault(wrapped);
                        } else {
                            removeFromChain(o);
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
        } catch (Throwable t) {
            //ignore
        }
    }

    private void removeFromChain(Authenticator a) {
        try {
            if (a.getClass().getName().equals(ReferencingAuthenticator.class.getName())) {
                //multiple referencing authenticators, we can remove ourself
                Field f2 = a.getClass().getDeclaredField("wrapped");
                f2.setAccessible(true);
                Authenticator a2 = (Authenticator) f2.get(a);
                if (a2 == this) {
                    f2.set(a, wrapped);
                } else {
                    removeFromChain(a2);
                }
            }
        } catch (Throwable t) {
            //ignore
        }
    }

    PasswordAuthentication tryWith(Authenticator a) throws Exception {
        if (a == null) {
            return null;
        }
        
        try {
            // Try Authenticator.requestPasswordAuthentication() first, JDK9+
            final MethodHandle mt = MethodHandles
               .lookup()
               .findStatic(Authenticator.class, "requestPasswordAuthentication", 
                   MethodType.methodType(PasswordAuthentication.class, new Class<?>[] {
                       Authenticator.class,
                       String.class,
                       InetAddress.class,
                       int.class,
                       String.class,
                       String.class,
                       String.class,
                       URL.class,
                       RequestorType.class
                   }));
    
            return (PasswordAuthentication)mt.invoke(a, getRequestingHost(), getRequestingSite(), 
                getRequestingPort(), getRequestingProtocol(), getRequestingPrompt(), getRequestingScheme(), 
                    getRequestingURL(), getRequestorType());
        } catch (final NoSuchMethodException | IllegalAccessException ex) {
            return tryWithInternal(a);
        } catch (final Throwable ex) {
            if (ex instanceof Exception) {
                throw (Exception)ex;
            } else {
                throw new Exception(ex);
            }
        }
    }
    
    private PasswordAuthentication tryWithInternal(Authenticator a) throws Exception {
        if (a == null) {
            return null;
        }
        Field[] fields = null;
        if (SKIPCHECK) {
            fields = Authenticator.class.getDeclaredFields();
        } else {
            fields = AccessController.doPrivileged(
                    (PrivilegedAction<Field[]>) () -> Authenticator.class.getDeclaredFields());

        }

        for (final Field f : fields) {
            if (!Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true);
                Object o = f.get(this);
                f.set(a, o);
            }
        }
        Method method;
        if (SKIPCHECK) {
            method = Authenticator.class.getDeclaredMethod("getPasswordAuthentication");
            method.setAccessible(true);
        } else {
            method = AccessController.doPrivileged(
                    (PrivilegedAction<Method>) () -> {
                try {
                    return Authenticator.class.getDeclaredMethod("getPasswordAuthentication");
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            });
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                method.setAccessible(true);
                return null;
            });
        }

        return (PasswordAuthentication) method.invoke(a);
    }
}