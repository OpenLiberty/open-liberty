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
package app1.web;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.kernel.server.ServerActionsMXBean;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import junit.framework.Assert;

@SuppressWarnings("serial")
@WebServlet("/ServerActionsMXBeanServlet")
public class ServerActionsMXBeanServlet extends FATServlet {

    interface DumpCaller {
        String call(ServerActionsMXBean serverActions);

        String call(ServerActionsMXBean serverActions, String targetDirectory);

        String call(ServerActionsMXBean serverActions, String targetDirectory, String nameToken);

        String call(ServerActionsMXBean serverActions, String targetDirectory, String nameToken, int maximum);
    }

    @Test
    public void testThreadDumps() throws Exception {
        dumpTests(new String[] { ".txt" }, new DumpCaller() {
            @Override
            public String call(ServerActionsMXBean serverActions) {
                return serverActions.threadDump();
            }

            @Override
            public String call(ServerActionsMXBean serverActions, String targetDirectory) {
                return serverActions.threadDump(targetDirectory);
            }

            @Override
            public String call(ServerActionsMXBean serverActions, String targetDirectory, String nameToken) {
                return serverActions.threadDump(targetDirectory, nameToken);
            }

            @Override
            public String call(ServerActionsMXBean serverActions, String targetDirectory, String nameToken, int maximum) {
                return serverActions.threadDump(targetDirectory, nameToken, maximum);
            }
        });
    }

    @Test
    @Mode(TestMode.FULL)
    public void testHeapDumps() throws Exception {
        dumpTests(new String[] { ".phd", ".hprof" }, new DumpCaller() {
            @Override
            public String call(ServerActionsMXBean serverActions) {
                return serverActions.heapDump();
            }

            @Override
            public String call(ServerActionsMXBean serverActions, String targetDirectory) {
                return serverActions.heapDump(targetDirectory);
            }

            @Override
            public String call(ServerActionsMXBean serverActions, String targetDirectory, String nameToken) {
                return serverActions.heapDump(targetDirectory, nameToken);
            }

            @Override
            public String call(ServerActionsMXBean serverActions, String targetDirectory, String nameToken, int maximum) {
                return serverActions.heapDump(targetDirectory, nameToken, maximum);
            }
        });
    }

    @Test
    @Mode(TestMode.FULL)
    public void testSystemDumps() throws Exception {
        dumpTests(new String[] { ".dmp" }, new DumpCaller() {
            @Override
            public String call(ServerActionsMXBean serverActions) {
                return serverActions.systemDump();
            }

            @Override
            public String call(ServerActionsMXBean serverActions, String targetDirectory) {
                return serverActions.systemDump(targetDirectory);
            }

            @Override
            public String call(ServerActionsMXBean serverActions, String targetDirectory, String nameToken) {
                return serverActions.systemDump(targetDirectory, nameToken);
            }

            @Override
            public String call(ServerActionsMXBean serverActions, String targetDirectory, String nameToken, int maximum) {
                return serverActions.systemDump(targetDirectory, nameToken, maximum);
            }
        });
    }

    /**
     * @throws MalformedObjectNameException
     */
    private void dumpTests(String[] expectedExtensions, DumpCaller caller) throws MalformedObjectNameException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ServerActionsMXBean serverActions = JMX.newMXBeanProxy(mbs, new ObjectName(ServerActionsMXBean.OBJECT_NAME), ServerActionsMXBean.class);

        String dumpFile = caller.call(serverActions);

        if (dumpFile != null) {

            boolean hasExpectedExtension = false;
            for (String expectedExtension : expectedExtensions) {
                if (dumpFile.endsWith(expectedExtension)) {
                    hasExpectedExtension = true;
                    break;
                }
            }
            Assert.assertTrue("Expected one of the extensions " + expectedExtensions + " for " + dumpFile, hasExpectedExtension);

            File file = new File(dumpFile);

            List<File> files = new ArrayList<File>();
            files.add(file);

            // Now make four dumps with a maximum of 3 and a name token
            final String nameToken = "_testtoken";
            files.add(new File(caller.call(serverActions, null, nameToken, 3)));
            files.add(new File(caller.call(serverActions, null, nameToken, 3)));
            files.add(new File(caller.call(serverActions, null, nameToken, 3)));
            files.add(new File(caller.call(serverActions, null, nameToken, 3)));

            for (int i = 0; i < files.size(); i++) {
                System.out.println("File " + i + ": " + files.get(i).getName());
            }

            // Make sure the nameToken is really there
            Assert.assertTrue("Expected name token missing", files.get(1).getName().contains(nameToken));

            // The original non-name token file should still be there
            Assert.assertTrue("Non-nameToken dump should exist", files.get(0).exists());

            // And out of the name token ones, we should only see the last three
            Assert.assertFalse("First nameToken dump should not exist", files.get(1).exists());
            Assert.assertTrue("Second nameToken dump should exist", files.get(2).exists());
            Assert.assertTrue("Third nameToken dump should exist", files.get(3).exists());
            Assert.assertTrue("Fourth nameToken dump should exist", files.get(4).exists());

            // Remove the one that no longer exists; otherwise, the later delete assertion will fail
            files.remove(1);

            // Let's check we can direct the dump to a particular directory
            String tmpDir = System.getProperty("java.io.tmpdir");
            System.out.println("Temp directory: " + tmpDir);
            File tmpDump = new File(caller.call(serverActions, tmpDir));
            System.out.println("Tmp dir file: " + tmpDump.getAbsolutePath());
            Assert.assertEquals("Dump did not go into expected directory", tmpDump.getParent(), tmpDir);
            files.add(tmpDump);

            // Finally delete all the dump files
            for (File f : files) {
                Assert.assertTrue("Could not delete dump " + f.getAbsolutePath(), f.delete());
            }
        }
    }
}
