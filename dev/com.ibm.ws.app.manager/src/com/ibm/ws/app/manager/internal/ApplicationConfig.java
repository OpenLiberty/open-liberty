/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.management.NotificationBroadcasterSupport;

import com.ibm.websphere.application.ApplicationMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.app.manager.ApplicationManager;

@Trivial
public final class ApplicationConfig {

    private static final TraceComponent tc = Tr.register(ApplicationConfig.class);

    private final String _configPid;
    private final Dictionary<String, ?> _config;
    private final ApplicationManager _applicationManager;
    private final String _location;
    private final String _type;
    private final String _name;
    private NotificationBroadcasterSupport _notificationBroadcasterSupport;

    /**
     * Create an application configuration, assigning a configuration ID,
     * assigning attributes using the supplied table, and in association with
     * a specified application manager.
     *
     * Several attributes are distinguished:
     * 
     * Assign the application location of the application based on the location
     * attribute. A location should normally be supplied.
     * 
     * Assign the application type based on the type attribute.  If no type
     * attribute was supplied, assign the type based on the extension of
     * the location.  (Assign a null type if neither a type attribute nor a
     * location attribute was specified.)
     * 
     * Assign the application name based on the name attribute.  If no name
     * attribute was supplied, assign the name using the base name of the
     * application location.  (Assign a null name if neither a name attribute
     * nor a location attribute was specified.)
     *
     * @param configPid The configuration ID to assign to the new application
     *     configuration.
     * @param config A table of attributes to assign to the application
     *     configuration.
     * @param applicationManager The application manager to assign to the
     *     application configuration.
     */
    public ApplicationConfig(
        String configPid,
        Dictionary<String, ?> config,
        ApplicationManager applicationManager) {

        _applicationManager = applicationManager; // Context
        _configPid = configPid; // Identity

        _config = config; // Raw attributes

        // Key attributes: Location, Type, and Name ...

        // The path to the application relative to the 'apps' folder.
        // A location is usually required.

        _location = (String) config.get(AppManagerConstants.LOCATION);

        // Types include, for example, 'ear', 'rar', and 'war'.
        // A type is usually required, either as a raw attribute or
        // as obtained from the location.

        String type = (String) config.get(AppManagerConstants.TYPE);
        if (type == null) {
            if (_location != null) {
                int index = _location.lastIndexOf('.');
                if (index != -1) {
                    type = _location.substring(index + 1);
                }
            }
        }
        _type = type;

        // The application name, as assigned here in the application
        // configuration, is not the only name of the application.
        //
        // A name may be supplied by an application
        // deployment descriptor.
        //
        // In order to force unique names, the name may be adjusted
        // by appending a numeric value.
        //
        // A name is usually required, as a raw attribute, or as obtained
        // from the location.

        String name = (String) config.get(AppManagerConstants.NAME);
        if (name == null || "".equals(name)) {
            if (_location != null) {
                int startIndex = _location.lastIndexOf('/');
                if (startIndex == -1) {
                    startIndex = _location.lastIndexOf('\\');
                }
                int endIndex = _location.lastIndexOf('.');
                if (endIndex > ++startIndex) {
                    name = _location.substring(startIndex, endIndex);
                } else {
                    name = _location.substring(startIndex);
                }
            }
        }
        _name = name;

        displayJandexMessages();
    }

    private static boolean ONE_TIME_JANDEX_MSGS_DISPLAYED = false;

    private void displayJandexMessages() {

        boolean appMgr_useJandex = _applicationManager.getUseJandex();

        // Display a message once if useJandex is set to true on the applicationManager.
        // This causes the default useJandex setting for each application to be set to true.
        if (!ONE_TIME_JANDEX_MSGS_DISPLAYED) {
            if (appMgr_useJandex) {
                Tr.info(tc, "APPLICATION_JANDEX_ENABLED_ALL");
            }
            ONE_TIME_JANDEX_MSGS_DISPLAYED = true;
        }

        // The application setting overrides the applicationManager setting.
        if (_config != null) {
            Object application_useJandex = _config.get(AppManagerConstants.USE_JANDEX);
            if (application_useJandex instanceof Boolean) {

                if ((Boolean) application_useJandex) {
                    Tr.info(tc, "APPLICATION_JANDEX_ENABLED", _name);
                } else {
                    Tr.info(tc, "APPLICATION_JANDEX_DISABLED", _name);
                }
                return;
            }
        }

        // Apparently, useJandex not set on the application.  So default to the appMgr setting.
        if (appMgr_useJandex) {
            Tr.info(tc, "APPLICATION_JANDEX_ENABLED", _name);

        } else {
            // Display nothing.  Since they are taking the defaults.
        }
    }

    //public ApplicationConfig(String configPid, Dictionary<String, ?> config) {
    //    this(configPid, config, null);
    //}

    public void setMBeanNotifier(NotificationBroadcasterSupport broadcaster) {
        _notificationBroadcasterSupport = broadcaster;
    }

    public NotificationBroadcasterSupport getMBeanNotifier() {
        return _notificationBroadcasterSupport;
    }

    public String getLocation() {
        return _location;
    }

    public String getType() {
        return _type;
    }

    public String getName() {
        return _name;
    }

    public String getLabel() {
        return ("rar".equals(_type) ? "resource adapter" : "app") + " " + _name;
    }

    public String getConfigPid() {
        return _configPid;
    }

    public Object getConfigProperty(String propName) {
        return _config.get(propName);
    }

    public boolean isValid() {
        String location = getLocation();
        String type = getType();
        String name = getName();

        return location != null && type != null && name != null;
    }

    public Hashtable<String, Object> getServiceProperties() {
        Dictionary<String, ?> config = _config;
        Hashtable<String, Object> result = new Hashtable<String, Object>();
        for (String key : Collections.list(config.keys())) {
            result.put(key, config.get(key));
        }

        result.put(AppManagerConstants.TYPE, getType());
        result.put(AppManagerConstants.NAME, getName());

        return result;
    }

    public String getMBeanName() {
        StringBuilder builder = new StringBuilder("WebSphere:service=");
        builder.append(ApplicationMBean.class.getName());
        builder.append(",name=");
        builder.append(getName());
        return builder.toString();
    }

    public boolean isAutoStarted() {
        Dictionary<String, ?> config = _config;
        if (config != null) {
            Object result = config.get(AppManagerConstants.AUTO_START);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }
        return true;
    }

    public boolean getUseJandex() {
        // First try to get the value from the application configuration
        // which overrides the value on the application manager configuration.
        if (_config != null) {
            Object result = _config.get(AppManagerConstants.USE_JANDEX);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }

        // If that fails, try to get the value from the application manager
        return _applicationManager.getUseJandex();
    }

    void describe(StringBuilder sb) {
        sb.append("app[" + getName() + "," + getType() + "]");
    }
}
