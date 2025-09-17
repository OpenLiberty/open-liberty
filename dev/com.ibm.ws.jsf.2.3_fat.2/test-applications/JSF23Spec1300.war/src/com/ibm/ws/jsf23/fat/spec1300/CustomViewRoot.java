/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
package com.ibm.ws.jsf23.fat.spec1300;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

/**
 * A custom UIViewRoot implementation.
 */
public class CustomViewRoot extends UIViewRoot {

    public CustomViewRoot() {
        super();
        FacesContext.getCurrentInstance().getExternalContext().log("CustomViewRoot invoked");
    }
}
