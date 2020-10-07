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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import org.apache.cxf.common.util.ReflectionUtil;

public class ReferencingAuthenticator extends Authenticator {
    final Reference<Authenticator> auth;
    final Authenticator wrapped;
    public ReferencingAuthenticator(Authenticator cxfauth, Authenticator wrapped) {
        this.auth = new WeakReference<Authenticator>(cxfauth);
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
            for (final Field f : Authenticator.class.getDeclaredFields()) {
                if (f.getType().equals(Authenticator.class)) {
                    try {
                        f.setAccessible(true);
                        Authenticator o = (Authenticator)f.get(null);
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
                Authenticator a2 = (Authenticator)f2.get(a);
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
        for (final Field f : ReflectionUtil.getDeclaredFields(Authenticator.class)) { // Liberty Change
            if (!Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true);
                Object o = f.get(this);
                f.set(a, o);
            }
        } 
        final Method m = Authenticator.class.getDeclaredMethod("getPasswordAuthentication");
        m.setAccessible(true);
        return (PasswordAuthentication)m.invoke(a);
    }
}