/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.jacksonJsonIgnore;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TestPojo {
    String fish = "fish";

    public String getFish() {
        return fish;
    }

    public void setFish(String fish) {
        this.fish = fish;
    }

    @JsonIgnore
    boolean nod = true;

    @JsonIgnore
    public boolean getNod() {
        return nod;
    }

    @JsonIgnore
    public boolean getNew() {
        return true;
    }
}
