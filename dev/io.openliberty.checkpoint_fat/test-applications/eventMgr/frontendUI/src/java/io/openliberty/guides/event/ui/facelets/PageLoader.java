// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.event.ui.facelets;

import javax.inject.Named;
import java.io.Serializable;
import javax.enterprise.context.SessionScoped;

@Named("pageLoader")
@SessionScoped
public class PageLoader implements Serializable {

    private static final long serialVersionUID = 1L;

    private String content = "content/mainPage.xhtml";
    private String headerContent = "header/header.xhtml";
    private String navBar = "navBar/leftNav.xhtml";
    private String currentPage = "Events";

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNavBar() {
        return navBar;
    }

    public void setNavBar(String navBar) {
        this.navBar = navBar;
    }

    public String getHeaderContent() {
        return this.headerContent;
    }

    public void setHeaderContent(String headerContent) {
        this.headerContent = headerContent;
    }

    public String getCurrentPage() {
        return this.currentPage;
    }

    public void setCurrentPage(String currentPage) {
        this.currentPage = currentPage;
    }

}
