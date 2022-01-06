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
package com.ibm.ws.jpa.olgh19176.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class WeavedEntityAOLGH19176 extends WeavedAbstractMSOLGH19176 {

    @Id
    private long id;
    private Short hiddenAttribute;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
