/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.featureverifier.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 */
public class FileUtils {

    enum Source {
        OPEN_LIBERTY_BUILD, COMMERCIAL_LIBERTY_BUILD
    }

    private File baselineZip;
    private File olBuildDir;
    private File clBuildDir;
    private File outDir;

    FileUtils(String[] args) throws IOException {
        parseArgs(args);
    }

    private void parseArgs(String args[]) throws IOException {
        if (args.length != 4) {
            usage(args);
        }

        String baseDirString = args[0];
        baselineZip = new File(baseDirString);
        if (!baselineZip.exists()) {
            System.out.println("Bad baseline zip file (or directory) '" + baselineZip + "'");
            usage(args);
        }

        String olDirString = args[1];
        olBuildDir = new File(olDirString);
        if (!olBuildDir.exists()) {
            System.out.println("Bad open liberty build directory argument '" + olBuildDir + "'");
            usage(args);
        }

        String newBuildDirString = args[2];
        clBuildDir = new File(newBuildDirString);
        if (!clBuildDir.exists() || !clBuildDir.isDirectory()) {
            System.out.println("Bad new build dir argument '" + newBuildDirString + "'");
            usage(args);
        }

        String outDirString = args[3];
        outDir = new File(outDirString);
        if (!outDir.exists() || !outDir.isDirectory()) {
            System.out.println("Bad output dir argument '" + outDirString + "'");
            usage(args);
        }

        if (!baselineZip.isDirectory()) {
            //we now support loading the base from a zip file.. by extracting it ;p
            if (baselineZip.getName().toLowerCase().endsWith(".zip")) {
                System.out.println("Extracting commercial liberty build archive" + baselineZip);
                File baseExtracted = new File(outDir, "Extracted");
                unZip(baselineZip, baseExtracted);
                baselineZip = baseExtracted;
                System.out.println("Using extracted directory " + baselineZip);
            } else {
                System.out.println("Bad base dir argument '" + baselineZip + "'");
                usage(args);
            }
        } else {
            System.out.println("Using baseline directory " + baselineZip);
        }
    }

    private void usage(String args[]) {
        System.out.println("Report <baseline repo workspace dir> <ol build dir> <cl build dir> <html output dir>");
        if (args.length > 0) {
            int arg = 0;
            for (String s : args) {
                System.out.println(" Actual Arg " + (arg++) + " : '" + s + "'");
            }
        }
        System.exit(-1);
    }

    private void unZip(File zip, File dest) throws IOException {
        System.out.println(" Probing binary starsystem of  " + zip.getAbsolutePath() + " from location " + dest.getAbsolutePath());
        dest.mkdirs();
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zip));
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
            String name = ze.getName();
            File out = new File(dest, name);
            File parent = new File(out.getParent());
            parent.mkdirs();
            if (ze.isDirectory()) {
                out.mkdirs();
            } else {
                System.out.println(" Unzipping file to  " + out.getAbsolutePath());

                FileOutputStream fos = new FileOutputStream(out);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            ze = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    public void copyOldFile(String path) {
        File compare = new File(outDir, "compare");
        File oldDir = new File(compare, "old");
        File src = new File(baselineZip, path);
        File dest = new File(oldDir, path);
        dest.getParentFile().mkdirs();
        copyFile(src, dest);
    }

    public void copyNewFile(String path) {

        File compare = new File(outDir, "compare");
        File newDir = new File(compare, "new");
        File src = new File(clBuildDir, path);
        if (!src.exists())
            src = new File(olBuildDir, path);
        File dest = new File(newDir, path);
        dest.getParentFile().mkdirs();
        copyFile(src, dest);
    }

    private void copyFile(File src, File dest) {
        try (FileInputStream is = new FileInputStream(src); FileOutputStream os = new FileOutputStream(dest)) {
            if (!dest.exists()) {
                dest.createNewFile();
            }
            FileChannel source = null;
            FileChannel destination = null;
            try {
                source = is.getChannel();
                destination = os.getChannel();
                destination.transferFrom(source, 0, source.size());
            } finally {
                if (source != null) {
                    source.close();
                }
                if (destination != null) {
                    destination.close();
                }
            }
        } catch (IOException io) {
            System.err.println(src.getAbsolutePath() + "-->" + dest.getAbsolutePath());
            io.printStackTrace();
        }
    }

    /**
     * @return
     */
    public File getBaseDir() {
        return this.baselineZip;
    }

    /**
     * @return
     */
    public File getCommercialLibertyBuildDir() {
        return this.clBuildDir;
    }

    /**
     * @return
     */
    public File getOpenLibertyBuildDir() {
        return this.olBuildDir;
    }

    /**
     * @return
     */
    public File getOutDir() {
        return this.outDir;
    }

    PrintWriter getLog(String logFileName, Map<String, File> logFilesUsed) {
        PrintWriter output = null;
        File logOutput;

        boolean first = false;
        if (logFilesUsed.containsKey(logFileName)) {
            logOutput = logFilesUsed.get(logFileName);
        } else {
            logOutput = new File(outDir, logFileName);
            logFilesUsed.put(logFileName, logOutput);
            first = true;
        }

        try {
            FileWriter fw = new FileWriter(logOutput, true);
            output = new PrintWriter(fw);
            if (first) {
                output.println("<!doctype public \"-//W3C//DTD XHTML 1.0 Transitional//EN\"  system \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
                               + "\n<html><head>"
                               + "\n  <link rel=\"stylesheet\" type=\"text/css\" href=\"review.css\" />"
                               + "\n  <link rel=\"stylesheet\" href=\"//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css\">"
                               + "\n  <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/2.1.3/jquery.min.js\"></script>"
                               + "\n  <script src=\"https://code.jquery.com/ui/1.11.4/jquery-ui.min.js\"></script>"
                               + "\n  <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css\">"
                               + "\n  <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap-theme.min.css\">"
                               + "\n  <script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/js/bootstrap.min.js\"></script>"
                               + " \n  <script type=\"text/javascript\" src=\"https://cdn.rawgit.com/wickedest/Mergely/mergely-2.5/lib/codemirror.min.js\"></script>"
                               + " \n  <link type=\"text/css\" rel=\"stylesheet\" href=\"https://cdn.rawgit.com/wickedest/Mergely/mergely-2.5/lib/codemirror.css\"/>"
                               + "\n  <script type=\"text/javascript\" src=\"https://cdn.rawgit.com/wickedest/Mergely/mergely-2.5/lib/mergely.min.js\"></script>"
                               + " \n  <link type=\"text/css\" rel=\"stylesheet\" href=\"https://cdn.rawgit.com/wickedest/Mergely/mergely-2.5/lib/mergely.css\" />"
                               + "\n  <script src=\"review.js\"></script>"
                               + "\n</head><body>");
                output.flush();
            }
            return output;
        } catch (IOException e) {
            System.err.println("Unable to write to " + logOutput.getAbsolutePath());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
