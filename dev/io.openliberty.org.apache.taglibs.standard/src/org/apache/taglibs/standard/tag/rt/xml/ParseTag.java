/*
 * Copyright (c) 1997-2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.taglibs.standard.tag.rt.xml;

import jakarta.servlet.jsp.JspTagException;

import org.apache.taglibs.standard.tag.common.xml.ParseSupport;
import org.xml.sax.XMLFilter;

/**
 * <p>A handler for &lt;parse&gt; that supports rtexprvalue-based
 * attributes.</p>
 *
 * @author Shawn Bayern
 */

public class ParseTag extends ParseSupport {

    //*********************************************************************
    // Accessor methods

    // Deprecated as of JSTL 1.1
    // for tag attribute
    public void setXml(Object xml) throws JspTagException {
        this.xml = xml;
    }

    // 'doc' replaces 'xml' as of JSTL 1.1
    public void setDoc(Object xml) throws JspTagException {
        this.xml = xml;
    }

    public void setSystemId(String systemId) throws JspTagException {
	this.systemId = systemId;
    }

    // for tag attribute
    public void setFilter(XMLFilter filter) throws JspTagException {
	this.filter = filter;
    }

}
