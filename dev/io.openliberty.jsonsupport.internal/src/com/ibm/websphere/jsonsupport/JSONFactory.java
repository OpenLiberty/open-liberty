/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
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
package com.ibm.websphere.jsonsupport;

import com.ibm.ws.jsonsupport.internal.JSONJacksonImpl;

/**
 *
 */
public class JSONFactory {

    private static JSON json;

    public static synchronized JSON newInstance() {
        if (json == null)
            json = new JSONJacksonImpl();
        return json;
    }

    public static JSON newInstance(JSONSettings settings) {
        return new JSONJacksonImpl(settings);
    }
}
