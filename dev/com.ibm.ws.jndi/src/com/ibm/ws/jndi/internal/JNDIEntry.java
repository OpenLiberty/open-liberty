/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jndi.internal.literals.LiteralParser;

/**
 * <p>
 * This DS component will register a JNDI property when one is configured in the server configuration XML such as:
 * </p>
 * <code>
 * &lt;jndiEntry jndiName="&lt;JNDI_NAME&gt;" value="&lt;VALUE&gt;" decode="&lt;DECODE&gt;"/><br/>
 * </code>
 * <p>
 * The &lt;VALUE&gt; will be passed through the {@link LiteralParser#parse(String)} method to convert it into a literal value. A service will then be registered under the class
 * name of the parsed value and will have the &lt;JNDI_NAME&gt; set in the "osgi.jndi.service.name" property of the service registration. &lt;DECODE&gt; is a boolean flag and
 * is optional; true indicates that the &lt;VALUE&gt; is encrypted and needs to be decoded.
 * </p>
 */
@Component(configurationPolicy = REQUIRE, property = "service.vendor=IBM")
public class JNDIEntry {

    private static final TraceComponent tc = Tr.register(JNDIEntry.class);

    ServiceRegistration<?> serviceRegistration;

    /**
     * Registers the JNDI service for the supplied properties as long as the jndiName and value are set
     * 
     * @param context
     * @param props The properties containing values for <code>"jndiName"</code> and <code>"value"</code>
     */
    protected synchronized void activate(BundleContext context, Map<String, Object> props) {

        String jndiName = (String) props.get("jndiName");
        String originalValue = (String) props.get("value");
        //Since we declare a default value of false in the metatype, if decode isn't specified, props should return false
        boolean decode = (Boolean) props.get("decode");

        if (jndiName == null || jndiName.isEmpty() || originalValue == null || originalValue.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to register JNDIEntry with jndiName " + jndiName + " and value " + originalValue + " because both must be set");
            }
            return;
        }
        String value = originalValue;
        if (decode) {
            try {
                value = PasswordUtil.decode(originalValue);
            } catch (Exception e) {
                Tr.error(tc, "jndi.decode.failed", originalValue, e);
            }
        }
        Object parsedValue = LiteralParser.parse(value);
        String valueClassName = parsedValue.getClass().getName();
        final Object serviceObject = decode ? new Decode(originalValue) : parsedValue;
        Dictionary<String, Object> propertiesForJndiService = new Hashtable<String, Object>();
        propertiesForJndiService.put("osgi.jndi.service.name", jndiName);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Registering JNDIEntry " + valueClassName + " with value " + parsedValue + " and JNDI name " + jndiName);
        }
        this.serviceRegistration = context.registerService(valueClassName, serviceObject, propertiesForJndiService);
    }

    /**
     * Unregisters a service if one was registered
     * 
     * @param context
     */
    protected synchronized void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Unregistering JNDIEntry " + serviceRegistration);
        }
        if (this.serviceRegistration != null) {
            this.serviceRegistration.unregister();
        }
    }

    /**
     * Extends the JNDIEntry class to allow for decryption of values. JNDIEntry elements that are to be decrypted should
     * add the attribute 'decode="true". If decode=false; the value is simply returned'
     */
    private static class Decode implements ServiceFactory<Object> {

        private final String value;

        public Decode(String value) {
            this.value = value;
        }

        @Override
        public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
            try {
                String decodedValue = PasswordUtil.decode(value);
                Object parsedValue = LiteralParser.parse(decodedValue);
                return parsedValue;
            } catch (Exception e) {
                Tr.error(tc, "jndi.decode.failed", value, e);
            }
            return value;
        }

        @Override
        public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {}

    }

}
