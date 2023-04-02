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

import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;

@Embeddable
public class UtilEmbeddable {

    private String embName;
    private String embNotLoaded;

    public String getEmbName() {
        return embName;
    }

    public void setEmbName(String name) {
        this.embName = name;
    }

    @Basic(fetch = FetchType.LAZY)
    public String getEmbNotLoaded() {
        return embNotLoaded;
    }

    public void setEmbNotLoaded(String notLoaded) {
        this.embNotLoaded = notLoaded;
    }
}
