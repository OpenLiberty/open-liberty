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
 * Tests custom ServerEndpoint Configurator's getEndpointInstance() method - 3.1.7 Customizing Endpoint Creation
 * getEndpointInstance() initializes default value for the ServerEndpoint for every Endpoint
 */
public class EndpointIntanceConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        try {
            EndpointIntanceConfigServerEP endpointInstance = (EndpointIntanceConfigServerEP) endpointClass.newInstance();
            //initialize some customer default data for every endpoint instance.
            endpointInstance.initializeRate();
            return (T) endpointInstance;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
