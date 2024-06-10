/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.myfaces4658;

import jakarta.faces.component.behavior.ClientBehaviorBase;
import jakarta.faces.component.behavior.ClientBehaviorContext;
import jakarta.faces.component.behavior.FacesBehavior;

@FacesBehavior(value = "confirm", managed = true)
public class ConfirmBehavior extends ClientBehaviorBase {

    @Override
    public String getScript(ClientBehaviorContext behaviorContext) {        
        return "return confirm('Click Cancel!');";
    }
}
