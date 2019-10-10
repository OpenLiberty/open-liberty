/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.test.cases;

import java.io.PrintWriter;
import java.util.LinkedList;

import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Fault;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Targets;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Fault;
import com.ibm.wsspi.anno.util.Util_BidirectionalMap;
import com.ibm.wsspi.anno.util.Util_InternMap;

public class AnnotationTest_TestResult {
    public String targetName;

    // testFailed flag should be set by any functional test that fails.
    // If true, it will result in a Junit test failure at exit from display(PrintWriter)
    public boolean testFailed;
    public boolean detailFlag;

    public int targetIterations;
    public int actualIterations;

    public long[] scanTimes;

    public long minScanTime;
    public long maxScanTime;

    public long totalScanTime;
    public long avgScanTime;

    public long infoTime;

    public int resourceExclusionCount;
    public int classExclusionCount;
    public int classInclusionCount;

    public int uniquePackages;
    public int uniquePackageAnnotations;

    public int uniqueClasses;
    public int uniqueClassAnnotations;

    public int uniqueClassesWithFieldAnnotations;
    public int uniqueFieldAnnotations;

    public int uniqueClassesWithMethodAnnotations;
    public int uniqueMethodAnnotations;

    public int classInternCount;
    public int classInternTotalSize;

    public int fieldInternCount;
    public int fieldInternTotalSize;

    public int methodInternCount;
    public int methodInternTotalSize;

    //InfoStore verification results
    public LinkedList<AnnotationTargets_Fault> verificationErrorMessages;

    public AnnotationTest_TestResult(String targetName,
                                     boolean detailFlag,
                                     int iterations) {
        super();

        this.targetName = targetName;

        this.detailFlag = detailFlag;

        this.targetIterations = iterations;
        this.actualIterations = 0;

        this.scanTimes = new long[iterations];

        this.minScanTime = 0L;
        this.totalScanTime = 0L;
        this.avgScanTime = 0L;
        this.maxScanTime = 0L;

        this.infoTime = 0L;

        this.resourceExclusionCount = 0;
        this.classExclusionCount = 0;
        this.classInclusionCount = 0;

        this.uniquePackages = 0;
        this.uniquePackageAnnotations = 0;

        this.uniqueClasses = 0;
        this.uniqueClassAnnotations = 0;

        this.uniqueClassesWithFieldAnnotations = 0;
        this.uniqueFieldAnnotations = 0;

        this.uniqueClassesWithMethodAnnotations = 0;
        this.uniqueMethodAnnotations = 0;

        this.classInternCount = 0;
        this.classInternTotalSize = 0;

        this.fieldInternCount = 0;
        this.fieldInternTotalSize = 0;

        this.methodInternCount = 0;
        this.methodInternTotalSize = 0;

        this.verificationErrorMessages = new LinkedList<AnnotationTargets_Fault>();
    }

    public void addScanTime(long scanTime) {
        this.actualIterations++;

        this.scanTimes[actualIterations - 1] = scanTime;

        if (this.actualIterations == 1) {
            this.minScanTime = scanTime;
            this.maxScanTime = scanTime;

        } else {
            if (scanTime < this.minScanTime) {
                this.minScanTime = scanTime;
            }

            if (scanTime > this.maxScanTime) {
                this.maxScanTime = scanTime;
            }
        }

        this.totalScanTime += scanTime;
        this.avgScanTime = this.totalScanTime / this.actualIterations;
    }

    public void setInfoTime(long infoTime) {
        this.infoTime = infoTime;
    }

    public void addResults(ClassSource_Aggregate classSource,
                           AnnotationTargetsImpl_Targets annotationTargets) {
        if (this.actualIterations == 1) {
            setFileCounts(classSource);
            setAnnotationCounts(annotationTargets);
            setInternCounts(annotationTargets);

            return;

        } else {
            String verifyFileCountsMsg = verifyFileCounts(classSource);
            if (verifyFileCountsMsg != null) {
                this.addVerificationMessage(new AnnotationTargetsImpl_Fault(verifyFileCountsMsg, null));
            }

            String verifyAnnotationCountsMsg = verifyAnnotationCounts(annotationTargets);
            if (verifyAnnotationCountsMsg != null) {
                this.addVerificationMessage(new AnnotationTargetsImpl_Fault(verifyAnnotationCountsMsg, null));
            }

            String verifyInternCountsMsg = verifyInternCounts(annotationTargets);
            if (verifyInternCountsMsg != null) {
                this.addVerificationMessage(new AnnotationTargetsImpl_Fault(verifyInternCountsMsg, null));
            }

            return;
        }
    }

    public void setFileCounts(ClassSource_Aggregate classSource) {
        this.resourceExclusionCount = classSource.getResourceExclusionCount();
        this.classExclusionCount = classSource.getClassExclusionCount();
        this.classInclusionCount = classSource.getClassInclusionCount();
    }

