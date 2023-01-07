/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.adapter.jbv;

import javax.validation.constraints.Size;

import com.ibm.adapter.FVTAdapterImpl;

public class JBVFATAdapterImpl extends FVTAdapterImpl implements JBVFATAdapter {

    @Size(max = 10, message = "The maximum size is 10")
    protected String dataBaseName;

    public String getDataBaseName() {
        return dataBaseName;
    }

}
