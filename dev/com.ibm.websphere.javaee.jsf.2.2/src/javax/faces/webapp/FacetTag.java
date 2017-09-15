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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * This tag adds its child as a facet of the nearest parent UIComponent. A child consisting of multiple elements should
 * be nested within a container component (i.e., within an h:panelGroup for HTML library components).
 * 
 * Unless otherwise specified, all attributes accept static values or EL expressions.
 * 
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 * 
 * @JSFJspTag name="f:facet" bodyContent="JSP"
 */
public class FacetTag extends TagSupport
{
    private static final long serialVersionUID = -5254277925259361302L;
    private String _name;

    public String getName()
    {
        return _name;
    }

    /**
     * The name of the facet to be created. This must be a static value.
     * 
     * @JSFJspAttribute required="true"
     */
    public void setName(String name)
    {
        _name = name;
    }

    @Override
    public void release()
    {
        super.release();
        _name = null;
    }

    @Override
    public int doStartTag() throws JspException
    {
        return EVAL_BODY_INCLUDE;
    }

}
