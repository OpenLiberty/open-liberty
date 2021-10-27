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
package encodedcert.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * To test on Liberty, appSecurity-2.0 feature needs to be enable
 */
@WebServlet("/TestCertificate")
public class TestOpenCertificate extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestOpenCertificate.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public TestOpenCertificate() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info("Testing X590 Client certificate. START");
        PrintWriter writer = response.getWriter();

        try {
            X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
            if (certs != null) {
                String dn = certs[0].getSubjectX500Principal().getName();
                LOG.info("Testing X590 client certificate found dn [" + dn + "]");
                writer.println("FOUND Cert DN [" + dn + "]");
            } else {
                // no certificate provided
                LOG.info("Testing Open Client encoded certificate, can not obtain cert");
                writer.println("Can NOT obtain Cert");
            }
        } catch (Exception e) {
            LOG.info("Exception : Testing Open Client encoded certificate, e [" + e.getMessage() + "]");
            writer.println("Exception during X590 process; e [" + e.getMessage() + "]");
        } finally {
            LOG.info("Testing X590 Client certificate. END");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        doGet(request, response);
    }

}
