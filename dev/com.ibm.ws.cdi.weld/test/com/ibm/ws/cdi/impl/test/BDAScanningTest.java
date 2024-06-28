package com.ibm.ws.cdi.impl.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;

import org.junit.Test;

public class BDAScanningTest {

    @Test
    public void testBDAScaninngOrderingWeb() throws CDIException {

        AtomicInteger id = new AtomicInteger(1);

        Set<MockBeanDeploymentArchive> allBDAs = new HashSet<MockBeanDeploymentArchive>();

        //create a root archive
        MockBeanDeploymentArchive root = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, 14, 14, "" + id.getAndIncrement());
        allBDAs.add(root);

        //create a bunch of mock libs
        Set<MockBeanDeploymentArchive> libs = new HashSet<MockBeanDeploymentArchive>();
        for (int i = 0; i < 10; i++) {
            libs.add(new MockBeanDeploymentArchive(ArchiveType.SHARED_LIB, 4, 13, "" + id.getAndIncrement()));
        }
        allBDAs.addAll(libs);

        //Shared libs can all see each other.
        for (MockBeanDeploymentArchive bda : libs) {
            libs.stream().filter(lib -> !lib.equals(bda)).forEach(lib -> lib.addDescendantBda(bda));
        }

        //create a bunch of runtime extensions configured to see app beans
        Set<MockBeanDeploymentArchive> runtimeExtensions = new HashSet<MockBeanDeploymentArchive>();
        for (int i = 0; i < 3; i++) {
            runtimeExtensions.add(new MockBeanDeploymentArchive(ArchiveType.RUNTIME_EXTENSION, 1, 3, "" + id.getAndIncrement()));
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

        allBDAs.stream().forEach(bda -> bda.assertScanned());
        allBDAs.stream().forEach(bda -> bda.seenInAcceptableOrder());

    }

    @Test
    public void testBDAScaninngOrderingTree() throws CDIException {

        AtomicInteger id = new AtomicInteger(1);

        Set<MockBeanDeploymentArchive> allBDAs = new HashSet<MockBeanDeploymentArchive>();

        //create a root archive (we're not using the acceptable order in this test.
        MockBeanDeploymentArchive root = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, 1, 100, "" + id.getAndIncrement());
        allBDAs.add(root);

        //Create two children
        MockBeanDeploymentArchive branch1 = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, 1, 100, "" + id.getAndIncrement());
        allBDAs.add(branch1);

        MockBeanDeploymentArchive branch2 = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, 1, 100, "" + id.getAndIncrement());
        allBDAs.add(branch2);

        root.addDescendantBda(branch1);
        root.addDescendantBda(branch2);

        //and create two children of each
        MockBeanDeploymentArchive leaf1 = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, 1, 100, "" + id.getAndIncrement());
        allBDAs.add(leaf1);

        MockBeanDeploymentArchive leaf2 = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, 1, 100, "" + id.getAndIncrement());
        allBDAs.add(leaf2);

        branch1.addDescendantBda(leaf1);
        branch1.addDescendantBda(leaf2);

        MockBeanDeploymentArchive leaf3 = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, 1, 100, "" + id.getAndIncrement());
        allBDAs.add(leaf3);

        MockBeanDeploymentArchive leaf4 = new MockBeanDeploymentArchive(ArchiveType.WEB_MODULE, 1, 100, "" + id.getAndIncrement());
        allBDAs.add(leaf4);

        branch2.addDescendantBda(leaf3);
        branch2.addDescendantBda(leaf4);

        //Now scan and test
        MockWebSphereCDIDeployment deployment = new MockWebSphereCDIDeployment(new ArrayList<WebSphereBeanDeploymentArchive>(allBDAs));
        deployment.scan();

        allBDAs.stream().forEach(bda -> bda.assertScanned());
        root.scannedAfter(branch1);
        root.scannedAfter(branch2);

        branch1.scannedAfter(leaf1);
        branch1.scannedAfter(leaf2);

        branch2.scannedAfter(leaf3);
        branch2.scannedAfter(leaf4);

    }

}
