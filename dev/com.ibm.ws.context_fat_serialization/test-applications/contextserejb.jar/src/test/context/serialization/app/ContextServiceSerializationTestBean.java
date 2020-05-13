/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.context.serialization.app;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.Executor;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ContextService;

/**
 * Performs serialization tests from within the confines of an EJB.
 */
@Stateless
@LocalBean
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ContextServiceSerializationTestBean {

    /** Name of environment entry to lookup. */
    public static final String ENV_ENTRY_NAME = "java:comp/env/env1";

    /** Value of environment entry */
    public static final int ENV_ENTRY_VALUE = 23;

    @Resource(lookup = "concurrent/jeeMetadataContextSvc")
    private ContextService jeeMetadataContextSvc;

    @Resource(lookup = "concurrent/classloaderContextSvc")
    private ContextService classloaderContextSvc;

    /** Default constructor. */
    public ContextServiceSerializationTestBean() {
    }

    /**
     * Serialize the JEE context from within an EJB.
     *
     * @throws IOException
     * @throws FileNotFoundException
     */
    public void testSerializeJEEMetadataContext() throws Exception {
        Executor contextualExecutor = jeeMetadataContextSvc.createContextualProxy(new CurrentThreadExecutor(), Executor.class);
        ObjectOutputStream outfile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("jeeMetadataContext-EJB-vNext.ser")));
        try {
            outfile.writeObject(contextualExecutor);
        } finally {
            outfile.close();
        }
    }

    /**
     * Serialize classloader context.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testSerializeClassloaderContext() throws Exception {
        Executor contextualExecutor = classloaderContextSvc.createContextualProxy(new CurrentThreadExecutor(), Executor.class);
        ObjectOutputStream outfile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("classloaderContext-EJB-vNext.ser")));
        try {
            outfile.writeObject(contextualExecutor);
        } finally {
            outfile.close();
        }
    }

}
