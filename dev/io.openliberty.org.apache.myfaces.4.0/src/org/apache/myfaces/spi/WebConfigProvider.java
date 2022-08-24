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
package org.apache.myfaces.spi;

import java.util.List;
import jakarta.faces.context.ExternalContext;
import org.apache.myfaces.util.ServletMapping;

/**
 * SPI to provide a custom WebConfigProvider implementation.
 *
 * @author Leonardo Uribe
 * @since 2.0.3
 */
public abstract class WebConfigProvider
{
    /**
     * Return the mappings configured on web.xml related to the Faces FacesServlet.
     * <p>
     * By default, the algorithm contemplate these three options:
     * </p>
     * <ol>
     *   <li>Mappings related to registered servlet class javax.faces.webapp.FacesServlet.</li>
     *   <li>Mappings related to registered servlet class implementing
     *   org.apache.myfaces.webapp.DelegatedFacesServlet interface.</li>
     *   <li>Mappings related to registered servlet class registered
     *   using org.apache.myfaces.DELEGATE_FACES_SERVLET web config param.</li>
     * </ol>
     *
     * @param externalContext
     * @return
     */
    public abstract List<ServletMapping> getFacesServletMappings(ExternalContext externalContext);

    /**
     * Indicate if an error page is configured on web.xml file
     * 
     * @param externalContext
     * @return
     */
    public abstract boolean isErrorPagePresent(ExternalContext externalContext);

}
