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
import jakarta.faces.flow.Flow;
import jakarta.faces.flow.builder.FlowBuilder;
import jakarta.faces.flow.builder.FlowBuilderParameter;
import jakarta.faces.flow.builder.FlowDefinition;
import jakarta.inject.Named;

/**
 * This test bean looks up the @FlowMap literal, and puts data into the map for testing.
 * This test bean also looks up the @FlowDefinition and @FlowBuilder literals, and ensures they were created.
 */
@Named(value = "flowMapNextBean")
@RequestScoped
@SuppressWarnings("serial")
public class FlowMapNextBean {

    private Map<Object, Object> flowMap;

    public void initFoo() {
        if (flowMap == null)
            flowMap = getFlowMapAndEnsureFlowExists();
        flowMap.put("foo", "barnested");
    }

    public String getFoo() {
        if (flowMap == null)
            flowMap = getFlowMapAndEnsureFlowExists();
        return (String) flowMap.get("foo");
    }

    private Map<Object, Object> getFlowMapAndEnsureFlowExists() {
        //Flow should have been created via HolderBean
        Flow flow = CDI.current().select(new TypeLiteral<Flow>() {}, FlowDefinition.Literal.INSTANCE).get();

        //Flow builder should have been created via HolderBean.buildMyFlow()
        FlowBuilder builder = CDI.current().select(new TypeLiteral<FlowBuilder>() {}, FlowBuilderParameter.Literal.INSTANCE).get();

        return CDI.current().select(new TypeLiteral<Map<Object, Object>>() {}, FlowMap.Literal.INSTANCE).get();
    }

}
