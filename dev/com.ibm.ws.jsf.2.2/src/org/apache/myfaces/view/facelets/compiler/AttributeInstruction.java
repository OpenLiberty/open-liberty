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
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.view.facelets.el.ELText;

final class AttributeInstruction implements Instruction
{
    private final String _alias;

    private final String _attr;

    private final ELText _txt;

    public AttributeInstruction(String alias, String attr, ELText txt)
    {
        _alias = alias;
        _attr = attr;
        _txt = txt;
    }

    public void write(FacesContext context) throws IOException
    {
        ResponseWriter out = context.getResponseWriter();
        try
        {
            out.writeAttribute(_attr, _txt.toString(context.getELContext()), null);
        }
        catch (ELException e)
        {
            throw new ELException(_alias + ": " + e.getMessage(), e.getCause());
        }
        catch (Exception e)
        {
            throw new ELException(_alias + ": " + e.getMessage(), e);
        }
    }

    public Instruction apply(ExpressionFactory factory, ELContext ctx)
    {
        ELText nt = _txt.apply(factory, ctx);
        if (nt == _txt)
        {
            return this;
        }

        return new AttributeInstruction(_alias, _attr, nt);
    }

    public boolean isLiteral()
    {
        return _txt.isLiteral();
    }
}
