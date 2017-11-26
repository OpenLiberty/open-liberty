/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jsf.cdi.flow.beans;

import java.io.Serializable;

import javax.enterprise.inject.Produces;
import javax.faces.flow.Flow;
import javax.faces.flow.builder.FlowBuilder;
import javax.faces.flow.builder.FlowBuilderParameter;
import javax.faces.flow.builder.FlowDefinition;

/**
 * Define a very simple flow via the FlowBuilder API
 */
public class MixedNested1 implements Serializable {

    private static final long serialVersionUID = 1L;

    @Produces
    @FlowDefinition
    public Flow defineFlow(@FlowBuilderParameter FlowBuilder flowBuilder) {

        String flowId = "mixedNested1";
        flowBuilder.id("", flowId);
        flowBuilder.viewNode(flowId, "/" + flowId + "/" + flowId + ".xhtml").markAsStartNode();

        flowBuilder.returnNode("goIndex").fromOutcome("/JSF22Flows_index.xhtml");
        flowBuilder.returnNode("goReturn").fromOutcome("/JSF22Flows_return.xhtml");

        // Call the second nested flow, which happens to be defined declaratively;
        // pass a parameter to the flow (the flowScope test value)
        flowBuilder.flowCallNode("callMixedNested2").flowReference("", "mixedNested2").outboundParameter("testValue", "#{flowScope.testValue}");
        return flowBuilder.getFlow();
    }
}
