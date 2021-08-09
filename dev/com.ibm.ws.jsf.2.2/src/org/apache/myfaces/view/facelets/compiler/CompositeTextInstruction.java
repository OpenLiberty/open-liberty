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

/**
 * This instruction group literal and non literal text instructions.
 * Used to avoid text space compression, but note it is only used by
 * TextUnit when there is no inline escape (jspx) and compression enabled.
 *
 */
final class CompositeTextInstruction implements Instruction
{
    private Instruction[] instructions;

    public CompositeTextInstruction(Instruction[] instructions)
    {
        this.instructions = instructions;
    }

    @Override
    public void write(FacesContext context) throws IOException
    {
        for (Instruction i : instructions)
        {
            i.write(context);
        }
    }

    @Override
    public Instruction apply(ExpressionFactory factory, ELContext ctx)
    {
        Instruction[] array = new Instruction[instructions.length];
        for (int i = 0; i < instructions.length; i++)
        {
            array[i] = instructions[i].apply(factory, ctx);
        }
        return new CompositeTextInstruction(array);
    }

    @Override
    public boolean isLiteral()
    {
        boolean literal = true;
        for (Instruction i : instructions)
        {
            if (!i.isLiteral())
            {
                literal = false;
                break;
            }
        }
        return literal;
    }
    
}
