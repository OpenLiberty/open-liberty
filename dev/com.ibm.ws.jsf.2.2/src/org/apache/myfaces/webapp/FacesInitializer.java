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
package org.apache.myfaces.webapp;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

/**
 * @author Mathias Broekelmann (latest modification by $Author: jakobk $)
 * @version $Revision: 963629 $ $Date: 2010-07-13 09:29:07 +0000 (Tue, 13 Jul 2010) $
 */
public interface FacesInitializer
{
    void initFaces(ServletContext servletContext);
    
    void destroyFaces(ServletContext servletContext);

    /**
     * @since 2.0.1
     * @param servletContext
     */
    FacesContext initStartupFacesContext(ServletContext servletContext);
    
    /**
     * @since 2.0.1
     * @param facesContext
     */
    void destroyStartupFacesContext(FacesContext facesContext);
        
    /**
     * @since 2.0.1
     * @param servletContext
     */
    FacesContext initShutdownFacesContext(ServletContext servletContext);    

    /**
     * @since 2.0.1
     * @param facesContext
     */
    void destroyShutdownFacesContext(FacesContext facesContext);
}
