/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.jsf23.fat.cdi.common.beans.faces40;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 * This @Named bean is only used during the EE10 Repeat. The jakarta.faces.bean
 * package was removed in Faces 4.0.
 */
@Named("testCustomBean")
@SessionScoped
public class TestCustomBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String data = ":TestCustomBean:";

    public void setData(String newData) {
        this.data += newData;
    }

    public String getData() {
        return this.data;
    }

}
