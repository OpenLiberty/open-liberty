/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mprestclient.fat.myrestclient.bundle;

import io.openliberty.mprestclient.fat.myrestclient.internal.MyRestAPI;

public class MyRestAPIWrapper {
    
    private final MyRestAPI restAPI;

    public MyRestAPIWrapper(MyRestAPI restAPI) {
        this.restAPI = restAPI;
    }

    public String greet() {
        return restAPI.greet();
    }

}
