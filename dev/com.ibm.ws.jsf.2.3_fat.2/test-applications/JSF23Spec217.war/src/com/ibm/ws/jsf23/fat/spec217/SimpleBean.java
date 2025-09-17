/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.spec217;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * A simple request scoped bean that contains a list of Integer values.
 */
@Named
@RequestScoped
public class SimpleBean {

    private List<Integer> values = new ArrayList<Integer>(Arrays.asList(new Integer(0),
                                                                        new Integer(1),
                                                                        new Integer(2),
                                                                        new Integer(3),
                                                                        new Integer(4)));

    public List<Integer> getValues() {
        return values;
    }

    public void setValues(List<Integer> values) {
        this.values = values;
    }

}
