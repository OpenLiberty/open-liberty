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
package com.ibm.ws.annocache.test.jandex;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.annocache.jandex.internal.SparseClassInfo;
import com.ibm.ws.annocache.jandex.internal.SparseDotName;
import com.ibm.ws.annocache.jandex.internal.SparseIndex;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import org.junit.Assert;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

import java.util.LinkedList;

@RunWith(Parameterized.class)
public class SparseIndexReadTest {

    @Parameters
    public static Collection<Object[]> data() {
        return JandexTestData.data();
    }

    //

    private static class TestData {
        public final String indexPath;
        public final int indexSize;

        public final Collection<? extends ClassInfo> fullClasses;
        public final Map<String, ClassInfo> fullClassesMap;

        public final Collection<? extends SparseClassInfo> sparseClasses;
        public final Map<String, SparseClassInfo> sparseClassesMap;

        public TestData(String indexPath, int indexSize) {
            this.indexPath = indexPath;
            this.indexSize = indexSize;

            // System.out.println("T: Reading [ " + this.indexPath + " ]");

            Index fullIndex = JandexTestUtils.readFullIndex(this.indexPath);
            this.fullClasses = fullIndex.getKnownClasses();
            Assert.assertEquals("Full index classes", this.indexSize, this.fullClasses.size());
            this.fullClassesMap = mapFull(this.fullClasses);

            // System.out.println("T: Full classes [ " + Integer.valueOf(fullClasses.size()) + " ]");

            SparseIndex sparseIndex = JandexTestUtils.readSparseIndex(this.indexPath);
            this.sparseClasses = sparseIndex.getKnownClasses();
            Assert.assertEquals("Sparse index classes", this.indexSize, this.sparseClasses.size());
            this.sparseClassesMap = mapSparse(this.sparseClasses);

            // System.out.println("T: Sparse classes [ " + Integer.valueOf(sparseClasses.size()) + " ]");
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
    }

    //

    protected static Map<Integer, TestData> allTestData = new HashMap<Integer, TestData>();
    protected static Map<Integer, Exception> allTestDataFailures = new HashMap<Integer, Exception>();

    public SparseIndexReadTest(Object indexNoObj, Object indexPathObj, Object indexSizeObj) throws Exception {
        Integer indexNo = (Integer) indexNoObj;

        Exception useTestFailure = allTestDataFailures.get(indexNo);
        if ( useTestFailure != null ) {
            throw useTestFailure;
        }

        TestData useTestData = allTestData.get(indexNo);
        if ( useTestData == null ) {
            try {
                useTestData = new TestData(
                    (String) indexPathObj,
                    ((Integer) indexSizeObj).intValue());
            } catch ( Exception e ) {
                allTestDataFailures.put(indexNo, e);
                throw e;
            }
            allTestData.put(indexNo,  useTestData);
        }

        this.testData = useTestData;
    }

    protected final TestData testData;

    //

    @Test
    public void testClasses() {
        Set<String> fullClassNames = testData.fullClassesMap.keySet();
        Set<String> sparseClassNames = testData.sparseClassesMap.keySet();

        Set<String> extraFull = new HashSet<String>();
        Set<String> extraSparse = new HashSet<String>();

        boolean diffClasses = diff(fullClassNames, sparseClassNames, null, extraFull, extraSparse); 
        if ( diffClasses ) {
            Assert.assertFalse("Extra full classes [ " + extraFull + " ] Extra sparse classes [ " + extraSparse + " ]", diffClasses);
        }
    }

