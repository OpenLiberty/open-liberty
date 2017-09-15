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
package org.apache.myfaces.view.facelets.tag.jstl.core;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.StateManager;
import javax.faces.component.UIComponent;
import javax.faces.event.PhaseId;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributeException;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.util.ExternalSpecifications;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.PageContext;
import org.apache.myfaces.view.facelets.el.FaceletStateValueExpression;
import org.apache.myfaces.view.facelets.el.FaceletStateValueExpressionUEL;
import org.apache.myfaces.view.facelets.tag.ComponentContainerHandler;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;
import org.apache.myfaces.view.facelets.tag.jsf.FaceletState;

/**
 * The basic iteration tag, accepting many different
 * collection types and supporting subsetting and other
 * functionality
 * 
 * @author Jacob Hookom
 * @author Andrew Robinson
 * @version $Id: ForEachHandler.java 1641470 2014-11-24 20:35:02Z lu4242 $
 */
@JSFFaceletTag(name="c:forEach")
public final class ForEachHandler extends TagHandler implements ComponentContainerHandler
{

    private static class ArrayIterator implements Iterator<Object>
    {

        protected final Object array;

        protected int i;

        protected final int len;

        public ArrayIterator(Object src)
        {
            this.i = 0;
            this.array = src;
            this.len = Array.getLength(src);
        }

        public boolean hasNext()
        {
            return this.i < this.len;
        }

