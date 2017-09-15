/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jndi.internal.literals.LiteralParser;

/**
 * <p>
 * This DS component will register a JNDI property when one is configured in the server configuration XML such as:
 * </p>
 * <code>
 * &lt;jndiURLEntry jndiName="&lt;JNDI_NAME&gt;" value="&lt;VALUE&gt;"/><br/>
 * </code>
 * <p>
 * The &lt;VALUE&gt; will be passed through the {@link LiteralParser#parse(String)} method to convert it into a literal value. A service will then be registered under the class
 * name of the parsed value and will have the &lt;JNDI_NAME&gt; set in the "osgi.jndi.service.name" property of the service registration.
 * </p>
 */
@Component(configurationPolicy = REQUIRE, property = "service.vendor=IBM", immediate = true)
public class JNDIURLEntry implements ObjectFactory {

    private static final TraceComponent tc = Tr.register(JNDIURLEntry.class);

    private transient ServiceRegistration<?> serviceRegistration;

    private static class CreateURL implements PrivilegedExceptionAction<URL> {
        private final String urlString;

        private CreateURL(String urlString) {
            this.urlString = urlString;
        }

        @Override
        public URL run() throws MalformedURLException {
            return new URL(urlString);
        }
    }

    /**
     * Registers the JNDI service for the supplied properties as long as the jndiName and value are set
     * 
     * @param context
     * @param props The properties containing values for <code>"jndiName"</code> and <code>"value"</code>
     */
    protected void activate(BundleContext context, Map<String, Object> props) {
        final String jndiName = (String) props.get("jndiName");
        final String urlValue = (String) props.get("value");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Registering JNDIURLEntry with value " + urlValue + " and JNDI name " + jndiName);
        }

        if (jndiName == null || jndiName.isEmpty() || urlValue == null || urlValue.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to register JNDIURLEntry with jndiName [" + jndiName + "] and value [" + urlValue + "] because both must be set");
            }
            return;
        }

        // Test that the URL is valid
        // creating a url should be a protected action
        createURL(jndiName, urlValue);

        Dictionary<String, Object> propertiesForJndiService = new Hashtable<String, Object>();
        propertiesForJndiService.put("osgi.jndi.service.name", jndiName);
        propertiesForJndiService.put(Constants.OBJECTCLASS, Reference.class);

        Reference ref = new Reference(URL.class.getName(), this.getClass().getName(), null);
        ref.add(new RefAddr("JndiURLEntry") {
            private static final long serialVersionUID = 5168161341101144689L;

            @Override
            public Object getContent() {
                return urlValue;
            }
        });

        this.serviceRegistration = context.registerService(Reference.class, ref, propertiesForJndiService);
    }

    /**
     * Unregisters a service if one was registered
     * 
     * @param context
     */
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Unregistering JNDIURLEntry " + serviceRegistration);
        }
        if (this.serviceRegistration != null) {
            this.serviceRegistration.unregister();
        }
    }

    @FFDCIgnore(PrivilegedActionException.class)
    private URL createURL(final String jndiName, final String urlValue) {
        try {
            return AccessController.doPrivileged(new CreateURL(urlValue));
        } catch (PrivilegedActionException e) {
            Throwable t = e.getCause() == null ? e : e.getCause();
            Tr.error(tc, "jndi.url.create.exception", new Object[] { urlValue, jndiName, t.toString() });
            FFDCFilter.processException(t, JNDIURLEntry.class.getName() + ".createURL", "147", new Object[] { jndiName, urlValue });
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context, java.util.Hashtable)
     */
    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        if (obj instanceof Reference) {
            try {
                String jndiName = nameCtx == null ? "" : nameCtx.getNameInNamespace();
                jndiName += name == null ? "" : name.toString();
                String urlValue = (String) ((Reference) obj).get(0).getContent();
                return createURL(jndiName, urlValue);
            } catch (Throwable t) {
                NamingException ne = new NamingException();
                ne.initCause(t);
                throw ne;
            }
        }

        return null;
    }
}
