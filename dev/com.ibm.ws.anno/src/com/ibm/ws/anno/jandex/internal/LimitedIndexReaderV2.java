 /*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
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
 * https://github.com/wildfly/jandex/blob/master/src/main/java/org/jboss/jandex/IndexReaderV2.java
 * commit - 36c2b049b7858205c6504308a5e162a4e943ff21
 */

package com.ibm.ws.anno.jandex.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;






final class LimitedIndexReaderV2 extends IndexReaderImpl {
    static final int MIN_VERSION = 6;
    static final int MAX_VERSION = 6;
    static final int MAX_DATA_VERSION = 4;
    private static final byte NULL_TARGET_TAG = 0;
    private static final byte FIELD_TAG = 1;
    private static final byte METHOD_TAG = 2;
    private static final byte METHOD_PARAMATER_TAG = 3;
    private static final byte CLASS_TAG = 4;
    private static final byte EMPTY_TYPE_TAG = 5;
    private static final byte CLASS_EXTENDS_TYPE_TAG = 6;
    private static final byte TYPE_PARAMETER_TAG = 7;
    private static final byte TYPE_PARAMETER_BOUND_TAG = 8;
    private static final byte METHOD_PARAMETER_TYPE_TAG = 9;
    private static final byte THROWS_TYPE_TAG = 10;
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
    private static final int HAS_ENCLOSING_METHOD = 1;

    private final PackedDataInputStream input;
    private byte[][] byteTable;
    private String[] stringTable;
    private DotName[] nameTable;
    private DotName[] typeTable;
    private DotName[][] typeListTable;
    private LimitedAnnotation[] annotationTable;
    private LimitedAnnotationHolder[] methodTable;
    private LimitedAnnotationHolder[] fieldTable;



    LimitedIndexReaderV2(PackedDataInputStream input) {
        this.input = input;
    }

    LimitedIndex read(int version) throws IOException {

            input.readPackedU32(); //annotationSize
            input.readPackedU32(); //implementorSize
            input.readPackedU32(); //subclassesSize 


            readByteTable();
            readStringTable();
            readNameTable();


            typeTable = new DotName[input.readPackedU32() + 1];
            typeListTable = new DotName[input.readPackedU32() + 1][];
            annotationTable = new LimitedAnnotation[input.readPackedU32() + 1];


            readTypeTable();
            readTypeListTable();
            readMethodTable();
            readFieldTable();
            
            
            return readClasses();
    }

    private void readByteTable() throws IOException {
        // Null is the implicit first entry
        int size = input.readPackedU32() + 1;
        byte[][] byteTable = this.byteTable = new byte[size][];
        for (int i = 1; i < size; i++) {
            int len = input.readPackedU32();
            byteTable[i] = new byte[len];
            input.readFully(byteTable[i], 0, len);
        }
    }

    private void readStringTable() throws IOException {
        // Null is the implicit first entry
        int size = input.readPackedU32() + 1;
        String[] stringTable = this.stringTable = new String[size];
        for (int i = 1; i < size; i++) {
            stringTable[i] = input.readUTF();
        }
    }

    private void readNameTable() throws IOException {
        // Null is the implicit first entry
        int entries = input.readPackedU32() + 1;
        int lastDepth = -1;
        DotName curr = null;

        nameTable = new DotName[entries];
        for (int i = 1; i < entries; i++) {
            int depth = input.readPackedU32();
            boolean inner = (depth & 1) == 1;
            depth >>= 1;

            String local = stringTable[input.readPackedU32()];

            if (depth <= lastDepth) {
                while (lastDepth-- >= depth) {
                    assert curr != null;
                    curr = curr.prefix();
                }
            }

            nameTable[i] = curr = new DotName(curr, local, true, inner);
            lastDepth = depth;
        }
    }

    private void readTypeTable() throws IOException {
        // Null is the implicit first entry
        for (int i = 1; i < typeTable.length; i++) {
            typeTable[i] = movePastReadTypeEntry();
        }
    }

    private int findNextNull(Object[] array, int start) {
        while (start < array.length) {
            if (array[start] == null) {
                return start;
            }
            start++;
        }

        return array.length;
    }

    private void readTypeListTable() throws IOException {
        // Null is the implicit first entry
        DotName[][] typeListTable = this.typeListTable;
        // Already emitted entries are omitted as gaps in the table portion
        for (int i = findNextNull(typeListTable, 1); i < typeListTable.length; i = findNextNull(typeListTable, i)) {
            typeListTable[i] = readTypeListEntry();
        }
    }

    private LimitedAnnotation[] readAnnotations( DotName target) throws IOException {
        int size = input.readPackedU32();
        if (size == 0) {
            return LimitedAnnotation.EMPTY_ARRAY;
        }
        LimitedAnnotation[] annotations = new LimitedAnnotation[size];
        for (int i = 0; i < size; i++) {
            //read in the reference number from the annotationTable
            int reference = input.readPackedU32();

            //if the current annotation hasn't been read before then read it in from the input
            //and add it to the annotation table under reference
            if (annotationTable[reference] == null) {
                annotationTable[reference] = readAnnotationEntry( target);
            }

            annotations[i] = annotationTable[reference];
        }
        return annotations;
    }

