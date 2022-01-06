/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.providers;

import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.impl.RequestPreprocessor;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.message.Message;


public class NoOpPreprocessor extends RequestPreprocessor {

    @Override
    public String preprocess(Message m, UriInfo u) {
        return new UriInfoImpl(m, null).getPath();
    }
}
