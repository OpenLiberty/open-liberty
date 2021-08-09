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
package org.apache.myfaces.view.facelets.compiler;

import java.io.IOException;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;

import org.apache.myfaces.shared.renderkit.html.HtmlRendererUtils;

final class BodyEndElementInstruction implements Instruction
{
    private final String element;

    public BodyEndElementInstruction(String element)
    {
        this.element = element;
    }

    public void write(FacesContext context) throws IOException
    {
        // render all unhandled FacesMessages when ProjectStage is Development
        if (context.isProjectStage(ProjectStage.Development))
        {
            HtmlRendererUtils.renderUnhandledFacesMessages(context);
        }
        
        context.getResponseWriter().endElement(this.element);
    }

    public Instruction apply(ExpressionFactory factory, ELContext ctx)
    {
        return this;
    }

    public boolean isLiteral()
    {
        return true;
    }
}
