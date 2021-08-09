/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.endpoint;

/**
 * EndPointInfoMBean represents a defined endpoint within the channel framework. Use this interface to
 * access the host, name, and port attributes of the channel framework endpoint.
 * <p>
 * MBeans of this type can be queried using the following filter <p>
 * &nbsp;&nbsp;WebSphere:feature=channelfw,type=endpoint,&#42;
 * 
 * @ibm-api
 */
public interface EndPointInfoMBean {

    /**
     * Query the name of this endpoint.
     * 
     * @return String
     */
    public String getName();

    /**
     * Query the host assigned to this endpoint.
     * 
     * @return String
     */
    public String getHost();

    /**
     * Query the port assigned to this endpoint.
     * 
     * @return int
     */
    public int getPort();

}
