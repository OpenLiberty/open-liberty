/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.test.jandex;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Index;

import org.junit.Assert;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

import java.util.LinkedList;

import com.ibm.ws.anno.jandex.internal.SparseClassInfo;
import com.ibm.ws.anno.jandex.internal.SparseDotName;
import com.ibm.ws.anno.jandex.internal.SparseIndex;

@RunWith(Parameterized.class)
public class SparseIndexReadTest {

    @Parameters
    public static Collection<Object[]> data() {
        return JandexTestData.data();
    }

    //

    private final String indexPath;
    private final int indexSize;

    private final Collection<? extends ClassInfo> fullClasses;
    private final Map<String, ClassInfo> fullClassesMap;

    private final Collection<? extends SparseClassInfo> sparseClasses;
    private final Map<String, SparseClassInfo> sparseClassesMap;

    //

    public SparseIndexReadTest(Object indexPathObj, Object indexSizeObj) {
        this.indexPath = (String) indexPathObj;
        this.indexSize = ((Integer) indexSizeObj).intValue();

        // System.out.println("Reading [ " + this.indexPath + " ]");

        Index fullIndex = JandexTestUtils.readFullIndex(this.indexPath);
        this.fullClasses = fullIndex.getKnownClasses();
        Assert.assertEquals("Full index classes", this.indexSize, this.fullClasses.size());
        this.fullClassesMap = mapFull(this.fullClasses);

        // System.out.println("Full classes [ " + Integer.valueOf(fullClasses.size()) + " ]");

        SparseIndex sparseIndex = JandexTestUtils.readSparseIndex(this.indexPath);
        this.sparseClasses = sparseIndex.getKnownClasses();
        Assert.assertEquals("Sparse index classes", this.indexSize, this.sparseClasses.size());
        this.sparseClassesMap = mapSparse(this.sparseClasses);

        // System.out.println("Sparse classes [ " + Integer.valueOf(sparseClasses.size()) + " ]");
    }

    private static Map<String, ClassInfo> mapFull(Collection<? extends ClassInfo> fullClasses) {
        Map<String, ClassInfo> fullClassesMap = new HashMap<String, ClassInfo>( fullClasses.size() );

        for ( ClassInfo fullClass : fullClasses ) {
            fullClassesMap.put( fullClass.name().toString(), fullClass );
        }

        return fullClassesMap;
    }

    private static Map<String, SparseClassInfo> mapSparse(Collection<? extends SparseClassInfo> sparseClasses) {
        Map<String, SparseClassInfo> sparseClassesMap = new HashMap<String, SparseClassInfo>( sparseClasses.size() );

        for ( SparseClassInfo sparseClass : sparseClasses ) {
            sparseClassesMap.put( sparseClass.name().toString(), sparseClass );
        }

        return sparseClassesMap;
    }    

    //

    @Test
    public void testClasses(){
        Set<String> fullClassNames = fullClassesMap.keySet();
        Set<String> sparseClassNames = sparseClassesMap.keySet();

        Set<String> extraFull = new HashSet<String>();
        Set<String> extraSparse = new HashSet<String>();

        boolean diffClasses = diff(fullClassNames, sparseClassNames, null, extraFull, extraSparse); 
        if ( diffClasses ) {
        	Assert.assertFalse("Extra full classes [ " + extraFull + " ] Extra sparse classes [ " + extraSparse + " ]", diffClasses);
        }
    }

    @Test
    public void testFlags(){
        for ( Map.Entry<String, SparseClassInfo> sparseEntry : sparseClassesMap.entrySet() ) {
            String sparseClassName = sparseEntry.getKey();
            SparseClassInfo sparseClass = sparseEntry.getValue();
            short sparseFlags = sparseClass.flags();

            ClassInfo fullClass = fullClassesMap.get(sparseClassName);
            if ( fullClass == null ) {
                continue;
            }
            short fullFlags = fullClass.flags();

            Assert.assertEquals(
                "Class [ " + sparseClassName + " ] full flag does not match sparse flag",
                fullFlags, sparseFlags);
        }
    }

