package com.ibm.wsspi.anno.service;

import java.util.Optional;

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
     * @return An AppKey for the given appName, or null if no key was found.
     */
    public AnnotationService_KeyService.AppKey getKeyForApp(String appName);  

    public class AppKey {
        private final String deploymentName;

        public AppKey(String deploymentName) {
            this.deploymentName = deploymentName;
        }

        public String getDeploymentName() {
            return deploymentName;
        }

        @Override
        public int hashCode() {
            return deploymentName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AppKey other = (AppKey) obj;

            return deploymentName.equals(other.getDeploymentName());
        }
    }

}