    public void setAnnotationCounts(AnnotationTargetsImpl_Targets annotationTargets) {
        Util_BidirectionalMap packageData = annotationTargets.getPackageAnnotationData();
        this.uniquePackages = packageData.getHolderSet().size();
        this.uniquePackageAnnotations = packageData.getHeldSet().size();

        Util_BidirectionalMap classData = annotationTargets.getClassAnnotationData();
        this.uniqueClasses = classData.getHolderSet().size();
        this.uniqueClassAnnotations = classData.getHeldSet().size();

        if (this.detailFlag) {
            Util_BidirectionalMap fieldData = annotationTargets.getFieldAnnotationData();
            this.uniqueClassesWithFieldAnnotations = fieldData.getHolderSet().size();
            this.uniqueFieldAnnotations = fieldData.getHeldSet().size();

            Util_BidirectionalMap methodData = annotationTargets.getMethodAnnotationData();
            this.uniqueClassesWithMethodAnnotations = methodData.getHolderSet().size();
            this.uniqueMethodAnnotations = methodData.getHeldSet().size();
        }
    }

    public String verifyFileCounts(ClassSource_Aggregate classSource) {
        int newResourceExclusionCount = classSource.getResourceExclusionCount();
        if (this.resourceExclusionCount != newResourceExclusionCount) {
            String verifyMsg = "Resource exclusion count error, prior result [ " +
                               this.resourceExclusionCount + " ] new result [ " +
                               newResourceExclusionCount + " ]";
            return verifyMsg;
        }

        int newClassExclusionCount = classSource.getClassExclusionCount();
        if (this.classExclusionCount != newClassExclusionCount) {
            String verifyMsg = "Class exclusion count error, prior result [ " +
                               this.classExclusionCount + " ] new result [ " +
                               newClassExclusionCount + " ]";
            return verifyMsg;
        }

        int newClassInclusionCount = classSource.getClassInclusionCount();
        if (this.classInclusionCount != newClassInclusionCount) {
            String verifyMsg = "Class inclusion count error, prior result [ " +
                               this.classInclusionCount + " ] new result [ " +
                               newClassInclusionCount + " ]";
            return verifyMsg;
        }

        return null;
    }

    public String verifyAnnotationCounts(AnnotationTargetsImpl_Targets annotationTargets) {
        Util_BidirectionalMap packageData = annotationTargets.getPackageAnnotationData();

        int newUniquePackages = packageData.getHolderSet().size();
        if (this.uniquePackages != newUniquePackages) {
            String verifyMsg = "Unique packages error, prior result [ " + this.uniqueClasses +
                               " ] new result [ " + newUniquePackages + " ]";
            return verifyMsg;
        }

        int newUniquePackageAnnotations = packageData.getHeldSet().size();
        if (this.uniquePackageAnnotations != newUniquePackageAnnotations) {
            String verifyMsg = "Unique package annotations error, prior result [ " +
                               this.uniquePackageAnnotations + " ] new result [ " +
                               newUniquePackageAnnotations + " ]";
            return verifyMsg;
        }

        //

        Util_BidirectionalMap classData = annotationTargets.getClassAnnotationData();

        int newUniqueClasses = classData.getHolderSet().size();
        if (this.uniqueClasses != newUniqueClasses) {
            String verifyMsg = "Unique classes error, prior result [ " + this.uniqueClasses +
                               " ] new result [ " + newUniqueClasses + " ]";
            return verifyMsg;
        }

        int newUniqueClassAnnotations = classData.getHeldSet().size();
        if (this.uniqueClassAnnotations != newUniqueClassAnnotations) {
            String verifyMsg = "Unique class annotations error, prior result [ " +
                               this.uniqueClassAnnotations + " ] new result [ " +
                               newUniqueClassAnnotations + " ]";
            return verifyMsg;
        }

        //

        if (detailFlag) {
            Util_BidirectionalMap fieldData = annotationTargets.getFieldAnnotationData();

            int newUniqueClassesWithFieldAnnotations = fieldData.getHolderSet().size();
            if (this.uniqueClassesWithFieldAnnotations != newUniqueClassesWithFieldAnnotations) {
                String verifyMsg = "Classes with field annotations error, prior result [ " +
                                   this.uniqueClassesWithFieldAnnotations + " ] new result [ " +
                                   newUniqueClassesWithFieldAnnotations + " ]";
                return verifyMsg;
            }

            int newUniqueFieldAnnotations = fieldData.getHeldSet().size();
            if (this.uniqueFieldAnnotations != newUniqueFieldAnnotations) {
                String verifyMsg = "Unique field annotations error, prior result [ " +
                                   this.uniqueFieldAnnotations + " ] new result [ " +
                                   newUniqueFieldAnnotations + " ]";
                return verifyMsg;
            }

            Util_BidirectionalMap methodData = annotationTargets.getMethodAnnotationData();

            int newUniqueClassesWithMethodAnnotations = methodData.getHolderSet().size();
            if (this.uniqueClassesWithMethodAnnotations != newUniqueClassesWithMethodAnnotations) {
                String verifyMsg = "Classes with method annotations error, prior result [ " +
                                   this.uniqueClassesWithMethodAnnotations + " ] new result [ " +
                                   newUniqueClassesWithMethodAnnotations + " ]";
                return verifyMsg;
            }

            int newUniqueMethodAnnotations = methodData.getHeldSet().size();
            if (this.uniqueMethodAnnotations != newUniqueMethodAnnotations) {
                String verifyMsg = "Unique method annotations error, prior result [ " +
                                   this.uniqueMethodAnnotations + " ] new result [ " +
                                   newUniqueMethodAnnotations + " ]";
                return verifyMsg;
            }
        }

        return null;
    }

