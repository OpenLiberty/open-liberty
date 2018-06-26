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
 */

package com.ibm.ws.anno.jandex.internal;

import java.lang.annotation.Annotation;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 * Represents a class entry in an index. A ClassInfo is only a partial view of a
 * Java class, it is not intended as a complete replacement for Java reflection.
 * Only the methods and fields which are references by an annotation are stored.
 *
 * <p>Global information including the parent class, implemented methodParameters, and
 * access flags are also provided since this information is often necessary.
 *
 * <p>Note that a parent class and interface may exist outside of the scope of the
 * index (e.g. classes in a different jar) so the references are stored as names
 * instead of direct references. It is expected that multiple indexes may need
 * to be queried to assemble a full hierarchy in a complex multi-jar environment
 * (e.g. an application server).
 *
 * <p><b>Thread-Safety</b></p>
 * This class is immutable and can be shared between threads without safe publication.
 *
 * @author Jason T. Greene
 *
 */
public final class ClassInfo{

    private final DotName name;
    private  short flags;
    private DotName[] interfaces;
    private DotName superClass;
    private final List<DotName> fields;
    private final List<DotName> methods;
    private final List<LimitedAnnotation> fieldAnnotations;
    private final List<LimitedAnnotation> methodAnnotations;
    private List<DotName> classAnnotations;

    

    ClassInfo(DotName name, DotName superClass, short flags, DotName[] interfaces){
        this.name = name;
        this.superClass = superClass;
        this.flags = flags;
        this.interfaces = interfaces;
        this.classAnnotations = new LinkedList<DotName>();
        this.fields = new LinkedList<DotName>();
        this.methods = new LinkedList<DotName>();
        this.fieldAnnotations = new LinkedList<LimitedAnnotation>();
        this.methodAnnotations = new LinkedList<LimitedAnnotation>();
    }
  
    public String toString() {
        return name.toString();
    }

    /**
     * Returns the name of the class
     *
     * @return the name of the class
     */
    public final DotName name() {
        return name;
    }

    /**
     * Returns the access flags for this class. The standard {@link java.lang.reflect.Modifier}
     * can be used to decode the value.
     *
     * @return the access flags
     */
    public final short flags() {
        return flags;
    }

    /**
     * Returns the name of the super class declared by the extends clause of this class. This
     * information is also available from the {@link #superClassType} method. For all classes,
     * with the one exception of <code>java.lang.Object</code>, which is the one class in the
     * Java language without a super-type, this method will always return a non-null value.
     *
     * @return the name of the super class of this class, or null if this class is <code>java.lang.Object</code>
     */
    public final DotName superName() {
        return superClass == null ? null : superClass;
    }



    /**
     * Returns a list of all annotations directly declared on this class.
     *
     * @return the list of annotations declared on this class
     */
    public final List<DotName> classAnnotations() {
        return classAnnotations;
    }


    public final List<DotName> methods(){
        return methods;
    }

    public final List<LimitedAnnotation> methodAnnotations(){
        return methodAnnotations;
    }
    public final List<DotName> fields(){
        return fields;
    }


    public final List<LimitedAnnotation> fieldAnnotations(){
        return fieldAnnotations;
    }

    public final DotName[] interfaceNames(){
        return interfaces;
    }

}
