/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.config.extensions;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 *
 */
public class ExtensionsTest implements ManagedServiceFactory, ConfigPropertiesProvider {

    //timeout if the service hasn't been called in 15 seconds
    private final long TIMEOUT = 15000;

    Map<String, Dictionary<String, ?>> propSets = new ConcurrentHashMap<String, Dictionary<String, ?>>();

    public Dictionary<String, ?> getPropertiesForId(String id) {
        Dictionary<String, ?> props = getPropsForId(id);
        synchronized (propSets) {
            while (props == null) {
                try {
                    propSets.wait(TIMEOUT);
                    props = getPropsForId(id);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted");
                }
                if (props == null)
                    throw new RuntimeException("Extensions test timed out waiting for ManagedServiceFactory updated call starting with config id " + id);
            }
        }
        return props;
    }

    Dictionary<String, ?> getPropsForId(String id) {
        return propSets.get(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
     */
    @Override
    public void deleted(String pid) {
    // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.cm.ManagedServiceFactory#updated(java.lang.String, java.util.Dictionary)
     */
    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        System.out.println("ExtensionsTest (mock ManagedServiceFactory) updated called with pid: " + pid + " and properties: " + properties);
        if (pid != null && properties != null) {
            synchronized (propSets) {
                String id;
                propSets.put(((id = (String) properties.get("id")) == null) ? pid : id, properties);
                propSets.notifyAll();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.cm.ManagedServiceFactory#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }
}
