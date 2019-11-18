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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.aries.util.manifest.ManifestProcessor;

class ManifestDiff {
    static enum HeaderType {
        SYMBOLICNAME, IMPORT, STRING, IGNORE
    };

    static final Map<String, HeaderType> headerTypes = new HashMap<String, HeaderType>();
    static {
        headerTypes.put("Subsystem-SymbolicName", HeaderType.SYMBOLICNAME);
        headerTypes.put("IBM-AppliesTo", HeaderType.SYMBOLICNAME);
        headerTypes.put("IBM-ShortName", HeaderType.STRING);
        headerTypes.put("IBM-Feature-Version", HeaderType.STRING);
        headerTypes.put("IBM-License-Agreement", HeaderType.STRING);
        // TEMP, can unignore once things have settled down
        headerTypes.put("IBM-License-Information", HeaderType.IGNORE);
        // TEMP, ignore
        headerTypes.put("IBM-ProductID", HeaderType.IGNORE);
        headerTypes.put("Subsystem-Description", HeaderType.STRING);
        headerTypes.put("Subsystem-License", HeaderType.IGNORE);
        headerTypes.put("Subsystem-Localization", HeaderType.STRING);
        headerTypes.put("Subsystem-ManifestVersion", HeaderType.STRING);
        headerTypes.put("Subsystem-Name", HeaderType.STRING);
        headerTypes.put("Subsystem-Type", HeaderType.STRING);
        headerTypes.put("Subsystem-Vendor", HeaderType.STRING);
        headerTypes.put("Subsystem-Version", HeaderType.STRING);
        headerTypes.put("IBM-API-Package", HeaderType.IMPORT);
        headerTypes.put("IBM-SPI-Package", HeaderType.IMPORT);
        headerTypes.put("IBM-Provision-Capability", HeaderType.IMPORT);
        headerTypes.put("Subsystem-Content", HeaderType.IMPORT);
        headerTypes.put("Bnd-LastModified", HeaderType.IGNORE);
        headerTypes.put("Tool", HeaderType.IGNORE);
        headerTypes.put("Created-By", HeaderType.IGNORE);
    }

    final Map<String, String> deletedHeaders = new TreeMap<String, String>();
    final Map<String, String> addedHeaders = new TreeMap<String, String>();

    enum Why {
        ADDED, REMOVED, CHANGED
    };

    public static class Change implements Comparable<Change> {
        String oldValue;
        String newValue;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                     + ((newValue == null) ? 0 : newValue.hashCode());
            result = prime * result
                     + ((oldValue == null) ? 0 : oldValue.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Change other = (Change) obj;
            if (newValue == null) {
                if (other.newValue != null)
                    return false;
            } else if (!newValue.equals(other.newValue))
                return false;
            if (oldValue == null) {
                if (other.oldValue != null)
                    return false;
            } else if (!oldValue.equals(other.oldValue))
                return false;
            return true;
        }

        @Override
        public int compareTo(Change o) {
            if (o == null) {
                return -1;
            } else if (oldValue.equals(o.oldValue)) {
                return newValue.compareTo(o.newValue);
            } else {
                return oldValue.compareTo(o.oldValue);
            }
        }
    }

    final Map<String, Map<Why, Set<Change>>> changedHeaders = new TreeMap<String, Map<Why, Set<Change>>>();

    public boolean hasChanges() {
        return !deletedHeaders.isEmpty() | !addedHeaders.isEmpty() | !changedHeaders.isEmpty();
    }

