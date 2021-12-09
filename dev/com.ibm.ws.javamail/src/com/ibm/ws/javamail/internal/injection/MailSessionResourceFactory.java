/*******************************************************************************
 * Copyright (c) 2015,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javamail.internal.injection;

import java.util.Map;

import com.ibm.ws.javamail.internal.MailSessionService;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 */
public class MailSessionResourceFactory extends MailSessionService implements com.ibm.ws.resource.ResourceFactory {

    /**
     * @see com.ibm.ws.resource.ResourceFactory#createResource(com.ibm.ws.resource.ResourceRefInfo)
     */
    @Override
    public Object createResource(ResourceRefInfo ref) throws Exception {
        return createResource((ResourceInfo) ref);
    }

    /**
     *
     * @throws Exception if an error occurs.
     */
    @Override
    public void destroy() throws Exception {
    }

    @Override
    public void modify(Map<String, Object> props) throws Exception {
        throw new UnsupportedOperationException();
    }
}
