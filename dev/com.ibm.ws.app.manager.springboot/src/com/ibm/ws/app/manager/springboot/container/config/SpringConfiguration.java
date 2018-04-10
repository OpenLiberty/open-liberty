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
package com.ibm.ws.app.manager.springboot.container.config;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class SpringConfiguration {
    private LinkedHashSet<SpringErrorPageData> errorPages = new LinkedHashSet<>();

    public HashSet<SpringErrorPageData> getErrorPages() {
        return errorPages;
    }

    public void addErrorPage(SpringErrorPageData errorPage) {
        this.errorPages.add(errorPage);
    }

    public void setErrorPages(LinkedHashSet<SpringErrorPageData> errorPages) {
        this.errorPages = errorPages;
    }
}
