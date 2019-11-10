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
package com.ibm.ws.jsf22.fat.thirdpartyapi;

import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.renderkit.html.HtmlListboxRenderer;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.renderkit.html.HtmlRendererUtils;
import org.apache.myfaces.shared.renderkit.html.util.HTMLEncoder;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
 
@Named
@RequestScoped
public class ThirdPartyBean {

    /**
     * Invoke a method from each package that is currently exposed by JSF as third-party API.
     * If any package isn't available to applications at runtime, this test will return a failure.
     */
    public String getSuccess() {

        try {
            String passed = "";

            HtmlListboxRenderer hlb = new HtmlListboxRenderer();
            passed += hlb.getRendersChildren() + ":";

            MyfacesConfig mfc = new MyfacesConfig();
            passed += mfc.isAutoScroll() + ":";
            passed += RendererUtils.isDefaultAttributeValue(null)+ ":";
            passed += HtmlRendererUtils.isDisplayValueOnly(null)+ ":";

            return HTMLEncoder.encode(passed + " test passed!");
        } catch (Exception e) {
            return "test failed; exception = " + e;
        }
    }
    
    public void setSuccess(String s) {
    }

    /**
     * Invoke a method from a package that is currently NOT exposed by JSF as third-party API.
     * A NoClassDefFoundError should be caught, and will result in the test passing
     */
    public String getFailure() {

        try {
            RuntimeConfig rc = new RuntimeConfig();
            // the NoClassDefFoundError should be thrown from here
            String test =  rc.getFacesVersion();
            return "test failed!!";
        } catch (java.lang.NoClassDefFoundError ncdfe) {
            return "test passed!";
        } catch (Exception e) {
            return "test failed; exception = " + e;
        }
    }

    public void setFailure(String s) {
    }
}
