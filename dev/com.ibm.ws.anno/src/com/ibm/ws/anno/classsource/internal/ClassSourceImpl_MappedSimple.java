/*
 * IBM Confidential OCO Source Material                                                                                     
 * 5724-J08, 5724-I63, 5724-H88, 5724-H89, 5655-N02, 5733-W70
 * (C) COPYRIGHT International Business Machines Corp. 2011, 2018
 * The source code for this program is not published or otherwise divested                                                  
 * of its trade secrets, irrespective of what has been deposited with the                                                   
 * U.S. Copyright Office.
 * 
 * Reason   Version  Date         User id   Description
 * ----------------------------------------------------------------------------
 * PI89708   9.0.0.7 11/01/17      jimblye   Ignore packages that have invalid names
 */

package com.ibm.ws.anno.classsource.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_MappedSimple;
import com.ibm.wsspi.anno.classsource.ClassSource_Streamer;
import com.ibm.wsspi.anno.util.Util_InternMap;

/**
 * <p>Default implementation for simple class sources.</p>
 *
 * <p>This implementation provides a basic implementation of steps for processing
 * named resources. A bound simple class provider provides a simplified API for
 * obtaining a collection of resources and the ability to open input streams for
 * resources.</p>
 *
 * </p>See the interface for more details.</p>
 */
