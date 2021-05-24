/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation. All rights reserved.
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
package com.ibm.ws.jpa.olgh10310.model;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
public class SimpleEntityOLGH10310 {

    @EmbeddedId
    private SimpleEmbeddableOLGH10310Id id;

    @Embedded
    //OverrideEmbeddableA.value must have the same field name and Column name as OverrideEmbeddableIdA.value
    @AttributeOverrides({
                          @AttributeOverride(name = "value", column = @Column(name = "OVERRIDE_VALUE")),
                          @AttributeOverride(name = "nestedValue.nestedValue", column = @Column(name = "OVERRIDE_NESTED_VALUE")) })
    private SimpleEmbeddableOLGH10310 id2;

    public SimpleEmbeddableOLGH10310Id getId() {
        return id;
    }

    public void setId(SimpleEmbeddableOLGH10310Id id) {
        this.id = id;
    }

    public SimpleEmbeddableOLGH10310 getId2() {
        return id2;
    }

    public void setId2(SimpleEmbeddableOLGH10310 id2) {
        this.id2 = id2;
    }
}
