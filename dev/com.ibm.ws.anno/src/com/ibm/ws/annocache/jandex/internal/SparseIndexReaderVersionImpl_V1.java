/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * All modifications made by IBM from initial source -
 * https://github.com/wildfly/jandex/blob/master/src/main/java/org/jboss/jandex/IndexReaderV1.java
 * commit - 36c2b049b7858205c6504308a5e162a4e943ff21
 */

package com.ibm.ws.annocache.jandex.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class SparseIndexReaderVersionImpl_V1 implements SparseIndexReaderVersion {

    // Handled range of index versions.

    public static final int MIN_VERSION = 2;
    public static final int MAX_VERSION = 3;

    public static boolean accept(int version) {
        return ( (version >= SparseIndexReaderVersionImpl_V1.MIN_VERSION) && 
                 (version <= SparseIndexReaderVersionImpl_V1.MAX_VERSION) );
    }

    private static final byte FIELD_TAG = 1; // Same as V2
    private static final byte METHOD_TAG = 2; // Same as V2
    private static final byte METHOD_PARAMATER_TAG = 3; // Same as V2
    private static final byte CLASS_TAG = 4;

    private static final int AVALUE_BYTE = 1;
    private static final int AVALUE_SHORT = 2;
    private static final int AVALUE_INT = 3;
    private static final int AVALUE_CHAR = 4;
    private static final int AVALUE_FLOAT = 5;
    private static final int AVALUE_DOUBLE = 6;
    private static final int AVALUE_LONG = 7;
    private static final int AVALUE_BOOLEAN = 8;
    private static final int AVALUE_STRING = 9;
    private static final int AVALUE_CLASS = 10;
    private static final int AVALUE_ENUM = 11;
    private static final int AVALUE_ARRAY = 12;
    private static final int AVALUE_NESTED = 13;

    private final PackedDataInputStream input;
    private final int version;

    private SparseDotName[] classTable;
    private String[] stringTable;

    //

    public SparseIndexReaderVersionImpl_V1(PackedDataInputStream input, int version) {
        this.input = input;
        this.version = version;
    }

    //

    @Override
    public int getVersion() {
        return version;
    }

    //

    @Override
    public SparseIndex read() throws IOException {
        readClassTable();
        readStringTable();
        return readClasses();
    }

    private SparseIndex readClasses() throws IOException {
        int numClasses = input.readPackedU32();

        Map<SparseDotName, SparseClassInfo> classes = new HashMap<SparseDotName, SparseClassInfo>();

        for ( int classNo = 0; classNo < numClasses; classNo++ ) {
            SparseDotName className = classTable[input.readPackedU32()];
            SparseDotName superClassName = classTable[input.readPackedU32()];
            short flags = input.readShort();

            input.readBoolean();

            int numOfInterfaces = input.readPackedU32();
            SparseDotName[] interfaces = new SparseDotName[numOfInterfaces];

            for ( int interfaceCounter = 0; interfaceCounter < numOfInterfaces; interfaceCounter++ ) {
                interfaces[interfaceCounter]=classTable[input.readPackedU32()];
            }

            SparseClassInfo currentClassEntry = new SparseClassInfo(className, superClassName, flags ,interfaces);

            readAnnotations(currentClassEntry);
            classes.put(className, currentClassEntry);
        }

        return new SparseIndex(classes);
    }

    private void readAnnotations(SparseClassInfo currentClass) throws IOException {
        int numAnnotations = input.readPackedU32();

        for ( int annotationNo = 0; annotationNo < numAnnotations; annotationNo++ ) {
            SparseDotName annotationName = classTable[input.readPackedU32()];
            int numTargets = input.readPackedU32();

            for ( int targetNo = 0; targetNo < numTargets; targetNo++ ) {
                int typeTag = input.readPackedU32();
                switch ( typeTag ) {
                    case FIELD_TAG:
                        SparseDotName fieldName = SparseDotName.createSimple(stringTable[input.readPackedU32()]);
                        movePastReadType();
                        input.readShort();
                        currentClass.addField(fieldName);
                        currentClass.addFieldAnnotation(annotationName);
                        break;
                    case METHOD_TAG:
                        SparseDotName methodName = SparseDotName.createSimple(movePastReadMethod());
                        currentClass.addMethod(methodName);
                        currentClass.addMethodAnnotation(annotationName);
                        break;
                    case METHOD_PARAMATER_TAG:
                        movePastReadMethod();
                        input.seekPackedU32();
                        break;
                    case CLASS_TAG:
                        currentClass.addClassAnnotation(annotationName);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                movePastReadAnnotationValues();
            }
        }
    }

    private void movePastReadAnnotationValues() throws IOException{
        int numOfValues = input.readPackedU32();

        for( int valuesCounter = 0; valuesCounter < numOfValues; valuesCounter++ ){
            input.seekPackedU32();

            int tag = input.readByte();
            switch( tag ) {
                case AVALUE_BYTE:
                    input.readByte();
                    break;
                case AVALUE_SHORT:
                    input.seekPackedU32();
                    break;
                case AVALUE_INT:
                    input.seekPackedU32();
                    break;
                case AVALUE_CHAR:
                    input.seekPackedU32();
                    break;
                case AVALUE_FLOAT:
                    input.readFloat();
                    break;
                case AVALUE_DOUBLE:
                    input.readDouble();
                    break;
                case AVALUE_LONG:
                    input.readLong();
                    break;
                case AVALUE_BOOLEAN:
                    input.readBoolean();
                    break;
                case AVALUE_STRING:
                    input.seekPackedU32();
                    break;
                case AVALUE_CLASS:
                    movePastReadType();
                    break;
                case AVALUE_ENUM:
                    input.seekPackedU32();
                    input.seekPackedU32();
                    break;
                case AVALUE_ARRAY:
                    movePastReadAnnotationValues();
                    break;
                case AVALUE_NESTED:
                    input.seekPackedU32();
                    movePastReadAnnotationValues();
                    break;
                default:
                    throw new IllegalStateException("Invalid annotation value tag:" + tag);
            }
        }
    }

    /*
     * Moves the stream past the information about the current method and returns
     * the method name.
    */
    private String movePastReadMethod() throws IOException{
        String name = stringTable[input.readPackedU32()];
        int numArgs = input.readPackedU32();

        for(int argsCounter = 0 ; argsCounter < numArgs; argsCounter++){
            input.readByte();
            input.seekPackedU32();
        }
        input.readByte();
        input.seekPackedU32();
        input.readShort();

        return name;
    }

    private void movePastReadType() throws IOException{
        input.readByte();
        input.seekPackedU32();
    }

    private void readStringTable() throws IOException {
        int numOfEntries = input.readPackedU32();
        stringTable = new String[numOfEntries];

        for (int entryCounter = 0; entryCounter < numOfEntries; entryCounter++) {
            stringTable[entryCounter] = input.readUTF();
        }
    }

    @SuppressWarnings("null")
	private void readClassTable() throws IOException {
        int entries = input.readPackedU32();
        int lastDepth = -1;
        SparseDotName curr = null;
        classTable = new SparseDotName[++entries]; //null is the first entry in the class table

        for (int entryCounter = 1; entryCounter < entries; entryCounter++) {
            int depth = input.readPackedU32();
            String local = input.readUTF();

            if (depth <= lastDepth) {
                while (lastDepth-- >= depth)
                    curr = curr.prefix();
            }
            classTable[entryCounter] = curr = new SparseDotName(curr, local, !SparseDotName.SIMPLE, !SparseDotName.INNER_CLASS);
            lastDepth = depth;
        }
    }
}
