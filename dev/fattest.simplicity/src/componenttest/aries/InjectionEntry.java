/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.aries;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 *
 */
public class InjectionEntry {
    public static final String INJECTION_PROP = "injection_files";

    private final Hashtable<String, String> filesToUpdate = new Hashtable<String, String>();
    private final Hashtable<String, String> filesToRemove = new Hashtable<String, String>();
    private final Hashtable<String, InjectionEntry> nestedInjections = new Hashtable<String, InjectionEntry>();

    public void addEntry(String fileNameInsideArchive, String newFileToUse) {
        filesToUpdate.put(fileNameInsideArchive, newFileToUse);
    }

    public void removeEntry(String fileNameInsideArchive) {
        filesToRemove.put(fileNameInsideArchive, "");
    }

    public void addNestedEntry(String fileNameInsideArchive, InjectionEntry child) {
        nestedInjections.put(fileNameInsideArchive, child);
    }

    public boolean containsFile(String fileName) {
        return filesToUpdate.containsKey(fileName) || filesToRemove.containsKey(fileName) || nestedInjections.containsKey(fileName);
    }

    public Enumeration<String> getEnumFilesToUpdate() {
        return filesToUpdate.keys();
    }

    public String getUpdatedFile(String key) {
        return filesToUpdate.get(key);
    }

    public Enumeration<String> getEnumNestedEntries() {
        return nestedInjections.keys();
    }

    public InjectionEntry getUpdatedNestedEntry(String key) {
        return nestedInjections.get(key);
    }

}
