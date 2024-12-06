/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
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
