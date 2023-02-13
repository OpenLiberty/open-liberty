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
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.faces.annotation.FlowMap;
import jakarta.inject.Named;

/**
 * This test bean looks up the FlowMap literal, and puts data into the map for testing
 */
@Named(value = "flowMapBean")
@RequestScoped
@SuppressWarnings("serial")
public class FlowMapBean {

    private Map<Object, Object> flowMap;

    public void initFoo() {
        if (flowMap == null)
            flowMap = CDI.current().select(new TypeLiteral<Map<Object, Object>>() {}, FlowMap.Literal.INSTANCE).get();

        flowMap.put("foo", "bar");
    }

    public String getFoo() {
        if (flowMap == null)
            flowMap = CDI.current().select(new TypeLiteral<Map<Object, Object>>() {}, FlowMap.Literal.INSTANCE).get();
        return (String) flowMap.get("foo");
    }

}
