/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.session.async.err.xmlerr2.ejb;

import java.util.logging.Logger;

/**
 * Bean implementation class for Enterprise Bean: Style1XMLwithParamsBean
 **/
public class Style1XMLwithParamsBean {
    public final static String CLASSNAME = Style1XMLwithParamsBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public void test_Style1XMLwithParams(int i, long l) {
        svLogger.info("test_Style1XMLwithParams test failed. The container should throw " +
                      "an exception, CNTR0204E, when method-params are defined while using " +
                      "Style 1 XML (i.e. * for method-name).");
        return;
    }
}
