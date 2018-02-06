/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013, 2017
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.artifact.fat_bvt.servlet;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;

/**
 *
 */
public class ThreadedArtifactTester extends Thread {
    ArtifactContainer test;
    CountDownLatch cdl;
    StringWriter sw = new StringWriter();
    PrintWriter writer = new PrintWriter(sw);

    public ThreadedArtifactTester(ArtifactContainer test, String threadName, CountDownLatch cdl) {
        super(threadName);
        this.test = test;
        this.cdl = cdl;
    }

    @Override
    public void run() {
        try {
            try {
                ArtifactEntry ent1 = test.getEntry("a/a.txt");
                ArtifactEntry ent = test.getEntry("dirEntry/junk.bmp");
                if (ent != null && ent1 == null) {
                    writer.println(Thread.currentThread() + " got entry ok.");
                } else {
                    writer.println("FAIL: got null while running getEntry.");
                    writer.println(Thread.currentThread() + " died.");
                }
            } catch (Exception e) {
                writer.println("FAIL: hit error while running getEntry.");
                writer.println(Thread.currentThread() + " died.");
                e.printStackTrace(writer);
            } finally {
                cdl.countDown();
            }
        } catch (Throwable t) {
            writer.println("FAIL:");
            t.printStackTrace(writer);
        }
    }

    public String getResults() {
        return sw.toString();
    }
}
