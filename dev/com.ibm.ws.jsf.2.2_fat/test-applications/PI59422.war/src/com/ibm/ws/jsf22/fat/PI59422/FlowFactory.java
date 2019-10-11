/*
 * Copyright (c)  2016  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.PI59422;

import java.io.Serializable;

import javax.enterprise.inject.Produces;
import javax.faces.flow.Flow;
import javax.faces.flow.builder.FlowBuilder;
import javax.faces.flow.builder.FlowBuilderParameter;
import javax.faces.flow.builder.FlowDefinition;

public class FlowFactory implements Serializable {
    private static final long serialVersionUID = 1L;

    @Produces @FlowDefinition
    public Flow defineTestFlow(@FlowBuilderParameter FlowBuilder flowBuilder) {
        final String flowId = "sample-flow";
        flowBuilder.id("", flowId);

        flowBuilder.initializer("#{flowManager.init()}");
        flowBuilder.viewNode("flow1", "/" + flowId + "/entry.xhtml").markAsStartNode();
        flowBuilder.returnNode("return-node").fromOutcome("home");
        flowBuilder.finalizer("#{flowManager.destroy()}");

        return flowBuilder.getFlow();
    }
}
