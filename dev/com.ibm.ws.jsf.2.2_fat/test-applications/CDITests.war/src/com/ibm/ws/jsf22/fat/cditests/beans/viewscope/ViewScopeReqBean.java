/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.cditests.beans.viewscope;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 *
 */
@Named
@RequestScoped
public class ViewScopeReqBean {

    private int count = 0;

    private int hashCode = 0;

    public ViewScopeReqBean() {
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
