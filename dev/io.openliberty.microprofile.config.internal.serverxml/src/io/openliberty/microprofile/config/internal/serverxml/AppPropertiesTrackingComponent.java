/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.serverxml;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.wsspi.application.Application;

/**
 * Tracks applications and their appProperties defined in server.xml
 * <p>
 * It's harder than you'd like to get the appProperties for an app out of config admin. This component takes the approach of tracking applications and their config separately, and
 * then linking up the references afterwards.
 * <ul>
 * <li>It references {@link Application} to get information about the application from the service properties.
 * <li>It references {@link AppPropertiesComponent} to get the information from appProperties.
 * </ul>
 * Whenever either of these references is updated, it checks which app references which appProperties config and builds the map of properties for any apps which have changed.
 * <p>
 * Note: I'm not sure if the locking is needed as I was unable to determine whether DS can call lifecycle methods concurrently, but it only affects updates which should be fairly
 * infrequent.
 * <p>
 * Note: I originally tried to create a component using {@code configurationPid = "com.ibm.ws.app.manager"} to get the app config directly, but that configuration appears to get
 * hidden from us by being bound to the app manager bundle.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class AppPropertiesTrackingComponent {

    /**
     * The (singleton) instance of this component. Needed for access from non-osgi code.
     */
    private static volatile AppPropertiesTrackingComponent instance;

    /**
     * Map from an appProperties PID to its corresponding component
     * <p>
     * Must hold lock on {@code this} to access
     */
    private final Map<String, AppPropertiesComponent> propertiesComponentByPid = new HashMap<>();

    /**
     * Map from an application PID to the list of appProperties PIDs which it references
     * <p>
     * Entry for a given app may be missing if it has no appProperties configured
     * <p>
     * Must hold lock on {@code this} to access
     */
    private final Map<String, List<String>> propertyPidByApp = new HashMap<>();

    /**
     * Map from an application PID to the list of config properties defined for it
     * <p>
     * Entry for a given app may be missing if it has no appProperties configured
     * <p>
     * Must hold lock on {@code this} to <b>write</b>, but not to read
     */
    private final Map<String, Map<String, String>> propertiesByApp = new ConcurrentHashMap<>();

    @Activate
    protected void activate() {
        instance = this;
    }

    @Deactivate
    protected void deactivate() {
        // Shouldn't be needed, there should only ever be one instance
        if (instance == this) {
            instance = null;
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void addAppPropertiesComponent(AppPropertiesComponent apc) {
        propertiesComponentByPid.put(apc.getPid(), apc);
        updateAppsUsingProperties(apc.getPid());
    }

    protected void updatedAppPropertiesComponent(AppPropertiesComponent apc) {
        updateAppsUsingProperties(apc.getPid());
    }

    protected void removeAppPropertiesComponent(AppPropertiesComponent apc) {
        propertiesComponentByPid.remove(apc.getPid(), apc);
        updateAppsUsingProperties(apc.getPid());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, service = Application.class)
    protected void addApp(Map<String, Object> appProps) {
        String appPid = (String) appProps.get("service.pid");
        String[] appPropertiesPids = (String[]) appProps.get("appProperties");
        if (appPid != null && appPropertiesPids != null) {
            synchronized (this) {
                propertyPidByApp.put(appPid, Arrays.asList(appPropertiesPids));
                updateAppProperties(appPid);
            }
        }
    }

    protected void updatedApp(Map<String, Object> appProps) {
        String appPid = (String) appProps.get("service.pid");
        String[] appPropertiesPids = (String[]) appProps.get("appProperties");
        if (appPid != null && appPropertiesPids != null) {
            synchronized (this) {
                propertyPidByApp.put(appPid, Arrays.asList(appPropertiesPids));
                updateAppProperties(appPid);
            }
        }
    }

    protected void removeApp(Map<String, Object> appProps) {
        String appPid = (String) appProps.get("service.pid");
        if (appPid != null) {
            synchronized (this) {
                propertyPidByApp.remove(appPid);
                updateAppProperties(appPid);
            }
        }
    }

    /**
     * Update all applications which reference the given appProperties PID
     * <p>
     * Caller must hold lock on {@code this}.
     *
     * @param appPropertiesPid the appProperties PID
     */
    private void updateAppsUsingProperties(String appPropertiesPid) {
        for (Entry<String, List<String>> entry : propertyPidByApp.entrySet()) {
            if (entry.getValue().contains(appPropertiesPid)) {
                updateAppProperties(entry.getKey());
            }
        }
    }

    /**
     * Update the properties for an application.
     * <p>
     * Updates the {@link #propertiesByApp} map based on the data in {@link #propertyPidByApp} and {@link #propertiesComponentByPid}.
     * <p>
     * Caller must hold lock on {@code this}.
     *
     * @param appPid the PID for the application
     */
    private void updateAppProperties(String appPid) {
        Map<String, String> newProperties = new HashMap<>();

        List<String> properitesPids = propertyPidByApp.get(appPid);
        if (properitesPids != null) {
            for (String propertiesPid : properitesPids) {
                AppPropertiesComponent propertiesComponent = propertiesComponentByPid.get(propertiesPid);
                if (propertiesComponent != null) {
                    newProperties.putAll(propertiesComponent.getProperties());
                }
            }
        }

        if (newProperties.isEmpty()) {
            propertiesByApp.remove(appPid);
        } else {
            propertiesByApp.put(appPid, Collections.unmodifiableMap(newProperties));
        }
    }

    /**
     * Returns the app properties for an application
     *
     * @param appPid the PID of the application
     * @return a read-only map of properties
     */
    public static Map<String, String> getAppProperties(String appPid) {
        AppPropertiesTrackingComponent instance = AppPropertiesTrackingComponent.instance;
        if (instance == null) {
            return Collections.emptyMap();
        }

        Map<String, String> properties = instance.propertiesByApp.get(appPid);

        if (properties == null) {
            return Collections.emptyMap();
        }

        return properties;
    }
}
