/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war;

import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/*
 * Server endpoint which sets a negative IdleTimeout
 * Logs are searched to confirm -12 value is found
 */
@ServerEndpoint(value = "/negativeTimeout")
public class NegativeTimeOutServerEP {

    Session session;

        @OnOpen
        public void onOpen(final Session session) {
            if (session != null) {
                this.session = session;
                // -12 will be searched in logs
                session.setMaxIdleTimeout(-12); 
            }
        }

        @OnMessage
        public String echo(String input) {    
            return String.valueOf(this.session.getMaxIdleTimeout());
        }

}

