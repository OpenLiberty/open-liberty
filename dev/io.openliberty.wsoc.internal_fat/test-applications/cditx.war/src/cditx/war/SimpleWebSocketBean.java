/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package cditx.war;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class SimpleWebSocketBean {

    String myResponse = "no response yet";
    int hc = 0;

    public SimpleWebSocketBean() {
        myResponse = ":SimpleWebSocketBean Inside Contructor";
    }

    @PostConstruct
    public void postCon() {
        myResponse = myResponse + ":SimpleWebSocketBean Inside PostContruct";
    }

    public String getResponse() {
        return myResponse = myResponse + ":SimpleWebSocketBean Inside getResponse";
    }

    @PreDestroy
    public void destruct() {
        // no, the test is not going there.
    }

}
