/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.rest.utils;

public class WSPurgeResponse {
    
    long instanceId = -1;
    PurgeStatus purgeStatus = null;
    String message = null;
    String redirectURL = null;
    
    public WSPurgeResponse() {}
    
    public WSPurgeResponse(long instanceId, PurgeStatus purgeStatus, String message, String redirectURL) {
        this.instanceId = instanceId;
        this.purgeStatus = purgeStatus;
        this.message = message;
        this.redirectURL = redirectURL;
    }

    public PurgeStatus getPurgeStatus() {
        return purgeStatus;
    }

    public void setPurgeStatus(PurgeStatus purgeStatus) {
        this.purgeStatus = purgeStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRedirectURL() {
        return redirectURL;
    }

    public void setRedirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(long instanceId) {
        this.instanceId = instanceId;
    }
    
    
}
