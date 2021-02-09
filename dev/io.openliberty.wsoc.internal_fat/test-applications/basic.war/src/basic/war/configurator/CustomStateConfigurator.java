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
package basic.war.configurator;

import javax.websocket.server.ServerEndpointConfig;

/**
 * Test for this section of the spec
 * "3.1.6 Custom State or Processing Across Server Endpoint Instances
 * The developer may also implement ServerEndpointConfig.Configurator in order to hold custom application
 * state or methods for other kinds of application specific processing that is accessible from all Endpoint
 * instances of the same logical endpoint via the EndpointConfig object."
 **/
public class CustomStateConfigurator extends ServerEndpointConfig.Configurator {

    public Integer state = 0;

    /**
     * @return the state
     */
    public Integer getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(Integer state) {
        this.state = state;
    }

}
