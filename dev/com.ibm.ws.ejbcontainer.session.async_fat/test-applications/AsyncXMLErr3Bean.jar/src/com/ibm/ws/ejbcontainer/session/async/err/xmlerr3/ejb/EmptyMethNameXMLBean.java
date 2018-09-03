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

package com.ibm.ws.ejbcontainer.session.async.err.xmlerr3.ejb;

import java.util.logging.Logger;

/**
 * Bean implementation class for Enterprise Bean: EmptyMethNameXMLBean
 **/
public class EmptyMethNameXMLBean {
    public final static String CLASSNAME = EmptyMethNameXMLBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public void test_emptyMethNameXML() {
        svLogger.info("test_emptyMethNameXML test failed. The container should throw " +
                      "an exception, CNTR0203E, when a method name for the method-name element " +
                      "of the async-methodType XML is an empty string.");
        return;
    }
}
