/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war;

import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import java.io.IOException;
import java.util.Map;

/*
 * OnMessage verifies two value are seen (SERVER-1 & MODIFY-1)
 * Then it swaps MODIFY-1 for MODIFY-2 and then sends the two user properties 
 * to the client to be verified.
 */
public class UserPropertiesServerEP extends Endpoint implements MessageHandler.Whole<String> {

  Session session;

  public void onOpen(Session session, EndpointConfig config) {
    this.session = session;
    // UserPropertiesServerEP extends MessageHandler so we can reference session from onMessage
    session.addMessageHandler(this);
  }

  public void onMessage(String msg) {

    Map<String, Object> userProperties = this.session.getUserProperties();
    if (userProperties.size() != 2) {
      throw new IllegalStateException("User properties map size differs. Expected: 2, Actual: " + userProperties.size());
    }

    // Modify user properties to test if they do not affect other endpoint sessions
    userProperties.remove("MODIFY-1");
    userProperties.put("MODIFY-2", new Object());

    for (Map.Entry<String, Object> entry : userProperties.entrySet()) {
      try {
        this.session.getBasicRemote().sendText(entry.getKey());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void onError(Session session, Throwable error) {
    error.printStackTrace();
  }
}
