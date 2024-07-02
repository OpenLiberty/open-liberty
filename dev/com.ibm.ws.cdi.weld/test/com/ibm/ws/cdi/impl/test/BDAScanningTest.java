package com.ibm.ws.cdi.impl.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;

import org.junit.Test;

import junit.framework.Assert;

public class BDAScanningTest {

    @Test
    public void testBDAScaninngOrderingWeb() throws CDIException {

        AtomicInteger id = new AtomicInteger(1);

        Set<MockBeanDeploymentArchive> allBDAs = new HashSet<MockBeanDeploymentArchive>();

        //There are a total of 14 archives, root should be scanned last.
        int rootAbsoluteOrder = 14;

        //create a root archive
        MockBeanDeploymentArchive root = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, rootAbsoluteOrder, rootAbsoluteOrder, "" + id.getAndIncrement());
        allBDAs.add(root);

        //The shared libs should be scanned after the three runtime extensions, and there are 9 of them.
        //So the 4th through 13th BDA scanned will be shared libs.
        int sharedLibOrderFloor = 4;
        int sharedLibOrderCeiling = 13;

        //create a bunch of mock libs
        Set<MockBeanDeploymentArchive> libs = new HashSet<MockBeanDeploymentArchive>();
        for (int i = 0; i < 10; i++) {
            libs.add(new MockBeanDeploymentArchive(ArchiveType.SHARED_LIB, sharedLibOrderFloor, sharedLibOrderCeiling, "" + id.getAndIncrement()));
        }
        allBDAs.addAll(libs);

        //Shared libs can all see each other.
        for (MockBeanDeploymentArchive bda : libs) {
            libs.stream().filter(lib -> !lib.equals(bda)).forEach(lib -> lib.addDescendantBda(bda));
        }

        //There are three runtime extensions, and they will be scanned first
        int runtimeExtbOrderFloor = 1;
        int runtimeExtOrderCeiling = 3;

        //create a bunch of runtime extensions configured to see app beans
        Set<MockBeanDeploymentArchive> runtimeExtensions = new HashSet<MockBeanDeploymentArchive>();
        for (int i = 0; i < 3; i++) {
            runtimeExtensions.add(new MockBeanDeploymentArchive(ArchiveType.RUNTIME_EXTENSION, runtimeExtbOrderFloor, runtimeExtOrderCeiling, "" + id.getAndIncrement()));
        }

        //they can all see each other.
        for (MockBeanDeploymentArchive bda : runtimeExtensions) {
            runtimeExtensions.stream().filter(ext -> !ext.equals(bda)).forEach(ext -> ext.addDescendantBda(bda));
        }

        //they can see all app beans.
        for (MockBeanDeploymentArchive bda : allBDAs) {
            runtimeExtensions.stream().forEach(ext -> ext.addDescendantBda(bda));
        }

        //and app beans can see them
        for (MockBeanDeploymentArchive bda : runtimeExtensions) {
            allBDAs.stream().forEach(ext -> ext.addDescendantBda(bda));
        }
        allBDAs.addAll(runtimeExtensions);

        MockWebSphereCDIDeployment deployment = new MockWebSphereCDIDeployment(new ArrayList<WebSphereBeanDeploymentArchive>(allBDAs));
        deployment.scan();

        allBDAs.stream().forEach(bda -> Assert.assertTrue("archive: " + bda.getId() + " was never scanned", bda.isScanned()));
        allBDAs.stream().forEach(bda -> Assert.assertTrue("archive: " + bda.getId() + " should have been scanned between " + bda.getOrderAcceptibleFloor() + " and "
                                                          + bda.getOrderAcceptibleCeiling() +
                                                          " but was scanned in the following position: " + bda.getOrderScanned(),
                                                          bda.getOrderScanned() >= bda.getOrderAcceptibleFloor() && bda.getOrderScanned() <= bda.getOrderAcceptibleCeiling()));

    }

    @Test
    public void testBDAScaninngOrderingTree() throws CDIException {

        AtomicInteger id = new AtomicInteger(1);

        Set<MockBeanDeploymentArchive> allBDAs = new HashSet<MockBeanDeploymentArchive>();

        // Use 1 and 100 because we're checking relative not absolute ordering in this test.
        int dontCheckAbsoluteOrderFloor = 1;
        int dontCheckAbsoluteOrderCeiling = 100;

        //create a root archive
        MockBeanDeploymentArchive root = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, dontCheckAbsoluteOrderFloor, dontCheckAbsoluteOrderCeiling, ""
                                                                                                                                                           + id.getAndIncrement());
        allBDAs.add(root);

        //Create two children
        MockBeanDeploymentArchive branch1 = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, dontCheckAbsoluteOrderFloor, dontCheckAbsoluteOrderCeiling, ""
                                                                                                                                                              + id.getAndIncrement());
        allBDAs.add(branch1);

        MockBeanDeploymentArchive branch2 = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, dontCheckAbsoluteOrderFloor, dontCheckAbsoluteOrderCeiling, ""
                                                                                                                                                              + id.getAndIncrement());
        allBDAs.add(branch2);

        root.addDescendantBda(branch1);
        root.addDescendantBda(branch2);

        //and create two children of each
        MockBeanDeploymentArchive leaf1 = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, dontCheckAbsoluteOrderFloor, dontCheckAbsoluteOrderCeiling, ""
                                                                                                                                                            + id.getAndIncrement());
        allBDAs.add(leaf1);

        MockBeanDeploymentArchive leaf2 = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, dontCheckAbsoluteOrderFloor, dontCheckAbsoluteOrderCeiling, ""
                                                                                                                                                            + id.getAndIncrement());
        allBDAs.add(leaf2);

        branch1.addDescendantBda(leaf1);
        branch1.addDescendantBda(leaf2);

        MockBeanDeploymentArchive leaf3 = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, dontCheckAbsoluteOrderFloor, dontCheckAbsoluteOrderCeiling, ""
                                                                                                                                                            + id.getAndIncrement());
        allBDAs.add(leaf3);

        MockBeanDeploymentArchive leaf4 = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, dontCheckAbsoluteOrderFloor, dontCheckAbsoluteOrderCeiling, ""
                                                                                                                                                            + id.getAndIncrement());
        allBDAs.add(leaf4);

        branch2.addDescendantBda(leaf3);
        branch2.addDescendantBda(leaf4);

        //Now scan and test
        MockWebSphereCDIDeployment deployment = new MockWebSphereCDIDeployment(new ArrayList<WebSphereBeanDeploymentArchive>(allBDAs));
        deployment.scan();

        allBDAs.stream().forEach(bda -> Assert.assertTrue("archive: " + bda.getId() + " was never scanned", bda.isScanned()));
        root.scannedAfter(branch1);
        root.scannedAfter(branch2);

        branch1.scannedAfter(leaf1);
        branch1.scannedAfter(leaf2);

        branch2.scannedAfter(leaf3);
        branch2.scannedAfter(leaf4);

    }

}
