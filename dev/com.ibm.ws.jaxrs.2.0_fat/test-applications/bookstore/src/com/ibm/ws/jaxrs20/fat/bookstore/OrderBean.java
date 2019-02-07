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
package com.ibm.ws.jaxrs20.fat.bookstore;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OrderBean {

    private Long id;
    private int weight;
    private Title customerTitle;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setWeight(int w) {
        this.weight = w;
    }

    public int getWeight() {
        return weight;
    }

    public Title getCustomerTitle() {
        return customerTitle;
    }

    public void setCustomerTitle(Title customerTitle) {
        this.customerTitle = customerTitle;
    }

    public static enum Title {
        MR,
        MS;
    }
}