    public void setInternCounts(AnnotationTargetsImpl_Targets annotationTargets) {
        Util_InternMap classInternMap = annotationTargets.getClassInternMap();

        this.classInternCount = classInternMap.getSize();
        this.classInternTotalSize = classInternMap.getTotalLength();

    }

    public String verifyInternCounts(AnnotationTargetsImpl_Targets annotationTargets) {
        Util_InternMap classInternMap = annotationTargets.getClassInternMap();

        int newClassInternCount = classInternMap.getSize();
        if (this.classInternCount != newClassInternCount) {
            String verifyMsg = "Class intern size error, prior result [ " + this.classInternCount +
                               " ] new result [ " + newClassInternCount + " ]";
            return verifyMsg;
        }

        int newClassInternTotalSize = classInternMap.getTotalLength();
        if (this.classInternTotalSize != newClassInternTotalSize) {
            String verifyMsg = "Class intern total size error, prior result [ " +
                               this.classInternTotalSize + " ] new result [ " +
                               newClassInternTotalSize + " ]";
            return verifyMsg;
        }

        return null;
    }

    /**
     * Add a message from the InfoStore verification test to a list of messages
     * 
     * @param message A String that contains an error message from the InfoStore verification test
     * 
     */
    public void addVerificationMessage(AnnotationTargets_Fault message) {
        if (null != message) {
            this.verificationErrorMessages.add(message);
            this.testFailed = true;
        }
    }

    /**
     * Get the value of the testFailed flag.
     * 
     * @return boolean True if any unit test has failed.
     */
    public boolean getTestFailed() {
        return this.testFailed;
    }

    /**
     * Set the value of the testFailed flag.
     * 
     * @param value boolean value to be used to set the testFailed flag.
     */
    public void setTestFailed(boolean value) {
        this.testFailed = value;
    }

    /**
     * Write out the test information contained in this object.
     * 
     * @param writer A PrintWriter to be used as the output
     */
    public void display(PrintWriter writer) {
        writer.println("============================================================");

        displayConstantResults(writer);

        writer.println("------------------------------------------------------------");

        displayScanTimes(writer);

        writer.println("------------------------------------------------------------");

        displayInternCounts(writer);

        writer.println("============================================================");

        junit.framework.Assert.assertFalse(this.testFailed);
    }

    public void displayConstantResults(PrintWriter writer) {
        writer.println("Target [ " + targetName + " ]");
        writer.println("Detail flag [ " + detailFlag + " ]");

        writer.println("------------------------------------------------------------");

        writer.println("Classes scanned [ " + classInclusionCount + " ]");
        writer.println("Classes masked  [ " + classExclusionCount + " ]");
        writer.println("Non-class files [ " + resourceExclusionCount + " ]");

        writer.println("------------------------------------------------------------");

        writer.println("Packages with annotations       [ " + uniquePackages + " ]");
        writer.println("Unique packages annotations     [ " + uniquePackageAnnotations + " ]");

        writer.println("Classes with class annotations  [ " + uniqueClasses + " ]");
        writer.println("Unique class annotations        [ " + uniqueClassAnnotations + " ]");

        if (detailFlag) {
            writer.println("Classes with field annotations  [ " + uniqueClassesWithFieldAnnotations + " ]");
            writer.println("Unique field annotations        [ " + uniqueFieldAnnotations + " ]");

            writer.println("Classes with method annotations [ " + uniqueClassesWithMethodAnnotations + " ]");
            writer.println("Unique method annotations       [ " + uniqueMethodAnnotations + " ]");
        }

        writer.println("------------------------------------------------------------");
        writer.println("Verification errors     [ " + this.verificationErrorMessages.size() + " ]");
        for (AnnotationTargets_Fault message : this.verificationErrorMessages) {
            writer.println("Problem: " + message.getResolvedText());
        }

    }

