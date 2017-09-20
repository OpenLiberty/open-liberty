/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/* Temporary file pending public availability of api jar */
package javax.servlet;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;

public abstract class GenericFilter implements Filter, FilterConfig, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    FilterConfig fc;

    public GenericFilter() {
        fc = null;
    }

    @Override
    public String getFilterName() {
        if (fc != null) {
            return fc.getFilterName();
        }
        return null;
    }

    @Override
    public String getInitParameter(String name) {
        if (fc != null) {
            return fc.getInitParameter(name);
        }
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        if (fc != null) {
            return fc.getInitParameterNames();
        }
        return Collections.emptyEnumeration();
    }

    @Override
    public ServletContext getServletContext() {
        if (fc != null) {
            return fc.getServletContext();
        }
        return null;
    }

    @Override
    public void destroy() {
        fc = null;

    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        fc = config;
    }

}
