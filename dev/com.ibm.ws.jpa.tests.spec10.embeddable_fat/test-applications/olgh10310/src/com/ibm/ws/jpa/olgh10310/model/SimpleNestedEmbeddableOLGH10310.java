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

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class SimpleNestedEmbeddableOLGH10310 {

    @Column(name = "NESTED_VALUE")
    private Integer nestedValue;

    public SimpleNestedEmbeddableOLGH10310() { }

    public SimpleNestedEmbeddableOLGH10310(Integer nestedValue) {
        this.nestedValue = nestedValue;
    }
}
