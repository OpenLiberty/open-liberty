/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.view.facelets.tag.jsf.html;

/**
 * @author Jacob Hookom
 * @version $Id: HtmlLibrary.java 1496014 2013-06-24 13:01:55Z lu4242 $
 */
public final class HtmlLibrary extends AbstractHtmlLibrary
{

    public final static String NAMESPACE = "http://xmlns.jcp.org/jsf/html";
    public final static String ALIAS_NAMESPACE = "http://java.sun.com/jsf/html";

    public final static HtmlLibrary INSTANCE = new HtmlLibrary();

    public HtmlLibrary()
    {
        super(NAMESPACE, ALIAS_NAMESPACE);
        
        this.addHtmlComponent ("body", "javax.faces.OutputBody", "javax.faces.Body");
        
        this.addHtmlComponent ("button", "javax.faces.HtmlOutcomeTargetButton", "javax.faces.Button");
        
        this.addHtmlComponent("column", "javax.faces.HtmlColumn", null);

        this.addHtmlComponent("commandButton", "javax.faces.HtmlCommandButton", "javax.faces.Button");

        this.addHtmlComponent("commandLink", "javax.faces.HtmlCommandLink", "javax.faces.Link");

        this.addHtmlComponent("dataTable", "javax.faces.HtmlDataTable", "javax.faces.Table");

        this.addHtmlComponent("doctype", "javax.faces.OutputDoctype", "javax.faces.Doctype");
        
        this.addHtmlComponent("form", "javax.faces.HtmlForm", "javax.faces.Form");

        this.addHtmlComponent("graphicImage", "javax.faces.HtmlGraphicImage", "javax.faces.Image");
        
        this.addHtmlComponent ("head", "javax.faces.OutputHead", "javax.faces.Head");
        
        this.addHtmlComponent("inputHidden", "javax.faces.HtmlInputHidden", "javax.faces.Hidden");

        this.addHtmlComponent("inputSecret", "javax.faces.HtmlInputSecret", "javax.faces.Secret");

        this.addHtmlComponent("inputText", "javax.faces.HtmlInputText", "javax.faces.Text");

        this.addHtmlComponent("inputTextarea", "javax.faces.HtmlInputTextarea", "javax.faces.Textarea");

        this.addHtmlComponent("inputFile", "javax.faces.HtmlInputFile", "javax.faces.File");
        
        this.addHtmlComponent ("link", "javax.faces.HtmlOutcomeTargetLink", "javax.faces.Link");
        
        this.addHtmlComponent("message", "javax.faces.HtmlMessage", "javax.faces.Message");

        this.addHtmlComponent("messages", "javax.faces.HtmlMessages", "javax.faces.Messages");

        this.addHtmlComponent("outputFormat", "javax.faces.HtmlOutputFormat", "javax.faces.Format");

        this.addHtmlComponent("outputLabel", "javax.faces.HtmlOutputLabel", "javax.faces.Label");

        this.addHtmlComponent("outputLink", "javax.faces.HtmlOutputLink", "javax.faces.Link");
        
        this.addComponent("outputScript", "javax.faces.Output", "javax.faces.resource.Script",
                          HtmlOutputScriptHandler.class);
        
        this.addComponent("outputStylesheet", "javax.faces.Output", "javax.faces.resource.Stylesheet",
                          HtmlOutputStylesheetHandler.class);
        
        this.addHtmlComponent("outputText", "javax.faces.HtmlOutputText", "javax.faces.Text");

        this.addHtmlComponent("panelGrid", "javax.faces.HtmlPanelGrid", "javax.faces.Grid");

        this.addHtmlComponent("panelGroup", "javax.faces.HtmlPanelGroup", "javax.faces.Group");

        this.addHtmlComponent("selectBooleanCheckbox", "javax.faces.HtmlSelectBooleanCheckbox", "javax.faces.Checkbox");

        this.addHtmlComponent("selectManyCheckbox", "javax.faces.HtmlSelectManyCheckbox", "javax.faces.Checkbox");

        this.addHtmlComponent("selectManyListbox", "javax.faces.HtmlSelectManyListbox", "javax.faces.Listbox");

        this.addHtmlComponent("selectManyMenu", "javax.faces.HtmlSelectManyMenu", "javax.faces.Menu");

        this.addHtmlComponent("selectOneListbox", "javax.faces.HtmlSelectOneListbox", "javax.faces.Listbox");

        this.addHtmlComponent("selectOneMenu", "javax.faces.HtmlSelectOneMenu", "javax.faces.Menu");

        this.addHtmlComponent("selectOneRadio", "javax.faces.HtmlSelectOneRadio", "javax.faces.Radio");
    }

}
