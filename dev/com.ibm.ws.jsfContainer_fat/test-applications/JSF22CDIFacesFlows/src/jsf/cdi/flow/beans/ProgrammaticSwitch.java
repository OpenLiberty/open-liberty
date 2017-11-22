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
 * Additionally define a switch node, which only allows navigation to the second page in the flow
 * when the value of flowScope.testValue is set to "next"
 */
public class ProgrammaticSwitch implements Serializable {

    private static final long serialVersionUID = 1L;

    @Produces
    @FlowDefinition
    public Flow defineFlow(@FlowBuilderParameter FlowBuilder flowBuilder) {
        String flowId = "programmaticSwitch";
        flowBuilder.id("", flowId);
        flowBuilder.viewNode(flowId, "/" + flowId + "/" + flowId + ".xhtml").markAsStartNode();

        flowBuilder.returnNode("goIndex").fromOutcome("/JSF22Flows_index.xhtml");
        flowBuilder.returnNode("goReturn").fromOutcome("JSF22Flows_return.xhtml");

        // Here we define a switch; it returns the next page in the flow iff the flowscope
        // has the testValue defined as 'next'
        flowBuilder.switchNode("switch-id").defaultOutcome("programmaticSwitch").switchCase().condition("#{flowScope.testValue eq 'next'}").fromOutcome("programmaticSwitch-2");

        return flowBuilder.getFlow();
    }
}
