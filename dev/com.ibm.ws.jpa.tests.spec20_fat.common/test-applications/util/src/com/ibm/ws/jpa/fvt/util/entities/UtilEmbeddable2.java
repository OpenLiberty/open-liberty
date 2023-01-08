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

package com.ibm.ws.jpa.fvt.util.entities;

import javax.persistence.Embeddable;

@Embeddable
public class UtilEmbeddable2 {

    private String embName2;

    public String getEmbName2() {
        return embName2;
    }

    public void setEmbName2(String name2) {
        this.embName2 = name2;
    }
}
