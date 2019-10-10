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
package com.ibm.ws.jca.internal;

import java.io.Serializable;
import java.util.HashMap;

import com.ibm.wsspi.threading.WorkContext;

//Implements WorkContext class to add context to JCA threads.
public class JCAWorkContext extends HashMap<String, Serializable> implements WorkContext {

    JCAWorkContext() {
        super();
    }

    @Override
    public String getWorkType() {
        return WorkContext.WORK_TYPE_JCA_MDB;
    }

}