/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_MappedSimple;
import com.ibm.wsspi.anno.classsource.ClassSource_Options;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_Streamer;
import com.ibm.wsspi.anno.util.Util_InternMap;

/**
 * <p>Mapped simple class source implementation.</p>
 * 
 * <p>A simple class source is a simple wrapper for a simple class
 * provider. A simple class provider provides a list of resource
 * names and an ability to open a named resource as a stream.</p>
 * 
 * <p>See {@link ClassSource_MappedSimple.SimpleClassProvider}.</p>
 */
public class ClassSourceImpl_MappedSimple
    extends ClassSourceImpl
    implements ClassSource_MappedSimple {

    /** <p>Cache of the class name for logging.</p> */
    public static final String CLASS_NAME = ClassSourceImpl_MappedSimple.class.getName();

    /** <p>Trace component for simple class sources.</p> */
    private static final TraceComponent tc = Tr.register(ClassSourceImpl_MappedSimple.class);

    // Top O' the world

    /**
     * <p>Create a new simple class source which wraps a simple class provider.</p>
     * 
     * @param factory The factory which created this simple class source.
     * @param internMap The intern map used by this class source.
     * @param name A simple name for this class source. The name is intended
     *            both for logging, and as a unique identified for any enclosing
     *            class source.
     * @param options Options for the class source.
     * @param provider Call-out utility for obtaining resource names and for
     *            opening resource.
     * 
     * @throws ClassSource_Exception Thrown in case of a failure constructing
     *             the new simple class source. Currently unused.
     */
    @Trivial
    public ClassSourceImpl_MappedSimple(ClassSourceImpl_Factory factory,
                                        Util_InternMap internMap,
                                        String name,
                                        ClassSource_Options options,
                                        ClassSource_MappedSimple.SimpleClassProvider provider)
        throws ClassSource_Exception {

        super( factory, internMap, name, options, provider.getName() );

        this.provider = provider;
    }

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
    @Trivial
    public ClassSource_MappedSimple.SimpleClassProvider getProvider() {
        return provider;
    }

    /**
     * <p>Answer the resource names of the mapped provider.</p>
     * 
     * <p>The resource names can have leading slashes. Annotation
     * processing removes that leading slash.</p>
     * 
     * <p>The possible removal of a leading slash makes for extra
     * complications when managing resource names. When handling
     * a resource name relative to the provider resource names, if
     * the resource name does not have a leading slash, processing
     * must consider both the resource name without the leading slash
     * and must consider the resource name with the leading slash.</p>
     * 
     * <p>Processing does not to the converse: If a resource name is
     * provided which has a leading slash, processing does not consider
     * the resource name with the leading slash removed, since
     * annotation process never adds a leading slash.</p>
     * 
     * <p>See {@link ClassSource_MappedSimple.SimpleClassProvider#getResourceNames()}.</p>
     * 
     * @return The names of the resources of the mapped provider.
     */
    @Trivial
    public Collection<String> getResourceNames() {
        return getProvider().getResourceNames();
    }

    /**
     * <p>Tell if the provider contains a resource.</p>
     * 
     * <p>If provided a resource name which does not have a leading slash,
     * test with the leading slash added.</p>
     * 
     * <p>We expect the collection of resource names to never have
     * the resource name with and without a leading slash both as members.</p>
     * 
     * @param resourceName The name to test against the provider's resource names.
     * 
     * @return True or false telling if the provider contains the named resource.
     */
    @Trivial
    public boolean isProviderResource(String resourceName) {
        Collection<String> useResourceNames = getResourceNames();
        if ( useResourceNames.contains(resourceName) ) {
            return true;
        }

        // If the resource name already has a leading slash, there
        // is no additional testing to do.  Do NOT test again by
        // removing a leading slash. Annotation processing will remove
        // a leading slass, but will never add a leading slash.

        // The resource name is expected to be the name of a class resource,
        // which should never be empty.  However, let's double check,
        // and avoid any possible IndexOutOfBounds exception.

        if ( resourceName.length() == 0)  {
            return false;
        } else if ( resourceName.charAt(0) == '/' ) {
            return false;
        }

        return useResourceNames.contains("/" + resourceName);
    }

    // Open/close API: These are NO-OPs for simple class sources.
    //
    // Caution: Sub-types may override to provide less trivial
    // implementations.

    /*
     * <p>Open the class source to begin processing. This implementation is a NO-OP.</p>
     * 
     * @throws ClassSource_Exception Thrown in case the open fails. Never
     * thrown by this implementation.
     */
    @Override
    @Trivial
    public void open() throws ClassSource_Exception {
        String methodName = "open";
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] ENTER/RETURN", getHashText(), methodName));
        }
    }

    /*
     * <p>Close the class source to complete processing. This implementation is a NO-OP.</p>
     * 
     * @throws ClassSource_Exception Thrown in case the close fails. Never
     * thrown by this implementation.
     */
    @Override
    @Trivial
    public void close() throws ClassSource_Exception {
        String methodName = "close";
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] ENTER/RETURN", getHashText(), methodName));
        }
    }

    //

    /**
     * <p>Scan the available classes. The class names parameter specified previously
     * scanned classes. The class names parameter does not specify the names of classes
     * which are to be scanned.</p>
     * 
     * <p>Processing obtains the names of resources from the mapped simple class provider.
     * See {@link ClassSource_MappedSimple.SimpleClassProvider#getResourceNames()}.
     * Resources which represent directories are ignored. Resources which represent
     * non-class type files are ignored. Resources which represent class type files
     * which were previously scanned are ignored.</p>
     * 
     * <p>Scanning is performed in the order as provided by the resource names which
     * were obtained from the mapped simple class provider.</p>
     * 
     * @param streamer Processor for the input stream obtained for each resource which
     *            is being scanned.
     * @param i_seedClassNames Set of class names which were previously scanned. The
     *            set uses interned class names. A name is added for each new class
     *            which is scanned.
     * @param scanPolicy The scan policy of the resources of this class source. Used
     *            to place the scan results.
     */
    @Override
    protected void processFromScratch(
        ClassSource_Streamer streamer,
        Set<String> i_seedClassNames,
        ScanPolicy scanPolicy) {

        String methodName = "processFromScratch";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, MessageFormat.format("[ {0} ] ENTER", getHashText()));
        }

        // ++ JANDEX
        //
        // A simple class source must be provided with a JANDEX index, if one is to be
        // used.
        //
        // ++ JANDEX

        startTimings();

        int initialClasses = i_seedClassNames.size();

        for ( String nextResourceName : provider.getResourceNames() ) {
            if ( isDirectoryResource(nextResourceName) ) {
                incrementResourceExclusionCount();

                // Mark all directories as non-root containers;
                // Note that root containers are not currently possible for jar elements.

                markResult(ClassSource_ScanCounts.ResultField.CONTAINER);
                markResult(ClassSource_ScanCounts.ResultField.NON_ROOT_CONTAINER);

            } else {
                if ( !isClassResource(nextResourceName) ) {
                    incrementResourceExclusionCount();

                    markResult(ClassSource_ScanCounts.ResultField.NON_CLASS);

                } else {
                    // Processing notes:
                    //
                    // Make sure to record the class before attempting processing.
                    //
                    // Only one version of the class is to be processed, even if processing
                    // fails on that one version.
                    //
                    // That is, if two child class sources have versions of a class, and
                    // the version from the first class source is non-valid, the version
                    // of the class in the second class source is still masked by the
                    // version in the first class source.

                    String nextClassName = getClassNameFromResourceName(nextResourceName);
                    if ( isJava9SpecificClass(nextClassName) ) {
                        Tr.debug(tc, MessageFormat.format("[ {0} ] Unsupported class; skipping [ {1} ]", 
                                                          new Object[] { getHashText(), nextClassName }));
                        continue;
                    }

                    String i_nextClassName = internClassName(nextClassName);
                    boolean didAdd = i_maybeAdd(i_nextClassName, i_seedClassNames);

                    if ( !didAdd ) {
                        incrementClassExclusionCount();

                        markResult(ClassSource_ScanCounts.ResultField.DUPLICATE_CLASS);

                    } else {
                        incrementClassInclusionCount();

                        boolean didProcess;

                        try {
                            didProcess = process(streamer, nextClassName, nextResourceName, scanPolicy); // throws ClassSource_Exception

                        } catch (ClassSource_Exception e) {
                            didProcess = false;

                            // TODO: NEW_MESSAGE: Need a new message here.

                            // String eMsg = "[ " + getHashText() + " ]" +
                            //               " Failed to process entry [ " + nextResourceName + " ]" +
                            //               " under root [ " + getJarPath() + " ]" +
                            //               " for class [ " + nextClassName + " ]";
                            // CWWKC0044W: An exception occurred while scanning class and annotation data.
                            Tr.warning(tc, "ANNO_TARGETS_SCAN_EXCEPTION", e);
                        }

                        if ( didProcess ) {
                            markResult(ClassSource_ScanCounts.ResultField.PROCESSED_CLASS);

                        } else {
                            markResult(ClassSource_ScanCounts.ResultField.UNPROCESSED_CLASS);
                        }
                    }

                    markResult(ClassSource_ScanCounts.ResultField.CLASS);
                }

                markResult(ClassSource_ScanCounts.ResultField.NON_CONTAINER);
            }

            // Mark in the totals count for every entry.
            markResult(ClassSource_ScanCounts.ResultField.ENTRY);
        }

        int finalClasses = i_seedClassNames.size();

        if ( tc.isDebugEnabled() ) {
            Object[] logParms = new Object[] { getHashText(), null, null };

            logParms[1] = Integer.valueOf(finalClasses - initialClasses);
            Tr.debug(tc, MessageFormat.format("[ {0} ] RETURN [ {1} ] Added classes", logParms));

            for (ClassSource_ScanCounts.ResultField resultField : ClassSource_ScanCounts.ResultField.values()) {
                int nextResult = getResult(resultField);
                String nextResultTag = resultField.getTag();

                logParms[1] = Integer.valueOf(nextResult);
                logParms[2] = nextResultTag;

                Tr.debug(tc, MessageFormat.format("[ {0} ]  [ {1} ] {2}", logParms));
            }
        }

        endTimings();

        if ( tc.isEntryEnabled() ) {
            Tr.exit(tc, methodName, getHashText());
        }
    }

    /**
     * <p>Generate a class name from a resource name.</p>
     * 
     * <p>Override: Simple class sources process bundle based resources.
     * Bundle based resource names are generated with leading slashes
     * for loosely mapped elements of the bundle. In addition to the
     * replacement of "/" characters with ".", strip off any leading "/"
     * before performing the replacement.</p>
     * 
     * <p>This implementation means the class name to resource name mapping
     * is no longer simply invertible. As a consequence, code which handles
     * resource names (for example, com.ibm.ws.eba.jpa.annotation.scanning.BundleBasedSimpleSourceProvider.openResource),
     * must handle a resource name which has a leading "/" the same as one
     * which does not have a leading "/".
     * 
     * <p>See {@link ClassSourceImpl#getResourceNameFromClassName(String)}.</p>
     * 
     * <p>See also {@link #isProviderResource(String)} and {@link #openResourceStream(String, String)}.</p>
     * 
     * @param resourceName The resource for which to obtain a class name.
     * 
     * @return The class name for the resource.
     */
    @Override
    public String getClassNameFromResourceName(String resourceName) {
        int endingOffset = resourceName.length() - ClassSource.CLASS_EXTENSION.length();
        int startingOffset = (resourceName.charAt(0) == '/') ? 1 : 0;
        String className = resourceName.substring(startingOffset, endingOffset);
        className = className.replace(RESOURCE_SEPARATOR_CHAR, ClassSource.CLASS_SEPARATOR_CHAR);
        return className;
    }

    //

    /** <p>Performance monitoring value: The start time of scanning.</p> */
    protected long startTime;

    /** <p>Performance monitoring value: The start time of scanning.</p> */
    protected long endTime;

    /**
     * <p>Performance monitoring value: The total time spent obtaining
     * input streams. The other main time constituent is expected to
     * be time spent scanning.</p>
     */
    protected long streamTime;

    /**
     * <p>Set the start time, and reset the stream time to zero.</p>
     */
    @Trivial
    protected void startTimings() {
        startTime = getTime();

        streamTime = 0L;
    }

    /**
     * <p>Add a time value to the stream time. Stream time is time
     * spent obtaining input streams.</p>
     * 
     * @param additionalTime A time value to add to the stream time.
     */
    @Trivial
    protected void addStreamTime(long additionalTime) {
        streamTime += additionalTime;
    }

    /**
     * <p>Set the end time, and log the accumulated timing statistics.</p>
     */
    @Trivial
    protected void endTimings() {
        endTime = getTime();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("Start time:  [ {0} ]", Long.valueOf(startTime)));
            Tr.debug(tc, MessageFormat.format("End time:    [ {0} ]", Long.valueOf(endTime)));
            Tr.debug(tc, MessageFormat.format("Delta time:  [ {0} ]", Long.valueOf(endTime - startTime)));
            Tr.debug(tc, MessageFormat.format("Stream time: [ {0} ]", Long.valueOf(streamTime)));
        }
    }

    //

    /**
     * <p>Main processing point: If enabled by the streamer, obtain a resource stream
     * for the specified class and process that class.</p>
     * 
     * <p>Answer true or false, telling if the class was processed. True is returned
     * and no processing is performed if the streamer is null.</p>
     * 
     * <p>Filtering and processing are provided by the stream. See {@link ClassSource_Streamer#doProcess(String, boolean, boolean, boolean)} and
     * {@link ClassSource_Streamer#process(String, String, InputStream, boolean, boolean, boolean)}.</p>
     * 
     * <p>Resource management is handled by the bound class provider.
     * See {@link ClassSource_MappedSimple.SimpleClassProvider#openResource(String)}.
     * The input stream is closed through a direct call. See {InputStream#close()}.</p>
     * 
     * <p>As a special case, a null input stream obtained from the simple class
     * provided is handled as a false result.
     * 
     * @param streamer A stream to handle the class data.
     * @param className The name of the class which is being processed.
     * @param resourceName The name of the resource associated with the class.
     * @param scanPolicy Control value provided to support web-module fragment cases.
     * 
     * @return True if the class was processed, or if the stream is null.
     * 
     * @throws ClassSource_Exception Thrown in case of a failure to open the input stream for
     *             the class, or in case of a processing failure. A failure
     *             to close the input stream is logged, but does not cause
     *             a class source exception to be thrown.
     */
    protected boolean process(
        ClassSource_Streamer streamer,
        String className, String resourceName,
        ScanPolicy scanPolicy) throws ClassSource_Exception {

        if ( streamer == null ) {
            return true;
        } else if ( !streamer.doProcess(className, scanPolicy) ) {
            return false;
        }

        InputStream inputStream = openResourceStream(className, resourceName); // throws ClassSource_Exception
        if ( inputStream == null ) {
            return false; // Null is returned if the resource is not a resource of the mapped provider.
        }

        try {
            streamer.process( getCanonicalName(), className, inputStream, scanPolicy );
            // 'process' throws ClassSourceException
        } finally {
            closeResourceStream(className, resourceName, inputStream);
        }

        return true;
    }

    //

    /**
     * <p>Open a stream for a named class which used a named resource.</p>
     * 
     * <p>Answer null if the provider does not contain the named resource.
     * See {@link #isProviderResource(String)}.</p>
     * 
     * @param className The name of the class which mapped to the resource.
     * @param resourceName The name of the resource used by the class.
     * 
     * @return An input stream for the resource.
     * 
     * @throws ClassSource_Exception Thrown in case of a failure to open
     *             the resource. Usually, because of an {@link IOException} from {@link ClassSource_MappedSimple.SimpleClassProvider#openResource(String)}.
     */
    @Override
    public InputStream openResourceStream(String className, String resourceName)
                    throws ClassSource_Exception {
        String methodName = "openResourceStream";

        if (!isProviderResource(resourceName)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] ENTER/RETURN [ null ]: Provider does not contain [ {2} ]",
                                                  getHashText(), methodName, resourceName));
            }
            return null;
        }

        InputStream result;

        try {
            long initialTime = getTime();
            result = getProvider().openResource(resourceName); // throws IOException
            long finalTime = getTime();
            addStreamTime(finalTime - initialTime);

        } catch (Throwable th) {
            // defect 84235:we are generating multiple Warning/Error messages for each error
            // due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level, 
            // preferably the ultimate consumer of the exception.
            //Tr.warning(tc, "ANNO_CLASSSOURCE_OPEN2_EXCEPTION",
            //           getHashText(), resourceName, null, null, className);

            String eMsg = "[ " + getHashText() + " ]" +
                          " Failed to open [ " + resourceName + " ]" +
                          " for class [ " + className + " ]";
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, th);
        }

        if (result == null) {
            // defect 84235:we are generating multiple Warning/Error messages for each error
            // due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level, 
            // preferably the ultimate consumer of the exception.
            //Tr.warning(tc, "ANNO_CLASSSOURCE_OPEN2_EXCEPTION",
            //           getHashText(), resourceName, null, null, className);

            String eMsg = "[ " + getHashText() + " ]" +
                          " Failed to open [ " + resourceName + " ]" +
                          " for class [ " + className + " ]";
            throw getFactory().newClassSourceException(eMsg);
        }

        return result;
    }

    @Override
    public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
        try {
            inputStream.close(); // throws IOException

        } catch (IOException e) {
            // String eMsg = "[ " + getHashText() + " ]" +
            //               " Failed to close [ " + resourceName + " ]" +
            //               " as [ " + entry + " ]" +
            //               " under root [ " + getContainer() + " ]" +
            //               " for class [ " + className + " ]";
            Tr.warning(tc, "ANNO_CLASSSOURCE_CLOSE3_EXCEPTION",
                       getHashText(), resourceName, null, null, className);
        }
    }

    //

    @Override
    @Trivial
    public void log(TraceComponent logger) {
        Tr.debug(logger, MessageFormat.format("Class Source [ {0} ]", getHashText()));
    }
}
