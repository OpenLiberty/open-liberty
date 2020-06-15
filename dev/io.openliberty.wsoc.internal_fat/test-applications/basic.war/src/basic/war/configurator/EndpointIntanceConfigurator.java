/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
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