    public void displayScanTimes(PrintWriter writer) {
        writer.println("Target iterations [ " + targetIterations + " ]");
        writer.println("Actual iterations [ " + actualIterations + " ]");

        if (actualIterations > 1) {
            for (int iterationCount = 0; iterationCount < actualIterations; iterationCount++) {
                long nextScanTime = scanTimes[iterationCount];
                writer.println(" Scan time  [ " + iterationCount + " ] [ " + nextScanTime + " ]");
            }
        }

        writer.println("Minimum scan time [ " + minScanTime + " ]");
        writer.println("Average scan time [ " + avgScanTime + " ]");
        writer.println("Maximum scan time [ " + maxScanTime + " ]");

        writer.println("        Info time [ " + infoTime + " ]");
    }

    public void displayInternCounts(PrintWriter writer) {
        writer.println("Class intern map:");
        writer.println("  Size [ " + Integer.valueOf(classInternCount) + " ]");
        writer.println("  Total Length [ " + Integer.valueOf(classInternTotalSize) + " ]");

        if (detailFlag) {
            writer.println("Field intern map:");
            writer.println("  Size [ " + Integer.valueOf(fieldInternCount) + " ]");
            writer.println("  Total Length [ " + Integer.valueOf(fieldInternTotalSize) + " ]");

            writer.println("Method intern map:");
            writer.println("  Size [ " + Integer.valueOf(methodInternCount) + " ]");
            writer.println("  Total Length [ " + Integer.valueOf(methodInternTotalSize) + " ]");
        }
    }

    public void displayNextResult(ClassSource_Aggregate classSource,
                                  AnnotationTargetsImpl_Targets annotationTargets,
                                  long startTime, long endTime,
                                  PrintWriter writer) {

        writer.println("============================================================");

        writer.println("------------------------------------------------------------");

        writer.println("Target [ " + targetName + " ]");

        writer.println("Target [ " + targetName + " ]");
        writer.println("Scan time [ " + (endTime - startTime) + " ]");
        writer.println("Detail flag [ " + detailFlag + " ]");

        writer.println("------------------------------------------------------------");

        long resourceExclusionCount = classSource.getResourceExclusionCount();
        long classExclusionCount = classSource.getClassExclusionCount();
        long classInclusionCount = classSource.getClassInclusionCount();

        writer.println("Classes scanned [ " + classInclusionCount + " ]");
        writer.println("Classes masked  [ " + classExclusionCount + " ]");
        writer.println("Non-class resources [ " + resourceExclusionCount + " ]");

        writer.println("------------------------------------------------------------");

        Util_BidirectionalMap packageData = annotationTargets.getPackageAnnotationData();
        int uniquePackages = packageData.getHolderSet().size();
        int uniquePackageAnnotations = packageData.getHeldSet().size();

        Util_BidirectionalMap classData = annotationTargets.getClassAnnotationData();
        int uniqueClasses = classData.getHolderSet().size();
        int uniqueClassAnnotations = classData.getHeldSet().size();

        writer.println("Packages with annotations       [ " + uniquePackages + " ]");
        writer.println("Unique packages annotations     [ " + uniquePackageAnnotations + " ]");

        writer.println("Classes with class annotations  [ " + uniqueClasses + " ]");
        writer.println("Unique class annotations        [ " + uniqueClassAnnotations + " ]");

        if (detailFlag) {
            Util_BidirectionalMap fieldData = annotationTargets.getFieldAnnotationData();
            int uniqueClassesWithFieldAnnotations = fieldData.getHolderSet().size();
            int uniqueFieldAnnotations = fieldData.getHeldSet().size();

            writer.println("Classes with field annotations  [ " + uniqueClassesWithFieldAnnotations + " ]");
            writer.println("Unique field annotations        [ " + uniqueFieldAnnotations + " ]");

            Util_BidirectionalMap methodData = annotationTargets.getMethodAnnotationData();
            int uniqueClassesWithMethodAnnotations = methodData.getHolderSet().size();
            int uniqueMethodAnnotations = methodData.getHeldSet().size();

            writer.println("Classes with method annotations [ " + uniqueClassesWithMethodAnnotations + " ]");
            writer.println("Unique method annotations       [ " + uniqueMethodAnnotations + " ]");
        }

        writer.println("------------------------------------------------------------");

        Util_InternMap classInternMap = annotationTargets.getClassInternMap();

        writer.println("Class intern map:");
        writer.println("  Log threshhold [ " + Integer.valueOf(classInternMap.getLogThreshHold()) + " ]");
        writer.println("  Size [ " + Integer.valueOf(classInternMap.getSize()) + " ]");
        writer.println("  Total Length [ " + Integer.valueOf(classInternMap.getTotalLength()) + " ]");

        writer.println("------------------------------------------------------------");

        writer.println("============================================================");
    }
}