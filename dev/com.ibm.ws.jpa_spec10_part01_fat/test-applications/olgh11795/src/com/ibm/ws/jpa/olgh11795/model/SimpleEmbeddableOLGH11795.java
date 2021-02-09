/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     01/06/2020 - Will Dazey
 *       - 347987: Fix Attribute Override for Complex Embeddables
 ******************************************************************************/
package com.ibm.ws.jpa.olgh11795.model;

import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Embeddable
public class SimpleEmbeddableOLGH11795 {

    @ManyToOne
    @JoinColumn(name = "BASE_PARENT_ID",
                referencedColumnName = "BASE_PARENT_ID")
    private SimpleParentEntityOLGH11795 parent;

    public SimpleParentEntityOLGH11795 getParent() {
        return parent;
    }

    public void setParent(SimpleParentEntityOLGH11795 parent) {
        this.parent = parent;
    }
}
