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
package org.apache.myfaces.cdi.view;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.ServletContext;

/**
 * This bean is used to store the ServletContext, so CDI beans can get it later. This is
 * used to ensure a valid FacesContext instance is passed when the bean is destroyed.
 */
@ApplicationScoped
public class ApplicationContextBean
{
    private ServletContext servletContext;

    /** Return the servlet context for the current application. */
    public ServletContext getServletContext() 
    {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext)
    {
        this.servletContext = servletContext;
    }
}
