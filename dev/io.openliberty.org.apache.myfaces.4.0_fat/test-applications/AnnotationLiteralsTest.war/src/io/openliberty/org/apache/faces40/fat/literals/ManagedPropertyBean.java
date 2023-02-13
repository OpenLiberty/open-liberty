/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.literals;

import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 * This test bean has property methods which will be used to test the @ManagedBean literal
 */
@Named(value = "managedPropertyBean")
@RequestScoped
public class ManagedPropertyBean {

    public Integer getMyInteger() {
        return 42;
    }

    public Map<String, String> getMyStringMap() {
        return Map.of("foo", "bar");
    }

}
