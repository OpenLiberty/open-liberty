/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.metadata;

import java.io.Serializable;

import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;

/**
 *
 */
public class ServiceModeInfo implements Serializable {
    private static final long serialVersionUID = -2438239508745943766L;
    private Service.Mode value;

    /**
     * @param value
     */
    public ServiceModeInfo(ServiceMode serviceMode) {
        this.value = serviceMode.value();
    }

    public ServiceModeInfo(Service.Mode mode) {
        this.value = mode;
    }

    public ServiceModeInfo(String modeValue) {
        if ("MESSAGE".equals(modeValue)) {
            this.value = Service.Mode.MESSAGE;
        } else {
            this.value = Service.Mode.PAYLOAD;
        }
    }

    public ServiceModeInfo() {
        this.value = Service.Mode.PAYLOAD;
    }

    /**
     * @return the value
     */
    public Service.Mode getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(Service.Mode value) {
        this.value = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServiceModeInfo other = (ServiceModeInfo) obj;
        if (value != other.value)
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ServiceModeInfo [value=" + value + "]";
    }

}
