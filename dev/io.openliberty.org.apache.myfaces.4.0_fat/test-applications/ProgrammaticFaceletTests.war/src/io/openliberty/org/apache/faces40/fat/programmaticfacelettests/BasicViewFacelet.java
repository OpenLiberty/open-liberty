/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.programmaticfacelettests;

import java.io.IOException;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.annotation.View;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIOutput;
import jakarta.faces.component.html.HtmlBody;
import jakarta.faces.component.html.HtmlForm;
import jakarta.faces.component.html.HtmlHead;
import jakarta.faces.component.html.HtmlOutputText;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.facelets.Facelet;

/**
 *
 */
@View("/BasicView.xhtml")
@ApplicationScoped
public class BasicViewFacelet extends Facelet {

    @Override
    public void apply(FacesContext facesContext, UIComponent root) throws IOException {
        List<UIComponent> rootChildren = root.getChildren();

        UIOutput output = new UIOutput();
        output.setValue("<html xmlns=\"http://www.w3.org/1999/xhtml\"\n"
                        + "    xmlns:h=\"jakarta.faces.html\">");
        rootChildren.add(output);

        HtmlHead head = new HtmlHead();
        head.setValue("<title>Java Test</title>");
        rootChildren.add(head);

        HtmlBody body = new HtmlBody();
        rootChildren.add(body);

        HtmlForm form = new HtmlForm();
        form.setId("testForm");
        body.getChildren().add(form);

        HtmlOutputText message = new HtmlOutputText();
        message.setValue("Hello World!");
        form.getChildren().add(message);

        output = new UIOutput();
        output.setValue("</html>");
        rootChildren.add(output);
    }

}
