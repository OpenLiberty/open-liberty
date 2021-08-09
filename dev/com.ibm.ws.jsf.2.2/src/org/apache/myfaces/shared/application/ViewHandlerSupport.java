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
package org.apache.myfaces.shared.application;

import javax.faces.context.FacesContext;

/**
 * TODO: RENAME - This class is now used by ViewDeclarationLanguageBase
 * 
 * A utility class to isolate a ViewHandler implementation from the underlying 
 * request/response framework.
 * <p>
 * For example, an implementation of this interface might support javax.servlet,
 * javax.portlet, or some other mechanism.
 */
public interface ViewHandlerSupport
{
    String calculateViewId(FacesContext context, String viewId);
    
    String calculateAndCheckViewId(FacesContext context, String viewId);

    /**
     * Return a string containing a webapp-relative URL that the user can invoke
     * to render the specified view.
     * <p>
     * URLs and ViewIds are not quite the same; for example a url of "/foo.jsf"
     * or "/faces/foo.jsp" may be needed to access the view "/foo.jsp". 
     */
    String calculateActionURL(FacesContext facesContext, String viewId); 
}
