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
 * this test shows how customer can share same instance across clients. In this case, as per the spec it's customer's responsibility
 * to make their code thread safe, although this test doesn't have thread safety as it's not needed for this simple test.
 */
public class SharedEndpointIntanceConfigurator extends ServerEndpointConfig.Configurator {

    public SharedEndpointIntanceConfigServerEP endpointInstance;

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        try {
            if (endpointInstance == null)
                endpointInstance = (SharedEndpointIntanceConfigServerEP) endpointClass.newInstance();
            return (T) endpointInstance;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