    @Test
    public void testClassAnnotations() {
        Map<String, Set<String>> fullClassAnno = new HashMap<String, Set<String>>();

        for ( ClassInfo fullClass: fullClasses ) {
        	String className = fullClass.name().toString();

        	Set<String> fullAnno = new HashSet<String>();
        	fullClassAnno.put(className, fullAnno);

        	for ( DotName anno : fullClass.annotations().keySet() ) {
        		fullAnno.add( anno.toString() );
        	}
        }

    	Set<String> sparseAnno = new HashSet<String>();

    	Set<String> inFull = new HashSet<String>();
    	Set<String> inSparse = new HashSet<String>();

        for ( SparseClassInfo sparseClass: sparseClasses ) {
        	String className = sparseClass.name().toString();

        	Set<String> fullAnno = fullClassAnno.remove(className);
            Assert.assertNotNull("Extra sparse class [ " + className + " ]", fullAnno);

        	for ( SparseDotName anno : sparseClass.classAnnotations() ) {
        		sparseAnno.add( anno.toString() );
        	}

            boolean diffAnno = diff(fullAnno, sparseAnno, null, inFull, inSparse);
            if ( diffAnno ) {
            	Assert.assertFalse("Extra full methods [ " + inFull + " ]; extra sparse methods [ " + inSparse + " ]", diffAnno);
            }

            sparseAnno.clear();
        }

        if ( !fullClassAnno.isEmpty() ) {
            Assert.assertTrue("Extra full classes [ " + fullClassAnno.keySet() + " ]", fullClassAnno.isEmpty());
        }
    }

    @Test
    public void testFieldsAndMethods(){
        Map<String, Set<String>> classField = new HashMap<String, Set<String>>();
        Map<String, Set<String>> classFieldAnno = new HashMap<String, Set<String>>();
        
        Map<String, Set<String>> classMethod = new HashMap<String, Set<String>>();
        Map<String, Set<String>> classMethodAnno = new HashMap<String, Set<String>>();

        for ( ClassInfo fullClass: fullClasses ) {
        	String className = fullClass.name().toString();

        	Set<String> fields = new HashSet<String>();
        	Set<String> fieldAnno = new HashSet<String>();

            for ( FieldInfo field: fullClass.fields() ) {
            	fields.add(field.name());
            	for ( AnnotationInstance anno : field.annotations() ) {
            		fieldAnno.add( anno.name().toString() );
            	}
            }

            classField.put(className,  fields);
            classFieldAnno.put(className,  fieldAnno);
            
        	Set<String> methods = new HashSet<String>();
        	Set<String> methodAnno = new HashSet<String>();

            for ( MethodInfo method: fullClass.methods() ) {
            	methods.add(method.name());
            	for ( AnnotationInstance anno : method.annotations() ) {
            		methodAnno.add( anno.name().toString() );
            	}
            }

            classMethod.put(className,  methods);
            classMethodAnno.put(className,  methodAnno);
        }

    	Set<String> sparseFields = new HashSet<String>();
    	Set<String> sparseFieldAnno = new HashSet<String>();

    	Set<String> sparseMethods = new HashSet<String>();
    	Set<String> sparseMethodAnno = new HashSet<String>();

    	Set<String> inFull = new HashSet<String>();
    	Set<String> inSparse = new HashSet<String>();

        for ( SparseClassInfo sparseClass: sparseClasses ) {
        	String className = sparseClass.name().toString();

        	Set<String> fields = classField.remove(className);
        	Set<String> fieldAnno = classFieldAnno.remove(className);

        	Set<String> methods = classMethod.remove(className);
        	Set<String> methodAnno = classMethodAnno.remove(className);

            Assert.assertNotNull("Extra sparse class [ " + className + " ]", fields);

            for ( SparseDotName method : sparseClass.methods() ) {
            	sparseMethods.add(method.toString());
            }
            for ( SparseDotName anno : sparseClass.methodAnnotations() ) {
            	sparseMethodAnno.add(anno.toString());
            }
            for ( SparseDotName field : sparseClass.fields() ) {
            	sparseFields.add(field.toString());
            }
            for ( SparseDotName anno : sparseClass.fieldAnnotations() ) {
            	sparseFieldAnno.add(anno.toString());
            }

            boolean diffMethods = diff(methods, sparseMethods, null, inFull, inSparse);
            if ( diffMethods ) {
            	Assert.assertFalse("Extra full methods [ " + inFull + " ]; extra sparse methods [ " + inSparse + " ]", diffMethods);
            }
            boolean diffMethodAnno = diff(methodAnno, sparseMethodAnno, null, inFull, inSparse);
            if ( diffMethodAnno ) {
            	Assert.assertFalse("Extra full method annotations [ " + inFull + " ]; extra sparse method annotations [ " + inSparse + " ]", diffMethods);
            }
            boolean diffFields = diff(fields, sparseFields, null, inFull, inSparse);
            if ( diffFields ) {
            	Assert.assertFalse("Extra full fields [ " + inFull + " ]; extra sparse fields [ " + inSparse + " ]", diffMethods);
            }
            boolean diffFieldAnno = diff(fieldAnno, sparseFieldAnno, null, inFull, inSparse);
            if ( diffFieldAnno ) {
            	Assert.assertFalse("Extra full field annotations [ " + inFull + " ]; extra sparse field annotations [ " + inSparse + " ]", diffMethods);
            }
            
            sparseMethods.clear();
            sparseMethodAnno.clear();
            
            sparseFields.clear();
            sparseFieldAnno.clear();
        }

        if ( !classField.isEmpty() ) {
            Assert.assertTrue("Extra full classes [ " + classField.keySet() + " ]", classField.isEmpty());
        }
    }

