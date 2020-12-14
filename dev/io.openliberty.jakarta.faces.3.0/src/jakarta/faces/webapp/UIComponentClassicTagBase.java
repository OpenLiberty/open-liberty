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
package jakarta.faces.webapp;

import jakarta.faces.render.ResponseStateManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIOutput;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.jstl.core.LoopTag;
import jakarta.servlet.jsp.tagext.BodyContent;
import jakarta.servlet.jsp.tagext.BodyTag;
import jakarta.servlet.jsp.tagext.JspIdConsumer;
import jakarta.servlet.jsp.tagext.Tag;

/**
 * @since 1.2
 */
public abstract class UIComponentClassicTagBase extends UIComponentTagBase implements BodyTag, JspIdConsumer
{

    // do not change this w/out doing likewise in UIComponentTag
    private static final String COMPONENT_STACK_ATTR = "org.apache.myfaces.COMPONENT_STACK";

    private static final String REQUEST_FACES_CONTEXT = "org.apache.myfaces.REQUEST_FACES_CONTEXT";

    private static final String VIEW_IDS = "org.apache.myfaces.VIEW_IDS";

    private static final String FORMER_CHILD_IDS_SET_ATTR = "org.apache.myfaces.FORMER_CHILD_IDS";
    private static final String FORMER_FACET_NAMES_SET_ATTR = "org.apache.myfaces.FORMER_FACET_NAMES";

    private static final String PREVIOUS_JSP_IDS_SET = "org.apache.myfaces.PREVIOUS_JSP_IDS_SET";

    private static final String BOUND_VIEW_ROOT = "org.apache.myfaces.BOUND_VIEW_ROOT";
    
    private static final String LOGICAL_PAGE_ID = "org.apache.myfaces.LOGICAL_PAGE_ID";
    
    private static final String LOGICAL_PAGE_COUNTER = "org.apache.myfaces.LOGICAL_PAGE_COUNTER";

    protected static final String UNIQUE_ID_PREFIX = UIViewRoot.UNIQUE_ID_PREFIX + "_";

    protected PageContext pageContext = null;
    protected BodyContent bodyContent = null;

    private boolean _created = false;

    private String _jspId = null;
    private String _facesJspId = null;

    private List<String> _childrenAdded = null;
    private List<String> _facetsAdded = null;

    private UIComponent _componentInstance = null;
    private String _id = null;

    private boolean isInAnIterator;

    // the parent tag
    private Tag _parent = null;

    // the enclosing "classic" parent tag
    private UIComponentClassicTagBase _parentClassicTag = null;

    private FacesContext _facesContext = null;

    protected abstract void setProperties(UIComponent component);

    protected abstract UIComponent createComponent(FacesContext context, String newId) throws JspException;

    public void release()
    {
        internalRelease();

        // members, that must/need only be reset when there is no more risk, that the container
        // wants to reuse this tag
        pageContext = null;
        _parent = null;
        _jspId = null;
        _id = null;
        _facesJspId = null;
        bodyContent = null;
    }

    /**
     * Reset any members that apply to the according component instance and must not be reused if the container wants to
     * reuse this tag instance. This method is called when rendering for this tag is finished ( doEndTag() ) or when
     * released by the container.
     */
    private void internalRelease()
    {
        _facesContext = null;
        _componentInstance = null;
        _created = false;

        _childrenAdded = null;
        _facetsAdded = null;
    }

    @Override
    public boolean getCreated()
    {
        return _created;
    }

    protected List<String> getCreatedComponents()
    {
        return _childrenAdded;
    }

    public static UIComponentClassicTagBase getParentUIComponentClassicTagBase(PageContext pageContext)
    {
        Stack<UIComponentClassicTagBase> stack = getStack(pageContext);

        int size = stack.size();

        return size > 0 ? stack.get(size - 1) : null;
    }

    public String getJspId()
    {
        return _jspId;
    }