    private void movePastAnnotationValues() throws IOException {
        int numValues = input.readPackedU32();
        for (int i = 0; i < numValues; i++) {

            //String name = stringTable[input.readPackedU32()];
            input.readPackedU32();

            int tag = input.readByte();

            switch (tag) {
                case AVALUE_BYTE:
                    input.readByte();
                    break;
                case AVALUE_SHORT:
                    input.readPackedU32();
                    break;
                case AVALUE_INT:
                    input.readPackedU32();
                    break;
                case AVALUE_CHAR:
                    input.readPackedU32();
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
                    input.readPackedU32();
                    break;
                case AVALUE_CLASS:
                    input.readPackedU32();
                    break;
                case AVALUE_ENUM:
                    input.readPackedU32();
                    input.readPackedU32();
                    break;
                case AVALUE_ARRAY:
                    movePastAnnotationValues();
                    break;
                case AVALUE_NESTED: {
                    int reference = input.readPackedU32();
                    LimitedAnnotation nestedInstance = annotationTable[reference];
                    if (nestedInstance == null) {
                        nestedInstance = annotationTable[reference] = readAnnotationEntry( null);
                    }

                    break;
                }
                default:
                    throw new IllegalStateException("Invalid annotation value tag:" + tag);
            }


        }
    }

    private LimitedAnnotation readAnnotationEntry( DotName caller) throws IOException {
        DotName name = nameTable[input.readPackedU32()];

        movePastAnnotationTarget();
        movePastAnnotationValues();

        return new LimitedAnnotation(name,caller);
    }

    private DotName[] readTypeListReference() throws IOException {
        int reference = input.readPackedU32();
        DotName[] types = typeListTable[reference];
        if (types != null) {
            return types;
        }

        return typeListTable[reference] = readTypeListEntry();
    }


    private DotName[] readTypeListEntry() throws IOException {
        int size = input.readPackedU32();
        if (size == 0) {
            return DotName.PLACEHOLDER_ARRAY;
        }

        DotName[] types = new DotName[size];
        for (int i = 0; i < size; i++) {
            types[i] = typeTable[input.readPackedU32()];
        }

        return types;
    }


    private DotName movePastReadTypeEntry() throws IOException{

        int kind = (int) input.readUnsignedByte();
        //0 - class
        //1 - Array
        //2 - Primitive
        //3 - void
        //4 - Type_variable
        //5 - Unresolved_TypeVariable
        //6 - WildCard Type
        //7 - Parametrized Type
        //default - 3

        switch (kind) {
            case 0: {
                DotName name = nameTable[input.readPackedU32()];
                readAnnotations(null);
                return name;
            }
            case 1: {
                input.readPackedU32();
                input.readPackedU32();
                readAnnotations( null);
                return DotName.PLACEHOLDER;
            }
            case 2: {
                input.readUnsignedByte();
                readAnnotations( null);
                return DotName.PLACEHOLDER;
            }
            default:
            case 3: {
                readAnnotations( null);
                return DotName.PLACEHOLDER;
            }
            case 4: {

                input.readPackedU32();
                readTypeListReference();
                readAnnotations( null);
                return DotName.PLACEHOLDER;
            }
            case 5: {
                input.readPackedU32();
                readAnnotations( null);
                return DotName.PLACEHOLDER;
            }
            case 6: {
                input.readPackedU32();
                input.readPackedU32();
                readAnnotations( null);
                return DotName.PLACEHOLDER;
            }
            case 7: {
                DotName name = nameTable[input.readPackedU32()];
                input.readPackedU32();
                readTypeListReference();
                readAnnotations( null);
                return name;

            }

        }
    }


    private void movePastAnnotationTarget() throws IOException {
        byte tag = input.readByte();
        switch(tag){
            case NULL_TARGET_TAG:
                return;
            case CLASS_TAG:
            case FIELD_TAG:
            case METHOD_TAG:
                return;
            case METHOD_PARAMATER_TAG: {
                input.readPackedU32();
                return;
            }
            case EMPTY_TYPE_TAG: {
                input.readPackedU32();
                input.readPackedU32();
                return;
            }
            case CLASS_EXTENDS_TYPE_TAG: {
                input.readPackedU32();
                input.readPackedU32();
                return;
            }
            case TYPE_PARAMETER_TAG: {
                input.readPackedU32();
                input.readPackedU32();
                return;
            }
            case TYPE_PARAMETER_BOUND_TAG: {
                input.readPackedU32();
                input.readPackedU32();
                input.readPackedU32();
                return;
            }
            case METHOD_PARAMETER_TYPE_TAG: {
                input.readPackedU32();
                input.readPackedU32();
                return;
            }
            case THROWS_TYPE_TAG: {
                input.readPackedU32();
                input.readPackedU32();
                return;
            }
        }
    }

