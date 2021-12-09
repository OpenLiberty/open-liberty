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
public class SimpleNestedEmbeddableOLGH10310Id implements Serializable{
    private static final long serialVersionUID = 1L;

    @Column(name = "NESTED_VALUE")
    private Integer nestedValue;

    public SimpleNestedEmbeddableOLGH10310Id() { }

    public SimpleNestedEmbeddableOLGH10310Id(Integer nestedValue) {
        this.nestedValue = nestedValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nestedValue == null) ? 0 : nestedValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimpleNestedEmbeddableOLGH10310Id other = (SimpleNestedEmbeddableOLGH10310Id) obj;
        if (nestedValue == null) {
            if (other.nestedValue != null)
                return false;
        } else if (!nestedValue.equals(other.nestedValue))
            return false;
        return true;
    }
}