    public void setJspId(String jspId)
    {
        // -= Leonardo Uribe =- The javadoc says the following about this method:
        //
        // 1. This method is called by the container before doStartTag(). 
        // 2. The argument is guaranteed to be unique within the page.
        //
        // Doing some tests it was found that the jspId generated in a
        // jsp:include are "reset", so if before call it it was id10
        // the tags inside jsp:include starts from id1 (really I suppose a
        // different counter is used), so if we assign this one
        // directly it is possible to cause duplicate id exceptions later.
        //
        // One problem is caused by f:view tag. This one is not included when
        // we check for duplicate id, so it is possible to assign to a component
        // in a jsp:include the id of the UIViewRoot instance and cause a 
        // duplicate id exception when the view is saved.
        //
        // Checking the javadoc it was found the following note:
        //
        // "... IMPLEMENTATION NOTE: This method will detect where we are in an 
        // include and assign a unique ID for each include in a particular 'logical page'. 
        // This allows us to avoid possible duplicate ID situations for included pages 
        // that have components without explicit IDs..."
        //
        // So we need to keep a counter per logical page or page context found. 
        // It is assumed the first one should not be suffixed. The others needs to be
        // suffixed, so all generated ids of those pages are different. The final result
        // is that jsp:include works correctly.
        //
        // Note this implementation detail takes precedence over c:forEach tag. If a
        // jsp:include is inside a c:forEach, jsp:include takes precedence and the 
        // iteration prefix is ignored. If a custom id is provided for a component, 
        // it will throw duplicate id exception, because this code is "override" 
        // by the custom id, and the iteration suffix only applies on generated ids.
        Integer logicalPageId = (Integer) pageContext.getAttribute(LOGICAL_PAGE_ID);
        
        if (logicalPageId != null)
        {
            if (logicalPageId.intValue() == 1)
            {
                //Base case, just pass it unchanged
                _jspId = jspId;
            }
            else
            {
                // We are on a different page context, suffix it with the logicalPageId
                _jspId = jspId + "pc" + logicalPageId;
            }
        }
        else
        {
            Map<Object, Object> attributeMap = getFacesContext().getAttributes();
            AtomicInteger logicalPageCounter = (AtomicInteger) attributeMap.get(LOGICAL_PAGE_COUNTER);
            
            if (logicalPageCounter == null)
            {
                //We are processing the first component tag. 
                logicalPageCounter = new AtomicInteger(1);
                logicalPageId = 1;
                attributeMap.put(LOGICAL_PAGE_COUNTER, logicalPageCounter);
                pageContext.setAttribute(LOGICAL_PAGE_ID, logicalPageId);
            }
            else
            {
                //We are on a different page context, so we need to assign and set.
                logicalPageId = logicalPageCounter.incrementAndGet();
                pageContext.setAttribute(LOGICAL_PAGE_ID, logicalPageId);
                _jspId = jspId + "pc" + logicalPageId;
            }
        }
        _facesJspId = null;
        checkIfItIsInAnIterator(_jspId);
    }

    @Override
    protected void addChild(UIComponent child)
    {
        if (_childrenAdded == null)
        {
            _childrenAdded = new ArrayList<String>();
        }

        _childrenAdded.add(child.getId());
    }

    @Override
    protected void addFacet(String name)
    {
        if (_facetsAdded == null)
        {
            _facetsAdded = new ArrayList<String>();
        }

        _facetsAdded.add(name);
    }

    /**
     * Return the UIComponent instance associated with this tag.
     * 
     * @return a UIComponent, never null.
     */
    @Override
    public UIComponent getComponentInstance()
    {
        return _componentInstance;
    }

    @Override
    protected FacesContext getFacesContext()
    {
        if (_facesContext != null)
        {
            return _facesContext;
        }

        _facesContext = pageContext == null ? null : (FacesContext)pageContext.getAttribute(REQUEST_FACES_CONTEXT);

        if (_facesContext != null)
        {
            return _facesContext;
        }

        _facesContext = FacesContext.getCurrentInstance();

        if (_facesContext != null)
        {
            if (pageContext != null)
            {
                pageContext.setAttribute(REQUEST_FACES_CONTEXT, _facesContext);
            }
            return _facesContext;
        }

        // should never be reached
        throw new RuntimeException("FacesContext not found");
    }

    @Override
    protected int getIndexOfNextChildTag()
    {
        if (_childrenAdded == null)
        {
            return 0;
        }

        return _childrenAdded.size();
    }

