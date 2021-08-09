/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.viewparameters.beans;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

/* A bean to test the NotNull annotation on a field that will be tested against the f:viewParam component in JSF 2.3
 * This is covering the JSF 2.3 spec issue in https://github.com/javaee/javaserverfaces-spec/issues/1329
 */

@Named
@RequestScoped
public class ViewParamBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull
    private String text1;

    @NotNull
    private String text2;

    private String text3;

    public ViewParamBean() {}

    /**
     * Return the text1.
     *
     * @return The text1 as a String.
     */
    public String getText1() {
        return text1;
    }

    /**
     * Set the text1.
     *
     * @param text1 The text1.
     */
    public void setText1(String text1) {
        this.text1 = text1;
    }

    /**
     * Return the text2.
     *
     * @return The text2 as a String.
     */
    public String getText2() {
        return text2;
    }

    /**
     * Set the text2.
     *
     * @param text2 The text2.
     */
    public void setText2(String text2) {
        this.text2 = text2;
    }

    /**
     * Return the text3.
     *
     * @return The text3 as a String.
     */
    public String getText3() {
        return text3;
    }

    /**
     * Set the text3.
     *
     * @param text3 The text3.
     */
    public void setText3(String text3) {
        this.text3 = text3;
    }
}
