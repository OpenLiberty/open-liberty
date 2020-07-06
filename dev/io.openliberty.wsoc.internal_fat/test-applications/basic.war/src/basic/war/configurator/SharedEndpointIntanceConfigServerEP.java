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

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * Tests custom ServerEndpoint Configurator's getEndpointInstance() method - 3.1.7 Customizing Endpoint Creation
 * this test shows how customer can share same instance across clients. In this case, as per the spec it's customer's responsibility
 * to make their code thread safe, although this test doesn't have thread safety as it's not needed for this simple test.
 */
@ServerEndpoint(value = "/sharedEndpointInstanceConfigurator", configurator = SharedEndpointIntanceConfigurator.class)
public class SharedEndpointIntanceConfigServerEP {

    private String mortgageType;
    private Double federalRate;
    private int closeCode;

    @OnMessage
    public String onMessage(String text) {
        String returnText = null;
        if (mortgageType == null && federalRate == null) { //call from client 1. Sets the mortage rate 
            mortgageType = "30 Year";
            federalRate = 3.5;
            returnText = text;
        }
        else if (mortgageType.equals("30 Year") && federalRate == 3.5) {
            //call from client 2. Since the endpoint instance is shared across client 1 and client 2, the rate should be already set for clinet2 call
            returnText = text;
        }
        return returnText;
    }

    //tests maxMessageSize
    //1st client call - incoming message size is 5 which is larger than maxMessageSize. Hence @OnMessage doesn't get called. Instead,
    //@OnClose gets called with closeCode TOO_BIG(1009). closeCode is set in the member variable of this endoint instance.
    //2nd client call - incoming message size is 4 which is <= maxMessageSize. Hence @OnMessage gets invoked. @OnMessage returns the 
    //closeCode set from previous client call from onClose() since this test is uses custom ServerEndpoint Configurator where same endpoint
    //instances are shared across multiple clients.

    @OnMessage(maxMessageSize = 4)
    public byte[] echoText(byte[] data) {
        byte[] returnValue = new byte[1];
        returnValue[0] = new Integer(closeCode).byteValue();
        return returnValue;
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        //  try {
        if (reason != null) {
            if (reason.getCloseCode().getCode() == CloseReason.CloseCodes.TOO_BIG.getCode()) {
                closeCode = CloseReason.CloseCodes.TOO_BIG.getCode();
            }
            // session.close();
        }
        // } catch (IOException e) {
        //      e.printStackTrace();
        //  }
    }
}
