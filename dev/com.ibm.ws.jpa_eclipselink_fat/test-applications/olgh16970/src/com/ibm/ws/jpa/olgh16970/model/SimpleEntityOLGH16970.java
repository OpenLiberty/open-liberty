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
package com.ibm.ws.jpa.olgh16970.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SimpleEntityOLGH16970 {

    @Id
    private long id;

    private Integer intVal1;
    private String strVal1;

    public SimpleEntityOLGH16970() {
    }

    public SimpleEntityOLGH16970(long id, Integer intVal1, String strVal1) {
        this.id = id;
        this.intVal1 = intVal1;
        this.strVal1 = strVal1;
    }
}