    @Override
    public void setId(String id)
    {
        if (id != null && id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
        {
            throw new IllegalArgumentException("Id is non-null and starts with UIViewRoot.UNIQUE_ID_PREFIX: " + id);
        }

        _id = id;
    }

    /**
     * Return the id (if any) specified as an xml attribute on this tag.
     */
    protected String getId()
    {
        return _id;
    }

    protected String getFacesJspId()
    {
        if (_facesJspId == null)
        {
            if (_jspId != null)
            {
                _facesJspId = UNIQUE_ID_PREFIX + _jspId;

                if (isIdDuplicated(_facesJspId))
                {
                    _facesJspId = createNextId(_facesJspId);
                }
            }
            else
            {
                _facesJspId = _facesContext.getViewRoot().createUniqueId();
            }
        }

        return _facesJspId;
    }

    public void setBodyContent(BodyContent bodyContent)
    {
        this.bodyContent = bodyContent;
    }

    public void doInitBody() throws JspException
    {
        // nothing by default
    }

    @SuppressWarnings("unchecked")
    public int doAfterBody() throws JspException
    {
        UIComponentClassicTagBase parentTag = getParentUIComponentClassicTagBase(pageContext);

        if (isRootTag(parentTag) || isInRenderedChildrenComponent(parentTag))
        {
            UIComponent verbatimComp = createVerbatimComponentFromBodyContent();

            if (verbatimComp != null)
            {
                List<String> childrenAddedIds =
                        (List<String>)_componentInstance.getAttributes().get(FORMER_CHILD_IDS_SET_ATTR);

                if (childrenAddedIds == null)
                {
                    _componentInstance.getChildren().add(verbatimComp);
                }
                else
                {
                    int index = _componentInstance.getChildCount();
                    if (childrenAddedIds.size() == index)
                    {
                        // verbatim already present, replace it
                        _componentInstance.getChildren().add(index - 1, verbatimComp);
                    }
                    else
                    {
                        _componentInstance.getChildren().add(verbatimComp);
                    }
                }

                // also tell the parent-tag about the new component instance
                if (parentTag != null)
                {
                    parentTag.addChild(verbatimComp);
                }
            }
        }

        return getDoAfterBodyValue();
    }

    /**
     * Standard method invoked by the JSP framework to inform this tag of the PageContext associated with the jsp page
     * currently being processed.
     */
    public void setPageContext(PageContext pageContext)
    {
        this.pageContext = pageContext;
    }

    /**
     * Returns the enclosing JSP tag object. Note that this is not necessarily a JSF tag.
     */
    public Tag getParent()
    {
        return _parent;
    }

    /**
     * Standard method invoked by the JSP framework to inform this tag of the enclosing JSP tag object.
     */
    public void setParent(Tag tag)
    {
        this._parent = tag;
    }

    public BodyContent getBodyContent()
    {
        return bodyContent;
    }

    public int doStartTag() throws JspException
    {
        this._facesContext = getFacesContext();

        if (_facesContext == null)
        {
            throw new JspException("FacesContext not found");
        }

        _childrenAdded = null;
        _facetsAdded = null;

        _parentClassicTag = getParentUIComponentClassicTagBase(pageContext);

        UIComponent verbatimComp = null;

        // create the verbatim component if not inside a facet (facets are rendered
        // by their parents) and in a component that renders children
        if (!isFacet())
        {
            Tag parent = getParent();

            // flush if in a loop tag and not in a jsp tag
            if (parent != null && parent instanceof LoopTag)
            {
                JspWriter outWriter = pageContext.getOut();
                boolean insideJspTag = (outWriter instanceof BodyContent);

                if (!insideJspTag)
                {
                    try
                    {
                        outWriter.flush();
                    }
                    catch (IOException e)
                    {
                        throw new JspException("Exception flushing when creating verbatim _componentInstance", e);
                    }
                }
            }

            // create the transient _componentInstance
            if (_parentClassicTag != null)
            {
                verbatimComp = _parentClassicTag.createVerbatimComponentFromBodyContent();
            }
        }

        // find the _componentInstance for this tag
        _componentInstance = findComponent(_facesContext);

        // add the verbatim component
        if (verbatimComp != null && _parentClassicTag != null)
        {
            addVerbatimBeforeComponent(_parentClassicTag, verbatimComp, _componentInstance);
        }

        Map<String, Object> viewComponentIds = getViewComponentIds();

        // check that the instance returned by the client ID for the viewComponentIds
        // is the same like this one, so we do not perform again the check for duplicated ids
        Object tagInstance = null;
        String clientId = null;
        if (_id != null)
        {
            clientId = _componentInstance.getClientId(_facesContext);
            tagInstance = (viewComponentIds.get(clientId) == this) ? this : null;
        }

        if (tagInstance == null)
        {
            // check for duplicated IDs
            if (_id != null)
            {
                if (clientId != null)
                {
                    if (viewComponentIds.containsKey(clientId))
                    {
                        throw new JspException("Duplicated component Id: '" + clientId + "' " + "for component: '"
                                + getPathToComponent(_componentInstance) + "'.");
                    }

                    viewComponentIds.put(clientId, this);
                }
            }

            // add to the component or facet to parent
            if (_parentClassicTag != null)
            {
                if (isFacet())
                {
                    _parentClassicTag.addFacet(getFacetName());
                }
                else
                {
                    _parentClassicTag.addChild(_componentInstance);
                }
            }
        }

        // push this tag on the stack
        pushTag();

        return getDoStartValue();
    }

    public int doEndTag() throws JspException
    {
        popTag();
        UIComponent component = getComponentInstance();

        removeFormerChildren(component);
        removeFormerFacets(component);

        try
        {
            UIComponentClassicTagBase parentTag = getParentUIComponentClassicTagBase(pageContext);

            UIComponent verbatimComp = createVerbatimComponentFromBodyContent();

            if (verbatimComp != null)
            {
                component.getChildren().add(verbatimComp);

                if (parentTag != null)
                {
                    parentTag.addChild(verbatimComp);
                }
            }
        }
        catch (Throwable e)
        {
            throw new JspException(e);
        }
        finally
        {
            component = null;
        }

        int retValue = getDoEndValue();

        internalRelease();

        return retValue;
    }

    /**
     * @throws JspException  
     */
    protected int getDoAfterBodyValue() throws JspException
    {
        return SKIP_BODY;
    }

    /**
     * Get the value to be returned by the doStartTag method to the JSP framework. Subclasses which wish to use the
     * inherited doStartTag but control whether the tag is permitted to contain nested tags or not can just override
     * this method to return Tag.SOME_CONSTANT.
     * 
     * @return BodyTag.EVAL_BODY_BUFFERED
     * @throws JspException 
     */
    protected int getDoStartValue() throws JspException
    {
        return BodyTag.EVAL_BODY_BUFFERED;
    }

    /**
     * Get the value to be returned by the doEndTag method to the JSP framework. Subclasses which wish to use the
     * inherited doEndTag but control whether the tag is permitted to contain nested tags or not can just override this
     * method to return Tag.SOME_CONSTANT.
     * 
     * @return Tag.EVAL_PAGE
     * @throws JspException 
     */
    protected int getDoEndValue() throws JspException
    {
        return Tag.EVAL_PAGE;
    }

    protected String getFacetName()
    {
        return isFacet() ? ((FacetTag)_parent).getName() : null;
    }

    /**
     * Creates a UIComponent from the BodyContent
     */
    protected UIComponent createVerbatimComponentFromBodyContent()
    {
        UIOutput verbatimComp = null;

        if (bodyContent != null)
        {
            String strContent = bodyContent.getString();

            if (strContent != null)
            {
                String trimmedContent = strContent.trim();
                if (trimmedContent.length() > 0 && !isComment(strContent))
                {
                    verbatimComp = createVerbatimComponent();
                    verbatimComp.setValue(strContent);
                }
            }

            bodyContent.clearBody();
        }

        return verbatimComp;
    }

    private static boolean isComment(String bodyContent)
    {
        return (bodyContent.startsWith("<!--") && bodyContent.endsWith("-->"));
    }

    /**
     * <p>
     * Creates a transient UIOutput using the Application, with the following characteristics:
     * </p>
     * <p>
     * <code>componentType</code> is <code>jakarta.faces.HtmlOutputText</code>.
     * </p>
     * <p>
     * <code>transient</code> is <code>true</code>.
     * </p>
     * <p>
     * <code>escape</code> is <code>false</code>.
     * </p>
     * <p>
     * <code>id</code> is <code>FacesContext.getViewRoot().createUniqueId()</code>
     * </p>
     */
    protected UIOutput createVerbatimComponent()
    {
        UIOutput verbatimComp =
                (UIOutput)getFacesContext().getApplication().createComponent("jakarta.faces.HtmlOutputText");
        verbatimComp.setTransient(true);
        verbatimComp.getAttributes().put("escape", Boolean.FALSE);
        verbatimComp.setId(getFacesContext().getViewRoot().createUniqueId());

        return verbatimComp;
    }

    @SuppressWarnings("unchecked")
    protected void addVerbatimBeforeComponent(UIComponentClassicTagBase parentTag, UIComponent verbatimComp,
                                              UIComponent component)
    {
        UIComponent parent = component.getParent();

        if (parent == null)
        {
            return;
        }

        List<UIComponent> children = parent.getChildren();
        // EDGE CASE:
        // Consider CASE 1 or 2 where the _componentInstance is provided via a
        // _componentInstance binding in session or application scope.
        // The automatically created UIOuput instances for the template text
        // will already be present. Check the JSP_CREATED_COMPONENT_IDS attribute,
        // if present and the number of created components is the same
        // as the number of children replace at a -1 offset from the current
        // value of indexOfComponentInParent, otherwise, call add()

        List<String> childrenAddedIds = (List<String>)parent.getAttributes().get(FORMER_CHILD_IDS_SET_ATTR);

        int parentIndex = children.indexOf(component);

        if (childrenAddedIds != null)
        {
            if (parentIndex > 0 && childrenAddedIds.size() == parentIndex)
            {
                UIComponent formerVerbatim = children.get(parentIndex - 1);

                if (formerVerbatim instanceof UIOutput && formerVerbatim.isTransient())
                {
                    children.set(parentIndex - 1, verbatimComp);
                }
            }
        }

        children.add(parentIndex, verbatimComp);

        parentTag.addChild(verbatimComp);
    }

    /**
     * <p>
     * Add <i>verbatim</i> as a sibling of <i>_componentInstance</i> in <i>_componentInstance</i> in the parent's child
     * list. <i>verbatim</i> is added to the list at the position immediatly following <i>_componentInstance</i>.
     * </p>
     */

    protected void addVerbatimAfterComponent(UIComponentClassicTagBase parentTag, UIComponent verbatim,
                                             UIComponent component)
    {
        int indexOfComponentInParent = 0;
        UIComponent parent = component.getParent();

        // invert the order of this if and the assignment below. Since this line is
        // here, it appears an early return is acceptable/desired if parent is null,
        // and, if it is null, we should probably check for that before we try to
        // access it. 2006-03-15 jdl
        if (null == parent)
        {
            return;
        }
        List<UIComponent> children = parent.getChildren();
        indexOfComponentInParent = children.indexOf(component);
        if (children.size() - 1 == indexOfComponentInParent)
        {
            children.add(verbatim);
        }
        else
        {
            children.add(indexOfComponentInParent + 1, verbatim);
        }
        parentTag.addChild(verbatim);
    }

    /**
     * @deprecated the ResponseWriter is now set by {@link jakarta.faces.application.ViewHandler#renderView}
     */
    @Deprecated
    protected void setupResponseWriter()
    {
    }

    /**
     * Invoke encodeBegin on the associated UIComponent. Subclasses can override this method to perform custom
     * processing before or after the UIComponent method invocation.
     * 
     * @deprecated
     */
    @Deprecated
    protected void encodeBegin() throws IOException
    {
        if (log.isLoggable(Level.FINE))
        {
            log.fine("Entered encodeBegin for client-Id: " + _componentInstance.getClientId(getFacesContext()));
        }
        _componentInstance.encodeBegin(getFacesContext());
        if (log.isLoggable(Level.FINE))
        {
            log.fine("Exited encodeBegin");
        }
    }

    /**
     * Invoke encodeChildren on the associated UIComponent. Subclasses can override this method to perform custom
     * processing before or after the UIComponent method invocation. This is only invoked for components whose
     * getRendersChildren method returns true.
     * 
     * @deprecated
     */
    @Deprecated
    protected void encodeChildren() throws IOException
    {
        if (log.isLoggable(Level.FINE))
        {
            log.fine("Entered encodeChildren for client-Id: " + _componentInstance.getClientId(getFacesContext()));
        }
        _componentInstance.encodeChildren(getFacesContext());
        if (log.isLoggable(Level.FINE))
        {
            log.fine("Exited encodeChildren for client-Id: " + _componentInstance.getClientId(getFacesContext()));
        }
    }

    /**
     * Invoke encodeEnd on the associated UIComponent. Subclasses can override this method to perform custom processing
     * before or after the UIComponent method invocation.
     * 
     * @deprecated
     */
    @Deprecated
    protected void encodeEnd() throws IOException
    {
        if (log.isLoggable(Level.FINE))
        {
            log.fine("Entered encodeEnd for client-Id: " + _componentInstance.getClientId(getFacesContext()));
        }
        _componentInstance.encodeEnd(getFacesContext());
        if (log.isLoggable(Level.FINE))
        {
            log.fine("Exited encodeEnd for client-Id: " + _componentInstance.getClientId(getFacesContext()));
        }

    }

    private boolean isRootTag(UIComponentClassicTagBase parentTag)
    {
        return (parentTag == this);
    }

    private boolean isInRenderedChildrenComponent(UIComponentClassicTagBase tag)
    {
        return (_parentClassicTag != null && tag.getComponentInstance().getRendersChildren());
    }

    private boolean isFacet()
    {
        return _parent != null && _parent instanceof FacetTag;
    }

    /** Map of <ID,Tag> in the view */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getViewComponentIds()
    {
        Map<Object, Object> attributes = _facesContext.getAttributes();
        Map<String, Object> viewComponentIds;

        if (_parent == null)
        {
            // top level _componentInstance
            viewComponentIds = new HashMap<String, Object>();
            attributes.put(VIEW_IDS, viewComponentIds);
        }
        else
        {
            viewComponentIds = (Map<String, Object>) attributes.get(VIEW_IDS);
            
            // Check if null, this can happen if someone programatically tries to do an include of a 
            // JSP fragment. This code will prevent NullPointerException from happening in such cases.
            if (viewComponentIds == null)
            {
                viewComponentIds = new HashMap<String, Object>();
                attributes.put(VIEW_IDS, viewComponentIds);
            }
        }

        return viewComponentIds;
    }

    @SuppressWarnings("unchecked")
    private static final Stack<UIComponentClassicTagBase> getStack(PageContext pageContext)
    {
        Stack<UIComponentClassicTagBase> stack =
                (Stack<UIComponentClassicTagBase>)pageContext.getAttribute(COMPONENT_STACK_ATTR,
                    PageContext.REQUEST_SCOPE);

        if (stack == null)
        {
            stack = new Stack<UIComponentClassicTagBase>();
            pageContext.setAttribute(COMPONENT_STACK_ATTR, stack, PageContext.REQUEST_SCOPE);
        }

        return stack;
    }

    /**
     * The pageContext's request scope map is used to hold a stack of JSP tag objects seen so far, so that a new tag can
     * find the parent tag that encloses it. Access to the parent tag is used to find the parent UIComponent for the
     * component associated with this tag plus some other uses.
     */
    private void popTag()
    {
        Stack<UIComponentClassicTagBase> stack = getStack(pageContext);

        int size = stack.size();
        stack.remove(size - 1);
        if (size <= 1)
        {
            pageContext.removeAttribute(COMPONENT_STACK_ATTR, PageContext.REQUEST_SCOPE);
        }

    }

    private void pushTag()
    {
        getStack(pageContext).add(this);
    }

    //private boolean isIncludedOrForwarded() {
    //    return getFacesContext().getExternalContext().getRequestMap().
    //            containsKey("jakarta.servlet.include.request_uri");
    //}

    /** Generate diagnostic output. */
    private String getPathToComponent(UIComponent component)
    {
        StringBuffer buf = new StringBuffer();

        if (component == null)
        {
            buf.append("{Component-Path : ");
            buf.append("[null]}");
            return buf.toString();
        }

        getPathToComponent(component, buf);

        buf.insert(0, "{Component-Path : ");
        buf.append("}");

        return buf.toString();
    }

    /** Generate diagnostic output. */
    private static void getPathToComponent(UIComponent component, StringBuffer buf)
    {
        if (component == null)
        {
            return;
        }

        StringBuffer intBuf = new StringBuffer();

        intBuf.append("[Class: ");
        intBuf.append(component.getClass().getName());
        if (component instanceof UIViewRoot)
        {
            intBuf.append(",ViewId: ");
            intBuf.append(((UIViewRoot)component).getViewId());
        }
        else
        {
            intBuf.append(",Id: ");
            intBuf.append(component.getId());
        }
        intBuf.append("]");

        buf.insert(0, intBuf);

        getPathToComponent(component.getParent(), buf);
    }

    /**
     * Remove any child components of the associated components which do not have corresponding tags as children of this
     * tag. This only happens when a view is being re-rendered and there are components in the view tree which don't
     * have corresponding JSP tags. Wrapping JSF tags in JSTL "c:if" statements is one way this can happen. <br />
     * Attention: programmatically added components are are not affected by this: they will not be on the old list of
     * created components nor on the new list of created components, so nothing will happen to them.
     */
    @SuppressWarnings("unchecked")
    private void removeFormerChildren(UIComponent component)
    {
        List<String> formerChildIds = (List<String>)component.getAttributes().get(FORMER_CHILD_IDS_SET_ATTR);
        if (formerChildIds != null)
        {
            for (String childId : formerChildIds)
            {
                if (_childrenAdded == null || !_childrenAdded.contains(childId))
                {
                    UIComponent childToRemove = component.findComponent(childId);
                    if (childToRemove != null)
                    {
                        component.getChildren().remove(childToRemove);
                    }
                }
            }
            if (_childrenAdded == null)
            {
                component.getAttributes().remove(FORMER_CHILD_IDS_SET_ATTR);
            }
            else
            {
                component.getAttributes().put(FORMER_CHILD_IDS_SET_ATTR, _childrenAdded);
            }
        }
        else
        {
            if (_childrenAdded != null)
            {
                component.getAttributes().put(FORMER_CHILD_IDS_SET_ATTR, _childrenAdded);
            }
        }
    }

    /** See removeFormerChildren. */
    @SuppressWarnings("unchecked")
    private void removeFormerFacets(UIComponent component)
    {
        List<String> formerFacetNames = (List<String>)component.getAttributes().get(FORMER_FACET_NAMES_SET_ATTR);
        if (formerFacetNames != null)
        {
            for (String facetName : formerFacetNames)
            {
                if (_facetsAdded == null || !_facetsAdded.contains(facetName))
                {
                    component.getFacets().remove(facetName);
                }
            }
            if (_facetsAdded == null)
            {
                component.getAttributes().remove(FORMER_FACET_NAMES_SET_ATTR);
            }
            else
            {
                component.getAttributes().put(FORMER_FACET_NAMES_SET_ATTR, _facetsAdded);
            }
        }
        else
        {
            if (_facetsAdded != null)
            {
                component.getAttributes().put(FORMER_FACET_NAMES_SET_ATTR, _facetsAdded);
            }
        }
    }

    /**
     * Return the corresponding UIComponent for this tag, creating it if necessary.
     * <p>
     * If this is not the first time this method has been called, then return the cached _componentInstance instance
     * found last time.
     * <p>
     * If this is not the first time this view has been seen, then locate the existing _componentInstance using the id
     * attribute assigned to this tag and return it. Note that this is simple for components with user-assigned ids. For
     * components with generated ids, the "reattachment" relies on the fact that UIViewRoot will generate the same id
     * values for tags in this page as it did when first generating the view. For this reason all JSF tags within a JSTL
     * "c:if" are required to have explicitly-assigned ids.
     * <p>
     * Otherwise create the _componentInstance, populate its properties from the xml attributes on this JSP tag and
     * attach it to its parent.
     * <p>
     * When a _componentInstance is found or created the parent JSP tag is also told that the _componentInstance has
     * been "seen". When the parent tag ends it will delete any components which were in the view previously but have
     * not been seen this time; see doEndTag for more details.
     */
    protected UIComponent findComponent(FacesContext context) throws JspException
    {
        // 1. If we have previously located this component, return it.
        if (_componentInstance != null)
        {
            return _componentInstance;
        }

        // 2. Locate the parent component by looking for a parent UIComponentTag instance,
        // and ask it for its component. If there is no parent UIComponentTag instance,
        // this tag represents the root component, so get it from the current Tree and return it.
        UIComponentClassicTagBase parentTag = getParentUIComponentClassicTagBase(pageContext);

        if (parentTag == null)
        {
            // This is the root
            _componentInstance = context.getViewRoot();

            // check if the view root is already bound to the tag
            Object alreadyBoundViewRootFlag = _componentInstance.getAttributes().get(BOUND_VIEW_ROOT);

            if (alreadyBoundViewRootFlag == null)
            {
                try
                {
                    setProperties(_componentInstance);
                }
                catch (Throwable e)
                {
                    throw new JspException(e);
                }

                if (_id != null)
                {
                    _componentInstance.setId(_id);
                }
                else
                {
                    _componentInstance.setId(getFacesJspId());
                }
                _componentInstance.getAttributes().put(BOUND_VIEW_ROOT, true);
                _created = true;

            }
            else if (hasBinding())
            {
                setProperties(_componentInstance);
            }

            return _componentInstance;
        }

        UIComponent parent = parentTag.getComponentInstance();

        if (parent == null)
        {
            throw new IllegalStateException("parent is null?");
        }

        String facetName = getFacetName();
        if (facetName != null)
        {
            // Facet
            String id = createUniqueId(context, parent);
            _componentInstance = parent.getFacet(facetName);
            if (_componentInstance == null)
            {
                _componentInstance = createComponent(context, id);
                _created = true;
                parent.getFacets().put(facetName, _componentInstance);
            }
            else
            {
                if (checkFacetNameOnParentExists(parentTag, facetName))
                {
                    throw new IllegalStateException("facet '" + facetName
                            + "' already has a child associated. current associated _componentInstance id: "
                            + _componentInstance.getClientId(context) + " class: "
                            + _componentInstance.getClass().getName());
                }
            }

            addFacetNameToParentTag(parentTag, facetName);
            return _componentInstance;
        }

        // Child
        //
        // Note that setProperties is called only when we create the
        // _componentInstance; on later passes, the attributes defined on the
        // JSP tag are set on this Tag object, but then completely
        // ignored.

        String id = createUniqueId(context, parent);

        // Warn users that this tag is about to find/steal the UIComponent
        // that has already been created for a sibling tag with the same id value .
        // _childrenAdded is a Set, and we will stomp over a past id when calling
        // addChildIdToParentTag.
        //
        // It would also be reasonable to throw an exception here rather than
        // just issue a warning as this is a pretty serious problem. However the
        // Sun RI just issues a warning...
        if (parentTag._childrenAdded != null && parentTag._childrenAdded.contains(id))
        {
            if (log.isLoggable(Level.WARNING))
            {
                log.warning("There is more than one JSF tag with an id : " + id);
            }
        }

        _componentInstance = findComponent(parent, id);
        if (_componentInstance == null)
        {
            _componentInstance = createComponent(context, id);
            if (id.equals(_componentInstance.getId()) )
            {
            _created = true;
            int index = parentTag.getIndexOfNextChildTag();
            if (index > parent.getChildCount())
            {
                index = parent.getChildCount();
            }

            List<UIComponent> children = parent.getChildren();
            children.add(index, _componentInstance);
        }
            // On weblogic portal using faces-adapter, the id set and the retrieved 
            // one for <netuix:namingContainer> is different. The reason is 
            // this custom solution for integrate jsf changes the id of the parent
            // component to allow the same native portlet to be allocated multiple
            // times in the same page
            else if (null == findComponent(parent,_componentInstance.getId()))
            {
                _created = true;
                int index = parentTag.getIndexOfNextChildTag();
                if (index > parent.getChildCount())
                {
                    index = parent.getChildCount();
                }

                List<UIComponent> children = parent.getChildren();
                children.add(index, _componentInstance);
            }
        }

        return _componentInstance;

    }

    private UIComponent findComponent(UIComponent parent, String id)
    {
        for (UIComponent child : parent.getChildren())
        {
            if (child.getId() != null && child.getId().equals(id))
            {
                return child;
            }
        }

        return null;
    }

    private String createUniqueId(FacesContext context, UIComponent parent) throws JspException
    {
        String id = getId();
        if (id == null)
        {
            id = getFacesJspId();
        }
        else if (isIdDuplicated(id))
        {
            if (isInAnIterator)
            {
                setId(createNextId(id));
                id = getId();
            }
            else
            {
                if (parent != null)
                {

                    UIComponent namingContainer;

                    if (parent instanceof NamingContainer)
                    {
                        namingContainer = parent;
                    }
                    else
                    {
                        namingContainer = parent.getParent();
                    }

                    if (namingContainer != null)
                    {
                        UIComponent component = namingContainer.findComponent(id);

                        if (component == null || isPostBack(context))
                        {
                            return id;
                        }
                    }
                }

                throw new JspException("Duplicated Id found in the view: " + id);
            }
        }

        return id;
    }

    private String createNextId(String componentId)
    {
        Integer currentCounter = (Integer) getFacesContext().getAttributes().get(componentId);

        int iCurrentCounter = 1;

        if (currentCounter != null)
        {
            iCurrentCounter = currentCounter;
            iCurrentCounter++;
        }

        getFacesContext().getAttributes().put(componentId, iCurrentCounter);

        //if (isIncludedOrForwarded())
        //{
        //    componentId = componentId + "pc" + iCurrentCounter;
        //}
        //else
        //{
        componentId = componentId + UNIQUE_ID_PREFIX + iCurrentCounter;            
        //}

        return componentId;
    }

    private void checkIfItIsInAnIterator(String jspId)
    {
        Set<String> previousJspIdsSet = getPreviousJspIdsSet();

        if (previousJspIdsSet.contains(jspId))
        {
            isInAnIterator = true;
        }
        else
        {
            previousJspIdsSet.add(jspId);
            isInAnIterator = false;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getPreviousJspIdsSet()
    {
        Set<String> previousJspIdsSet =
                (Set<String>)getFacesContext().getAttributes().get(PREVIOUS_JSP_IDS_SET);

        if (previousJspIdsSet == null)
        {
            previousJspIdsSet = new HashSet<String>();
            // Add it to the context! The next time is called
            // this method it takes the ref from the RequestContext
            getFacesContext().getAttributes().put(PREVIOUS_JSP_IDS_SET, previousJspIdsSet);
        }

        return previousJspIdsSet;
    }

    private boolean isIdDuplicated(String componentId)
    {
        boolean result = false;
        if (_parentClassicTag != null)
        {
            if (_parentClassicTag.isInAnIterator)
            {
                return true;
            }
            List<String> childComponents = _parentClassicTag.getCreatedComponents();

            if (childComponents != null)
            {
                result = childComponents.contains(componentId);
                if (result && (!isInAnIterator))
                {
                    return true;
                }
            }
        }

        return result;
    }

    private boolean isPostBack(FacesContext facesContext)
    {
        return facesContext.getExternalContext().getRequestParameterMap().containsKey(
            ResponseStateManager.VIEW_STATE_PARAM);
    }

    /**
     * check if the facet is already added to the parent
     */
    private boolean checkFacetNameOnParentExists(UIComponentClassicTagBase parentTag, String facetName)
    {
        return parentTag._facetsAdded != null && parentTag._facetsAdded.contains(facetName);
    }

    /**
     * Notify the enclosing JSP tag of the id of this facet's id. The parent tag will later delete any existing view
     * facets that were not seen during this rendering phase; see doEndTag for details.
     */
    private void addFacetNameToParentTag(UIComponentClassicTagBase parentTag, String facetName)
    {
        if (parentTag._facetsAdded == null)
        {
            parentTag._facetsAdded = new ArrayList<String>();
        }
        parentTag._facetsAdded.add(facetName);
    }

    protected abstract boolean hasBinding();

    public JspWriter getPreviousOut()
    {
        return bodyContent.getEnclosingWriter();
    }
}
