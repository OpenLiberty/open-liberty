/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.defaultSources.tests;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.microprofile.config.Config;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Helper class to raise the level of abstraction when testing config instances
 */
public class CHelper {
    private final Config c;
    private AbstractAppConfigTestApp test;

    CHelper(Config c) {
        this.c = c;
    }

    /**
     * @param abstractAppConfigTestApp
     * @param config
     */
    public CHelper(AbstractAppConfigTestApp test, Config config) {
        this.c = config;
        this.test = test;
    }

    /**
     * @return
     */
    public String namesToString() {
        Iterable<String> names = c.getPropertyNames();
        StringBuffer buff = new StringBuffer();
        for (Iterator<String> iterator = names.iterator(); iterator.hasNext();) {
            String name = iterator.next();
            buff.append("\n");
            buff.append(name);
            buff.append("=");
            buff.append(c.getValue(name, String.class));
        }
        return buff.toString();
    }

    /**
     * @param propertyName
     * @return
     */
    public String getString(String propertyName) {
        return c.getValue(propertyName, String.class);
    }

    /**
     * Faster than contains("name") but property has to be a String.
     *
     * @param propertyName
     * @return
     */
    public boolean containsString(String propertyName) {
        try {
            boolean result = c.getOptionalValue(propertyName, String.class).isPresent();
            if (!result) {
                test.ffdc = test.ffdc + "Config does not contain property name: " + propertyName;
            }
            return result;
        } catch (IllegalArgumentException e) {
            Log.error(this.getClass(), "containsString", e);
            return false;
        }
    }

    /**
     * @param string
     * @param string2
     * @return
     */
    public boolean contains(String propertyName, String propertyValue) {
        String val = c.getValue(propertyName, String.class);
        boolean result = propertyValue != null && propertyValue.equals(val);
        if (!result) {
            test.ffdc = test.ffdc + "\nConfig in " + test.getClass().getSimpleName() + " does not contain property name: " + propertyName + " as value " + propertyValue
                        + ", it is:" + val;
        }
        return result;
    }

    /**
     * @param buff
     * @param c
     * @return
     */
    private String toString(StringBuffer buff) {
        Iterable<String> names = c.getPropertyNames();
        for (Iterator<String> iterator = names.iterator(); iterator.hasNext();) {
            String name = iterator.next();
            buff.append("\n" + name + " = " + c.getValue(name, String.class));
        }
        String result = buff.toString();
        return result;
    }

    @Override
    public String toString() {
        return toString(new StringBuffer());
    }

    /**
     * @param env
     * @return
     */
    public boolean contains(Map<String, String> env) {
        for (Iterator<String> iterator = env.keySet().iterator(); iterator.hasNext();) {
            String jkey = iterator.next();
            if (!env.get(jkey).equals(c.getOptionalValue(jkey, String.class).orElse("not there"))) {
                //TODO improve logging
                return false;
            }
        }
        return true;
    }

    public String diff(Map<String, String> env) {
        StringBuffer buf = new StringBuffer();
        for (Iterator<String> iterator = env.keySet().iterator(); iterator.hasNext();) {
            String jkey = iterator.next();
            if (!env.get(jkey).equals(c.getValue(jkey, String.class))) {
                buf.append(jkey + "=" + c.getOptionalValue(jkey, String.class).orElse("not there") + ",!=" + env.get(jkey));
            }
        }
        if (buf.length() == 0) {
            return null;
        } else {
            return buf.toString();
        }
    }
}
