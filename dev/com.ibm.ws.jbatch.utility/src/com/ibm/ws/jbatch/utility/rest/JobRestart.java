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
 * Helper class for serializing the POST payload for the batch 
 * REST restart api.
 */
public class JobRestart {
    
    private Properties jobParameters;
    
    public JobRestart() {}
    
    public JobRestart(Properties jobParameters) {
        this.jobParameters = jobParameters;
    }
    
    public Properties getJobParameters() {
        return jobParameters;
    }
}