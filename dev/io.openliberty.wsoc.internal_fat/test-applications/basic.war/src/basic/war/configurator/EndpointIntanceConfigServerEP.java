/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war.configurator;

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

/**
 * Tests custom ServerEndpoint Configurator's getEndpointInstance() method - 3.1.7 Customizing Endpoint Creation
 */
@ServerEndpoint(value = "/endpointInstanceConfigurator", configurator = EndpointIntanceConfigurator.class)
public class EndpointIntanceConfigServerEP {

    private String mortgageType;
    private double federalRate;

    @OnMessage
    public String onMessage(String text) {
        String returnText;
        if (mortgageType.equals("30 Year") && federalRate == 3.5)
            returnText = text; //success
        else
            returnText = "fail"; //failed
        return returnText;
    }

    public void initializeRate() {
        this.mortgageType = "30 Year";
        this.federalRate = 3.5;
    }
}
