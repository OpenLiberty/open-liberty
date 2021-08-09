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

package com.ibm.adapter.jbv;

import javax.validation.constraints.Size;

import com.ibm.adapter.FVTAdapterImpl;

public class JBVFATAdapterImpl extends FVTAdapterImpl implements JBVFATAdapter {

    @Size(max = 10, message = "The maximum size is 10")
    protected String dataBaseName;

    @Override
    public String getDataBaseName() {
        return dataBaseName;
    }

}
