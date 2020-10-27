/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.depchain;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class FeatureList {

    private static final Class<?> c = FeatureList.class;

    private static final String FAT_FEATURE_LIST = "fatFeatureList.xml";

    private static File featureList = null;
    private static final AtomicBoolean generated = new AtomicBoolean();

    @SuppressWarnings("resource")
    public static File get(LibertyServer server) throws Exception {
        final String m = "createFeatureList";

        featureList = new File(server.getUserDir() + "/servers", FAT_FEATURE_LIST);

        if (featureList.exists() && generated.get())
            return featureList;

        synchronized (FeatureList.class) {
            if (featureList.exists())
                return featureList;

            // If fatFeatureList.xml doesn't exist already, generate it
            Log.info(c, m, FAT_FEATURE_LIST + " not found.  Need to generate.");
            String featureListJar = findRunnableJar(server.getInstallRoot());
            Process featureListProc = new ProcessBuilder("java", "-jar", featureListJar, featureList.getAbsolutePath())
                            .redirectErrorStream(true)
                            .start();
            boolean completed = featureListProc.waitFor(45, TimeUnit.SECONDS);
            if (!completed) {
                String cmdOutput;
                try (Scanner s = new Scanner(featureListProc.getInputStream()).useDelimiter("\\A")) {
                    cmdOutput = s.hasNext() ? s.next() : "";
                }
                Exception e = new Exception(cmdOutput);
                Log.error(c, m, e, "Process timed out running " + featureList.getAbsolutePath() + " generation:");
                throw e;
            }

            generated.set(true);
        }

        return featureList;
    }

    private static String findRunnableJar(String wlpInstallDir) {
        String featureListJar = wlpInstallDir + "/bin/tools/ws-featureList.jar";
        if (new File(featureListJar).exists())
            return featureListJar;

        // Depending on OS, the featureList jar may have a lowercase or uppercase L
        featureListJar = wlpInstallDir + "/bin/tools/ws-featurelist.jar";
        if (new File(featureListJar).exists())
            return featureListJar;

        throw new IllegalStateException("Unable to generate feature dependencies because ws-featureList.jar could not be found at: " + featureListJar);
    }

    public static void reset() throws IOException {
        Log.info(c, "reset", "Removing existing " + FAT_FEATURE_LIST);
        generated.set(false);
        if (featureList != null)
            if (!featureList.delete())
                throw new IOException("Unable to delete old " + FAT_FEATURE_LIST + " at: " + featureList.getAbsolutePath());
        featureList = null;
    }
}
