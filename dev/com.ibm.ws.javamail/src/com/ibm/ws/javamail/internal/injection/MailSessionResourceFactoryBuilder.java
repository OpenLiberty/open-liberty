/*******************************************************************************
 * Copyright (c) 2015,2021
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javamail.internal.injection;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javamail.internal.MailSessionService;
import com.ibm.ws.resource.ResourceFactory;
import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 *
 */
@Component(service = { ResourceFactoryBuilder.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM",
                                                                                                                                        "creates.objectClass=javax.mail.Session"
})
public class MailSessionResourceFactoryBuilder implements ResourceFactoryBuilder {

    private static final TraceComponent tc = Tr.register(MailSessionResourceFactoryBuilder.class);

    /**
     * Unique identifier attribute name.
     */
    private static final String ID = "id";

    private BundleContext bundleContext;

    /**
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context context for this component
     */
    protected void activate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(this, tc, "activate", context);
        bundleContext = context.getBundleContext();

    }

    /** {@inheritDoc} */
    @Override
    public ResourceFactory createResourceFactory(Map<String, Object> props) throws Exception {

        Hashtable<String, Object> mailSessionSvcProps = new Hashtable<String, Object>();

        Map<String, Object> annotationProps = new HashMap<String, Object>();

        for (Map.Entry<String, Object> prop : props.entrySet()) {
            Object value = prop.getValue();
            annotationProps.put(prop.getKey(), value);
        }
        String application = (String) annotationProps.remove("application");
        String module = (String) annotationProps.remove("module");
        String component = (String) annotationProps.remove("component");
        String jndiName = (String) annotationProps.remove(MailSessionService.JNDI_NAME);
        String mailSessionID = getMailSessionID(application, module, component, jndiName);

        StringBuilder filter = new StringBuilder(FilterUtils.createPropertyFilter(ID, mailSessionID));
        filter.insert(filter.length() - 1, '*');
        // Fail if server.xml is already using the id
        if (!removeExistingConfigurations(filter.toString()))
            throw new IllegalArgumentException(mailSessionID); // internal error, shouldn't ever have been permitted in server.xml

        mailSessionSvcProps.put(MailSessionService.JNDI_NAME, jndiName);
        mailSessionSvcProps.put(MailSessionService.MAILSESSIONID, mailSessionID);
        if (application != null) {
            mailSessionSvcProps.put("application", application);
            if (module != null) {
                mailSessionSvcProps.put("module", module);
                if (component != null)
                    mailSessionSvcProps.put("component", component);
            }
        }

        if (!annotationProps.isEmpty()) {
            Set<String> annotationKeys = annotationProps.keySet();
            for (String key : annotationKeys) {
                mailSessionSvcProps.put(key, annotationProps.get(key));
            }
        }

        MailSessionResourceFactory mss = new MailSessionResourceFactory();
        mss.processProperties(mailSessionSvcProps);

        // MailSessionService does not use the Resource, pass null
        mss.createResource(null);

        return mss;
    }

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context context for this component
     */
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(this, tc, "deactivate", context);
    }

    /**
     * Utility method that creates a unique identifier for an application defined data source.
     * For example,
     * application[MyApp]/module[MyModule]/connectionFactory[java:module/env/jdbc/cf1]
     *
     * @param application application name if data source is in java:app, java:module, or java:comp. Otherwise null.
     * @param module      module name if data source is in java:module or java:comp. Otherwise null.
     * @param component   component name if data source is in java:comp and isn't in web container. Otherwise null.
     * @param jndiName    configured JNDI name for the data source. For example, java:module/env/jca/cf1
     * @return the unique identifier
     */
    private static final String getMailSessionID(String application, String module, String component, String jndiName) {
        StringBuilder sb = new StringBuilder(jndiName.length() + 80);
        if (application != null) {
            sb.append("application").append('[').append(application).append(']').append('/');
            if (module != null) {
                sb.append("module").append('[').append(module).append(']').append('/');
                if (component != null)
                    sb.append("component").append('[').append(component).append(']').append('/');
            }
        }
        return sb.append(MailSessionService.MAILSESSIONID).append('[').append(jndiName).append(']').toString();
    }

    @Override
    public final boolean removeExistingConfigurations(String filter) throws Exception {
        return true;
    }
}
