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
import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.faces.context.FacesContext;

/**
 *
 * @author Leonardo Uribe
 */
public class DoctypeInstruction implements Instruction
{
    private final String name;
    private final String publicId;
    private final String systemId;
    private final boolean html5Doctype;

    public DoctypeInstruction(String name, String publicId, String systemId, boolean html5Doctype)
    {
        this.name = name;
        this.publicId = publicId;
        this.systemId = systemId;
        this.html5Doctype = html5Doctype;
    }

    @Override
    public void write(FacesContext context) throws IOException
    {
        StringBuilder sb = new StringBuilder(64);
        if (html5Doctype)
        {
            sb.append("<!DOCTYPE html>\n");
        }
        else
        {
            sb.append("<!DOCTYPE ").append(name);
            if (publicId != null)
            {
                sb.append(" PUBLIC \"").append(publicId).append('"');
                if (systemId != null)
                {
                    sb.append(" \"").append(systemId).append('"');
                }
            }
            else if (systemId != null)
            {
                sb.append(" SYSTEM \"").append(systemId).append('"');
            }
            sb.append(">\n");
        }
        context.getResponseWriter().writeDoctype(sb.toString());
    }

    @Override
    public Instruction apply(ExpressionFactory factory, ELContext ctx)
    {
        return this;
    }

    @Override
    public boolean isLiteral()
    {
        return true;
    }
}
