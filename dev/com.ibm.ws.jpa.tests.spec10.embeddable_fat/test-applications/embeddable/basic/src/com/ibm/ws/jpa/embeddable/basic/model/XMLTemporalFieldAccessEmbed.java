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

package com.ibm.ws.jpa.embeddable.basic.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class XMLTemporalFieldAccessEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Date temporalValueFA;

    public XMLTemporalFieldAccessEmbed() {
    }

    public XMLTemporalFieldAccessEmbed(Date temporalValueFA) {
        this.temporalValueFA = temporalValueFA;
    }

    public Date getTemporalValueFA() {
        return this.temporalValueFA;
    }

    public void setTemporalValueFA(Date temporalValueFA) {
        this.temporalValueFA = temporalValueFA;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLTemporalFieldAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
        return "temporalValueFA=" + (temporalValueFA != null ? sdf.format(temporalValueFA).toString() : "null");
    }

}
