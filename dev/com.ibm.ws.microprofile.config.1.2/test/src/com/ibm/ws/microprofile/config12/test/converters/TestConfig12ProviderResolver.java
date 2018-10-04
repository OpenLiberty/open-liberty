/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config12.test.converters;

import com.ibm.ws.microprofile.config12.archaius.Config12ProviderResolverImpl;

public class TestConfig12ProviderResolver extends Config12ProviderResolverImpl {

    @Override
    public String getApplicationName() {
        return "UNIT_TEST_APPLICATION";
    }

}
