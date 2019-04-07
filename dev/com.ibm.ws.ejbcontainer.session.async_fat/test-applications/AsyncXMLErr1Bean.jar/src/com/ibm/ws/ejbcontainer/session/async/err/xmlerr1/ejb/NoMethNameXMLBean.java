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

package com.ibm.ws.ejbcontainer.session.async.err.xmlerr1.ejb;

import java.util.logging.Logger;

/**
 * Bean implementation class for Enterprise Bean: NoMethNameXMLBean
 **/
public class NoMethNameXMLBean {
    public final static String CLASSNAME = NoMethNameXMLBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public void test_noMethNameXML() {
        svLogger.info("test_noMethNameXML test failed. The container should throw " +
                      "an exception, CNTR0203E, when the method-name element " +
                      "of the async-methodType XML is not present.");
        return;
    }
}