    @Test
    public void testFlags() {
        for ( Map.Entry<String, SparseClassInfo> sparseEntry : testData.sparseClassesMap.entrySet() ) {
            String sparseClassName = sparseEntry.getKey();
            SparseClassInfo sparseClass = sparseEntry.getValue();
            short sparseFlags = sparseClass.flags();

            ClassInfo fullClass = testData.fullClassesMap.get(sparseClassName);
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

        for ( ClassInfo fullClass: testData.fullClasses ) {
            String className = fullClass.name().toString();

            // System.out.println("T: Full class [ " + className + " ]");

            Set<String> fullAnno = new HashSet<String>();

            for ( Map.Entry<DotName, List<AnnotationInstance>> annoEntry : fullClass.annotations().entrySet() ) {
                DotName annoClassDotName = annoEntry.getKey();
                String annoClassName = null;
                for ( AnnotationInstance annoInstance : annoEntry.getValue() ) {
                    if ( annoInstance.target().kind() == AnnotationTarget.Kind.CLASS ) {
                        if ( annoClassName == null ) {
                            annoClassName = annoClassDotName.toString();
                        }
                        fullAnno.add(annoClassName);
                    }
                }
            }

            // System.out.println("T:  Full Class Annos [ " + fullAnno + " ]");

            fullClassAnno.put(className, fullAnno);
        }

        Set<String> sparseAnno = new HashSet<String>();

        Set<String> inFull = new HashSet<String>();
        Set<String> inSparse = new HashSet<String>();

        for ( SparseClassInfo sparseClass: testData.sparseClasses ) {
            String className = sparseClass.name().toString();

            // System.out.println("T: Sparse class [ " + className + " ]");

            Set<String> fullAnno = fullClassAnno.remove(className);
            Assert.assertNotNull("Extra sparse class [ " + className + " ]", fullAnno);

            for ( SparseDotName anno : sparseClass.classAnnotations() ) {
                sparseAnno.add( anno.toString() );
            }

            // System.out.println("T:  Sparse Class Annos [ " + sparseAnno + " ]");

            boolean diffAnno = diff(fullAnno, sparseAnno, null, inFull, inSparse);
            if ( diffAnno ) {
                Assert.assertFalse("Class [ " + className + " ] Extra full annotations [ " + inFull + " ]; extra sparse annotations [ " + inSparse + " ]", diffAnno);
            }

            sparseAnno.clear();
        }

        if ( !fullClassAnno.isEmpty() ) {
            Assert.assertTrue("Extra full classes [ " + fullClassAnno.keySet() + " ]", fullClassAnno.isEmpty());
        }
    }

    @Test
    public void testFieldAndMethodAnnotations(){
        Map<String, Set<String>> classFieldAnno = new HashMap<String, Set<String>>();
        Map<String, Set<String>> classMethodAnno = new HashMap<String, Set<String>>();

        for ( ClassInfo fullClass: testData.fullClasses ) {
            String className = fullClass.name().toString();

            // System.out.println("T: Full class [ " + className + " ]");

            Set<String> fullFields = new HashSet<String>();
            Set<String> fullFieldAnno = new HashSet<String>();

            Set<String> sparseMethods = new HashSet<String>();
            Set<String> sparseMethodAnno = new HashSet<String>();

            for ( Map.Entry<DotName, List<AnnotationInstance>> annoEntry : fullClass.annotations().entrySet() ) {
                String annoClassName = annoEntry.getKey().toString();

                for ( AnnotationInstance anno : annoEntry.getValue() ) {
                    AnnotationTarget annoTarget = anno.target();
                    AnnotationTarget.Kind annoKind = annoTarget.kind();

                    if ( annoKind == AnnotationTarget.Kind.FIELD ) {
                        fullFields.add( annoTarget.asField().name().toString() );
                        fullFieldAnno.add(annoClassName);
                    } else if ( annoKind == AnnotationTarget.Kind.METHOD ) {
                        sparseMethods.add( annoTarget.asMethod().name().toString() );
                        sparseMethodAnno.add(annoClassName);
                    } else  {
                        // Ignore: Not a type of interest.
                    }
                }
            }

            // System.out.println("T:  Full Fields [ " + fullFields + " ]");
            // System.out.println("T:  Full Field Annos [ " + fullFieldAnno + " ]");

            // System.out.println("T:  Full Methods [ " + sparseMethods + " ]");
            // System.out.println("T:  Full Method Annos [ " + sparseMethodAnno + " ]");

            classFieldAnno.put(className,  fullFieldAnno);
            classMethodAnno.put(className,  sparseMethodAnno);
        }

        Set<String> sparseFieldAnno = new HashSet<String>();
        Set<String> sparseMethodAnno = new HashSet<String>();

        Set<String> inFull = new HashSet<String>();
        Set<String> inSparse = new HashSet<String>();

        for ( SparseClassInfo sparseClass: testData.sparseClasses ) {
            String className = sparseClass.name().toString();

            // System.out.println("T: Sparse class [ " + className + " ]");

            Set<String> fieldAnno = classFieldAnno.remove(className);
            Set<String> methodAnno = classMethodAnno.remove(className);

            Assert.assertNotNull("Extra sparse class [ " + className + " ]", fieldAnno);

            for ( SparseDotName anno : sparseClass.fieldAnnotations() ) {
                sparseFieldAnno.add(anno.toString());
            }
            for ( SparseDotName anno : sparseClass.methodAnnotations() ) {
                sparseMethodAnno.add(anno.toString());
            }

            // System.out.println("T:  Sparse Field Annos [ " + sparseFieldAnno + " ]");
            // System.out.println("T:  Sparse Method Annos [ " + sparseMethodAnno + " ]");

            boolean diffFieldAnno = diff(fieldAnno, sparseFieldAnno, null, inFull, inSparse);
            if ( diffFieldAnno ) {
                Assert.assertFalse("Class [ " + className + " ] Extra full field annotations [ " + inFull + " ]; extra sparse field annotations [ " + inSparse + " ]", diffFieldAnno);
            }
            boolean diffMethodAnno = diff(methodAnno, sparseMethodAnno, null, inFull, inSparse);
            if ( diffMethodAnno ) {
                Assert.assertFalse("Class [ " + className + " ] Extra full method annotations [ " + inFull + " ]; extra sparse method annotations [ " + inSparse + " ]", diffMethodAnno);
            }

            sparseFieldAnno.clear();
            sparseMethodAnno.clear();
        }

        if ( !classFieldAnno.isEmpty() ) {
            Assert.assertTrue("Extra full classes [ " + classFieldAnno.keySet() + " ]", classFieldAnno.isEmpty());
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
    public void testSuperclassNames(){
        HashMap<String, String> classToSuper = new HashMap<String,String>();
        String nameOfSuper;
        for(ClassInfo fullClass: testData.fullClasses){
            classToSuper.put(fullClass.name().toString(), fullClass.superName().toString());
        }
        for(SparseClassInfo sparseClass: testData.sparseClasses){
            nameOfSuper = classToSuper.get(sparseClass.name().toString());
            Assert.assertNotNull(sparseClass.name().toString() + " class is missing from the full sized index",nameOfSuper);
            Assert.assertTrue("Super names don't match up for class " + sparseClass.name().toString(),nameOfSuper.equals(sparseClass.superName().toString()));

        }

    }

    @Test
    public void testInterfaces(){
        HashMap<String,List<String>> classToInterfaceNames = new HashMap<String,List<String>>();
        List<String> nameHolder;
        for(ClassInfo fullClass: testData.fullClasses){
            nameHolder = new LinkedList<String>();
            for(DotName interfaceName : fullClass.interfaceNames()){
                nameHolder.add(interfaceName.toString());
            }
            classToInterfaceNames.put(fullClass.name().toString(), nameHolder);
        }

        for(SparseClassInfo sparseClass: testData.sparseClasses){
            nameHolder = classToInterfaceNames.get(sparseClass.name().toString());
            Assert.assertNotNull(sparseClass.name().toString() + " class is missing from the full sized index",nameHolder);
            for(SparseDotName interfaceName: sparseClass.interfaceNames()){
                Assert.assertTrue(nameHolder.remove(interfaceName.toString()));
            }

            Assert.assertTrue("There are more interfaces in full index for class" + sparseClass.name().toString(), nameHolder.size() == 0);
        }
    }

}