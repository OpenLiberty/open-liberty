/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.olgh10515.model;

import java.io.Serializable;
import java.util.Objects;

public class SimpleEntityIdOLGH10515 implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;

    private int version;

    public SimpleEntityIdOLGH10515() {}

    public SimpleEntityIdOLGH10515(String id, int version) {
        this.id = id;
        this.version = version;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SimpleEntityIdOLGH10515)) {
            return false;
        }
        final SimpleEntityIdOLGH10515 carDtoId = (SimpleEntityIdOLGH10515) other;
        return this.version == carDtoId.version &&
               this.id.equals(carDtoId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.version);
    }
}
