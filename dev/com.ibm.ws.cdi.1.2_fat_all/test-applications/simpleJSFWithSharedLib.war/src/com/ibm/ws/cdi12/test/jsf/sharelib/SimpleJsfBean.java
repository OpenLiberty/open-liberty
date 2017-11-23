/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.jsf.sharelib;

import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.ws.cdi12.test.shared.InjectedHello;

@Named
public class SimpleJsfBean {
    private @Inject
    InjectedHello bean;

    public String getMessage() {
        String response = "SimpleJsfBean";
        if (this.bean != null) {
            response = response + " injected with: " + this.bean.areYouThere(response);
        }
        return response;
    }
}
