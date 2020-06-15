/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war.configurator;

import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

/**
 * Test for this section of the spec
 * "3.1.6 Custom State or Processing Across Server Endpoint Instances
 * The developer may also implement ServerEndpointConfig.Configurator in order to hold custom application
 * state or methods for other kinds of application specific processing that is accessible from all Endpoint
 * instances of the same logical endpoint via the EndpointConfig object."
 **/
@ServerEndpoint(value = "/customStateConfigurator", configurator = CustomStateConfigurator.class)
public class CustomStateConfiguratorServerEP {
    ServerEndpointConfig endPointConfig;

    @OnMessage
    public String onMessage(String text) {
        Integer state = ((CustomStateConfigurator) endPointConfig.getConfigurator()).getState();
        if (state == 0) { //call1. Sets the custom state to 1 
            ((CustomStateConfigurator) endPointConfig.getConfigurator()).setState(1);
        } else if (state == 1) {
            //call2. Checks the previously set state and seets the custom state to 2
            ((CustomStateConfigurator) endPointConfig.getConfigurator()).setState(2);
        }
        String returnState = ((CustomStateConfigurator) endPointConfig.getConfigurator()).getState().toString();
        return returnState;
    }

    @OnOpen
    public void onOpen(Session sess, EndpointConfig epc) {
        try {
            this.endPointConfig = (ServerEndpointConfig) epc;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
