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
/**
 * A simple managed bean that will be used to test very simple bean functionality.
 *   This bean tests some of the new functions in JSF 2.2
 * 
 * @author Jim Lawwill
 *
 */
package com.ibm.ws.jsf22.fat.miscbean;

import java.io.Serializable;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.el.ExpressionFactory;
import javax.faces.application.Application;
import javax.faces.application.ViewHandler;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.FacesListener;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspFactory;

@ManagedBean
@SessionScoped
public class MiscellaneousBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean componentSystemEventWorked = false;
    private String results = "";

    public void setResults(String results) {
        this.results = results;
    }

    public String getResults() {
        return results;
    }

    //  This method runs through a number of the "miscellaneous" changes to the JSF 2.2 spec.
    //    It creates a "state" string which lists the results of the tests.
    public void runTests() {

        FacesContext facesContext = FacesContext.getCurrentInstance();
        Application application = facesContext.getApplication();
        ViewHandler viewHandler = application.getViewHandler();
        ExternalContext externalContext = facesContext.getExternalContext();

        // From the JSF 2.2 spec, you'll see this line:
        //   12.1.3 add this text to the javax.faces.STATE_SAVING_METHOD spec. When examining the value, 
        //   the runtime must ignore the case.
        //
        // So, in this test, we are sending the "isSavingStateInClient" value back to the client.
        String output = "isSavingStateInClient = " +
                        application.getStateManager().isSavingStateInClient(facesContext);
        output += "\n";

        //  This confirms https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1012
        output += "getApplicationContextPath = " + externalContext.getApplicationContextPath();
        output += "\n";

        //  This confirms http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-917
        output += "isSecure = " + externalContext.isSecure();
        output += "\n";
        externalContext.setSessionMaxInactiveInterval(10000);
        output += "getSessionMaxInactiveInterval = " + externalContext.getSessionMaxInactiveInterval();
        output += "\n";
        //   Save out the partialViewContext request state so we can set it back later.
        PartialViewContext partialViewContext = facesContext.getPartialViewContext();
        boolean partialRequest = partialViewContext.isPartialRequest();
        partialViewContext.setPartialRequest(true);
        output += "setPartialRequest = " + partialViewContext.isPartialRequest();
        output += "\n";
        //   set this back to its original state.
        partialViewContext.setPartialRequest(partialRequest);

        // From the spec, these are new "protected" methods for JSF 2.2
        //  public Set<String> getProtectedViewsUnmodifiable();
        //  public void addProtectedView(String urlPattern()
        //  public boolean removeProtectedView(String urlPattern)
        //
        // Add a protected view, retrieve the views, remove the view.
        // Check that this works properly.
        viewHandler.addProtectedView("/protectedPage2.xhtml");
        Set<String> protectedViews = viewHandler.getProtectedViewsUnmodifiable();
        int numProtectedViews = protectedViews.size();
        boolean removed = viewHandler.removeProtectedView("/protectedPage2.xhtml");
        if (removed == false)
            numProtectedViews = 0;

        output += "getProtectedViewsUnmodifiable = " + numProtectedViews;
        output += "\n";

        //  Return the state of compontentSystemEventWorked.   This tests the new APIs 
        //    listed here --->  http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-997
        output += "componentSystemEventChangesWorked = " + componentSystemEventWorked;
        output += "\n";

        results = output;
    }

    public void handleEvent(ComponentSystemEvent event) {

        //  This test is related to --->  http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-997
        //    We test to see if the isAppropriateListener() and processListener() functions work
        componentSystemEventWorked = true;

        MyFaceListener listener1 = new MyFaceListener();
        //  This is a FacesListener that is NOT an instanceOf ComponentSystemEventListener, 
        //    so it should return false
        if (event.isAppropriateListener(listener1) == true) {
            componentSystemEventWorked = false;
            return;
        }

        //  This is a FacesListener that is an instanceOf ComponentSystemEventListener, 
        //    so it should return true
        MyComponentSystemEventListener listener2 = new MyComponentSystemEventListener();
        if (event.isAppropriateListener(listener2) == false) {
            componentSystemEventWorked = false;
            return;
        }

        //  We'll set the boolean to false,   if the callback is actually called,  it will set this to true.
        componentSystemEventWorked = false;
        event.processListener(listener2);
    }

    /*
     * Check the ExpressionFactory objects returned from both the JSP and 
     * JSF impls - according to the EE7 spec, they should be the same.
     */
    @PostConstruct
    public void init() {
        ServletContext servletContext = (ServletContext) FacesContext
                        .getCurrentInstance().getExternalContext().getContext();
        ExpressionFactory el1 = JspFactory.getDefaultFactory().getJspApplicationContext(servletContext).getExpressionFactory();
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExpressionFactory el2 = facesContext.getApplication().getExpressionFactory();
        if (el1.toString().equals(el2.toString())) {
            this.setResults(el1 + " == " + el2 + "; ExpressionFactory-instance test passed");
        }
        else
            this.setResults(el1 + " != " + el2 + "; ExpressionFactory-instance test failed!");
    }

    public class MyFaceListener implements FacesListener {}

    public class MyComponentSystemEventListener implements ComponentSystemEventListener {

        @Override
        public void processEvent(ComponentSystemEvent event) {
            componentSystemEventWorked = true;
        }
    }
}
