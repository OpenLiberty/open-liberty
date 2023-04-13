/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.cditests.beans.viewscope;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 *
 */
@Named
@ApplicationScoped
public class ViewScopeAppBean {

    private int count = 0;

    private int hashCode = 0;

    public ViewScopeAppBean() {
        count = 1;
        hashCode = this.hashCode();
    }

    public int getCount() {
        return count;
    }

    public void setCount(int c) {
        count = c;
    }

    public int getHashCode() {
        return hashCode;
    }

    public void setHashCode(int hc) {
        hashCode = hc;
    }

}
