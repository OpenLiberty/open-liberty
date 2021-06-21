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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

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
    static Authenticator wrapped;	// Liberty change:CXFAuthenticator instance is replaced by Authenticator wrapped
    static boolean setup; // Liberty change: added


    public CXFAuthenticator() {
		try { // Liberty change: addition start
            for (Field f : Authenticator.class.getDeclaredFields()) {
                    if (f.getType().equals(Authenticator.class)) {
                        try {
                        wrapped = AccessController.doPrivileged(new PrivilegedExceptionAction<Authenticator>() {
                            @Override
                            public Authenticator run() throws Exception {
                                f.setAccessible(true);
                                return (Authenticator)f.get(null);
                            }
                        });
                    } catch (PrivilegedActionException pae) {
                            // ignore
                        }
                    }
                }
        } catch (Throwable ex) {
            //ignore
        } // Liberty change: addition end
    }

    public static synchronized void addAuthenticator() {
        if (!setup) {// Liberty change: addition start
          try {
              AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                  @Override
                  public Void run() throws Exception {
                      Authenticator.setDefault(new CXFAuthenticator());
                      return null;
                  }
              });
          } catch (PrivilegedActionException pae) {
              }
              setup = true;
          }	// Liberty change: addition end
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        PasswordAuthentication auth = null;
        // Liberty change: code below added
        if (wrapped != null) {
          try {
              for (Field f : Authenticator.class.getDeclaredFields()) {
                  if (!Modifier.isStatic(f.getModifiers())) {
                      f.setAccessible(true);
                      f.set(wrapped, f.get(this));
                  }
              }
              Method m = Authenticator.class.getDeclaredMethod("getPasswordAuthentication");
              m.setAccessible(true);
              auth = (PasswordAuthentication)m.invoke(wrapped);
            } catch (Throwable t) {
                //ignore
            }
        }
        if (auth != null) {
          return auth;
        }
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

    // Liberty change: method below is added
    public static Authenticator getDefault() {
    try {
        // In java 9 there is a static Authenticator.getDefault() method
        // which does not require using reflection to read private fields
        return (Authenticator) Authenticator.class.getDeclaredMethod("getDefault").invoke(null);
    } catch (Exception ignore) {
        //ignore
    }
    // end change

    try {
        for (Field f : Authenticator.class.getDeclaredFields()) {
            if (f.getType().equals(Authenticator.class)) {
                f.setAccessible(true);
                return (Authenticator) f.get(null);
            }
        }
    } catch (Throwable ex) {
    }
    throw new IllegalStateException("Unable to locate default java.net.Authenticator");
  }
}
