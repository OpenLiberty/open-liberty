/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.converter_validator.beans;

import javax.faces.component.behavior.ClientBehaviorBase;
import javax.faces.component.behavior.ClientBehaviorContext;
import javax.faces.component.behavior.FacesBehavior;
import javax.inject.Inject;

/**
 *
 */
@FacesBehavior(value = "testBehavior", managed = true)
public class TestBehavior extends ClientBehaviorBase {

    @Inject
    private TestCDIBean testBean;

    @Override
    public String getScript(ClientBehaviorContext behaviorContext) {
        return "alert('Hello " + testBean.getWorld() + "')";
    }
}
