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
package io.openliberty.org.apache.faces40.fat.layoutattribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named
@RequestScoped
public class ShoppingCenterBean {

    List<Shop> shops = new ArrayList<>();

    @PostConstruct
    public void populateInventory() {

        Shop grocery = new Shop("Buchannan", Arrays.asList(//
                                                           new Item(0L, "Banana", 15, 0.99), //
                                                           new Item(1L, "Apple", 100, 1.15), //
                                                           new Item(2L, "Potato", 75, 0.75), //
                                                           new Item(3L, "Spinach", 5, 2) //
        ));
        Shop clothing = new Shop("Jeffersons", Arrays.asList(//
                                                             new Item(10L, "Shirt", 10, 9.99), //
                                                             new Item(11L, "Pants", 15, 25.99), //
                                                             new Item(12L, "Socks", 60, 5.50), //
                                                             new Item(13L, "Tie", 5, 30.00) //
        ));

        shops.addAll(Arrays.asList(grocery, clothing));
    }

    /**
     * @return the shops
     */
    public List<Shop> getShops() {
        return shops;
    }

    /**
     * @param shops the shops to set
     */
    public void setShops(List<Shop> shops) {
        this.shops = shops;
    }
}
