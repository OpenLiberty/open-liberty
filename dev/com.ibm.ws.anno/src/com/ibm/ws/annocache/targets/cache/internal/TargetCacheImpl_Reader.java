/*******************************************************************************
  * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.cache.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.TargetsTableAnnotations;
import com.ibm.ws.annocache.targets.TargetsTableClasses;
import com.ibm.ws.annocache.targets.TargetsTableClassesMulti;
import com.ibm.ws.annocache.targets.TargetsTableContainers;
import com.ibm.ws.annocache.targets.TargetsTableDetails;
import com.ibm.ws.annocache.targets.TargetsTableTimeStamp;
import com.ibm.ws.annocache.targets.cache.TargetCache_ParseError;
import com.ibm.ws.annocache.targets.cache.TargetCache_Reader;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_ExternalConstants;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_InternalConstants;

public class TargetCacheImpl_Reader implements TargetCache_Reader, TargetCache_InternalConstants {
    private static final String CLASS_NAME = TargetCacheImpl_Reader.class.getSimpleName();

    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    //

    public TargetCacheImpl_Reader(TargetCacheImpl_Factory factory,
                                  String path, InputStream stream,
                                  String encoding) throws UnsupportedEncodingException {
        super();

        this.factory = factory;

        this.path = path;
        this.stream = stream;

        this.encoding = encoding;

        this.reader = new InputStreamReader(stream, encoding); // throws UnsupportedEncodingException
        this.bufferedReader = new BufferedReader(reader);

        //

        this.lineNo = 0;
        this.line = null;

        this.parsedName = null;
        this.parsedValue = null;

        this.parseErrors = new ArrayList<TargetCache_ParseError>();
    }

    //

    protected final TargetCacheImpl_Factory factory;

    @Trivial
    public TargetCacheImpl_Factory getFactory() {
        return factory;
    }

    //

    protected final String path;
    protected final InputStream stream;

    protected final String encoding;
    protected final InputStreamReader reader;
    protected final BufferedReader bufferedReader;

    @Trivial
    public String getPath() {
        return path;
    }

    @Trivial
    public InputStream getStream() {
        return stream;
    }

    @Trivial
    public String getEncoding() {
        return encoding;
    }

    @Trivial
    public InputStreamReader getReader() {
        return reader;
    }

    @Override
    public void close() throws IOException {
        getReader().close(); // 'close' throws IOException
    }

    @Trivial
    public BufferedReader getBufferedReader() {
        return bufferedReader;
    }

    protected String readNextLine() throws IOException {
        return bufferedReader.readLine(); // throws IOException
    }

    //

    // Core line parsing ...

    protected int lineNo;
    protected String line;

    protected String parsedName;
    protected String parsedValue;

    @Trivial
    protected boolean isComment() {
        return line.startsWith(COMMENT_TAG);
    }

    @Trivial
    protected boolean isEndDelimiter() {
        return line.startsWith(END_DELIMITER_TEST_TAG);
    }

    protected String parseLine() {
        int separatorOffset = line.indexOf(COLON_TAG);

        if (separatorOffset == -1) {
            return "Colon delimited value expected.";

        } else if (separatorOffset == 0) {
            return "Null initial value.";

        } else if (separatorOffset == line.length() - 1) {
            return "Null final value.";

        } else {
            String leadingText = line.substring(0, separatorOffset);
            String trailingText = line.substring(separatorOffset + 1).trim();

            parsedName = leadingText;
            parsedValue = trailingText;

            return null;
        }
    }

    /**
     * <p>Handle the next line as a header line.  Header
     * fields are {@link #TABLE_TAG}, {@link #VERSION_TAG},
     * {@link #ENCODING_TAG}, and {@link #TIMESTAMP_TAG}.</p>
     *
     * @param expectedTable The expected table tag.
     * @param expectedTableVersion The expected table version.
     *
     * @return True if the line was handled as a header line.
     *         Otherwise, false.
     */
    public boolean handleHeader(String expectedTableTag, String expectedTableVersion) {
        boolean isHeaderField;

        if (parsedName.equals(TABLE_TAG)) {
            if ( !parsedValue.equals(expectedTableTag)) {
                addParseError("Value [ " + parsedValue + " ] does not match [ " + expectedTableTag + " ] for class [ " + getClass().getName() + " ]");
            }

            isHeaderField = true;

        } else if (parsedName.equals(VERSION_TAG)) {
            if ( !parsedValue.equals(expectedTableVersion)) {
                addParseError("Value [ " + parsedValue + " ] does not match [ " + expectedTableVersion + " ] for class [ " + getClass().getName() + " ]");
            }

            isHeaderField = true;

        } else if (parsedName.equals(ENCODING_TAG)) {
            isHeaderField = true;

        } else if (parsedName.equals(TIMESTAMP_TAG)) {
            isHeaderField = true;

        } else {
            isHeaderField = false;
        }

        return isHeaderField;
    }

    // Main parse step ...

    /**
     * <p>Parse data from the bound stream; place the data
     * according to {@link #handleHeader()}, {@link #handleBody()},
     * and {@link #handleTrailer()}.</p>
     *
     * @param useReader The reader which is active.
     *
     * @return The errors generated by the parse.
     *
     * @throws IOException Thrown in case of an read failure.  Not thrown
     *     in case of a parse error.  Parsing continues after a parse error.
     */
    public List<TargetCache_ParseError> parse(TargetsReader useReader)
        throws IOException {

        String methodName = "parse";

        String expectedTableTag = useReader.getTableTag();
        String expectedTableVersion = useReader.getTableVersion();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "Expected table [ " + expectedTableTag + " ]");
            logger.logp(Level.FINER, CLASS_NAME, methodName, "Expected table version [ " + expectedTableVersion + " ]");            
        }

        while ((line = readNextLine()) != null) { // IOException
            lineNo++;

            line = line.trim(); // Skip leading and trailing white space.

            if (line.length() == 0) { // Skip empty lines.
                continue;
            } else if (isEndDelimiter()) { // Stop on the end delimiter
                // The end delimiter test must be first!
                // The end delimiter is also a comment line.
                break;
            } else if (isComment()) { // Skip comments.
                continue;
            }

            String parseMessage = parseLine();
            if (parseMessage != null) {
                addParseError(parseMessage);
                continue;
            }

            if ( !handleHeader(expectedTableTag, expectedTableVersion) ) {
                if ( !useReader.handleBody() ) {
                    // Ignore
                }
            }
        }

        useReader.handleTrailer();

        return parseErrors;
    }

    // Common error handling ...

    public static final int PARSE_ERROR_LIMIT = 100;

    protected final List<TargetCache_ParseError> parseErrors;

    protected TargetCache_ParseError addParseError(String parseMessage) {
        TargetCache_ParseError parseError;

        int numParseErrors = parseErrors.size();

        if (numParseErrors < PARSE_ERROR_LIMIT) {
            parseError = new TargetCache_ParseError(TargetCache_ParseError.NULL_MSG_ID, parseMessage,
                                                    getPath(), lineNo, line);

        } else if ( numParseErrors == PARSE_ERROR_LIMIT ) {
            parseMessage = "Parse error limit reached [ " + PARSE_ERROR_LIMIT + " ]";
            parseError = new TargetCache_ParseError(TargetCache_ParseError.NULL_MSG_ID, parseMessage, getPath(), lineNo, line);

        } else {
            parseError = null;
        }

        if ( parseError != null ) {
            parseErrors.add(parseError);
        }

        return parseError;
    }

    //

    public abstract class TargetsReader {
        public TargetsReader() {
            super();

            this.i_packageName = null;
            this.i_className = null;
            this.modifiers = null;
            this.i_fieldName = null;
            this.i_methodSignature = null;
        }

        // Header protocol:

        /**
         * <p>Answer the tag which is expected within the table line
         * ("Table: &lt;tableTag&gt;").</p>
         *
         * <p>Used by {@link #handleHeader()} to validate the table which
         * is being parsed.</p>
         *
         * @return The tag which is expected on the table line.
         */
        public abstract String getTableTag();

        /**
         * <p>Answer the version which is expected within the version line
         * ("Version: &lt;tableVersion&gt;").</p>
         *
         * <p>Used by {@link #handleHeader()} to validate the table which
         * is being parsed.</p>
         *
         * @return The version which is expected on the version line.
         */
        public abstract String getTableVersion();

        // Body protocol:

        // Intern operations ... depend on the target data

        protected String internPackageName(String packageName) {
            throw new UnsupportedOperationException();
        }

        protected String internClassName(String className) {
            throw new UnsupportedOperationException();
        }

        protected String internFieldName(String fieldName) {
            throw new UnsupportedOperationException();
        }

        protected String internMethodSignature(String methodSignature) {
            throw new UnsupportedOperationException();
        }

        // Superset of body data:

        protected String i_packageName;
        protected String i_className;
        protected String modifiers;
        protected String i_fieldName;
        protected String i_methodSignature;

        protected String i_annotationClassName;

        public abstract boolean handleBody();

        // Trailer protocol:

        public void handleTrailer() {
            i_annotationClassName = null;

            i_methodSignature = null;
            i_fieldName = null;

            modifiers = null;
            i_className = null;

            i_packageName = null;
        }
    }

    public class ContainerTableReader extends TargetsReader {
        public ContainerTableReader(TargetsTableContainers containerTable) {
            this.containerTable = containerTable;

            this.name = null;
        }

        //

        protected final TargetsTableContainers containerTable;

        //

        @Override
        public String getTableTag() {
            return CONTAINER_TABLE_TAG;
        }

        @Override
        public String getTableVersion() {
            return CONTAINER_TABLE_VERSION;
        }

        //

        protected String name;

        @Override
        public boolean handleBody() {
            boolean didHandle;

            if ( parsedName.equals(NAME_TAG) ) {
                if ( name != null ) {
                    addParseError("Tag [ " + parsedName + " ] immediately follows unclosed [ " + NAME_TAG + " ]: Ignoring prior name [ " + name + " ]");
                    name = null;
                }

                if ( parsedValue.equals(TargetCache_ExternalConstants.ROOT_CONTAINER_NAME) ) {
                    name = TargetCache_ExternalConstants.CANONICAL_ROOT_CONTAINER_NAME;
                } else {
                    name = parsedValue;
                }

                didHandle = true;

            } else if ( parsedName.equals(POLICY_TAG) ) {
                if ( name == null ) {
                    addParseError("Tag [ " + parsedName + " ] does not immediately follow [ " + NAME_TAG + " ]: Ignoring");

                } else {
                    String useName = name; // Always consume the name.
                    name = null;

                    ScanPolicy policy;
                    try {
                        policy = ScanPolicy.valueOf(parsedValue);
                    } catch ( IllegalArgumentException e ) {
                        policy = null;
                        addParseError("Value [ " + parsedValue + " ] is not a valid policy value: Ignoring");
                    }

                    if ( policy != null ) {
                        ScanPolicy lastPolicy = containerTable.removeName(name);
                        if ( lastPolicy != null ) {
                            addParseError("Policy [ " + parsedValue + " ] collision at [ " + name + " ] with prior assignment to policy [ " + lastPolicy.name() + " ]: Replacing");
                        }

                        containerTable.addName(useName, policy);
                    }
                }

                didHandle = true;

            } else {
                didHandle = false;
            }

            return didHandle;
        }

        //

        @Override
        public void handleTrailer() {
            if ( name != null ) {
                addParseError("Missing [ " + POLICY_TAG + " ] for last [ " + NAME_TAG + " ]");
            }

            super.handleTrailer();
        }
    }

    public class RefsReader extends TargetsReader {
        public RefsReader(String tableTag, String tableVersion,
                          UtilImpl_InternMap internMap, Set<String> refs) {
            this.tableTag = tableTag;
            this.tableVersion = tableVersion;

            this.internMap = internMap;
            this.refs = refs;
        }

        protected final UtilImpl_InternMap internMap;

        public String intern(String className) {
            return internMap.intern(className);
        }

        protected final Set<String> refs;

        protected void addRef(String className) {
            refs.add( intern(className) ) ;
        }

        //

        protected final String tableTag;

        @Override
        public String getTableTag() {
            return tableTag;
        }

        protected final String tableVersion;

        @Override
        public String getTableVersion() {
            return tableVersion;
        }

        //

        @Override
        public boolean handleBody() {
            if ( parsedName.equals(CLASS_TAG) ) {
                addRef(parsedValue);
                return true;
            } else {
                return false;
            }
        }

        //

        @Override
        public void handleTrailer() {
            super.handleTrailer(); // Nothing specific to do.
        }
    }

    public class StampTableReader extends TargetsReader {
        public StampTableReader(TargetsTableTimeStamp stampTable) {
            this.stampTable = stampTable;

            this.name = null;
            this.stamp = null;
        }

        protected final TargetsTableTimeStamp stampTable;

        private String name;
        private String stamp;

        //

        @Override
        public String getTableTag() {
            return STAMP_TABLE_TAG;
        }

        @Override
        public String getTableVersion() {
            return STAMP_TABLE_VERSION;
        }

        @Override
        public boolean handleBody() {
            boolean didHandle;

            if (parsedName.equals(NAME_TAG)) {
                if ( name != null ) {
                    addParseError("Multiple [ " + NAME_TAG + " ]: Replacing [ " + name + " ] with [ " + parsedValue + " ]");
                }

                stampTable.setName(parsedValue);
                didHandle = true;

            } else if ( parsedName.equals(STAMP_TAG) ) {
                if ( stamp != null ) {
                    addParseError("Multiple [ " + STAMP_TAG + " ]: Replacing [ " + stamp + " ] with [ " + parsedValue + " ]");
                }

                stampTable.setStamp(parsedValue);
                didHandle = true;

            } else {
                didHandle = false;
            }

            return didHandle;
        }

        //

        @Override
        public void handleTrailer() {
            if ( stampTable.getName() == null ) {
                addParseError("No [ " + NAME_TAG + " ] element was read.");
            }

            if ( stampTable.getStamp() == null ) {
                addParseError("No [ " + STAMP_TAG + " ] element was read.");
            }

            super.handleTrailer();
        }
    }

    public class ClassTableReader extends TargetsReader {
        public ClassTableReader(TargetsTableClasses classTable) {
            this.classTable = classTable;

            i_superclassName = null;
            i_interfaceNames = new ArrayList<String>();
        }

        protected final TargetsTableClasses classTable;

        @Override
        @Trivial
        protected String internPackageName(String packageName) {
            return classTable.internClassName(packageName);
        }

        @Override
        @Trivial
        protected String internClassName(String className) {
            return classTable.internClassName(className);
        }

        @Override
        @Trivial
        protected String internFieldName(String fieldName) {
            throw new UnsupportedOperationException();
        }

        @Override
        @Trivial
        protected String internMethodSignature(String methodSignature) {
            throw new UnsupportedOperationException();
        }

        //

        @Override
        public String getTableTag() {
            return CLASSES_TABLE_TAG;
        }

        @Override
        public String getTableVersion() {
            return CLASSES_TABLE_VERSION;
        }

        // Additional body data for package and class tables:

        protected String i_superclassName;
        protected List<String> i_interfaceNames;

        protected void consumePackageOrClass() {
            if ( i_packageName != null ) {
                classTable.record(i_packageName);
                i_packageName = null;

            } else if ( i_className != null ) {
                int modifiersValue;
                if ( modifiers == null ) {
                    modifiersValue = 0;
                } else {
                    modifiersValue = Integer.parseInt(modifiers, 16);
                }

                classTable.record(i_className, i_superclassName, i_interfaceNames, modifiersValue);

                i_className = null;
                i_superclassName = null;
                i_interfaceNames.clear();
                modifiers = null;

            } else {
                // Nothing to do!
            }
        }

        @Override
        public boolean handleBody() {
            boolean didHandle;

            if ( parsedName.equals(PACKAGE_TAG) ) {
                consumePackageOrClass();

                i_packageName = internPackageName(parsedValue);

                didHandle = true;

            } else if ( parsedName.equals(CLASS_TAG) ) {
                consumePackageOrClass();

                i_className = internClassName(parsedValue);

                didHandle = true;

            } else if ( parsedName.equals(SUPERCLASS_TAG) ) {
                if ( i_className == null ) {
                    addParseError("Tag [ " + parsedName + " ] must follow [ " + CLASS_TAG + " ]");
                } else {
                    i_superclassName = internClassName(parsedValue);
                }

                didHandle = true;

            } else if ( parsedName.equals(INTERFACE_TAG) ) {
                if ( i_className == null ) {
                    addParseError("Tag [ " + parsedName + " ] must follow [ " + CLASS_TAG + " ]");
                } else {
                    String i_interfaceName = internClassName(parsedValue);
                    i_interfaceNames.add(i_interfaceName);
                }

                didHandle = true;

            } else if ( parsedName.equals(MODIFIERS_TAG) ) {
                if ( i_className == null ) {
                    addParseError("Tag [ " + parsedName + " ] must follow [ " + CLASS_TAG + " ]");
                } else {
                    int startOffset = 2; // Skip "0x";
                    int endOffset = parsedValue.indexOf(" "); // Skip " public static ...";
                    modifiers = parsedValue.substring(startOffset, endOffset);
                }

                didHandle = true;

            } else {
                didHandle = false;
            }

            return didHandle;
        }

        //

        @Override
        public void handleTrailer() {
            // Classes are not processed until data for a subsequent
            // class (or package) is encountered.  That means that
            // class data is usually present and not yet consumed
            // upon reaching the end of the serialized data.

            consumePackageOrClass();

            super.handleTrailer();
        }
    }

    public class ClassTableMultiReader extends TargetsReader {
        public ClassTableMultiReader(TargetsTableClassesMulti classTable) {
            this.classTable = classTable;

            classSourceName = null;

            i_superclassName = null;
            i_interfaceNames = new ArrayList<String>();
        }

        protected final TargetsTableClassesMulti classTable;

        @Override
        @Trivial
        protected String internPackageName(String packageName) {
            return classTable.internClassName(packageName);
        }

        @Override
        @Trivial
        protected String internClassName(String className) {
            return classTable.internClassName(className);
        }

        @Override
        @Trivial
        protected String internFieldName(String fieldName) {
            throw new UnsupportedOperationException();
        }

        @Override
        @Trivial
        protected String internMethodSignature(String methodSignature) {
            throw new UnsupportedOperationException();
        }

        //

        @Override
        public String getTableTag() {
            return CLASSES_TABLE_TAG;
        }

        @Override
        public String getTableVersion() {
            return CLASSES_TABLE_VERSION;
        }

        // Additional body data for package and class tables:

        protected String classSourceName;

        protected String i_superclassName;
        protected List<String> i_interfaceNames;

        protected void consumePackageOrClass() {
            if ( classSourceName != null ) {
                if ( i_packageName != null ) {
                    classTable.record(classSourceName, i_packageName);
                    i_packageName = null;

                } else {
                    int modifiersValue;
                    if ( modifiers == null ) {
                        modifiersValue = 0;
                    } else {
                        modifiersValue = Integer.parseInt(modifiers, 16);
                    }
                    classTable.record(classSourceName, i_className, i_superclassName, i_interfaceNames, modifiersValue);
                }
            }

            classSourceName = null;

            i_className = null;
            i_superclassName = null;
            i_interfaceNames.clear();
            modifiers = null;
        }

        @Override
        public boolean handleBody() {
            boolean didHandle;

            if ( parsedName.equals(PACKAGE_TAG) ) {
                consumePackageOrClass();

                i_packageName = internPackageName(parsedValue);

                didHandle = true;

            } else if ( parsedName.equals(CLASS_TAG) ) {
                consumePackageOrClass();

                i_className = internClassName(parsedValue);

                didHandle = true;

            } else if ( parsedName.equals(CLASS_SOURCE_TAG) ) {
                if ( (i_className == null) && (i_packageName == null) ) {
                    addParseError("Tag [ " + parsedName + " ] must follow [ " + PACKAGE_TAG + " ] or [ " + CLASS_TAG + " ]");
                } else {
                    classSourceName = parsedValue;
                }

                didHandle = true;

            } else if ( parsedName.equals(SUPERCLASS_TAG) ) {
                if ( i_className == null ) {
                    addParseError("Tag [ " + parsedName + " ] must follow [ " + CLASS_TAG + " ]");
                } else {
                    i_superclassName = internClassName(parsedValue);
                }

                didHandle = true;

            } else if ( parsedName.equals(INTERFACE_TAG) ) {
                if ( i_className == null ) {
                    addParseError("Tag [ " + parsedName + " ] must follow [ " + CLASS_TAG + " ]");
                } else {
                    String i_interfaceName = internClassName(parsedValue);
                    i_interfaceNames.add(i_interfaceName);
                }

                didHandle = true;

            } else if ( parsedName.equals(MODIFIERS_TAG) ) {
                if ( i_className == null ) {
                    addParseError("Tag [ " + parsedName + " ] must follow [ " + CLASS_TAG + " ]");
                } else {
                    int startOffset = 2; // Skip "0x";
                    int endOffset = parsedValue.indexOf(" "); // Skip " public static ...";
                    modifiers = parsedValue.substring(startOffset, endOffset);
                }

                didHandle = true;

            } else {
                didHandle = false;
            }

            return didHandle;
        }

        //

        @Override
        public void handleTrailer() {
            // Classes are not processed until data for a subsequent
            // class (or package) is encountered.  That means that
            // class data is usually present and not yet consumed
            // upon reaching the end of the serialized data.

            consumePackageOrClass();

            super.handleTrailer();
        }
    }

    public class TargetTableReader extends TargetsReader {
        public TargetTableReader(TargetsTableAnnotations targetTable) {
            this.targetTable = targetTable;
        }

        //

        protected final TargetsTableAnnotations targetTable;

        @Override
        @Trivial
        protected String internPackageName(String packageName) {
            return targetTable.internClassName(packageName);
        }

        @Override
        @Trivial
        protected String internClassName(String className) {
            return targetTable.internClassName(className);
        }

        @Override
        @Trivial
        protected String internFieldName(String fieldName) {
            throw new UnsupportedOperationException();
        }

        @Override
        @Trivial
        protected String internMethodSignature(String methodSignature) {
            throw new UnsupportedOperationException();
        }

        //

        @Override
        public String getTableTag() {
            return TARGETS_TABLE_TAG;
        }

        @Override
        public String getTableVersion() {
            return TARGETS_TABLE_VERSION;
        }

        //

        @Override
        public boolean handleBody() {
            boolean didHandle;

            if ( parsedName.equals(PACKAGE_TAG) ) {
                i_packageName = null;
                i_className = null;

                i_packageName = internPackageName(parsedValue);

                didHandle = true;

            } else if ( parsedName.equals(CLASS_TAG) ) {
                i_packageName = null;
                i_className = null;

                i_className = internClassName(parsedValue);

                didHandle = true;

            } else if ( parsedName.equals(PACKAGE_ANNOTATION_TAG) ) {
                if ( i_packageName == null ) {
                    addParseError("Tag [ " + parsedName + " ] with no proceeding [ " + PACKAGE_TAG + " ]: Ignoring");

                } else {
                    String i_parsedAnnotationClassName = internClassName(parsedValue);
                    targetTable.recordPackageAnnotation(i_packageName, i_parsedAnnotationClassName);
                }

                didHandle = true;

            } else if ( parsedName.equals(CLASS_ANNOTATION_TAG) ) {
                if ( i_className == null ) {
                    addParseError("Tag [ " + parsedName + " ] with no proceeding of [ " + CLASS_TAG + " ]: Ignoring");

                } else {
                    String i_parsedAnnotationClassName = internClassName(parsedValue);
                    targetTable.recordClassAnnotation(i_className, i_parsedAnnotationClassName);
                }

                didHandle = true;

            } else if ( parsedName.equals(FIELD_ANNOTATION_TAG) ) {
                if ( i_className == null ) {
                    addParseError("Tag [ " + parsedName + " ] with no proceeding [ " + CLASS_TAG + " ]: Ignoring");

                } else {
                    String i_parsedAnnotationClassName = internClassName(parsedValue);
                    targetTable.recordFieldAnnotation(i_className, i_parsedAnnotationClassName);
                }

                didHandle = true;

            } else if ( parsedName.equals(METHOD_ANNOTATION_TAG) ) {
                if ( i_className == null ) {
                    addParseError("Tag [ " + parsedName + " ] with no proceeding [ " + CLASS_TAG + " ]: Ignoring");

                } else {
                    String i_parsedAnnotationClassName = internClassName(parsedValue);
                    targetTable.recordMethodAnnotation(i_className, i_parsedAnnotationClassName);
                }

                didHandle = true;

            } else {
                didHandle = false;
            }

            return didHandle;
        }

        //

        @Override
        public void handleTrailer() {
            super.handleTrailer();
        }
    }

    public class DetailTableReader extends TargetsReader {
        public DetailTableReader(TargetsTableDetails detailTable) {
            this.detailTable = detailTable;
        }

        //

        protected final TargetsTableDetails detailTable;

        @Override
        @Trivial
        protected String internPackageName(String packageName) {
            return detailTable.getParentData().internClassName(packageName);
        }

        @Override
        @Trivial
        protected String internClassName(String className) {
            return detailTable.getParentData().internClassName(className);
        }

        @Override
        @Trivial
        protected String internFieldName(String fieldName) {
            return detailTable.getParentData().internFieldName(fieldName);
        }

        @Override
        @Trivial
        protected String internMethodSignature(String methodSignature) {
            return detailTable.getParentData().internMethodSignature(methodSignature);
        }

        //

        @Override
        public String getTableTag() {
            return DETAILS_TABLE_TAG;
        }

        @Override
        public String getTableVersion() {
            return DETAILS_TABLE_VERSION;
        }

        //

        @Override
        public boolean handleBody() {
            boolean didHandle;

            if ( parsedName.equals(PACKAGE_TAG) ) {
                i_packageName = null;
                i_className = null;

                i_packageName = internPackageName(parsedValue);

                didHandle = true;

            } else if ( parsedName.equals(CLASS_TAG) ) {
                i_packageName = null;
                i_className = null;

                i_className = internClassName(parsedValue);

                didHandle = true;

            } else if ( parsedName.equals(FIELD_TAG) ) {
                if ( i_className == null ) {
                    addParseError("Tag [ " + parsedName + " ] with no proceeding [ " + CLASS_TAG + " ]: Ignoring");
                } else {
                    i_fieldName = internFieldName(parsedValue);
                }

                didHandle = true;

            } else if ( parsedName.equals(METHOD_TAG) ) {
                if ( i_className == null ) {
                    addParseError("Tag [ " + parsedName + " ] with no proceeding [ " + CLASS_TAG + " ]: Ignoring");
                } else {
                    i_methodSignature = internMethodSignature(parsedValue);
                }

                didHandle = true;

            } else if ( parsedName.equals(ANNOTATION_TAG) ) {
                if ( i_annotationClassName != null ) {
                    addParseError("Tag [ " + parsedName + " ] following unclosed [ " + ANNOTATION_TAG + " ]: Ignoring");

                } else if ( (i_packageName == null) && (i_className == null) ) {
                    addParseError("Tag [ " + parsedName + " ] with no proceeding [ " + PACKAGE_TAG + " ] or [ " + CLASS_TAG + " ]: Ignoring");

                } else {
                    i_annotationClassName = internClassName(parsedValue);
                }

                didHandle = true;

            } else if ( parsedName.equals(DETAIL_TAG) ) {
                String annotationDetail = parsedValue;

                if ( i_annotationClassName == null ) {
                    addParseError("Tag [ " + parsedName + " ] with no proceeding [ " + ANNOTATION_TAG + " ]: Ignoring");
                }

                String i_useAnnotationClassName = i_annotationClassName;
                i_annotationClassName = null;

                if ( i_packageName != null ) {
                    detailTable.i_putPackageAnnotation(i_packageName, i_useAnnotationClassName, annotationDetail);

                } else if ( i_methodSignature != null ) {
                    detailTable.i_putMethodAnnotation(i_className, i_methodSignature, i_useAnnotationClassName, annotationDetail);

                } else if ( i_fieldName != null ) {
                    detailTable.i_putFieldAnnotation(i_className, i_fieldName, i_useAnnotationClassName, annotationDetail);

                } else if ( i_className != null ) {
                    detailTable.i_putClassAnnotation(i_className, i_useAnnotationClassName, annotationDetail);

                } else {
                    throw new IllegalStateException("Annotation [ " + i_useAnnotationClassName + " ] set with no target value.");
                }

                didHandle = true;

            } else {
                didHandle = false;
            }

            return didHandle;
        }

        @Override
        public void handleTrailer() {
            if ( i_annotationClassName != null ) {
                addParseError("Tag [ " + ANNOTATION_TAG + " ] not completed by [ " + DETAIL_TAG + " ]");
            }

            super.handleTrailer();
        }
    }

    //

    @Override
    public List<TargetCache_ParseError> read(TargetsTableContainers containerTable) throws IOException {
        ContainerTableReader containerReader = new ContainerTableReader(containerTable);
        return parse(containerReader); // throws IOException
    }

    @Override
    public List<TargetCache_ParseError> read(TargetsTableTimeStamp stampTable) throws IOException {
        StampTableReader stampReader = new StampTableReader(stampTable);
        return parse(stampReader); // throws IOException
    }

    @Override
    public List<TargetCache_ParseError> readResolvedRefs(UtilImpl_InternMap internMap, Set<String> i_resolvedClassNames) throws IOException {
        RefsReader resolvedRefsReader = new RefsReader(TargetCache_InternalConstants.RESOLVED_REFS_TAG,
                                                       TargetCache_InternalConstants.RESOLVED_REFS_VERSION,
                                                       internMap,
                                                       i_resolvedClassNames);

        return parse(resolvedRefsReader); // throws IOException
    }

    @Override
    public List<TargetCache_ParseError> readUnresolvedRefs(UtilImpl_InternMap internMap, Set<String> i_unresolvedClassNames) throws IOException {
        RefsReader unresolvedRefsReader = new RefsReader(TargetCache_InternalConstants.UNRESOLVED_REFS_TAG,
                                                         TargetCache_InternalConstants.UNRESOLVED_REFS_VERSION,
                                                         internMap,
                                                         i_unresolvedClassNames);

        return parse(unresolvedRefsReader); // throws IOException
    }

    @Override
    public List<TargetCache_ParseError> read(TargetsTableClasses classTable) throws IOException {
        ClassTableReader classReader = new ClassTableReader(classTable);
        return parse(classReader); // throws IOException
    }

    @Override
    public List<TargetCache_ParseError> readMulti(TargetsTableClassesMulti classTable) throws IOException {
        ClassTableMultiReader classReader = new ClassTableMultiReader(classTable);
        return parse(classReader); // throws IOException
    }

    @Override
    public List<TargetCache_ParseError> read(TargetsTableAnnotations targetTable) throws IOException {
        TargetTableReader targetReader = new TargetTableReader(targetTable);
        return parse(targetReader); // throws IOException
    }

    @Override
    public List<TargetCache_ParseError> read(TargetsTableDetails detailTable) throws IOException {
        DetailTableReader detailReader = new DetailTableReader(detailTable);
        return parse(detailReader); // throws IOException
    }
}
