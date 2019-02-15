/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.test;

/**
 *
 */
public class Pizza {
    private String toping;
    private int size;

    public Pizza(String toping, int size) {
        this.setTopping(toping);
        this.setSize(size);
    }

    /**
     * @return the toping
     */
    public String getToping() {
        return toping;
    }

    /**
     * @param topping the toping to set
     */
    public void setTopping(String toping) {
        this.toping = toping;
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return getSize() + " inch " + getToping() + " pizza";
    }

}