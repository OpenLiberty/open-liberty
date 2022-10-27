/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.uiViewRoot.getDoctype.bean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.component.Doctype;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

/**
 * A bean that uses the Faces 4.0 UIViewRoot.getDoctype() API.
 */
@Named
@RequestScoped
public class DoctypeBean {
    private boolean isHtml5Doctype;

    public DoctypeBean() {
        Doctype doctype = FacesContext.getCurrentInstance().getViewRoot().getDoctype();
        String doctypeRoot = doctype.getRootElement();
        String doctypeSystem = doctype.getSystem();
        String doctypePublic = doctype.getPublic();

        // <!DOCTYPE html> - Make sure that the Doctype is as expected, an HTML 5 Doctype as
        // defined in the UIViewRootGetDoctype.xhtml
        if (doctypeRoot.equals("html") && doctypeSystem == null && doctypePublic == null) {
            isHtml5Doctype = true;
        }
    }

    public boolean isHtml5Doctype() {
        return isHtml5Doctype;
    }

    public void setHtml5Doctype(boolean isHtml5Doctype) {
        this.isHtml5Doctype = isHtml5Doctype;
    }
}
