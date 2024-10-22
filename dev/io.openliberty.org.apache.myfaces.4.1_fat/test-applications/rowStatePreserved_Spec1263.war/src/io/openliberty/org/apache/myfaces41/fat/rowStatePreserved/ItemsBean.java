/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces41.fat.rowStatePreserved;

import java.io.Serializable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

@Named
@SessionScoped
public class ItemsBean implements Serializable {
 
    private Item[] arr = new Item[2];

    @PostConstruct
    public void init(){
        arr[0] = new Item();
        arr[1] = new Item();
    }

   
    public Item[] getItems() {
        return arr;
    }

    public void submit(){
        // no op
    }
}
