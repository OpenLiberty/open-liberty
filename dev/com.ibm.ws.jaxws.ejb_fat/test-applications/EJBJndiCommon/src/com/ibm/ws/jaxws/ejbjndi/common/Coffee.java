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
package com.ibm.ws.jaxws.ejbjndi.common;

public class Coffee {

    private final StringBuffer coffee = new StringBuffer();

    public Coffee() {
    }

    public Coffee(String type) {
        coffee.append("Type = [" + type + "]");
    }

    public void addMilk(String milk) {
        coffee.append(" Milk = [" + milk + "]");
    }

    public void addCoffeemate(String coffeemate) {
        coffee.append(" coffeemate = [" + coffeemate + "]");
    }

    public String getCoffee() {
        return coffee.toString();
    }
}
