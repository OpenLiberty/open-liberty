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
package com.ibm.ws.jndi.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.StringRefAddr;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jndi.JNDIConstants;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jndi.WSEncryptedStringRefAddr;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           configurationPid = "com.ibm.ws.jndi.referenceEntry")
public class JNDIReferenceEntry {
    private static final TraceComponent tc = Tr.register(JNDIReferenceEntry.class);

    private static final String PROPERTIES_PREFIX = "properties.0.";
    private static final String PROPERTIES_REFERENCE_TYPE = PROPERTIES_PREFIX + "config.referenceType";

    private JNDIObjectFactory factory;
    private ServiceRegistration<?> registration;

    // The reference target is specified via metatype.
    @Reference(name = "factory")
    protected void setFactory(JNDIObjectFactory factory) {
        this.factory = factory;
    }

    protected void unsetFactory(ServiceReference<?> ref) {}

    protected void activate(BundleContext context, Map<String, Object> props) {
        String jndiName = (String) props.get("jndiName");

        javax.naming.Reference ref = new javax.naming.Reference(factory.getObjectClassName(), factory.getClassName(), null);
        boolean decode = (Boolean) props.get("decode");
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(PROPERTIES_PREFIX) && !key.equals(PROPERTIES_REFERENCE_TYPE)) {
                String addrType = key.substring(PROPERTIES_PREFIX.length());
                String contents = (String) entry.getValue();
                StringRefAddr sra;
                if (decode && PasswordUtil.isEncrypted(contents)) {
                    try {
                        PasswordUtil.decode(contents);
                    } catch (Exception e) {
                        // TODO: If there is a chance to modify the code, please change the message level
                        // as Tr.warning and also, modify the contents of the message accordingly.
                        // Unless the data is being consumed, it is harmless, but at the same time raising
                        // the warning is also important.
                        Tr.error(tc, "jndi.decode.failed", contents, e);
                    }
                    // the object is created even the error happened, since the if the issue exists on the passwordUtil class,
                    // there is a potential that it would be resolved before consuming the value.
                    sra = new WSEncryptedStringRefAddr(addrType, contents);
                } else {
                    sra = new StringRefAddr(addrType, contents);
                }
                ref.add(sra);
            }
        }

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(JNDIConstants.JNDI_SERVICENAME, jndiName);
        this.registration = context.registerService(javax.naming.Reference.class.getName(), ref, properties);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "registration=" + registration);
    }

    protected void deactivate(ComponentContext context) {
        if (this.registration != null) {
            this.registration.unregister();
        }
    }
}
