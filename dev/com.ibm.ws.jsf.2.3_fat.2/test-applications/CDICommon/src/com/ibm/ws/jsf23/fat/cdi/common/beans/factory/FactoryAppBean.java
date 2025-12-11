/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.jsf23.fat.cdi.common.beans.factory;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 *
 */
@Named
@ApplicationScoped
public class FactoryAppBean {

    private int count;

    private List<String> messages = null;

    public FactoryAppBean() {
        messages = new ArrayList<String>();
    }

    public List<String> getMessages() {
        return messages;
    }

    public void addMessage(String msg) {
        messages.add(msg);
    }

    public int incrementAndGetCount() {
        count++;
        return count;

    }

    public int getCount() {
        return count;
    }

    public void setCount(int c) {
        count = 0;

    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

}
