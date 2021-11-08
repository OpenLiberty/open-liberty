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
package com.ibm.ws.jpa.olgh16686.model;

import java.util.Date;

import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Embeddable
public class ElementCollectionEmbeddableTemporalOLGH16686 {

    @Temporal(value = TemporalType.DATE)
    private Date temporalValue;

    public ElementCollectionEmbeddableTemporalOLGH16686() {
    }

    public ElementCollectionEmbeddableTemporalOLGH16686(Date temporalValue) {
        this.temporalValue = temporalValue;
    }
}
