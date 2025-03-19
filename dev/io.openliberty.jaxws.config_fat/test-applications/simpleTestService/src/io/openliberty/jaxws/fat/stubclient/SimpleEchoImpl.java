/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jaxws.fat.stubclient;

import javax.jws.WebService;

import io.openliberty.jaxws.fat.stubclient.client.SimpleEcho;

@WebService(serviceName="SimpleEchoService", portName="SimpleEchoPort")
public class SimpleEchoImpl implements SimpleEcho {

    public String echo(String value) {
        return value;
    }
}