        public Object next()
        {
            return Array.get(this.array, this.i++);
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * If items specified:
     * Iteration begins at the item located at the
     * specified index. First item of the collection has
     * index 0.
     * If items not specified:
     * Iteration begins with index set at the value
     * specified.
     */
    @JSFFaceletAttribute(className="int")
    private final TagAttribute begin;

    /**
     * If items specified:
     * Iteration ends at the item located at the
     * specified index (inclusive).
     * If items not specified:
     * Iteration ends when index reaches the value
     * specified.
     */
    @JSFFaceletAttribute(className="int")
    private final TagAttribute end;

    /**
     * Collection of items to iterate over.
     */
    @JSFFaceletAttribute(className="javax.el.ValueExpression")
    private final TagAttribute items;

    /**
     * Iteration will only process every step items of
     * the collection, starting with the first one.
     */
    @JSFFaceletAttribute(className="int")
    private final TagAttribute step;

    private final TagAttribute tranzient;

    /**
     * Name of the exported scoped variable for the
     * current item of the iteration. This scoped
     * variable has nested visibility. Its type depends
     * on the object of the underlying collection.
     */
    @JSFFaceletAttribute(className="java.lang.String")
    private final TagAttribute var;

    /**
     * Name of the exported scoped variable for the
     * status of the iteration. 
     */
    @JSFFaceletAttribute(className="java.lang.String")
    private final TagAttribute varStatus;

    /**
     * @param config
     */
    public ForEachHandler(TagConfig config)
    {
        super(config);
        this.items = this.getAttribute("items");
        this.var = this.getAttribute("var");
        this.begin = this.getAttribute("begin");
        this.end = this.getAttribute("end");
        this.step = this.getAttribute("step");
        this.varStatus = this.getAttribute("varStatus");
        this.tranzient = this.getAttribute("transient");

        if (this.items == null && this.begin != null && this.end == null)
        {
            throw new TagAttributeException(this.tag, this.begin,
                                            "If the 'items' attribute is not specified, but the 'begin' attribute is, "
                                            + "then the 'end' attribute is required");
        }
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException
    {
        int e = this.getEnd(ctx);
        Object src = null;
        ValueExpression srcVE = null;
        if (this.items != null)
        {
            srcVE = this.items.getValueExpression(ctx, Object.class);
            src = srcVE.getValue(ctx);
        }
        else
        {
            byte[] b = new byte[e + 1];
            for (int i = 0; i < b.length; i++)
            {
                b[i] = (byte) i;
            }
            src = b;
        }
        FaceletCompositionContext fcc = FaceletCompositionContext.getCurrentInstance(ctx);
        AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
        // Just increment one number to ensure the prefix doesn't conflict later if two
        // c:forEach are close between each other. Note c:forEach is different from
        // c:if tag and doesn't require a section because c:forEach requires to provide
        // multiple sections starting with a specified "base" related to the element
        // position and value in the collection.
        fcc.incrementUniqueComponentId();
        String uniqueId = actx.generateUniqueFaceletTagId(fcc.generateUniqueId(), tagId);
        if (src != null)
        {
            PageContext pctx = actx.getPageContext();
            // c:forEach is special because it requires FaceletState even if no pss is used.
            FaceletState restoredFaceletState = ComponentSupport.getFaceletState(ctx, parent, false);
            IterationState restoredSavedOption = (restoredFaceletState == null) ? null : 
                (IterationState) restoredFaceletState.getState(uniqueId);

            if (restoredSavedOption != null)
            {            
                if (!PhaseId.RESTORE_VIEW.equals(ctx.getFacesContext().getCurrentPhaseId()))
                {
                    // Refresh, evaluate and synchronize state
                    applyOnRefresh(ctx, fcc, pctx, parent, uniqueId, src, srcVE, restoredSavedOption);
                }
                else
                {
                    // restore view, don't record iteration, use the saved value
                    applyOnRestore(ctx, fcc, pctx, parent, uniqueId, src, srcVE, restoredSavedOption);
                }
            }
            else
            {
                // First time         
                applyFirstTime(ctx, fcc, pctx, parent, uniqueId, src, srcVE);
            }
        }

        if (fcc.isUsingPSSOnThisView() && fcc.isRefreshTransientBuildOnPSS() && !fcc.isRefreshingTransientBuild())
        {
            //Mark the parent component to be saved and restored fully.
            ComponentSupport.markComponentToRestoreFully(ctx.getFacesContext(), parent);
        }
        if (fcc.isDynamicComponentSection())
        {
            ComponentSupport.markComponentToRefreshDynamically(ctx.getFacesContext(), parent);
        }
    }
    
    private void setVar(FaceletContext ctx, UIComponent parent,
        String uniqueId, String base, boolean t, Object src, 
        ValueExpression srcVE, Object value, String v, int i)
    {
        // set the var
        if (v != null)
        {
            ValueExpression ve;
            if (t || srcVE == null)
            {
                if (value == null)
                {
                    ve = null;
                }
                else
                {
                    ve = ctx.getExpressionFactory().createValueExpression(
                                value, Object.class);
                }
            }
            else
            {
                ve = this.getVarExpr(srcVE, src, value, i);
            }
            setVar(ctx, parent, uniqueId, base, v, ve, srcVE);
        }
    }
    
    private void setVar(FaceletContext ctx, UIComponent parent,
        String uniqueId, String base, String v, ValueExpression ve, ValueExpression srcVE)
    {
        AbstractFaceletContext actx = ((AbstractFaceletContext) ctx);
        PageContext pctx = actx.getPageContext();
        //if (ELExpressionCacheMode.alwaysRecompile.equals(actx.getELExpressionCacheMode()))
        //{
        if(srcVE != null) 
        {
            FaceletState faceletState = ComponentSupport.getFaceletState(ctx, parent, true);
            faceletState.putBinding(uniqueId, base, ve);

            //Put the indirect EL into context
            ValueExpression fve;
            if (ExternalSpecifications.isUnifiedELAvailable())
            {
                fve = new FaceletStateValueExpressionUEL(uniqueId, base);
            }
            else
            {
                fve = new FaceletStateValueExpression(uniqueId, base);
            }
            pctx.getAttributes().put(v, fve);
        }
        else
        {
            pctx.getAttributes().put(v, ve);
        }
    }
    
    private void applyFirstTime(FaceletContext ctx, FaceletCompositionContext fcc, PageContext pctx, 
        UIComponent parent, String uniqueId, Object src, ValueExpression srcVE) throws IOException
    {
        int s = this.getBegin(ctx);
        int e = this.getEnd(ctx);
        int m = this.getStep(ctx);
        Integer sO = this.begin != null ? Integer.valueOf(s) : null;
        Integer eO = this.end != null ? Integer.valueOf(e) : null;
        Integer mO = this.step != null ? Integer.valueOf(m) : null;
        boolean t = this.getTransient(ctx);
        IterationState iterationState = new IterationState();

        boolean serializableValues = true;
        Iterator<?> itr = this.toIterator(src);
        if (itr != null)
        {
            int i = 0;

            // move to start
            while (i < s && itr.hasNext())
            {
                itr.next();
                i++;
            }

            String v = this.getVarName(ctx);
            String vs = this.getVarStatusName(ctx);
            ValueExpression vO = this.capture(v, pctx);
            ValueExpression vsO = this.capture(vs, pctx);
            int mi = 0;
            Object value = null;
            try
            {
                boolean first = true;
                while (i <= e && itr.hasNext())
                {
                    value = itr.next();

                    // first time, use the counter
                    Integer count = iterationState.getCounter();
                    String base = count.toString();
                    iterationState.setCounter(count+1);

                    if (value instanceof Serializable)
                    {
                        iterationState.getValueList().add(
                            new Object[]{base, value, i});
                    }
                    else
                    {
                        serializableValues = false;
                    }

                    try
                    {
                        fcc.startComponentUniqueIdSection(base);

                        setVar(ctx, parent, uniqueId, base, t, src, srcVE, value, v, i);
                        boolean last = !itr.hasNext();
                        // set the varStatus
                        if (vs != null)
                        {
                            IterationStatus itrS = new IterationStatus(first, last, i, sO, eO, mO, value);
                            ValueExpression ve;
                            if (t || srcVE == null)
                            {
                                if (srcVE == null)
                                {
                                    ve = null;
                                }
                                else
                                {
                                    ve = ctx.getExpressionFactory().createValueExpression(
                                                itrS, Object.class);
                                }
                            }
                            else
                            {
                                ve = new IterationStatusExpression(itrS);
                            }
                            setVar(ctx, parent, uniqueId, base+"_vs", vs, ve, srcVE);
                        }

                        // execute body
                        this.nextHandler.apply(ctx, parent);
                    }
                    finally
                    {
                        fcc.endComponentUniqueIdSection(base);
                    }

                    // increment steps
                    mi = 1;
                    while (mi < m && itr.hasNext())
                    {
                        itr.next();
                        mi++;
                        i++;
                    }
                    i++;

                    first = false;
                }
            }
            finally
            {
                removeVarAndVarStatus(pctx, v, vO, vs, vsO);
            }
        }
        if (serializableValues)
        {
            FaceletState faceletState = ComponentSupport.getFaceletState(ctx, parent, true);
            faceletState.putState(uniqueId, iterationState);
        }
    }
    
    private void applyOnRestore(FaceletContext ctx, FaceletCompositionContext fcc, PageContext pctx, 
        UIComponent parent, String uniqueId, Object src, ValueExpression srcVE, IterationState restoredSavedOption)
        throws IOException
    {
        int s = this.getBegin(ctx);
        int e = this.getEnd(ctx);
        int m = this.getStep(ctx);
        Integer sO = this.begin != null ? Integer.valueOf(s) : null;
        Integer eO = this.end != null ? Integer.valueOf(e) : null;
        Integer mO = this.step != null ? Integer.valueOf(m) : null;
        boolean t = this.getTransient(ctx);

        // restore view, don't record iteration, use the saved value
        String v = this.getVarName(ctx);
        String vs = this.getVarStatusName(ctx);
        ValueExpression vO = this.capture(v, pctx);
        ValueExpression vsO = this.capture(vs, pctx);
        Object value = null;
        try
        {
            int size = restoredSavedOption.getValueList().size();
            for (int si = 0; si < size; si++)
            {
                Object[] stateValue = restoredSavedOption.getValueList().get(si);
                value = stateValue[1];
                String base = (String) stateValue[0];

                try
                {
                    fcc.startComponentUniqueIdSection(base);

                    setVar(ctx, parent, uniqueId, base, t, src, srcVE, value, v, (Integer) stateValue[2]);

                    boolean first = (si == 0);
                    boolean last = (si == size-1);
                    int i = (Integer)stateValue[2];
                    // set the varStatus
                    if (vs != null)
                    {
                        IterationStatus itrS = new IterationStatus(first, last, i, sO, eO, mO, value);
                        ValueExpression ve;
                        if (t || srcVE == null)
                        {
                            if (srcVE == null)
                            {
                                ve = null;
                            }
                            else
                            {
                                ve = ctx.getExpressionFactory().createValueExpression(
                                            itrS, Object.class);
                            }
                        }
                        else
                        {
                            ve = new IterationStatusExpression(itrS);
                        }
                        setVar(ctx, parent, uniqueId, base+"_vs", vs, ve, srcVE);
                    }

                    // execute body
                    this.nextHandler.apply(ctx, parent);
                }
                finally
                {
                    fcc.endComponentUniqueIdSection(base);
                }
            }
        }
        finally
        {
            removeVarAndVarStatus(pctx, v, vO, vs, vsO);
        }
    }
    
    private void applyOnRefresh(FaceletContext ctx, FaceletCompositionContext fcc, PageContext pctx, 
        UIComponent parent, String uniqueId, Object src, ValueExpression srcVE, IterationState restoredSavedOption)
        throws IOException
    {
        int s = this.getBegin(ctx);
        int e = this.getEnd(ctx);
        int m = this.getStep(ctx);
        Integer sO = this.begin != null ? Integer.valueOf(s) : null;
        Integer eO = this.end != null ? Integer.valueOf(e) : null;
        Integer mO = this.step != null ? Integer.valueOf(m) : null;
        boolean t = this.getTransient(ctx);

        // Refresh, evaluate and synchronize state
        Iterator<?> itr = this.toIterator(src);
        IterationState iterationState = new IterationState();
        iterationState.setCounter(restoredSavedOption.getCounter());
        if (itr != null)
        {
            int i = 0;

            // move to start
            while (i < s && itr.hasNext())
            {
                itr.next();
                i++;
            }

            String v = this.getVarName(ctx);
            String vs = this.getVarStatusName(ctx);
            ValueExpression vO = this.capture(v, pctx);
            ValueExpression vsO = this.capture(vs, pctx);
            int mi = 0;
            Object value = null;
            int stateIndex = 0;
            try
            {
                boolean first = true;
                while (i <= e && itr.hasNext())
                {
                    value = itr.next();
                    Object[] stateValue = null; /*restoredSavedOption.getValueList().get(stateIndex);*/
                    String base = null;
                    boolean found = false;

                    // The important thing here is use the same base for the generated component ids
                    // for each element in the iteration that was used on the restore. To do that
                    // 
                    int stateIndexCheck = stateIndex;
                    for (; stateIndexCheck < restoredSavedOption.getValueList().size(); stateIndexCheck++)
                    {
                        stateValue = restoredSavedOption.getValueList().get(stateIndexCheck);
                        if (value.equals(stateValue[1]))
                        {
                            found = true;
                            break;
                        }
                    }
                    if (found)
                    {
                        stateIndex = stateIndexCheck;
                        base = (String) stateValue[0];
                        stateIndex++;
                    }
                    else
                    {
                        // No state, added item, create new count
                        Integer count = iterationState.getCounter();
                        base = count.toString();
                        iterationState.setCounter(count+1);
                        stateValue = null;
                    }

                    if (value instanceof Serializable)
                    {
                        iterationState.getValueList().add(
                            new Object[]{base, value, i});
                    }

                    try
                    {
                        fcc.startComponentUniqueIdSection(base);

                        setVar(ctx, parent, uniqueId, base, t, src, srcVE, value, v, i);

                        boolean last = !itr.hasNext();
                        // set the varStatus
                        if (vs != null)
                        {
                            IterationStatus itrS = new IterationStatus(first, last, i, sO, eO, mO, value);
                            ValueExpression ve;
                            if (t || srcVE == null)
                            {
                                if (srcVE == null)
                                {
                                    ve = null;
                                }
                                else
                                {
                                    ve = ctx.getExpressionFactory().createValueExpression(
                                                itrS, Object.class);
                                }
                            }
                            else
                            {
                                ve = new IterationStatusExpression(itrS);
                            }
                            setVar(ctx, parent, uniqueId, base+"_vs", vs, ve, srcVE);
                        }
                        //setVarStatus(ctx, pctx, t, sO, eO, mO, srcVE, value, vs, first, !itr.hasNext(), i);

                        // execute body
                        boolean markInitialState = (stateValue == null);// !restoredSavedOption.equals(i)
                        boolean oldMarkInitialState = false;
                        Boolean isBuildingInitialState = null;
                        try
                        {
                            if (markInitialState && fcc.isUsingPSSOnThisView())
                            {
                                //set markInitialState flag
                                oldMarkInitialState = fcc.isMarkInitialState();
                                fcc.setMarkInitialState(true);
                                isBuildingInitialState = (Boolean) ctx.getFacesContext().getAttributes().put(
                                        StateManager.IS_BUILDING_INITIAL_STATE, Boolean.TRUE);
                            }                                
                            this.nextHandler.apply(ctx, parent);
                        }
                        finally
                        {
                            if (markInitialState && fcc.isUsingPSSOnThisView())
                            {
                                //unset markInitialState flag
                                if (isBuildingInitialState == null)
                                {
                                    ctx.getFacesContext().getAttributes().remove(
                                            StateManager.IS_BUILDING_INITIAL_STATE);
                                }
                                else
                                {
                                    ctx.getFacesContext().getAttributes().put(
                                            StateManager.IS_BUILDING_INITIAL_STATE, isBuildingInitialState);
                                }
                                fcc.setMarkInitialState(oldMarkInitialState);
                            }
                        }
                    }
                    finally
                    {
                        fcc.endComponentUniqueIdSection(base);
                    }

                    // increment steps
                    mi = 1;
                    while (mi < m && itr.hasNext())
                    {
                        itr.next();
                        mi++;
                        i++;
                    }
                    i++;

                    first = false;
                }
            }
            finally
            {
                removeVarAndVarStatus(pctx, v, vO, vs, vsO);
            }
        }
        FaceletState faceletState = ComponentSupport.getFaceletState(ctx, parent, true);
        faceletState.putState(uniqueId, iterationState);
    }
    
    private void removeVarAndVarStatus(PageContext pctx, String v, ValueExpression vO, String vs, ValueExpression vsO)
    {
        //Remove them from PageContext
        if (v != null)
        {
            pctx.getAttributes().put(v, vO);
        }
        else
        {
            pctx.getAttributes().remove(v);
        }
        if (vs != null)
        {
            pctx.getAttributes().put(vs, vsO);
        }
        else
        {
            pctx.getAttributes().remove(vs);
        }
    }

    private ValueExpression capture(String name, PageContext pctx)
    {
        if (name != null)
        {
            return pctx.getAttributes().put(name, null);
        }
        return null;
    }

    private int getBegin(FaceletContext ctx)
    {
        if (this.begin != null)
        {
            return this.begin.getInt(ctx);
        }
        return 0;
    }

    private int getEnd(FaceletContext ctx)
    {
        if (this.end != null)
        {
            return this.end.getInt(ctx);
        }
        return Integer.MAX_VALUE - 1; // hotspot bug in the JVM
    }

    private int getStep(FaceletContext ctx)
    {
        if (this.step != null)
        {
            return this.step.getInt(ctx);
        }
        return 1;
    }

    private boolean getTransient(FaceletContext ctx)
    {
        if (this.tranzient != null)
        {
            return this.tranzient.getBoolean(ctx);
        }
        return false;
    }

    private ValueExpression getVarExpr(ValueExpression ve, Object src, Object value, int i)
    {
        if (src instanceof List || src.getClass().isArray())
        {
            //return new IndexedValueExpression(ve, i);
            return new IteratedValueExpression(ve, value);
        }
        else if (src instanceof Map && value instanceof Map.Entry)
        {
            return new MappedValueExpression(ve, (Map.Entry) value);
        }
        else if (src instanceof Collection)
        {
            return new IteratedValueExpression(ve, value);
        }
        throw new IllegalStateException("Cannot create VE for: " + src);
    }

    private String getVarName(FaceletContext ctx)
    {
        if (this.var != null)
        {
            return this.var.getValue(ctx);
        }
        return null;
    }

    private String getVarStatusName(FaceletContext ctx)
    {
        if (this.varStatus != null)
        {
            return this.varStatus.getValue(ctx);
        }
        return null;
    }

    private Iterator<?> toIterator(Object src)
    {
        if (src == null)
        {
            return null;
        }
        else if (src instanceof Collection)
        {
            return ((Collection<?>) src).iterator();
        }
        else if (src instanceof Map)
        {
            return ((Map<?, ?>) src).entrySet().iterator();
        }
        else if (src.getClass().isArray())
        {
            return new ArrayIterator(src);
        }
        else
        {
            throw new TagAttributeException(this.tag, this.items,
                    "Must evaluate to a Collection, Map, Array, or null.");
        }
    }

}