public class ClassSourceImpl_MappedSimple
    extends ClassSourceImpl implements ClassSource_MappedSimple {

    public static final String CLASS_NAME = ClassSourceImpl_MappedSimple.class.getSimpleName();

    // Top O' the world

    public ClassSourceImpl_MappedSimple(
        ClassSourceImpl_Factory factory, Util_InternMap internMap,
        String name, ClassSource_MappedSimple.SimpleClassProvider provider) throws ClassSource_Exception {
        this(factory, internMap, NO_ENTRY_PREFIX, name, provider);
        // throws ClassSource_Exception
    }

    /**
     * <p>Create a new simple class source. Key API points are provided
     * by a simple class parameter.</p>
     *
     * @param factory The factory which created this simple class source.
     * @param internMap The intern map used by this class source.
     * @param entryPrefix Optional prefix for selecting entries.
     * @param name A simple name for this class source. The name is intended
     *            both for logging, and as a unique identified for any enclosing
     *            class source.
     * @param provider Call-out utility for obtaining and opening resource names.
     *
     * @throws ClassSource_Exception Thrown in case of a failure constructing
     *             the new simple class source. Currently unused,
     *             but provided for extenders.
     */
    public ClassSourceImpl_MappedSimple(
        ClassSourceImpl_Factory factory,
        Util_InternMap internMap,
        String entryPrefix,
        String name,
        ClassSource_MappedSimple.SimpleClassProvider provider) throws ClassSource_Exception {

        super( factory, internMap, entryPrefix, name, provider.getName() );
        // throws ClassSource_Exception

        String methodName = "<init>";

        this.provider = provider;

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.getHashText());
        }
    }

    /**
     * <p>Compute and return a time stamp for this class source.</p>
     *
     * <p>Time stamps are not available for simple class sources:
     * Answer the unrecorded stamp value {@link ClassSource#UNRECORDED_STAMP}.</p>
     *
     * @return The computed stamp for this class source.  This implementation always
     *         returns the unrecorded stamp value.
     */
    @Override
    protected String computeStamp() {
        return UNRECORDED_STAMP;
    }

    //

    // Base state: A simple class source wraps a simple class provider.

    /** <p>The simple class provider of this class source.</p> */
    protected final ClassSource_MappedSimple.SimpleClassProvider provider;

    /**
     * <p>Answer the simple class provider of this class source. The provider
     * was bound when the simple class source was created, and cannot be changed
     * thereafter.</p>
     *
     * @return The simple class provider of this simple class source.
     */
    @Override
    public ClassSource_MappedSimple.SimpleClassProvider getProvider() {
        return provider;
    }

    // Open/close API: These are NO-OPs for simple class sources.
    //
    // Caution: Sub-types may override to provide less trivial
    // implementations.

    /*
     * <p>Open the class source for processing. The default implementation for
     * simple class sources is a NO-OP.</p>
     *
     * @throws ClassSource_Exception Thrown in case the open fails. Preserved in the API
     * for sub-type use. This implementation will never throw
     * an exception.
     */
    @Override
    public void open() throws ClassSource_Exception {
        String methodName = "open";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER/RETURN", getHashText());
        }
    }

    /*
     * <p>Close the class source for processing. The default implementation for
     * simple class sources is a NO-OP.</p>
     *
     * @throws ClassSource_Exception Thrown in case the close fails. Preserved in the API
     * for sub-type use. This implementation will never throw
     * an exception.
     */
    @Override
    public void close() throws ClassSource_Exception {
        String methodName = "close";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER/RETURN", getHashText());
        }
    }

    //

    @Override
    public InputStream openResourceStream(String className, String resourceName)
        throws ClassSource_Exception {
        String methodName = "openResourceStream";

        if ( !getProvider().getResourceNames().contains(resourceName) ) {
            return null;
        }

        InputStream result;

        try {
            result = getProvider().openResource(resourceName); // throws IOException

        } catch ( Throwable th ) {
            String eMsg = "[ " + getHashText() + " ]" +
                          " Failed to open [ " + resourceName + " ]" +
                          " for class [ " + className + " ]";
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, th);
        }

        if ( result == null ) {
            String eMsg = "[ " + getHashText() + " ]" +
                          " Failed to open [ " + resourceName + " ]" +
                          " for class [ " + className + " ]";
            throw getFactory().newClassSourceException(eMsg);
        }

        return result;
    }

    @Override
    public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
        String methodName = "closeResourceStream";
        try {
            inputStream.close(); // throws IOException
        } catch ( IOException e ) {
            // "[ {0} ] Failed to close [ {1} ] as [ null ] under root [ null ] for class [ {2} ]: {3}"
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_CLOSE3_EXCEPTION", 
                new Object[] { getHashText(), resourceName, className, e });
        }
    }

    //

    @Override
    public void log(Logger useLogger) {
        String methodName = "log";
        useLogger.logp(Level.FINER, CLASS_NAME, methodName,  "Class Source [ {0} ]", getHashText());
    }

    //

    @Override
    public int processFromScratch(ClassSource_Streamer streamer)
        throws ClassSource_Exception {

        String methodName = "processFromScratch";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,  "[ {0} ] ENTER", getHashText());
        }

        int classCount = 0;

        for ( String nextResourceName : getProvider().getResourceNames() ) {
            if ( ClassSourceImpl.isDirectoryResource(nextResourceName) ) {
                continue;
            } else if ( !ClassSourceImpl.isClassResource(nextResourceName) ) {
                continue;
            }

            String nextClassName = ClassSourceImpl.getClassNameFromResourceName(nextResourceName);
            if ( ClassSourceImpl.isJava9PackageName(nextClassName) ) {  // PI89708
                logger.logp(Level.FINER, CLASS_NAME, methodName,  "Java9 class name [ {0} ]", nextClassName);
                continue;
            }
             
            classCount++;

            String i_nextClassName = internClassName(nextClassName);

            try {
                scan(streamer, i_nextClassName, nextResourceName); // throws ClassSource_Exception
            } catch ( ClassSource_Exception e ) {
                // "CWWKC0044W: An exception occurred while scanning class and annotation data: {0}",
                logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_SCAN_EXCEPTION", e);
            }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,  "[ {0} ] RETURN [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(classCount) });
        }
        return classCount;
    }

    @Override
    public void processSpecific(ClassSource_Streamer streamer, Set<String> i_classNames)
        throws ClassSource_Exception {

        String methodName = "processSpecific";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(i_classNames.size()) });
            }

        long scanStart = System.nanoTime();

        Collection<String> providerResourceNames = getProvider().getResourceNames();

        for ( String i_className : i_classNames ) {
            String resourceName = ClassSourceImpl.getResourceNameFromClassName(i_className);
            if ( !providerResourceNames.contains(resourceName) ) {
                continue;
            }

            try {
                scan(streamer, i_className, resourceName); // throws ClassSource_Exception

            } catch ( ClassSource_Exception e ) {
                // "CWWKC0044W: An exception occurred while scanning class and annotation data: {0}",
                logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_SCAN_EXCEPTION", e);
            }
        }

        long scanTime = System.nanoTime() - scanStart;

        setProcessTime(scanTime);
        setProcessCount( i_classNames.size() );

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN", getHashText());
        }
    }

    //

    protected void scan(ClassSource_Streamer streamer, String i_className, String resourceName)
        throws ClassSource_Exception {

        InputStream inputStream = openClassResourceStream(i_className, resourceName); // throws ClassSource_Exception

        try {
            streamer.process(i_className, inputStream); // throws ClassSource_Exception

        } finally {
            closeResourceStream(i_className, resourceName, inputStream);
        }
    }
}
