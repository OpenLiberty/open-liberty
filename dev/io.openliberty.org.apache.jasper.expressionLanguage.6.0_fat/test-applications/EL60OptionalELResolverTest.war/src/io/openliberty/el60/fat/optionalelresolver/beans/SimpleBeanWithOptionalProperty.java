/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el60.fat.optionalelresolver.beans;

import java.util.Optional;

/**
 * A bean to use for testing the Jakarta Expression Language 6.0 OptionalELResolver.
 *
 * This bean has a property of type Optional<String> as well as a String property
 * that has a getter that returns an Optional<String>.
 *
 * The AnotherSimpleBean property is also returned as an Optional<AnotherSimpleBean> and is used
 * to test method invocation with the OptionalELResolver.
 */
public class SimpleBeanWithOptionalProperty {
    private String testString;
    private AnotherSimpleBean anotherSimpleBean;
    private Optional<String> optionalString;

    public SimpleBeanWithOptionalProperty() {
    }

    public SimpleBeanWithOptionalProperty(String testString) {
        this.testString = testString;
    }

    public SimpleBeanWithOptionalProperty(AnotherSimpleBean anotherSimpleBean) {
        this.anotherSimpleBean = anotherSimpleBean;
    }

    public void setTestString(String testString) {
        this.testString = testString;
    }

    public Optional<String> getTestString() {
        return Optional.ofNullable(testString);
    }

    public void setAnotherSimpleBean(AnotherSimpleBean anotherSimpleBean) {
        this.anotherSimpleBean = anotherSimpleBean;
    }

    public Optional<AnotherSimpleBean> getAnotherSimpleBean() {
        return Optional.ofNullable(anotherSimpleBean);
    }

    public void setOptionalString(Optional<String> optionalString) {
        this.optionalString = optionalString;
    }

    public Optional<String> getOptionalString() {
        return this.optionalString;
    }
}
