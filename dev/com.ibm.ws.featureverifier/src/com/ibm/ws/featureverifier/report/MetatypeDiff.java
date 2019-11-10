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
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import org.xmlunit.input.WhitespaceNormalizedSource;

import difflib.DiffUtils;
import difflib.Patch;

class MetatypeDiff {

    final Set<String> deletedMetatypes = new TreeSet<String>();
    final Set<String> addedMetatypes = new TreeSet<String>();

    final Set<String> deletedProps = new TreeSet<String>();
    final Set<String> addedProps = new TreeSet<String>();

    enum DiffType {
        UNIFIED_TEXT, PATCH_OBJECT, XMLDIFF_DIFF, WHY_TEXT
    };

    final Map<String, Map<MetatypeDiff.DiffType, Object>> alteredMetatypes = new TreeMap<String, Map<MetatypeDiff.DiffType, Object>>();
    final Map<String, Map<MetatypeDiff.DiffType, Object>> alteredProps = new TreeMap<String, Map<MetatypeDiff.DiffType, Object>>();
    private final FileUtils fileUtils;

    public MetatypeDiff(FileUtils fileUtils, File oldDir, Set<String> oldMetaPath, File newDir, Set<String> newMetaPath) {
        this.fileUtils = fileUtils;

        if (oldMetaPath == null)
            oldMetaPath = Collections.emptySet();

        if (newMetaPath == null)
            newMetaPath = Collections.emptySet();

        Set<String> overlap = new HashSet<String>(oldMetaPath);
        overlap.retainAll(newMetaPath);

        Set<String> removed = new TreeSet<String>(oldMetaPath);
        removed.removeAll(overlap);
        deletedMetatypes.addAll(removed);

        Set<String> added = new TreeSet<String>(newMetaPath);
        added.removeAll(overlap);
        addedMetatypes.addAll(added);

        for (String metatype : addedMetatypes) {
            File f = new File(newDir, metatype);
            File newProps = getPropsForMetatype(f);
            if (newProps != null) {
                Path filePath = Paths.get(newProps.toURI());
                Path basePath = Paths.get(newDir.toURI());

                Path relativePath = basePath.relativize(filePath);
                String fPathAsString = relativePath.toString();
                addedProps.add(fPathAsString);

            }
        }
        for (String metatype : overlap) {
            compareMetatype(oldDir, newDir, metatype);
        }
    }

    private File getPropsForMetatype(File metatype) {
        String metatypeName = metatype.getName();
        String baseName = metatypeName.substring(0, metatypeName.length() - ".xml".length());
        File metatypeFolder = metatype.getParentFile();
        File parentOfMetatypeFolder = metatypeFolder.getParentFile();
        File i10nFolder = new File(parentOfMetatypeFolder, "l10n");
        File i10nProps = new File(i10nFolder, baseName + ".properties");
        return i10nProps;
    }

    private void compareMetatype(File oldDir, File newDir, String metatypePath) {
        File oldM = new File(oldDir, metatypePath);
        File newM = new File(newDir, metatypePath);
        if (oldM.exists() && oldM.isFile() && newM.exists() && newM.isFile()) {
            //using xml unit compare.
            Diff f = DiffBuilder.compare(new WhitespaceNormalizedSource(Input.fromFile(oldM).build())).withTest(new WhitespaceNormalizedSource(Input.fromFile(newM).build())).build();
            if (f.hasDifferences()) {
                String why = "";

                alteredMetatypes.put(metatypePath, new HashMap<MetatypeDiff.DiffType, Object>());
                alteredMetatypes.get(metatypePath).put(DiffType.XMLDIFF_DIFF, f);

                //using google diffutils compare.
                List<String> oldLines = Report.fileToLines(oldM);
                List<String> newLines = Report.fileToLines(newM);
                Patch patch = DiffUtils.diff(oldLines, newLines);

                alteredMetatypes.get(metatypePath).put(DiffType.PATCH_OBJECT, patch);

                why += "   why: (unified diff)\n";
                List<String> unified = DiffUtils.generateUnifiedDiff(metatypePath, metatypePath, oldLines, patch, 5);
                String unifiedString = "";
                for (String s : unified) {
                    why += "      " + s + "\n";
                    unifiedString += s + "\n";
                }

                why = why.substring(0, why.length() - 1);
                alteredMetatypes.get(metatypePath).put(DiffType.UNIFIED_TEXT, unifiedString);
                alteredMetatypes.get(metatypePath).put(DiffType.WHY_TEXT, why);
            }

            //if the metatype existed.. lets look for nls properties for it.
            File oldProps = getPropsForMetatype(oldM);
            File newProps = getPropsForMetatype(newM);

            if (oldProps.exists() && oldProps.isFile() && !newProps.exists()) {
                Path basePath = Paths.get(newDir.toURI());
                Path filePath = Paths.get(newProps.toURI());
                Path relativePath = basePath.relativize(filePath);
                String fPathAsString = relativePath.toString();
                deletedProps.add(fPathAsString);
            }
            if (!oldProps.exists() && newProps.exists() && newProps.isFile()) {
                Path basePath = Paths.get(oldDir.toURI());
                Path filePath = Paths.get(oldProps.toURI());
                Path relativePath = basePath.relativize(filePath);
                String fPathAsString = relativePath.toString();
                addedProps.add(fPathAsString);
            }
            if (oldProps.exists() && oldProps.isFile() && newProps.exists() && newProps.isFile()) {
                //using google diffutils compare.
                List<String> oldLines = Report.fileToLines(oldProps);
                List<String> newLines = Report.fileToLines(newProps);
                Patch patch = DiffUtils.diff(oldLines, newLines);

                if (patch.getDeltas().size() > 0) {
                    Path basePath = Paths.get(oldDir.toURI());
                    Path filePath = Paths.get(oldProps.toURI());
                    Path relativePath = basePath.relativize(filePath);
                    String fPathAsString = relativePath.toString();

                    alteredProps.put(fPathAsString, new HashMap<MetatypeDiff.DiffType, Object>());
                    alteredProps.get(fPathAsString).put(DiffType.PATCH_OBJECT, patch);
                    String why = "";
                    why += "   why: (unified diff)\n";
                    List<String> unified = DiffUtils.generateUnifiedDiff(fPathAsString, fPathAsString, oldLines, patch, 5);
                    String unifiedString = "";
                    for (String s : unified) {
                        why += "      " + s + "\n";
                        unifiedString += s + "\n";
                    }

                    why = why.substring(0, why.length() - 1);
                    alteredProps.get(fPathAsString).put(DiffType.UNIFIED_TEXT, unifiedString);
                    alteredProps.get(fPathAsString).put(DiffType.WHY_TEXT, why);
                }
            }
        }
    }

