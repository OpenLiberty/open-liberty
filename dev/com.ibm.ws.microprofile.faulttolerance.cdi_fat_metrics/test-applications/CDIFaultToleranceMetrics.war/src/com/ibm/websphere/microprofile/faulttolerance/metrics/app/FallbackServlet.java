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
package com.ibm.websphere.microprofile.faulttolerance.metrics.app;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance.metrics.app.beans.FallbackBean;
import com.ibm.websphere.microprofile.faulttolerance.metrics.utils.ConnectException;
import com.ibm.websphere.microprofile.faulttolerance.metrics.utils.Connection;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class Test
 */
@WebServlet("/fallback")
public class FallbackServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    FallbackBean bean;

    @Test
    public void testFallback() throws ConnectException {
        //should be retried twice and then fallback and we get a result
        Connection connection = bean.connectA();
        String data = connection.getData();
        assertThat(data, equalTo("Fallback for: connectA - data!"));
        assertThat("Call count", bean.getConnectCountA(), is(3));
    }

}
