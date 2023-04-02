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

import javax.inject.Inject;
import javax.inject.Named;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.faces.annotation.ManagedProperty;

@Named
@SessionScoped
public class PageDispatcher implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    @ManagedProperty(value = "#{pageLoader}")
    private PageLoader pageLoader;

    String currentRole = null;

    public PageLoader getPageLoader() {
        return pageLoader;
    }

    public void setPageLoader(PageLoader pageLoader) {
        this.pageLoader = pageLoader;
    }

    public void showEventForm() {
        pageLoader.setContent("content/eventForm.xhtml");
        pageLoader.setCurrentPage("Event Creation");
    }

    public void showMainPage() {
        pageLoader.setContent("content/mainPage.xhtml");
        pageLoader.setCurrentPage("Events");
    }

    public void showEditPage() {
        pageLoader.setContent("content/updateEventForm.xhtml");
        pageLoader.setCurrentPage("Edit Event");
    }

}
