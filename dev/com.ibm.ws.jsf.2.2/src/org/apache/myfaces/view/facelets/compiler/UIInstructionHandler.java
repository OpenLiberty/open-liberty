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
import java.io.Writer;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UniqueIdVendor;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;

import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.el.ELText;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;
import org.apache.myfaces.view.facelets.util.FastWriter;

/**
 * @author Adam Winer
 * @version $Id: UIInstructionHandler.java 1523460 2013-09-15 16:59:12Z lu4242 $
 */
final class UIInstructionHandler extends AbstractUIHandler
{

    private final String alias;

    private final String id;

    private final ELText txt;

    private final Instruction[] instructions;

    private final int length;

    private final boolean literal;

    public UIInstructionHandler(String alias, String id, Instruction[] instructions, ELText txt)
    {
        this.alias = alias;
        this.id = id;
        this.instructions = instructions;
        this.txt = txt;
        this.length = txt.toString().length();

        boolean literal = true;
        int size = instructions.length;

        for (int i = 0; i < size; i++)
        {
            Instruction ins = this.instructions[i];
            if (!ins.isLiteral())
            {
                literal = false;
                break;
            }
        }

        this.literal = literal;
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException
    {
        if (parent != null)
        {
            String facetName = this.getFacetName(ctx, parent);
            
            // our id
            String id = ctx.generateUniqueId(this.id);

            // grab our component
            UIComponent c = null;
            FaceletCompositionContext mctx= FaceletCompositionContext.getCurrentInstance(ctx);
            
            if (mctx.isRefreshingSection())
            {
                if (facetName != null)
                {
                    c = ComponentSupport.findChildInFacetByTagId(parent, id, facetName);
                }
                else
                {
                    c = ComponentSupport.findChildInChildrenByTagId(parent, id);
                }
            }
            boolean componentFound = false;
            if (c != null)
            {
                componentFound = true;

                mctx.incrementUniqueComponentId();
                // mark all children for cleaning
                mctx.markForDeletion(c);
            }
            else
            {
                Instruction[] applied;
                String componentId = mctx.generateUniqueComponentId();
                if (this.literal)
                {
                    applied = this.instructions;
                }
                else
                {
                    int size = this.instructions.length;
                    applied = new Instruction[size];
                    // Create a new list with all of the necessary applied
                    // instructions
                    Instruction ins;
                    for (int i = 0; i < size; i++)
                    {
                        ins = this.instructions[i];
                        applied[i] = ins.apply(ctx.getExpressionFactory(), ctx);
                    }
                }

                c = new UIInstructions(txt, applied);
                // mark it owned by a facelet instance
                //c.setId(ComponentSupport.getViewRoot(ctx, parent).createUniqueId());

                UniqueIdVendor uniqueIdVendor
                        = mctx.getUniqueIdVendorFromStack();
                if (uniqueIdVendor == null)
                {
                    uniqueIdVendor = ComponentSupport.getViewRoot(ctx, parent);
                }
                if (uniqueIdVendor != null)
                {
                    // UIViewRoot implements UniqueIdVendor, so there is no need to cast to UIViewRoot
                    // and call createUniqueId(). Also, note that UIViewRoot.createUniqueId() javadoc
                    // says we could send as seed the facelet generated id.
                    String uid = uniqueIdVendor.createUniqueId(ctx.getFacesContext(), componentId);
                    c.setId(uid);
                }                
                //c.getAttributes().put(ComponentSupport.MARK_CREATED, id);
                ((UIInstructions)c).setMarkCreated(id);
            }
            
            boolean oldProcessingEvents = ctx.getFacesContext().isProcessingEvents();
            // finish cleaning up orphaned children
            if (componentFound)
            {
                mctx.finalizeForDeletion(c);
                if (mctx.isRefreshingSection())
                {
                    ctx.getFacesContext().setProcessingEvents(false); 
                }
                if (facetName == null)
                {
                    parent.getChildren().remove(c);
                }
                else
                {
                    ComponentSupport.removeFacet(ctx, parent, c, facetName);
                }
                if (mctx.isRefreshingSection())
                {
                    ctx.getFacesContext().setProcessingEvents(oldProcessingEvents);
                }
            }
            if (componentFound && mctx.isRefreshingSection())
            {
                ctx.getFacesContext().setProcessingEvents(false); 
            }
            if (facetName == null)
            {
                parent.getChildren().add(c);
            }
            else
            {
                ComponentSupport.addFacet(ctx, parent, c, facetName);
            }
            if (componentFound && mctx.isRefreshingSection())
            {
                ctx.getFacesContext().setProcessingEvents(oldProcessingEvents);
            }
        }
    }

    public String toString()
    {
        return this.txt.toString();
    }

    public String getText()
    {
        return this.txt.toString();
    }

    public String getText(FaceletContext ctx)
    {
        Writer writer = new FastWriter(this.length);
        try
        {
            this.txt.apply(ctx.getExpressionFactory(), ctx).write(writer, ctx);
        }
        catch (IOException e)
        {
            throw new ELException(this.alias + ": " + e.getMessage(), e.getCause());
        }
        return writer.toString();
    }

}
