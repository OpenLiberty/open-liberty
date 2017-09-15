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
package test.server.config.dynamic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import test.server.BaseTest;

public class FactoryTest extends BaseTest implements ManagedServiceFactory {

    private final Map<String, String> pid2IdMapping;
    private final Map<String, String> id2PidMapping;
    private final Map<String, List<Dictionary<String, Object>>> dictionaries;
    private final String idAttribute;

    public FactoryTest(String name) {
        this(name, "id");
    }

    public FactoryTest(String name, String idAttribute) {
        super(name);
        this.dictionaries = new HashMap<String, List<Dictionary<String, Object>>>();
        this.pid2IdMapping = Collections.synchronizedMap(new HashMap<String, String>());
        this.id2PidMapping = Collections.synchronizedMap(new HashMap<String, String>());
        this.idAttribute = idAttribute;
    }

    public void reset() {
        synchronized (dictionaries) {
            dictionaries.clear();
        }
    }

    @Override
    public String[] getServiceClasses() {
        return new String[] { ManagedServiceFactory.class.getName() };
    }

    @SuppressWarnings("unchecked")
    @Override
    public void updated(String pid, @SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
        String id = (String) properties.get(idAttribute);
        pid2IdMapping.put(pid, id);
        id2PidMapping.put(id, pid);
        notify(id, properties);
    }

    @Override
    public void deleted(String pid) {
        String id = pid2IdMapping.get(pid);
        notify(id, null);
    }

    public String getPid(String id) {
        return id2PidMapping.get(id);
    }

    public Set<String> getPids() {
        return pid2IdMapping.keySet();
    }

    private void notify(String id, Dictionary<String, Object> properties) {
        List<Dictionary<String, Object>> list = getDictionaries(id);
        synchronized (list) {
            list.add(properties);
            list.notifyAll();
        }
    }

    private List<Dictionary<String, Object>> getDictionaries(String id) {
        synchronized (dictionaries) {
            List<Dictionary<String, Object>> updates = dictionaries.get(id);
            if (updates == null) {
                updates = new ArrayList<Dictionary<String, Object>>();
                dictionaries.put(id, updates);
            }
            return updates;
        }
    }

    public Dictionary<String, Object> waitForUpdate(String id) {
        return waitForUpdate(id, TIMEOUT);
    }

    /**
     * @param string
     * @param i
     * @return
     */
    public Dictionary<String, Object> waitForUpdate(String id, long timeToWait) {
        List<Dictionary<String, Object>> list = getDictionaries(id);
        synchronized (list) {
            while (list.isEmpty()) {
                try {
                    list.wait(timeToWait);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted");
                }
                if (list.isEmpty()) {
                    throw new RuntimeException("Timed out");
                }
            }
            return list.remove(0);
        }
    }

    /**
     * @param string
     * @return
     */
    public boolean hasDictionary(String id) {
        List<Dictionary<String, Object>> list = getDictionaries(id);
        synchronized (list) {
            try {
                list.wait(5000);
            } catch (InterruptedException e) {
                //Ignore, we're just waiting five seconds to make this method more accurate
            }
            return !list.isEmpty();
        }
    }

}
