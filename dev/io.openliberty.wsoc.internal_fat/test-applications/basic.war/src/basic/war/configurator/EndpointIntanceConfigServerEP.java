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
