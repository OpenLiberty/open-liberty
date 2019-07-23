/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.thirdpartyapi;

import org.apache.myfaces.renderkit.html.HtmlListboxRenderer;
import org.apache.myfaces.renderkit.ErrorPageWriter;
import org.apache.myfaces.webapp.AbstractFacesInitializer;
import org.apache.myfaces.shared.renderkit.html.util.HTMLEncoder;
import org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguageStrategy;
import org.apache.myfaces.push.WebsocketComponent;
import org.apache.myfaces.application.NavigationHandlerImpl;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
 
@Named
@RequestScoped
public class ThirdPartyBean {
  
    private String success = "test failed";

    public String getSuccess() {

        try {
            String debug_key = ErrorPageWriter.DEBUG_INFO_KEY;
            String passed = "A few internal MyFaces keys: " + debug_key + ":" 
                    + HtmlListboxRenderer.PASSTHROUGH_RENDERER_LOCALNAME_KEY + ":" 
                    + AbstractFacesInitializer.CDI_BEAN_MANAGER_INSTANCE +":";

            FaceletViewDeclarationLanguageStrategy fc = new FaceletViewDeclarationLanguageStrategy();
            passed += fc.getMinimalImplicitOutcome("index") + ":";
            WebsocketComponent wc = new WebsocketComponent();
            passed += wc.getFamily() + ":";
            NavigationHandlerImpl nh = new NavigationHandlerImpl();
            passed += nh.beforeNavigation("index") + ":";
            this.success =  HTMLEncoder.encode(passed + " test passed!");
        } catch (Exception e) {
            this.success = "test failed; exception = " + e;
        }
        return this.success;
    }
    
    public void setSuccess(String s) {
        this.success = s;
    }
}
