/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.jsonsupport;

import com.ibm.ws.jsonsupport.internal.JSONJacksonImpl;

/**
 *
 */
public class JSONFactory {

    private static JSON json;

    public static synchronized JSON newInstance() throws JSONMarshallException {
        if (json == null)
            json = new JSONJacksonImpl();
        return json;
    }

    public static JSON newInstance(JSONSettings settings) throws JSONMarshallException {
        return new JSONJacksonImpl(settings);
    }
}
