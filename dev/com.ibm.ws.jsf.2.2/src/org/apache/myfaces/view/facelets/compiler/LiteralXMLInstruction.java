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
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

final class LiteralXMLInstruction implements Instruction
{

    private final static char[] STOP = new char[0];

    private final char[] instruction;
    private final int len;

    public LiteralXMLInstruction(String literal)
    {
        this.instruction = literal.toCharArray();
        this.len = this.instruction.length;
    }

    public void write(FacesContext context) throws IOException
    {
        ResponseWriter rw = context.getResponseWriter();
        rw.writeText(STOP, 0, 0); // hack to get closing elements
        rw.write(this.instruction, 0, this.len);
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
