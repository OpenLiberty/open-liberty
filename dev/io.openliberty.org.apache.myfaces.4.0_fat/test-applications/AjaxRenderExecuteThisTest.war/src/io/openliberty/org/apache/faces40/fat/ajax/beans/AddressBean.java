/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.ajax.beans;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

@Named
@RequestScoped
public class AddressBean {

    private static final String counterCharacter = "^";

    //Control test for ajax to render messages
    private String testRenderStreet;
    private String testRenderCity;
    private String testRenderState;

    //Test ajax execute="@this"
    private String testExecuteThisStreet;
    private String testExecuteThisCity;
    private String testExecuteThisState;

    //Test ajax render="@this ..."
    private String testRenderThisStreet;
    private String testRenderThisCity;
    private String testRenderThisState;

    private static void addMessage(String clientId, String message) {
        FacesContext.getCurrentInstance().addMessage(clientId, new FacesMessage(message));
    }

    //// Setters that keep track of render calls
    public void setTestRenderStreet(String testRenderStreet) {
        addMessage("testRender", "setTestRenderStreet:" + testRenderStreet);
        this.testRenderStreet = testRenderStreet;
    }

    public void setTestRenderCity(String testRenderCity) {
        addMessage("testRender", "setTestRenderCity:" + testRenderCity);
        this.testRenderCity = testRenderCity;
    }

    public void setTestRenderState(String testRenderState) {
        addMessage("testRender", "setTestRenderState:" + testRenderState);
        this.testRenderState = testRenderState;
    }

    public void setTestExecuteThisStreet(String testExecuteThisStreet) {
        addMessage("testExecuteThis", "setTestExecuteThisStreet:" + testExecuteThisStreet);
        this.testExecuteThisStreet = testExecuteThisStreet;
    }

    public void setTestExecuteThisCity(String testExecuteThisCity) {
        addMessage("testExecuteThis", "setTestExecuteThisCity:" + testExecuteThisCity);
        this.testExecuteThisCity = testExecuteThisCity;
    }

    public void setTestExecuteThisState(String testExecuteThisState) {
        addMessage("testExecuteThis", "setTestExecuteThisState:" + testExecuteThisState);
        this.testExecuteThisState = testExecuteThisState;
    }

    public void setTestRenderThisStreet(String testRenderThisStreet) {
        addMessage("testRenderThis", "setTestRenderThisStreet:" + testRenderThisStreet);
        this.testRenderThisStreet = testRenderThisStreet;
    }

    public void setTestRenderThisCity(String testRenderThisCity) {
        addMessage("testRenderThis", "setTestRenderThisCity:" + testRenderThisCity);
        this.testRenderThisCity = testRenderThisCity;
    }

    public void setTestRenderThisState(String testRenderThisState) {
        addMessage("testRenderThis", "setTestRenderThisState:" + testRenderThisState);
        this.testRenderThisState = testRenderThisState;
    }

    ////// Simple getters

    public String getTestRenderStreet() {
        return testRenderStreet == null ? "" : testRenderStreet + counterCharacter;
    }

    public String getTestRenderCity() {
        return testRenderCity == null ? "" : testRenderCity + counterCharacter;
    }

    public String getTestRenderState() {
        return testRenderState == null ? "" : testRenderState + counterCharacter;
    }

    public String getTestExecuteThisStreet() {
        return testExecuteThisStreet == null ? "" : testExecuteThisStreet + counterCharacter;
    }

    public String getTestExecuteThisCity() {
        return testExecuteThisCity == null ? "" : testExecuteThisCity + counterCharacter;
    }

    public String getTestExecuteThisState() {
        return testExecuteThisState == null ? "" : testExecuteThisState + counterCharacter;
    }

    public String getTestRenderThisStreet() {
        return testRenderThisStreet == null ? "" : testRenderThisStreet + counterCharacter;
    }

    public String getTestRenderThisCity() {
        return testRenderThisCity == null ? "" : testRenderThisCity + counterCharacter;
    }

    public String getTestRenderThisState() {
        return testRenderThisState == null ? "" : testRenderThisState + counterCharacter;
    }

}
