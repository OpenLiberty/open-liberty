/*******************************************************************************
 * Copyright (c) 2012,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.servlet.notification;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactListener;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactNotification;

public abstract class ArtifactNotificationTestImpl implements ArtifactNotificationTest {
    public ArtifactNotificationTestImpl(String testName, PrintWriter writer) {
        this.testName = testName;
        this.writer = writer;
    }

    private final String testName;

    @Override
    public String getTestName() {
        return testName;
    }

    //

    private final PrintWriter writer;

    @Override
    public void println(String text) {
        writer.println(text);
    }

    @Override
    public void printStackTrace(Throwable th) {
        th.printStackTrace(writer);
    }

    //

    public static boolean copyFile(File source, File dest) {
        if (!source.exists() || source.isDirectory() || (dest.exists() && dest.isDirectory()))
            return false;
        BufferedInputStream bis = null;
        OutputStream os = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(source));
            os = new FileOutputStream(dest);
            byte buffer[] = new byte[4096];
            int read = 0;
            while ((read = bis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
        } catch (FileNotFoundException fnf) {
            return false;
        } catch (IOException io) {
            return false;
        } finally {
            try {
                if (bis != null)
                    bis.close();
                if (os != null) {
                    os.flush();
                    os.close();
                }
            } catch (IOException io) {
                //no-op
            }
        }
        return true;
    }

    /**
     * Recursively remove a file/directory.
     */
    public static boolean removeFile(File f) {
        if (f == null) {
            return true;
        }
        if (!f.exists()) {
            return true;
        }
        if (f.isFile()) {
            return f.delete();
        } else {
            //delete directory f..
            boolean ok = true;
            //remove all the children..
            File children[] = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    ok &= removeFile(child);
                }
            }
            //once children are deleted, remove the directory
            if (ok) {
                ok &= f.delete();
            }
            return ok;
        }
    }

    public File createTempDir(String id) {
        File temp = new File("NOTIFYTEST" + id + System.nanoTime());
        if ( !temp.mkdirs() ) {
            println("FAIL: Unable to create directory [ " + temp.getAbsolutePath() + " ]");
            return null;
        } else {
            println("TestDIR: [ " + temp.getAbsolutePath() + " ]");
            return temp;
        }
    }

    public ArtifactNotifier getNotifier(ArtifactContainer rootContainer) {
        ArtifactNotifier baseNotifier = rootContainer.getArtifactNotifier();
        if (baseNotifier == null) {
            println("FAIL: Unable to obtain notifier instance for container ");
            return null;
        }
        return baseNotifier;
    }

    public boolean waitForInvoked(Set<ArtifactListener> invoked, int wantedCount, long interval, long timeout) {
        if (wantedCount == 0) {
            timeout = 10 * 1000; //hard set the negative notification to only 10 seconds.
        }

        long endTime = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(interval);
                if (wantedCount > 0 && invoked.size() == wantedCount)
                    return true;
                if (wantedCount == 0 && invoked.size() != wantedCount)
                    return false;
            } catch (InterruptedException i) {
                if (invoked.size() == wantedCount)
                    return true;
                if (wantedCount == 0 && invoked.size() != wantedCount)
                    return false;
            }
        }
        return invoked.size() == wantedCount;
    }

    public boolean verifyCounts(NotificationListener an, String[] added, String[] removed, String[] modified) {

        if (an.getAdded().size() + an.getRemoved().size() + an.getModified().size() == 0) {
            if (added.length + removed.length + modified.length > 0) {
                println("FAIL: notifier was not notified when expected to have been for A[" + Arrays.asList(added) + "] R[" + Arrays.asList(removed) + "] M["
                            + Arrays.asList(modified) + "]");
                return false;
            }
        }

        //first, lets fail if we ever see more than 1 notification in a given listener.. 
        if (an.getAdded().size() > 1) {
            println("FAIL: listener was passed multiple notifications for add.. ");
            return false;
        }
        if (an.getRemoved().size() > 1) {
            println("FAIL: listener was passed multiple notifications for removed.. ");
            return false;
        }
        if (an.getModified().size() > 1) {
            println("FAIL: listener was passed multiple notifications for modified.. ");
            return false;
        }

        List<String> as = Arrays.asList(added);
        List<String> rs = Arrays.asList(removed);
        List<String> ms = Arrays.asList(modified);

        //now compare the paths.. 
        if (!(an.getAdded().get(0).getPaths().size() == as.size()) || !an.getAdded().get(0).getPaths().containsAll(as)) {
            println("FAIL: added path set was not as expected.. wanted " + as + " got " + an.getAdded().get(0).getPaths());
            return false;
        }
        if (!(an.getRemoved().get(0).getPaths().size() == rs.size()) || !an.getRemoved().get(0).getPaths().containsAll(rs)) {
            println("FAIL: removed path set was not as expected.. wanted " + rs + " got " + an.getRemoved().get(0).getPaths());
            return false;
        }
        if (!(an.getModified().get(0).getPaths().size() == ms.size()) || !an.getModified().get(0).getPaths().containsAll(ms)) {
            println("FAIL: modified path set was not as expected.. wanted " + ms + " got " + an.getModified().get(0).getPaths());
            return false;
        }

        return true;
    }

    protected class NotificationListener implements ArtifactListener {
        volatile Set<ArtifactListener> waitingToBeInvoked;
        volatile Set<ArtifactListener> invoked;
        List<ArtifactNotification> added = new ArrayList<ArtifactNotification>();
        List<ArtifactNotification> removed = new ArrayList<ArtifactNotification>();
        List<ArtifactNotification> modified = new ArrayList<ArtifactNotification>();

        public NotificationListener(Set<ArtifactListener> invocable, Set<ArtifactListener> invoked) {
            this.waitingToBeInvoked = invocable;
            this.invoked = invoked;
            this.waitingToBeInvoked.add(this);
        }

        @Override
        public synchronized void notifyEntryChange(
            ArtifactNotification newAdded,
            ArtifactNotification newRemoved,
            ArtifactNotification newModified) {

            this.waitingToBeInvoked.remove(this);
            this.invoked.add(this);
            this.added.add(newAdded);
            this.removed.add(newRemoved);
            this.modified.add(newModified);
        }

        public synchronized List<ArtifactNotification> getAdded() {
            return this.added;
        }

        public synchronized List<ArtifactNotification> getRemoved() {
            return this.removed;
        }

        public synchronized List<ArtifactNotification> getModified() {
            return this.modified;
        }

        @Override
        public String toString() {
            StringBuffer result = new StringBuffer();
            result.append("N: i?[" + this.invoked.contains(this) + "] Add[ ");
            for (ArtifactNotification a : this.added) {
                result.append("AN[ " + a.getPaths() + " ]");
            }
            result.append(" ] Remove[ ");
            for (ArtifactNotification a : this.removed) {
                result.append("AN[ " + a.getPaths() + " ]");
            }
            result.append(" ] Modify[ ");
            for (ArtifactNotification a : this.modified) {
                result.append("AN[ " + a.getPaths() + " ]");
            }
            result.append(" ]");

            return result.toString();
        }
    }
}
