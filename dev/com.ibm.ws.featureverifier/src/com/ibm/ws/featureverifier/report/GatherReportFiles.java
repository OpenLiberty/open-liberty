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
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 */
public class GatherReportFiles {
    public final static String fileSep = Pattern.quote(File.separator);

    File clBuildDir;
    File olBuildDir;
    File outDir;

    private void usage(String args[]) {
        System.out.println("GatherReportFiles <commercial liberty repo workspace dir> <open liberty repo workspace dir> <zip output dir>");
        if (args.length > 0) {
            int arg = 0;
            for (String s : args) {
                System.out.println(" Actual Arg " + (arg++) + " : '" + s + "'");
            }
        }
        System.exit(-1);
    }

    private void parseArgs(String args[]) throws IOException {
        if (args.length != 3) {
            usage(args);
        }

        String baseDirString = args[0];
        clBuildDir = new File(baseDirString);
        if (!clBuildDir.exists() || !clBuildDir.isDirectory()) {
            System.out.println("Bad commercial liberty build dir argument '" + clBuildDir + "'");
            usage(args);
        }

        String olBuildDirString = args[1];
        olBuildDir = new File(olBuildDirString);
        if (!olBuildDir.exists() || !olBuildDir.isDirectory()) {
            System.out.println("Bad open liberty build directory argument '" + olBuildDir + "'");
            usage(args);
        }

        String outDirString = args[2];
        outDir = new File(outDirString);
        if (!outDir.exists() || !outDir.isDirectory()) {
            System.out.println("Bad output dir argument '" + outDirString + "'");
            usage(args);
        }

    }

    private void findFiles(File base, File currentDir, Map<Pattern, Set<String>> results) {
        int count = 0;
        int max = 0;
        int percent = 0;
        if (base.equals(currentDir)) {
            max = count = currentDir.listFiles().length;
            System.out.print("Looking for files in " + currentDir.getPath() + " [");
        }

        for (File f : currentDir.listFiles()) {
            if (count > 0) {
                count--;
                int newPercent = (int) ((50.0 / max) * count);
                if (newPercent != percent) {
                    System.out.print(".");
                    System.out.flush();
                    percent = newPercent;
                }
            }

            if (f.isDirectory()) {
                //skip .dirs =)
                if (!f.getName().startsWith(".")) {
                    findFiles(base, f, results);
                }
            } else {
                Path basePath = Paths.get(base.toURI());
                Path filePath = Paths.get(f.toURI());
                Path relativePath = basePath.relativize(filePath);
                String fPathAsString = relativePath.toString();
                for (Entry<Pattern, Set<String>> e : results.entrySet()) {
                    if (e.getKey().matcher(fPathAsString).matches()) {
                        e.getValue().add(fPathAsString);
                    }
                }
            }
        }

        if (base.equals(currentDir)) {
            System.out.println("]");
        }
    }

    /*
     *
     *
     *
     *
     * // Gather BND style features
     * Pattern bnds = Pattern.compile("(?!.*(test|bvt|fat|build).*).*\\.feature");
     * patternsToSeek.put(bnds, new TreeSet<String>());
     *
     *
     *
     */

    private void gatherFiles() throws IOException {
        Map<Pattern, Set<String>> patternsToSeek = new HashMap<Pattern, Set<String>>();

        //select any feature manifests published from feature directories.
        Pattern manifests = Pattern.compile("(?!.*(test|bvt|fat|build).*).*" + fileSep + "features" + fileSep + "(?!.*(test|bvt|fat|build).*).*.mf");
        patternsToSeek.put(manifests, new TreeSet<String>());
        //select any feature.bnds (old style generated manifest)
        Pattern oldbnds = Pattern.compile("(?!.*(test|bvt|fat|build).*).*feature.bnd");
        patternsToSeek.put(oldbnds, new TreeSet<String>());

        Pattern buildImageManifests = Pattern.compile("build.image" + fileSep + "wlp" + fileSep + "lib" + fileSep + "features" + fileSep + "(?!.*(test|bvt|fat|build).*).*.mf");
        patternsToSeek.put(buildImageManifests, new TreeSet<String>());

        //select any .feature files (new style generated manifests)
        Pattern bnds = Pattern.compile("(?!.*(test|bvt|fat|build|internal.laos).*).*.feature");
        patternsToSeek.put(bnds, new TreeSet<String>());

        Pattern bndFiles = Pattern.compile("(?!.*(test|bvt|fat|build).*).*bnd.bnd");
        patternsToSeek.put(bndFiles, new TreeSet<String>());

        //select the generated manifests from the .bnd/.feature files
        Pattern bndSubsystems = Pattern.compile("(?!.*(test|bvt|fat).*).*subsystem.mf");
        patternsToSeek.put(bndSubsystems, new TreeSet<String>());
        //select the l10n content (english only)
        Pattern props = Pattern.compile("(?!.*(test|bvt|fat|build).*).*OSGI-INF.*l10n.*[a-zA-Z][a-zA-Z][a-zA-Z].properties");
        patternsToSeek.put(props, new TreeSet<String>());
        //select the metatype info.
        Pattern metatypes = Pattern.compile("(?!.*(test|bvt|fat|build).*).*" + fileSep + "metatype" + fileSep + "(?!.*(test|bvt|fat|build).*).*.xml");
        patternsToSeek.put(metatypes, new TreeSet<String>());

        findFiles(olBuildDir, olBuildDir, patternsToSeek);
        findFiles(clBuildDir, clBuildDir, patternsToSeek);
        byte buffer[] = new byte[1024];
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(outDir, "baselineGather.zip")));
        for (Map.Entry<Pattern, Set<String>> e : patternsToSeek.entrySet()) {
            for (String s : e.getValue()) {
                //convert local file slashes into zip slashes..
                if (File.separator != "/") {
                    s = s.replaceAll("\\\\", "/");
                }
                ZipEntry ze = new ZipEntry(s);
                zos.putNextEntry(ze);
                int len;
                File f = new File(olBuildDir, s);
                if (!f.exists())
                    f = new File(clBuildDir, s);
                FileInputStream fis = new FileInputStream(f);
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                fis.close();
            }
        }

        // Create a timestamp to indicate when the baseline was generated. We can't use the timestamp on the file
        // because it will be the RTC extract time rather than the generation time
        ZipEntry timestamp = new ZipEntry(ReportConstants.TIMESTAMP_FILE);
        zos.putNextEntry(timestamp);
        zos.write(String.valueOf(System.currentTimeMillis()).getBytes("UTF-8"));

        // Gather any reviewedErrors files
        File[] reviewedFiles = outDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith(ReportConstants.REVIEWED_PREFIX) && name.endsWith(ReportConstants.XML_SUFFIX)) {
                    return true;
                }
                return false;
            }
        });

        for (File f : reviewedFiles) {
            ZipEntry ze = new ZipEntry(f.getName());
            zos.putNextEntry(ze);
            int len;
            FileInputStream fis = new FileInputStream(f);
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            fis.close();
        }
        zos.close();

        System.out.println("Done. Gathered Zip is at " + new File(outDir, "baselineGather.zip").getAbsolutePath());
    }

    public GatherReportFiles(String args[]) throws IOException {
        parseArgs(args);
        gatherFiles();
    }

    public static void main(String args[]) {
        try {
            new GatherReportFiles(args);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
}
