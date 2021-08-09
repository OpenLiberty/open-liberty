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
package com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod;

import javax.enterprise.inject.Alternative;

@Alternative
public class LifeCycleTeacher implements LifeCyclePerson {

    @Override
    public String talk() {
        return "I am a Teacher.";
    }
}
