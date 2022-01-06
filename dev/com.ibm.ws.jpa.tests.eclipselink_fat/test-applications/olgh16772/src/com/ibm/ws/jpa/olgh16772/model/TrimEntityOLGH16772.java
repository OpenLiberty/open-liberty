/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.olgh16772.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TrimEntityOLGH16772 {

    @Id
    private long id;

    private String strVal1;

    public TrimEntityOLGH16772() {
    }

    public TrimEntityOLGH16772(Long id, String strVal1) {
        this.id = id;
        this.strVal1 = strVal1;
    }
}
