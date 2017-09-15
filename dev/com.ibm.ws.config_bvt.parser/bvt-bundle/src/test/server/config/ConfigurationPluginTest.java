/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import java.util.Dictionary;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ConfigurationPlugin;

public class ConfigurationPluginTest extends ManagedFactoryTest implements ConfigurationPlugin {

    /**  */
    private static final String VALUE = "VALUE";
    /**  */
    private static final String NEW_KEY = "NEW_KEY";
    /**  */
    private static final String INJECT = "inject";

    public ConfigurationPluginTest(String name) {
        super(name, 2);
    }

    @Override
    public String[] getServiceClasses() {
        String[] baseServices = super.getServiceClasses();
        String[] services = new String[baseServices.length + 1];
        services[0] = ConfigurationPlugin.class.getName();
        System.arraycopy(baseServices, 0, services, 1, baseServices.length);
        return services;
    }

    @Override
    public void configurationUpdated(String pid, Dictionary<String, ?> props) throws ConfigurationException {
        if (Boolean.TRUE.equals(props.get(INJECT))) {
            if (!props.get(NEW_KEY).equals(VALUE)) {
                throw new ConfigurationException(NEW_KEY, "Missing " + VALUE);
            }
        }
    }

    @Override
    public void modifyConfiguration(ServiceReference<?> ref, Dictionary<String, Object> props) {
        if (name.equals(ref.getProperty(Constants.SERVICE_PID)) && Boolean.TRUE.equals(props.get(INJECT))) {
            props.put(NEW_KEY, VALUE);
        }
    }

}
