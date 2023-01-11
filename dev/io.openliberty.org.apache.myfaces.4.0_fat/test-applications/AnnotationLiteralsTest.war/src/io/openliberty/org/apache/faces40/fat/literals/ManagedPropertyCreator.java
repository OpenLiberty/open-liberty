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

import java.io.Serializable;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.annotation.ManagedProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * This class just injects the managed properties forcing CDI to create @ManagedProperty beans for testing
 */
@Named(value = "managedPropertyCreator")
@ApplicationScoped
public class ManagedPropertyCreator implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    @ManagedProperty("#{managedPropertyBean.myInteger}")
    private Integer myInteger;

    @Inject
    @ManagedProperty("#{managedPropertyBean.myStringMap}")
    private Map<String, String> myStringMap;

}
