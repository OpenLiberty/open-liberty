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
package javax.faces.webapp;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

/**
 * Base class for all JSP tags that represent a JSF UIComponent.
 * <p>
 * <i>Disclaimer</i>: The official definition for the behaviour of this class is the JSF specification but for legal
 * reasons the specification cannot be replicated here. Any javadoc present on this class therefore describes the
 * current implementation rather than the officially required behaviour, though it is believed that this class does
 * comply with the specification.
 * 
 * see Javadoc of <a href="http://java.sun.com/j2ee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a> for
 * more.
 * 
 * @deprecated replaced by {@link UIComponentELTag}
 */
public abstract class UIComponentTag extends UIComponentClassicTagBase
{

    // tag attributes
    private String _binding = null;
    private String _rendered = null;

    private Boolean _suppressed = null;

    public UIComponentTag()
    {

    }

    @Override
    public void release()
    {
        super.release();

        _binding = null;
        _rendered = null;
    }

    /** Setter for common JSF xml attribute "binding". 
     * @throws JspException 
     */
    public void setBinding(String binding) throws JspException
    {
        if (!isValueReference(binding))
        {
            throw new IllegalArgumentException("not a valid binding: " + binding);
        }
        _binding = binding;
    }

    /** Setter for common JSF xml attribute "rendered". */
    public void setRendered(String rendered)
    {
        _rendered = rendered;
    }

    /**
     * Return the nearest JSF tag that encloses this tag.
     * 
     * @deprecated
     */
    public static UIComponentTag getParentUIComponentTag(PageContext pageContext)
    {
        UIComponentClassicTagBase parentTag = getParentUIComponentClassicTagBase(pageContext);

        return parentTag instanceof UIComponentTag ? (UIComponentTag)parentTag : new UIComponentTagWrapper(parentTag);

    }

    /**
     * Return true if the specified string contains an EL expression.
     * <p>
     * UIComponent properties are often required to be value-binding expressions; this method allows code to check
     * whether that is the case or not.
     */
    public static boolean isValueReference(String value)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }

        int start = value.indexOf("#{");
        if (start < 0)
        {
            return false;
        }

        int end = value.lastIndexOf('}');
        return (end >= 0 && start < end);
    }

    /**
     * Create a UIComponent. Abstract method getComponentType is invoked to determine the actual type name for the
     * component to be created.
     * 
     * If this tag has a "binding" attribute, then that is immediately evaluated to store the created component in the
     * specified property.
     */
    @Override
    protected UIComponent createComponent(FacesContext context, String id)
    {
        String componentType = getComponentType();
        if (componentType == null)
        {
            throw new NullPointerException("componentType");
        }

        if (_binding != null)
        {
            Application application = context.getApplication();
            ValueBinding componentBinding = application.createValueBinding(_binding);
            UIComponent component = application.createComponent(componentBinding, context, componentType);

            component.setId(id);
            component.setValueBinding("binding", componentBinding);
            setProperties(component);

            return component;
        }

        UIComponent component = context.getApplication().createComponent(componentType);
        component.setId(id);
        setProperties(component);

        return component;

    }

    private boolean isFacet()
    {
        return getParent() != null && getParent() instanceof FacetTag;
    }

    /**
     * Determine whether this component renders itself. A component is "suppressed" when it is either not rendered, or
     * when it is rendered by its parent component at a time of the parent's choosing.
     */
    protected boolean isSuppressed()
    {
        if (_suppressed == null)
        {
            // we haven't called this method before, so determine the suppressed
            // value and cache it for later calls to this method.

            if (isFacet())
            {
                // facets are always rendered by their parents --> suppressed
                _suppressed = Boolean.TRUE;
                return true;
            }

            UIComponent component = getComponentInstance();

            // Does any parent render its children?
            // (We must determine this first, before calling any isRendered method
            // because rendered properties might reference a data var of a nesting UIData,
            // which is not set at this time, and would cause a VariableResolver error!)
            UIComponent parent = component.getParent();
            while (parent != null)
            {
                if (parent.getRendersChildren())
                {
                    // Yes, parent found, that renders children --> suppressed
                    _suppressed = Boolean.TRUE;
                    return true;
                }
                parent = parent.getParent();
            }

            // does component or any parent has a false rendered attribute?
            while (component != null)
            {
                if (!component.isRendered())
                {
                    // Yes, component or any parent must not be rendered --> suppressed
                    _suppressed = Boolean.TRUE;
                    return true;
                }
                component = component.getParent();
            }

            // else --> not suppressed
            _suppressed = Boolean.FALSE;
        }
        return _suppressed.booleanValue();
    }

    @Override
    protected void setProperties(UIComponent component)
    {
        if (getRendererType() != null)
        {
            component.setRendererType(getRendererType());
        }

        if (_rendered != null)
        {
            if (isValueReference(_rendered))
            {
                ValueBinding vb = getFacesContext().getApplication().createValueBinding(_rendered);
                component.setValueBinding("rendered", vb);
            }
            else
            {
                boolean b = Boolean.valueOf(_rendered).booleanValue();
                component.setRendered(b);
            }
        }
    }

    /**
     * Class used to create an UIComponentTag from a UIComponentClassicTagBase.
     * <p>
     * This is a standard use of the decorator pattern, to make the logic of the JSF12 UIComponentClassicTagBase class
     * available via the old JSF11 UIComponentTag api.
     */
    private static class UIComponentTagWrapper extends UIComponentTag
    {
        private UIComponentClassicTagBase target;

        public UIComponentTagWrapper(UIComponentClassicTagBase classicTag)
        {
            target = classicTag;
        }

        // -----------------------------------------------------------
        // Methods that can reasonably be called on a parent tag object
        // -----------------------------------------------------------

        @Override
        public String getComponentType()
        {
            return target.getComponentType();
        }

        @Override
        public String getRendererType()
        {
            return target.getRendererType();
        }

        @Override
        public boolean getCreated()
        {
            return target.getCreated();
        }

        @Override
        public String getId()
        {
            return target.getId();
        }

        @Override
        public UIComponent getComponentInstance()
        {
            return target.getComponentInstance();
        }

        @Override
        public Tag getParent()
        {
            return target.getParent();
        }

        // -----------------------------------------------------------
        // Methods that should never be called on a parent tag object
        // -----------------------------------------------------------

        @Override
        public void release()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setBinding(String binding) throws JspException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setId(String id)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRendered(String state)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected UIComponent createComponent(FacesContext context, String newId)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPageContext(PageContext context)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setParent(Tag parent)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected UIComponent findComponent(FacesContext context) throws JspException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected FacesContext getFacesContext()
        {
            throw new UnsupportedOperationException();
        }

        // Methods that no sane user of this class would call, so we do not need to override here:
        // doStartTag, doEndTag, getDoStartValue, getDoEndValue, isSupressed
        // encodeBegin, encodeChildren, encodeEnd, getFacetName
        // setProperties, setupResponseWriter
    }

    @Override
    protected boolean hasBinding()
    {
        return _binding != null;
    }
}
