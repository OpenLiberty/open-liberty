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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class SimpleEmbeddableOLGH10310 implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "VALUE")
    private Integer value;

    private SimpleNestedEmbeddableOLGH10310 nestedValue;

    public SimpleEmbeddableOLGH10310() {
    }

    public SimpleEmbeddableOLGH10310(Integer value, SimpleNestedEmbeddableOLGH10310 nestedValue) {
        this.value = value;
        this.nestedValue = nestedValue;
    }
}
