/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.http.dispatcher.internal.channel;

import java.io.Serializable;
import java.util.HashMap;

import com.ibm.wsspi.threading.WorkContext;

/**
 *
 */
public class HttpWorkContext extends HashMap<String, Serializable> implements WorkContext {

    @Override
    public String getWorkType() {
        // TODO Auto-generated method stub
        return WorkContext.WORK_TYPE_HTTP;
    }
}