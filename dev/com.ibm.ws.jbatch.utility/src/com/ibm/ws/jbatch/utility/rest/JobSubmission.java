/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility.rest;

import java.util.Properties;

/**
 * Helper class for serializing the POST payload for submitting a 
 * new batch job to the REST api.
 */
public class JobSubmission {
    
    private String applicationName;
    
    private String moduleName;
    
    private String componentName;
    
    private String jobXMLName;
    
    private Properties jobParameters;
    
    private String jobXMLFile;
    
    public JobSubmission() {}
    
    public JobSubmission(String applicationName, 
                         String moduleName, 
                         String componentName,
                         String jobXMLName, 
                         Properties jobParameters,
                         String jobXMLFile) {
        this.applicationName = applicationName;
        this.moduleName = moduleName;
        this.componentName = componentName;
        this.jobXMLName = jobXMLName;
        this.jobParameters = jobParameters;
        this.jobXMLFile = jobXMLFile;
    }
    
    public String getApplicationName() {
        return applicationName;
    }
    
    public String getModuleName() {
        return moduleName;
    }
    
    public String getComponentName() {
        return componentName;
    }
    
    public String getJobXMLName() {
        return jobXMLName;
    }
    
    public Properties getJobParameters() {
        return jobParameters;
    }
    
    public String getJobXMLFile() {
        return jobXMLFile;
    }
}