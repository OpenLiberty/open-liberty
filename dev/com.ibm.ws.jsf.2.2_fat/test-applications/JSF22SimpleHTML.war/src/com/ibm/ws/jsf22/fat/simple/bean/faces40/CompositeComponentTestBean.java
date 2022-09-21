/*
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.simple.bean.faces40;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * Simple bean to test the h:commandButton action on first click when
 * there is one composite component with comments on interface section.
 */
@Named
@RequestScoped
public class CompositeComponentTestBean {

    public String execAction() {
        return "testCommandButtonExecution";
    }
}
