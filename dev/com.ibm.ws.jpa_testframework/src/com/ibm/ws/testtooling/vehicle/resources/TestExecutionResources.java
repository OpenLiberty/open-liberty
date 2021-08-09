/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testtooling.vehicle.resources;

import java.util.HashMap;

import com.ibm.ws.testtooling.msgcli.MessagingClient;

public class TestExecutionResources {
    private HashMap<String, JPAResource> jpaResourceMap = new HashMap<String, JPAResource>();
    private HashMap<String, MessagingClient> msgCliResourceMap = new HashMap<String, MessagingClient>();

    public TestExecutionResources() {}

    public final HashMap<String, JPAResource> getJpaResourceMap() {
        return jpaResourceMap;
    }

    public HashMap<String, MessagingClient> getMsgCliResourceMap() {
        return msgCliResourceMap;
    }
}
