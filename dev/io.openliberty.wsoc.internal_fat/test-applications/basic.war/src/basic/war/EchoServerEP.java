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

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

/*
* Echos messages sent to this endpoint.
*/
public class EchoServerEP {

    public EchoServerEP(){

    }

    @OnOpen
    public void onOpen(final Session session) {

    }

    @OnMessage
    public String echo(String input) {
        return input;
    }

}
