/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.client.window;

import java.io.Serializable;

import jakarta.enterprise.inject.Produces;
import jakarta.faces.flow.Flow;
import jakarta.faces.flow.builder.FlowBuilder;
import jakarta.faces.flow.builder.FlowBuilderParameter;
import jakarta.faces.flow.builder.FlowDefinition;

public class Confirm implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String flowID = "confirm";

    @Produces
    @FlowDefinition
    public Flow build(@FlowBuilderParameter FlowBuilder builder) {
        builder.id("unique", flowID);
        builder.flowCallNode("callConfirmNext").flowReference("unique", ConfirmNext.flowID);
        return builder.getFlow();
    }
}
