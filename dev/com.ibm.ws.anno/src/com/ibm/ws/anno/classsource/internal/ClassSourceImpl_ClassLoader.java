/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.classsource.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.anno.classsource.ClassSource_ClassLoader;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_Streamer;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.util.Util_InternMap;

public class ClassSourceImpl_ClassLoader
    extends ClassSourceImpl
    implements ClassSource_ClassLoader {

    @SuppressWarnings("hiding")
    public static final String CLASS_NAME = ClassSourceImpl_ClassLoader.class.getName();
    private static final TraceComponent tc = Tr.register(ClassSourceImpl_ClassLoader.class);

    // Top O' the world

    public ClassSourceImpl_ClassLoader(ClassSourceImpl_Factory factory,
                                       Util_InternMap internMap,
                                       String name,
                                       ClassLoader classLoader) {

        super(factory, internMap, name, String.valueOf(classLoader));

        this.classLoader = classLoader;

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, this.hashText);
        }
    }

    /**
     * <p>Open this class source.  This implementation does nothing.</p>
     * 
     * @throws ClassSource_Exception Thrown if the open failed.
     */
    @Override
    @Trivial
    public void open() throws ClassSource_Exception {
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] ENTER/RETURN", getHashText()));
        }
    }

    /**
     * <p>Close this class source.  This implementation does nothing.</p>
     * 
     * @throws ClassSource_Exception Thrown if the close failed.
     */
    @Override
    @Trivial
    public void close() throws ClassSource_Exception {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] ENTER/RETURN", getHashText()));
        }
    }

    //

    protected final ClassLoader classLoader;

    @Override
    @Trivial
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    // Leaf class source API.
    //
    // Class loader class sources should only be set with the EXTERNAL
    // scan policy, and external regions are never scanned iteratively.

    @Override
    public void scanClasses(
        ClassSource_Streamer streamer,
        Set<String> i_seedClassNamesSet,
        ScanPolicy scanPolicy) {

        throw new UnsupportedOperationException();
    }

    @Override
    protected void processFromScratch(
        ClassSource_Streamer streamer,
        Set<String> i_seedClassNames,
        ScanPolicy scanPolicy) {

        throw new UnsupportedOperationException();
    }

    //

    @Override
    @FFDCIgnore({ IOException.class })
    public InputStream openResourceStream(String className, String resourceName)
        throws ClassSource_Exception {

        String methodName = "openResourceStream";

        ClassLoader useClassLoader = getClassLoader();

        URL url = useClassLoader.getResource(resourceName);
        if ( url == null ) {
            return null;
        }

        try {
            return url.openStream(); // throws IOException

        } catch ( IOException e) {
            // do NOT process with FFDC

            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level, 
            // preferably the ultimate consumer of the exception.
            //Tr.warning(tc, "ANNO_CLASSSOURCE_OPEN1_EXCEPTION", getHashText(), resourceName, className);

            String eMsg =
                "[ " + getHashText() + " ]" +
                " Failed to open resource [ " + resourceName + " ]" +
                " for class [ " + className + " ]";
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
        }
    }

    @Override
    public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
        try {
            inputStream.close(); // throws IOException

        } catch ( IOException e ) {
            // autoFFDC will display the stack trace
            // [ {0} ]: The close of resource [{1}] for class [{2}] failed with an exception. The message is {3}
            Tr.warning(tc, "ANNO_CLASSSOURCE_RESOURCE_CLOSE_EXCEPTION",
                getHashText(), resourceName, className, e.getMessage());
        }
    }

    //

    @Override
    @Trivial
    public void log(TraceComponent logger) {
        if ( logger.isDebugEnabled() ) {
            Tr.debug(logger, MessageFormat.format("Class Source [ {0} ]", getHashText()));
        }
    }
}