    private void compareAttributes(Attributes oldAttribs, Attributes newAttribs) {

        if ((newAttribs.getValue("Created-By") != null) &&
            (oldAttribs.getValue("Created-By") == null)) {
            System.out.println("BND style feature conversion, temporarily ignoring this.");
            return;
        }
        //new headers..
        for (Entry<Object, Object> o : newAttribs.entrySet()) {
            String header = o.getKey().toString();
            if (!headerTypes.containsKey(header) || headerTypes.get(header) != HeaderType.IGNORE) {
                String value = o.getValue().toString();
                if (oldAttribs.getValue(header) == null) {
                    addedHeaders.put(header, value);
                }
            }
        }

        //removed headers..
        for (Entry<Object, Object> o : oldAttribs.entrySet()) {
            String header = o.getKey().toString();
            if (!headerTypes.containsKey(header) || headerTypes.get(header) != HeaderType.IGNORE) {
                String value = o.getValue().toString();
                if (newAttribs.getValue(header) == null) {
                    deletedHeaders.put(header, value);
                }
            }
        }

        //changed headers..
        for (Entry<Object, Object> o : oldAttribs.entrySet()) {
            String header = o.getKey().toString();
            String value = o.getValue().toString();
            if (newAttribs.getValue(header) != null) {
                String nValue = newAttribs.getValue(header);
                HeaderType h = headerTypes.containsKey(header) ? headerTypes.get(header) : HeaderType.STRING;
                switch (h) {
                    case IGNORE: {
                        break;
                    }
                    case STRING: {
                        if (!nValue.equals(value)) {
                            if (!changedHeaders.containsKey(header)) {
                                changedHeaders.put(header, new HashMap<Why, Set<Change>>());
                            }
                            if (!changedHeaders.get(header).containsKey(Why.CHANGED)) {
                                changedHeaders.get(header).put(Why.CHANGED, new TreeSet<Change>());
                            }
                            Change c = new Change();
                            c.oldValue = value;
                            c.newValue = nValue;
                            changedHeaders.get(header).get(Why.CHANGED).add(c);
                        }
                        break;
                    }
                    case SYMBOLICNAME: {
                        NameValuePair oldName = ManifestHeaderProcessor.parseBundleSymbolicName(value);
                        NameValuePair newName = ManifestHeaderProcessor.parseBundleSymbolicName(nValue);
                        if (oldName.getAttributes() != null && newName.getAttributes() != null) {
                            // Ignore the productVersion attribute so we don't get version change spam for every release
                            oldName.getAttributes().remove("productVersion");
                            newName.getAttributes().remove("productVersion");

                            //TEMP, also ignore product edition
                            oldName.getAttributes().remove("productEdition");
                            newName.getAttributes().remove("productEdition");

                            // Also temporary, tons of spam from license change
                            oldName.getAttributes().remove("http://www.ibm.com/licenses/wlp-featureterms-restricted-v1");
                            newName.getAttributes().remove("http://www.ibm.com/licenses/wlp-featureterms-v1");
                        }
                        if (!oldName.getName().equals(newName.getName()) | (oldName.getAttributes() != null && !oldName.getAttributes().equals(newName.getAttributes()))) {
                            if (!changedHeaders.containsKey(header)) {
                                changedHeaders.put(header, new HashMap<Why, Set<Change>>());
                            }
                            if (!changedHeaders.get(header).containsKey(Why.CHANGED)) {
                                changedHeaders.get(header).put(Why.CHANGED, new TreeSet<Change>());
                            }
                            Change c = new Change();
                            c.oldValue = value;
                            c.newValue = nValue;
                            changedHeaders.get(header).get(Why.CHANGED).add(c);
                        }
                        break;
                    }
                    case IMPORT: {
                        Map<String, Map<String, String>> oldValue = ManifestHeaderProcessor.parseImportString(value);
                        Map<String, Map<String, String>> newValue = ManifestHeaderProcessor.parseImportString(nValue);

                        Set<String> keysToCompareContent = newValue.keySet();

                        if (!oldValue.keySet().equals(newValue.keySet())) {

                            if (!changedHeaders.containsKey(header)) {
                                changedHeaders.put(header, new HashMap<Why, Set<Change>>());
                            }
                            if (!changedHeaders.get(header).containsKey(Why.CHANGED)) {
                                changedHeaders.get(header).put(Why.CHANGED, new TreeSet<Change>());
                            }

                            Set<String> overlap = new HashSet<String>(oldValue.keySet());
                            overlap.retainAll(newValue.keySet());

                            keysToCompareContent = overlap;

                            Set<String> removed = new TreeSet<String>(oldValue.keySet());
                            removed.removeAll(overlap);

                            Set<String> added = new TreeSet<String>(newValue.keySet());
                            added.removeAll(overlap);

                            if (!removed.isEmpty()) {
                                if (!changedHeaders.get(header).containsKey(Why.REMOVED)) {
                                    changedHeaders.get(header).put(Why.REMOVED, new TreeSet<Change>());
                                }
                                for (String rem : removed) {
                                    //rebuild the entry..
                                    Map<String, String> params = oldValue.get(rem);
                                    String rebuilt = rem;
                                    if (params != null) {
                                        for (Map.Entry<String, String> e : params.entrySet()) {
                                            rebuilt += ";" + e.getKey() + "=\"" + e.getValue() + "\"";
                                        }
                                    }
                                    Change c1 = new Change();
                                    c1.oldValue = rebuilt;
                                    c1.newValue = "";
                                    changedHeaders.get(header).get(Why.REMOVED).add(c1);
                                }
                            }
                            if (!added.isEmpty()) {
                                if (!changedHeaders.get(header).containsKey(Why.ADDED)) {
                                    changedHeaders.get(header).put(Why.ADDED, new TreeSet<Change>());
                                }
                                for (String add : added) {
                                    //rebuild the entry..
                                    Map<String, String> params = newValue.get(add);
                                    String rebuilt = add;
                                    if (params != null) {
                                        for (Map.Entry<String, String> e : params.entrySet()) {
                                            rebuilt += ";" + e.getKey() + "=\"" + e.getValue() + "\"";
                                        }
                                    }
                                    Change c1 = new Change();
                                    c1.oldValue = "";
                                    c1.newValue = rebuilt;
                                    changedHeaders.get(header).get(Why.ADDED).add(c1);
                                }
                            }
                        }

                        //now we process the overlapping keys..

                        for (String key : keysToCompareContent) {
                            Map<String, String> oldEntrySet = oldValue.get(key);
                            Map<String, String> newEntrySet = newValue.get(key);
                            if ((oldEntrySet == null && newEntrySet != null) |
                                (oldEntrySet != null && !oldEntrySet.equals(newEntrySet))) {

                                if (!changedHeaders.containsKey(header)) {
                                    changedHeaders.put(header, new HashMap<Why, Set<Change>>());
                                }
                                if (!changedHeaders.get(header).containsKey(Why.CHANGED)) {
                                    changedHeaders.get(header).put(Why.CHANGED, new TreeSet<Change>());
                                }

                                //rebuild the entry..
                                Map<String, String> oldparams = oldValue.get(key);
                                String oldRebuilt = key;
                                if (oldparams != null) {
                                    for (Map.Entry<String, String> e : oldparams.entrySet()) {
                                        oldRebuilt += ";" + e.getKey() + "=\"" + e.getValue() + "\"";
                                    }
                                }
                                Map<String, String> newparams = newValue.get(key);
                                String newRebuilt = key;
                                if (newRebuilt != null) {
                                    for (Map.Entry<String, String> e : newparams.entrySet()) {
                                        newRebuilt += ";" + e.getKey() + "=\"" + e.getValue() + "\"";
                                    }
                                }

                                Change c = new Change();
                                c.oldValue = oldRebuilt;
                                c.newValue = newRebuilt;
                                changedHeaders.get(header).get(Why.CHANGED).add(c);
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    public ManifestDiff(File oldDir, String oldSourcePath, File newDir, String newSourcePath) {
        File oldM = new File(oldDir, oldSourcePath);
        File newM = new File(newDir, newSourcePath);
        if (newM.getName().startsWith("com.ibm.websphere.appserver") && !oldM.getName().startsWith("com.ibm.websphere.appserver")) {
            System.out.println("BND conversion from " + oldM.getName() + " to " + newM.getName() + " - Ignoring");
            return;
        }
        if (oldM.exists() && oldM.isFile() && newM.exists() && newM.isFile()) {
            FileInputStream oldFis = null;
            FileInputStream newFis = null;
            try {
                oldFis = new FileInputStream(oldM);
                newFis = new FileInputStream(newM);
                try {
                    Manifest o = ManifestProcessor.parseManifest(oldFis);
                    Manifest n = ManifestProcessor.parseManifest(newFis);

                    Attributes oma = o.getMainAttributes();
                    Attributes nma = n.getMainAttributes();

                    compareAttributes(oma, nma);

                    for (Entry<String, Attributes> x : o.getEntries().entrySet()) {
                        //System.out.println("Comparing "+String.valueOf(x.getKey())+" ");
                        Attributes oa = x.getValue();
                        Attributes na = n.getAttributes(x.getKey());

                        compareAttributes(oa, na);
                    }

                } catch (IOException io) {
                    io.printStackTrace();
                }
            } catch (FileNotFoundException f) {
                f.printStackTrace();
            } finally {
                try {
                    oldFis.close();
                    newFis.close();
                } catch (IOException io) {
                }
            }
        } else {
            System.out.println("Error performing tricorder scan of " + oldSourcePath + " with " + newSourcePath + " Paths were not found.");
        }
    }
}