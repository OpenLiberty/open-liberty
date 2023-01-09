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
package com.ibm.ws.jpa.olgh16686.model;

import java.util.Date;

import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Embeddable
public class ElemCollEmbedTemporalOLGH16686 {

    @Temporal(value = TemporalType.DATE)
    private Date temporalValue;

    public ElemCollEmbedTemporalOLGH16686() {
    }

    public ElemCollEmbedTemporalOLGH16686(Date temporalValue) {
        this.temporalValue = temporalValue;
    }
}
