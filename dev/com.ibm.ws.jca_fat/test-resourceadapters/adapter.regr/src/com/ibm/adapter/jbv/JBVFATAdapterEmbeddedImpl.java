/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.jbv;

import javax.resource.spi.ResourceAdapter;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * This is a test Resource Adapter implementation class that extends
 * JBVFATAdapterImpl. This class is used to test JavaBean Validation.
 *
 */
public class JBVFATAdapterEmbeddedImpl extends JBVFATAdapterImpl implements ResourceAdapter {

    private int idleTimeout;

    private Boolean fail = false;

    private String mode;

    @Min(value = 0)
    private final float cost = 0;

    public JBVFATAdapterEmbeddedImpl() {
        super();
    }

    @Max(value = 100, message = "The maximum value allowed is 100")
    @Min(value = 10)
    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    @NotNull(message = "This property cannot be null")
    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isFail() {
        return fail;
    }

    public void setFail(boolean fail) {
        this.fail = fail;
    }

    /*
     * private void changeStatus(){ String propValue =
     * System.getProperty("adapter_jca16_jbv_ResourceAdapter_fail");
     * if(propValue != null){ fail = Boolean.parseBoolean(propValue); } else {
     * fail = false; } if(fail) cost = -1; else cost = 10; }
     */

    @Override
    public String getDataBaseName() {
        return dataBaseName;
    }

    public void setDataBaseName(String dataBaseName) {
        this.dataBaseName = dataBaseName;
    }
}
