/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package filterandwrapper.war.files;

import javax.servlet.*;
import javax.servlet.http.*;

public class MyFilter implements javax.servlet.Filter {
    public FilterConfig filterConfig;

    public void init(final FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        System.out.println("========== MyFilter.init ========== ");
    }

    public void destroy() {
        System.out.println("========== MyFilter.destroy ========== ");
    }

    public void doFilter(final ServletRequest request, final ServletResponse response, FilterChain chain) throws java.io.IOException, javax.servlet.ServletException {

        System.out.println("========== MyFilter.doFilter, START ========== ");

        MyResponseWrapper myResponse = new MyResponseWrapper((HttpServletResponse) response);
        System.out.println("========== MyFilter.doFilter, wrapped response ==========  " + myResponse);

        System.out.println("isComitted is: " + myResponse.isCommitted());
        System.out.println("getBufferSize is: " + myResponse.getBufferSize());

        System.out.println("========== MyFilter.doFilter, BEFORE chain() ========== ");
        chain.doFilter(request, myResponse);
        System.out.println("========== MyFilter.doFilter, AFTER chain(). END ========== ");
    }
}
