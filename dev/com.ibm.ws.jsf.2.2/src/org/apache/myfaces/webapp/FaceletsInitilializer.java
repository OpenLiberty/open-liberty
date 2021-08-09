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

import javax.el.ExpressionFactory;
import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.servlet.ServletContext;

/**
 * This initializer initializes only Facelets. Specially checks for
 * org.apache.myfaces.EXPRESSION_FACTORY parameter.
 * 
 * @author Martin Koci
 */
public class FaceletsInitilializer extends org.apache.myfaces.webapp.AbstractFacesInitializer
{

    @Override
    protected void initContainerIntegration(ServletContext servletContext, ExternalContext externalContext)
    {

        ExpressionFactory expressionFactory = getUserDefinedExpressionFactory(externalContext);
        if (expressionFactory == null)
        {
            throw new FacesException("No javax.el.ExpressionFactory found. Please provide" +
                    " <context-param> in web.xml: org.apache.myfaces.EXPRESSION_FACTORY");
        }

        buildConfiguration(servletContext, externalContext, expressionFactory);
    }

}
