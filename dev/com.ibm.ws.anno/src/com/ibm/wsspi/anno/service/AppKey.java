package com.ibm.wsspi.anno.service;

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
        if (deploymentName == null) {
            return 0;
        }
        
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
        
        if (deploymentName == null) {
            return other.getDeploymentName() == null;
        }

        return deploymentName.equals(other.getDeploymentName());
    }
}