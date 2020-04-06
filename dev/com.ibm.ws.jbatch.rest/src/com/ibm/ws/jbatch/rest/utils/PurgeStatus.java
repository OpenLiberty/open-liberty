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
package com.ibm.ws.jbatch.rest.utils;

public enum PurgeStatus {
    
    /**
     * Job purge completed successfully
     */
    COMPLETED,
    
    /**
     * Job purge failed
     */
    FAILED,
    
    /**
     * Job purge failed because it was still active
     */
    STILL_ACTIVE,
    
    /**
     * Database purge failed, but the Job Logs were successfully purged
     */
    JOBLOGS_ONLY,
    
    /**
     * Job purge failed because the job isn't local
     */
    NOT_LOCAL;

}
