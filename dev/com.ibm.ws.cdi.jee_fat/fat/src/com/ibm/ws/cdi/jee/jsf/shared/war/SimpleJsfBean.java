/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.cdi.jee.jsf.shared.war;

import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.ws.cdi.jee.jsf.shared.lib.InjectedHello;

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
