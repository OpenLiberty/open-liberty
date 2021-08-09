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
package org.apache.myfaces.view.facelets;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;

/**
 * FaceletHandlers can implement this contract and push themselves into the FaceletContext for participating in
 * templating. Templates will attempt to resolve content for a specified name until one of the TemplatClients return
 * 'true'.
 * 
 * @author Jacob Hookom
 * @version $Id: TemplateClient.java 1187701 2011-10-22 12:21:54Z bommel $
 */
public interface TemplateClient
{

    /**
     * This contract is much like the normal FaceletHandler.apply method, but it takes in an optional String name which
     * tells this instance what fragment/definition it's looking for. If you are a match, apply your logic to the passed
     * UIComponent and return true, otherwise do nothing and return false.
     * 
     * @param ctx
     *            the FaceletContext of <i>your</i> instance, not the templates'
     * @param parent
     *            current UIComponent instance to be applied
     * @param name
     *            the String name or null if the whole body should be included
     * @return true if this client matched/applied the definition for the passed name
     * @throws IOException
     * @throws FacesException
     * @throws FaceletException
     * @throws ELException
     */
    public boolean apply(FaceletContext ctx, UIComponent parent, String name) throws IOException, FacesException,
            FaceletException, ELException;;
}
