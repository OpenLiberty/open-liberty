/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/WrapperTest", asyncSupported = true)
public class RequestWrapperTest extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(RequestWrapperTest.class.getName());

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws IOException, ServletException {

        PrintWriter pw = res.getWriter();
        pw.println("ServletRequest.class.isAssignableFrom(req.getClass()) true : " + ServletRequest.class.isAssignableFrom(req.getClass()));
        pw.println("ServletResponse.class.isAssignableFrom(res.getClass()) true : " + ServletResponse.class.isAssignableFrom(res.getClass()));

        RequestWrapper1 reqW1 = new RequestWrapper1(req);
        RequestWrapper2 reqW2 = new RequestWrapper2(reqW1);
        RequestWrapper3 reqW3 = new RequestWrapper3(reqW2);

        ResponseWrapper1 resW1 = new ResponseWrapper1(res);
        ResponseWrapper2 resW2 = new ResponseWrapper2(resW1);
        ResponseWrapper3 resW3 = new ResponseWrapper3(resW2);

        pw.println("reqW3 wraps reqW2 which wraps reqW1 which wraps req");
        pw.println("reqW3.isWrapperFor(reqW2) true : " + reqW3.isWrapperFor(reqW2));
        pw.println("reqW3.isWrapperFor(reqW1) true : " + reqW3.isWrapperFor(reqW1));
        pw.println("reqW3.isWrapperFor(req) true : " + reqW3.isWrapperFor(req));
        pw.println("reqW2.isWrapperFor(reqW3) false : " + reqW2.isWrapperFor(reqW3));

        pw.println("reqW3.isWrapperFor(RequestWrapper2.class) true : " + reqW3.isWrapperFor(RequestWrapper2.class));
        pw.println("reqW3.isWrapperFor(RequestWrapper1.class) true : " + reqW3.isWrapperFor(RequestWrapper1.class));
        pw.println("reqW3.isWrapperFor(ServletRequest.class) true : " + reqW3.isWrapperFor(ServletRequest.class));
        pw.println("reqW2.isWrapperFor(RequestWrapper3.class) false : " + reqW2.isWrapperFor(RequestWrapper3.class));

        RequestWrapper4 reqW4 = new RequestWrapper4(reqW1);
        pw.println();
        pw.println("reqW4 wraps reqW1 which wraps req");
        pw.println("reqW4.isWrapperFor(reqW1) true : " + reqW4.isWrapperFor(reqW1));
        pw.println("reqW4.isWrapperFor(req) true : " + reqW4.isWrapperFor(req));
        pw.println("reqW4.isWrapperFor(reqW2) false : " + reqW4.isWrapperFor(reqW2));
        pw.println("reqW3.isWrapperFor(reqW1) true : " + reqW3.isWrapperFor(reqW1));
        pw.println("reqW3.isWrapperFor(reqW4) false : " + reqW3.isWrapperFor(reqW4));

        pw.println("reqW4.isWrapperFor(RequestWrapper1.class) true : " + reqW4.isWrapperFor(RequestWrapper1.class));
        pw.println("reqW4.isWrapperFor(ServletRequest.class) true : " + reqW4.isWrapperFor(ServletRequest.class));
        pw.println("reqW4.isWrapperFor(RequestWrapper3.class) false : " + reqW4.isWrapperFor(RequestWrapper3.class));
        pw.println("reqW3.isWrapperFor(RequestWrapper1.class) true : " + reqW3.isWrapperFor(RequestWrapper1.class));
        pw.println("reqW3.isWrapperFor(RequestWrapper4.class) false : " + reqW3.isWrapperFor(RequestWrapper4.class));

        pw.println();
        pw.println();
        pw.println("resW3 wraps resW2 which wraps resW1 which wraps res");
        pw.println("resW3.isWrapperFor(resW2) true : " + resW3.isWrapperFor(resW2));
        pw.println("resW3.isWrapperFor(resW1) true : " + resW3.isWrapperFor(resW1));
        pw.println("resW3.isWrapperFor(res) true : " + resW3.isWrapperFor(res));
        pw.println("resW2.isWrapperFor(resW3) false : " + resW2.isWrapperFor(resW3));

        pw.println("resW3.isWrapperFor(ResponseWrapper2.class) true : " + resW3.isWrapperFor(ResponseWrapper2.class));
        pw.println("resW3.isWrapperFor(ResponseWrapper1.class) true : " + resW3.isWrapperFor(ResponseWrapper2.class));
        pw.println("resW3.isWrapperFor(ServletResponse.class) true : " + resW3.isWrapperFor(ServletResponse.class));
        pw.println("resW2.isWrapperFor(ResponseWrapper3.class) false : " + resW2.isWrapperFor(ResponseWrapper3.class));

        ResponseWrapper4 resW4 = new ResponseWrapper4(resW1);
        pw.println();
        pw.println("resW4 wraps resW1 which wraps res");

        pw.println("resW4.isWrapperFor(resW1) true : " + resW4.isWrapperFor(resW1));
        pw.println("resW4.isWrapperFor(res) true : " + resW4.isWrapperFor(res));
        pw.println("resW4.isWrapperFor(resW2) false : " + resW4.isWrapperFor(resW2));
        pw.println("resW3.isWrapperFor(resW1) true : " + resW3.isWrapperFor(resW1));
        pw.println("resW3.isWrapperFor(resW4) false : " + resW3.isWrapperFor(resW4));

        pw.println("resW4.isWrapperFor(ResponseWrapper1.class) true : " + resW4.isWrapperFor(ResponseWrapper1.class));
        pw.println("resW4.isWrapperFor(ServletResponse.class) true : " + resW4.isWrapperFor(ServletResponse.class));
        pw.println("resW4.isWrapperFor(ResponseWrapper2.class) false : " + resW4.isWrapperFor(ResponseWrapper2.class));
        pw.println("resW3.isWrapperFor(ResponseWrapper1.class) true : " + resW3.isWrapperFor(ResponseWrapper1.class));
        pw.println("resW3.isWrapperFor(ResponseWrapper4.class) false : " + resW3.isWrapperFor(ResponseWrapper4.class));

    }

    class RequestWrapper1 extends ServletRequestWrapper {

        ServletRequest _req;

        public RequestWrapper1(ServletRequest req) {
            super(req);
            _req = req;
        }
    }

    class RequestWrapper2 extends ServletRequestWrapper {

        ServletRequest _req;

        public RequestWrapper2(ServletRequest req) {
            super(req);
            _req = req;
        }
    }

    class RequestWrapper3 extends ServletRequestWrapper {

        ServletRequest _req;

        public RequestWrapper3(ServletRequest req) {
            super(req);
            _req = req;
        }
    }

    class RequestWrapper4 extends ServletRequestWrapper {

        ServletRequest _req;

        public RequestWrapper4(ServletRequest req) {
            super(req);
            _req = req;
        }
    }

    class ResponseWrapper1 extends ServletResponseWrapper {

        public ResponseWrapper1(ServletResponse resp) {
            super(resp);
        }
    }

    class ResponseWrapper2 extends ServletResponseWrapper {

        public ResponseWrapper2(ServletResponse resp) {
            super(resp);
        }
    }

    class ResponseWrapper3 extends ServletResponseWrapper {

        public ResponseWrapper3(ServletResponse resp) {
            super(resp);
        }
    }

    class ResponseWrapper4 extends ServletResponseWrapper {

        public ResponseWrapper4(ServletResponse resp) {
            super(resp);
        }
    }

}
