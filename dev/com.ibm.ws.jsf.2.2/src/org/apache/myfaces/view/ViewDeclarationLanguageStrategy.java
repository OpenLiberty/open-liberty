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
package org.apache.myfaces.view;

import javax.faces.view.ViewDeclarationLanguage;

/**
 * This class represents a supported {@link ViewDeclarationLanguage} in the application. Notably,
 * the default ViewDeclarationLanguageFactory maintains an ordered list of supported languages for 
 * the purpose of determining which one to use for a given view id by calling the {@link #handles} 
 * method of each ofthe registered support and using the first match.
 * 
 * @author Simon Lessard (latest modification by $Author: bommel $)
 * @version $Revision: 1187701 $ $Date: 2011-10-22 12:21:54 +0000 (Sat, 22 Oct 2011) $
 *
 * @since 2.0
 */
public interface ViewDeclarationLanguageStrategy
{
    /**
     * Gets the {@link ViewDeclarationLanguage} represented by this support.
     *  
     * @return the {@link ViewDeclarationLanguage} represented by this support
     */
    public ViewDeclarationLanguage getViewDeclarationLanguage();
    
    /**
     * Determines if the {@link ViewDeclarationLanguage} represented by this support should be used 
     * to handle the specified view identifier.
     * 
     * @param viewId the view identifier
     * 
     * @return <code>true</code> if the {@link ViewDeclarationLanguage} represented by this support 
     *         should be used to handle the specified view identifier, <code>false</code> otherwise
     */
    public boolean handles(String viewId);
}