    public <T> boolean diff(Set<T> set1, Set<T> set2, Set<T> inBoth, Set<T> in1, Set<T> in2) {
    	boolean foundDiff = false;
        for ( T elem1 : set1 ) {
            if ( set2.contains(elem1) ) {
            	if ( inBoth != null ) {
            		inBoth.add(elem1);
            	}
            } else {
            	foundDiff = true;
            	if ( in1 != null ) {
            		in1.add(elem1);
            	}
            }
        }
        for ( T elem2 : set2 ) {
            if ( set1.contains(elem2) ) {
                // Discard; already added to inBoth.
            } else {
            	foundDiff = true;
            	if ( in2 != null ) {
            		in2.add(elem2);
            	}
            }
        }
        return foundDiff;
    }

    @Test
    public void testSuperNamesAlign(){
        HashMap<String, String> classToSuper = new HashMap<String,String>();
        String nameOfSuper;
        for(ClassInfo fullClass: fullClasses){
            classToSuper.put(fullClass.name().toString(), fullClass.superName().toString());
        }
        for(SparseClassInfo sparseClass: sparseClasses){
            nameOfSuper = classToSuper.get(sparseClass.name().toString());
            Assert.assertNotNull(sparseClass.name().toString() + " class is missing from the full sized index",nameOfSuper);
            Assert.assertTrue("Super names don't match up for class " + sparseClass.name().toString(),nameOfSuper.equals(sparseClass.superName().toString()));

        }

    }

    @Test
    public void testNumberOfInterfaces(){
        HashMap<String, Integer> classToInterfaceSize = new HashMap<String,Integer>();
        Integer numOfInterfaces;
        for(ClassInfo fullClass: fullClasses){
            classToInterfaceSize.put(fullClass.name().toString(), fullClass.interfaceNames().size());
        }

        for(SparseClassInfo sparseClass: sparseClasses){
            numOfInterfaces = classToInterfaceSize.get(sparseClass.name().toString());
            Assert.assertNotNull(sparseClass.name().toString() + " class is missing from the full sized index",numOfInterfaces);
            Assert.assertTrue("Interface Sizes for class "+sparseClass.name().toString() +" do not match", sparseClass.interfaceNames().length == numOfInterfaces.intValue());
            
        }
    }

    @Test
    public void testInterfacesHaveSameNames(){
        HashMap<String,List<String>> classToInterfaceNames = new HashMap<String,List<String>>();
        List<String> nameHolder;
        for(ClassInfo fullClass: fullClasses){
            nameHolder = new LinkedList<String>();
            for(DotName interfaceName : fullClass.interfaceNames()){
                nameHolder.add(interfaceName.toString());
            }
            classToInterfaceNames.put(fullClass.name().toString(), nameHolder);
        }

        for(SparseClassInfo sparseClass: sparseClasses){
            nameHolder = classToInterfaceNames.get(sparseClass.name().toString());
            Assert.assertNotNull(sparseClass.name().toString() + " class is missing from the full sized index",nameHolder);
            for(SparseDotName interfaceName: sparseClass.interfaceNames()){
                Assert.assertTrue(nameHolder.remove(interfaceName.toString()));
            }

            Assert.assertTrue("There are more interfaces in full index for class" + sparseClass.name().toString(), nameHolder.size() == 0);
        }
    }

}