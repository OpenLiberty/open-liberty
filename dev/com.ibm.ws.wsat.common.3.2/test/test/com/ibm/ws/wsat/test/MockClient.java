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
package com.ibm.ws.wsat.test;

import java.lang.reflect.Field;

import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.WebClient;

/**
 * Mock web client for unit tests
 */
public class MockClient extends WebClient {
    public MockClient() {
        try {
            Field f = WebClient.class.getDeclaredField("testClient");
            f.setAccessible(true);
            f.set(null, this);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Override
    public EndpointReferenceType register(EndpointReferenceType participant) throws WSATException {
        return null;
    }

    @Override
    public void prepare() throws WSATException {}

    @Override
    public void rollback() throws WSATException {}

    @Override
    public void commit() throws WSATException {}

    @Override
    public void prepared() throws WSATException {}

    @Override
    public void readOnly() throws WSATException {}

    @Override
    public void aborted() throws WSATException {}

    @Override
    public void committed() throws WSATException {}

}