    public boolean hasChanges() {
        return !deletedMetatypes.isEmpty() | !addedMetatypes.isEmpty() | !alteredMetatypes.isEmpty()
               | !deletedProps.isEmpty() | !addedProps.isEmpty() | !alteredProps.isEmpty();
    }

    /**
     * @param output
     */
    public void reportChanges(PrintWriter output) {

        if (hasChanges()) {

            output.println("  <div class=\"metatypechanges\">");

            if (!addedMetatypes.isEmpty()) {
                output.println("    <div class=\"addedmetatypes\">");
                System.out.println(" - Added metatypes : ");
                for (String header : addedMetatypes) {
                    fileUtils.copyNewFile(header);
                    output.println("      <div class=\"metatype\">");
                    System.out.println("  " + header);
                    output.println("        <div class=\"filename\">" + header + "</div>");
                    output.println("      </div>");
                }
                output.println("    </div>");
            }
            if (!deletedMetatypes.isEmpty()) {
                output.println("    <div class=\"deletedmetatypes\">");
                System.out.println(" - Removed metatypes : ");
                for (String header : deletedMetatypes) {
                    fileUtils.copyOldFile(header);
                    output.println("      <div class=\"metatype\">");
                    System.out.println("  " + header);
                    output.println("        <div class=\"filename\">" + header + "</div>");
                    output.println("      </div>");
                }
                output.println("    </div>");
            }
            if (!alteredMetatypes.isEmpty()) {
                output.println("    <div class=\"alteredmetatypes\">");
                System.out.println(" - Altered metatypes : ");
                for (Map.Entry<String, Map<MetatypeDiff.DiffType, Object>> header : alteredMetatypes.entrySet()) {
                    fileUtils.copyOldFile(header.getKey());
                    fileUtils.copyNewFile(header.getKey());
                    output.println("      <div class=\"metatype\">");
                    output.println("        <div class=\"filename\">" + header.getKey() + "</div>");
                    System.out.println("  " + header.getKey() + " has changed. \n" + header.getValue().get(MetatypeDiff.DiffType.WHY_TEXT));
                    //we have to escape..
                    String escaped = ((String) header.getValue().get(MetatypeDiff.DiffType.UNIFIED_TEXT)).replaceAll("<", "&lt;");
                    escaped = escaped.replaceAll(">", "&gt;");
                    output.println("        <div class=\"detail\">\n" + escaped);
                    output.println("        </div>");
                    output.println("      </div>");
                }
                output.println("    </div>");
            }
            if (!addedProps.isEmpty()) {
                output.println("    <div class=\"addedprops\">");
                System.out.println(" - Added nls : ");
                for (String header : addedProps) {
                    fileUtils.copyNewFile(header);
                    output.println("      <div class=\"metatype\">");
                    System.out.println("  " + header);
                    output.println("        <div class=\"filename\">" + header + "</div>");
                    output.println("      </div>");
                }
                output.println("    </div>");
            }
            if (!deletedProps.isEmpty()) {
                output.println("    <div class=\"deletedprops\">");
                System.out.println(" - Removed nls : ");
                for (String header : deletedProps) {
                    fileUtils.copyOldFile(header);
                    output.println("      <div class=\"metatype\">");
                    System.out.println("  " + header);
                    output.println("        <div class=\"filename\">" + header + "</div>");
                    output.println("      </div>");
                }
                output.println("    </div>");
            }
            if (!alteredProps.isEmpty()) {
                output.println("    <div class=\"alteredprops\">");
                System.out.println(" - Altered nls : ");
                for (Map.Entry<String, Map<MetatypeDiff.DiffType, Object>> header : alteredProps.entrySet()) {
                    output.println("      <div class=\"nlsprops\">");
                    output.println("        <div class=\"filename\">" + header.getKey() + "</div>");
                    fileUtils.copyOldFile(header.getKey());
                    fileUtils.copyNewFile(header.getKey());
                    System.out.println("  " + header.getKey() + " has changed. \n" + header.getValue().get(MetatypeDiff.DiffType.WHY_TEXT));
                    //we have to escape..
                    String escaped = ((String) header.getValue().get(MetatypeDiff.DiffType.UNIFIED_TEXT)).replaceAll("<", "&lt;");
                    escaped = escaped.replaceAll(">", "&gt;");
                    output.println("        <div class=\"detail\">\n" + escaped);
                    output.println("        </div>");
                    output.println("      </div>");
                }
                output.println("    </div>");
            }
            output.println("  </div>");
        }

    }
}