    private void readMethodTable() throws IOException {
        // Null holds the first slot
        int size = input.readPackedU32() + 1;
        methodTable = new LimitedAnnotationHolder[size];
        for (int i = 1; i < size; i++) {
            methodTable[i] = readMethodEntry();
        }

    }

    private void readFieldTable() throws IOException {
        // Null holds the first slot
        int size = input.readPackedU32() + 1;
        fieldTable = new LimitedAnnotationHolder[size];
        for (int i = 1; i < size; i++) {
            fieldTable[i] = readFieldEntry();
        }
    }

    private LimitedAnnotationHolder readMethodEntry() throws IOException {
        byte[] name = byteTable[input.readPackedU32()];

        input.readPackedU32();
        input.readPackedU32();
        input.readPackedU32();
        input.readPackedU32();
        input.readPackedU32();
        input.readPackedU32();

        LimitedAnnotation[] annotations = readAnnotations( DotName.createSimple(Utils.fromUTF8(name)));
        return new LimitedAnnotationHolder(DotName.createSimple(Utils.fromUTF8(name)), annotations);
    }

    private LimitedAnnotationHolder readFieldEntry() throws IOException {
        byte[] name = byteTable[input.readPackedU32()];

        input.readPackedU32();
        input.readPackedU32();


        LimitedAnnotation[] annotations = readAnnotations( DotName.createSimple(Utils.fromUTF8(name)));

        return new LimitedAnnotationHolder(DotName.createSimple(Utils.fromUTF8(name)), annotations);
    }

    private ClassInfo readClassEntry() throws IOException {
        
        //read in the code for the current class and pull it from the nameTable
        DotName name  = nameTable[input.readPackedU32()];

        //read in the flags for the class
        short flags = (short) input.readPackedU32();


        //read in the codes for the super type, parameter types, and interface types and retieve them from the tables
        DotName superType = typeTable[input.readPackedU32()];
        

        input.readPackedU32();

        
        DotName[] interfaceTypes = typeListTable[input.readPackedU32()];

        input.readPackedU32();
        input.readPackedU32();


        readPastEnclosingMethod();


        int size = input.readPackedU32();
        //create the ClassInfo object for the current entry
        ClassInfo clazz = new ClassInfo(name, superType, flags, interfaceTypes);


        //get the fieldinternal array and set that as the fields in the class
        readClassFields(clazz);

        //get the method internal array and set that as the methods associated with the current class

        readClassMethods(clazz);
        //iterate over all the annotations
        for (int i = 0; i < size; i++) {
            //read in the annotations and create instances

            List<LimitedAnnotation> instances = Arrays.asList(readAnnotations(clazz.name()));

            //if there are annotations instances for this class then add the list to the master maps and the class map
            if (instances.size() > 0) {
                for(LimitedAnnotation temp: instances){
                    if(temp.getTargetName().equals(clazz.name())){
                        clazz.classAnnotations().add(temp.getName());
                    }
                }
            }
        }



        return clazz;
    }

    private void readClassFields( ClassInfo clazz) throws IOException {
        //read in the number of fields
        int len = input.readPackedU32();

        
        for (int i = 0; i < len; i++) {
            //pull the field object from the table in order to assign it to current classinfo
            LimitedAnnotationHolder field = fieldTable[input.readPackedU32()];
            clazz.fields().add(field.getName());
            for(LimitedAnnotation temp: field.getAnnotations()){
                clazz.fieldAnnotations().add(temp);
            }
            //iterate over all the annotations in the current field and update the annotations taget class info



        }
    }

    private void readClassMethods( ClassInfo clazz) throws IOException {
        int len = input.readPackedU32();
        
        for (int i = 0; i < len; i++) {
            
            //retrieve method information from table to assign to current ClassInfo
            LimitedAnnotationHolder method = methodTable[input.readPackedU32()];

            //update the annotations target class to the current ClassInfo

            clazz.methods().add(method.getName());
            for(LimitedAnnotation temp: clazz.methodAnnotations()){
                clazz.methodAnnotations().add(temp);
            }

            //if the method has no parameters and it is a constructor set the flag to true

        }
       
    }

    private void readPastEnclosingMethod() throws IOException{
        //check if there is an enclosing method
        if (input.readUnsignedByte() != HAS_ENCLOSING_METHOD) {
            return;
        }
        else{
            //eName
            input.readPackedU32();
            //eClass
            input.readPackedU32();
            //returnType
            input.readPackedU32();
            //parameters
            input.readPackedU32();
        }
    }

    private LimitedIndex readClasses() throws IOException {

        int classesSize = input.readPackedU32();
        Map<DotName, ClassInfo> classes = new HashMap<DotName, ClassInfo>(classesSize);

        for (int currentEntry = 0; currentEntry < classesSize; currentEntry++) {

            ClassInfo currentClassEntry = readClassEntry();
            classes.put(currentClassEntry.name(), currentClassEntry);

        }

        return new LimitedIndex(classes);
    }

    int toDataVersion(int version) {
        return MAX_DATA_VERSION;
    }
}
 