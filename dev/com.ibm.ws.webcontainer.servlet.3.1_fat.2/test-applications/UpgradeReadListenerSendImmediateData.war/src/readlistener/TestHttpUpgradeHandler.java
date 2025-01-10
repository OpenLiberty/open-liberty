/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package readlistener;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;

public class TestHttpUpgradeHandler implements HttpUpgradeHandler {
    private static final String CLASS_NAME = TestHttpUpgradeHandler.class.getName();

    public void init(WebConnection wc) {
        LOG("init , wc [" + wc + "] , this [" + this + "]");
        try {
            ServletInputStream input = wc.getInputStream();
            ServletOutputStream output = wc.getOutputStream();
            TestReadListener readListener = new TestReadListener(input, output);
            LOG("init, setReadLisener [" + readListener + "]");
            input.setReadListener(readListener);
        } catch (Exception ex) {
            LOG("init, exception [" + ex + "]");
            throw new RuntimeException(ex);
        } 
    }

    public void destroy() {
        LOG("destroy, this [" + this + "]");
    }

    private static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
