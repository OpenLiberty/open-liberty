/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.componentrenderer.jsf479.bean.faces40;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import com.ibm.ws.jsf22.fat.componentrenderer.jsf479.Thing;

@Named("things")
@RequestScoped
public class Things implements Serializable {
    private static final long serialVersionUID = 1L;
    Collection<Thing> things;

    public Collection<Thing> getThings() {
        return things;
    }

    public void setThings(Collection<Thing> things) {
        this.things = things;
    }

    public Things() {
        // Insert a bunch of Thing into Things.
        things = new ArrayList<Thing>();
        Thing t;
        for (int i = 0; i < 10; i++) {
            t = new Thing();
            t.setPropOne("One Thing " + i);
            t.setPropTwo("Two Thing " + i);
            things.add(t);
        }
    }
}
