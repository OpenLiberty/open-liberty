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
package javax.faces.view.facelets;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * The parent or root object in a FaceletHandler composition. The Facelet will take care of populating the passed
 * UIComponent parent in relation to the create/restore lifecycle of JSF.
 */
public abstract class Facelet
{

    /**
     * The passed UIComponent parent will be populated/restored in accordance with the JSF 1.2 specification.
     * 
     * @param facesContext
     *            The current FacesContext (Should be the same as FacesContext.getInstance())
     * @param parent
     *            The UIComponent to populate in a compositional fashion. In most cases a Facelet will be base a
     *            UIViewRoot.
     * @throws IOException
     * @throws FacesException
     * @throws FaceletException
     * @throws ELException
     */
    public abstract void apply(FacesContext facesContext, UIComponent parent) throws IOException, FacesException,
            FaceletException, ELException;
}
