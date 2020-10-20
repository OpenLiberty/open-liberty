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

package com.ibm.adapter.work;

import javax.resource.spi.work.WorkContext;

public class TestWorkContext extends TestWorkContextImpl implements WorkContext {

    @Override
    public String getDescription() {

        return "Test Work Context";
    }

    @Override
    public String getName() {

        return "TestWorkContext";
    }

}
