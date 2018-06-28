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

package com.ibm.ws.anno.jandex.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;




final class LimitedIndexReaderV1 extends IndexReaderImpl {
    
    static final int MIN_VERSION = 2;
    static final int MAX_VERSION = 3;
    private static final byte FIELD_TAG = 1;
    private static final byte METHOD_TAG = 2;
    private static final byte METHOD_PARAMATER_TAG = 3;
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
    
    private PackedDataInputStream input;
    private DotName[] classTable;
    private String[] stringTable;


    /**
     * Constructs a new IndedReader using the passed input. The stream is not
     * read from until the read method is called.
     *
     * @param input a stream which points to a jandex index file
     */
    LimitedIndexReaderV1(PackedDataInputStream input) {
        this.input = input;
    }

    /**
     * Read the index at the associated stream of this reader. This method can be called multiple
     * times if the stream contains multiple index files.
     *
     * @return the Index contained in the stream
     * @throws java.io.IOException if an I/O error occurs
     * @throws IllegalArgumentException if the stream does not point to Jandex index data
     * @throws org.jboss.jandex.UnsupportedVersion if the index data is tagged with a version not known to this reader
     */
    LimitedIndex read(int version) throws IOException {
            readClassTable();
            readStringTable();
            return readClasses( version);

    }


    private LimitedIndex readClasses( int version) throws IOException {
        int numOfClasses = input.readPackedU32();

        HashMap<DotName, ClassInfo> classes = new HashMap<DotName, ClassInfo>();

        for (int classesCounter = 0; classesCounter < numOfClasses; classesCounter++) {
            DotName name = classTable[input.readPackedU32()];
            DotName superName = classTable[input.readPackedU32()];
            short flags = input.readShort();
            
            input.readBoolean();

            int numOfInterfaces = input.readPackedU32();
            DotName[] interfaces = new DotName[numOfInterfaces];

            for (int interfaceCounter = 0; interfaceCounter < numOfInterfaces; interfaceCounter++) {
                interfaces[interfaceCounter]=classTable[input.readPackedU32()];
            }
            
            ClassInfo currentClassEntry = new ClassInfo(name, superName, flags ,interfaces);

            readAnnotations(currentClassEntry);
            classes.put(name, currentClassEntry);
        }

        
        return new LimitedIndex(classes);
    }

    /*
        Will read and add the name of a field or method and moves the stream past everything elses
    */
    private void readAnnotations( ClassInfo currentClass) throws IOException {
        int numAnnotations = input.readPackedU32();

        for (int annotationCounter = 0; annotationCounter < numAnnotations; annotationCounter++) {
            DotName annotationName = classTable[input.readPackedU32()];
            int numOfTargets = input.readPackedU32();

            for (int targetCounter = 0; targetCounter < numOfTargets; targetCounter++) {
                int tag = input.readPackedU32();

                switch (tag) {
                    case FIELD_TAG: {
                        DotName targetName = DotName.createSimple(stringTable[input.readPackedU32()]);
                        
                        movePastReadType();
                        input.readShort();
                        if(targetName.equals(currentClass.name())){
                            currentClass.addFieldAnnotation(new LimitedAnnotation(annotationName, targetName));
                            currentClass.addField(targetName);
                        }
                        break;
                    }
                    case METHOD_TAG: {
                        DotName targetName = DotName.createSimple(movePastReadMethod());

                        if(targetName.equals(currentClass.name())){
                            currentClass.methodAnnotations().add(new LimitedAnnotation(annotationName,targetName));
                            currentClass.methods().add(targetName);
                        }
                        break;
                    }
                    case METHOD_PARAMATER_TAG: {
                        movePastReadMethod();
                        input.seekPackedU32();
                        break;
                    }
                    case CLASS_TAG: {
                        currentClass.classAnnotations().add(annotationName);
                        break;
                    }
                    default:
                        throw new UnsupportedOperationException();
                }
                movePastReadAnnotationValues();
            }
        }
    }



    private void movePastReadAnnotationValues() throws IOException{
        int numOfValues = input.readPackedU32();

        for(int valuesCounter = 0; valuesCounter < numOfValues; valuesCounter++){
            input.seekPackedU32();

            int tag = input.readByte();

            switch(tag)
            {
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
                case AVALUE_NESTED: {
                    input.seekPackedU32();
                    movePastReadAnnotationValues();
                    break;
                }
                default:
                    throw new IllegalStateException("Invalid annotation value tag:" + tag);
            }
        }
    }


    /*
        moves the stream past the information about the current method and returns the name as a string
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


    private void readClassTable() throws IOException {
        int entries = input.readPackedU32();
        int lastDepth = -1;
        DotName curr = null;
        classTable = new DotName[++entries]; //null is the first entry in the class table

        for (int entryCounter = 1; entryCounter < entries; entryCounter++) {
            int depth = input.readPackedU32();
            String local = input.readUTF();

            if (depth <= lastDepth) {
                while (lastDepth-- >= depth)
                    curr = curr.prefix();
            }
            classTable[entryCounter] = curr = new DotName(curr, local, true, false);
            lastDepth = depth;
        }
    }

    int toDataVersion(int version) {
        return version;
    }
}
