/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.dispatcher.internal.channel;

import java.util.HashMap;

import com.ibm.wsspi.threading.WorkContext;

/**
 *
 */
public class HttpWorkContext extends HashMap<String, String> implements WorkContext {

    @Override
    public String getWorkType() {
        // TODO Auto-generated method stub
        return WorkContext.WORK_TYPE_HTTP;
    }

}