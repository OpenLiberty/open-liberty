/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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

    public ApplicationConfig(String configPid, Dictionary<String, ?> config, ApplicationManager applicationManager) {
        _configPid = configPid;
        _config = config;
        _applicationManager = applicationManager;
        _location = (String) config.get(AppManagerConstants.LOCATION);
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

    public String[] getStartAfter() {
        String[] resultPids = null;
        if (_config != null) {
            resultPids = (String[]) _config.get(AppManagerConstants.START_AFTER);
        }

        if (resultPids == null)
            resultPids = new String[0];
        return resultPids;

    }

    void describe(StringBuilder sb) {
        sb.append("app[" + getName() + "," + getType() + "]");
    }
}
