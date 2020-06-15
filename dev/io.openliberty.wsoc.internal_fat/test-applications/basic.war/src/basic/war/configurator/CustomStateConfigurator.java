/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
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
