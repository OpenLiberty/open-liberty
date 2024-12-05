package com.ibm.ws.container.service.annocache.internal;

import com.ibm.wsspi.anno.service.AppKey;

/**
 * This service provides keys with a 1:1 mapping to each application installed.
 * These keys can be used in a WeakHashMap to ensure the value is garbage collected
 * when the application shuts down
 */
public interface AnnotationService_KeyService {

    /**
     * Gets an AppKey for a given application
     *
     * @param appName the name of the application, this must be the deploymentName from com.ibm.ws.container.service.app.deploy.ApplicationInfo
     * @return An AppKey for the given appName
     */
    public AppKey getKeyForApp(String appName);

}