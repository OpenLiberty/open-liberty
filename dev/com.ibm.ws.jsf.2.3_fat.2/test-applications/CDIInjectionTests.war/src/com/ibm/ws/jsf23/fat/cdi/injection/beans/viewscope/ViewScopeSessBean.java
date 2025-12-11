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
package com.ibm.ws.jsf23.fat.cdi.injection.beans.viewscope;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 *
 */
@Named
@SessionScoped
public class ViewScopeSessBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    private int count = 0;

    private int hashCode = 0;

    public ViewScopeSessBean() {
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
