/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.channelfw;

import java.util.List;

/**
 * Manager class that holds inbound endpoint definitions. These are
 * host/port combinations that represent inbound listening sockets.
 */
public interface EndPointMgr {

    /**
     * Create a new endpoint definition with the input parameters.
     *
     * @param name
     * @param host
     * @param port
     * @return EndPointInfo
     * @throws IllegalArgumentException if input values are incorrect
     */
    EndPointInfo defineEndPoint(String name, String host, int port);

    /**
     * Delete the endpoint that matches the provided name.
     *
     * @param name
     */
    void removeEndPoint(String name);

    /**
     * Query any existing endpoint defined with the input name.
     *
     * @param name
     * @return EndPointInfo, null if not found
     */
    EndPointInfo getEndPoint(String name);

    /**
     * Query all currently defined end points.
     *
     * @return List<EndPointInfo>, never null but might be empty
     */
    List<EndPointInfo> getEndsPoints();

    /**
     * Query the possible list of endpoints that match the provided
     * address and port. A wildcard address is * and a wildcard port
     * is 0.
     *
     * @param address
     * @param port
     * @return List<EndPointInfo>, never null but might be empty
     */
    List<EndPointInfo> getEndPoints(String address, int port);
}
