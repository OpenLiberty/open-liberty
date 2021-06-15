/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.depScanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class Repository {

    private final Map<String, List<Module>> moduleMap = new HashMap<>();

    public Repository(File repoDir) {
        Module.setRoot(repoDir.getAbsolutePath());

        for (File f : Utils.findJars(repoDir)) {
            try {
                Module m = new Module(f);
                List<Module> modules = moduleMap.computeIfAbsent(m.getModuleId(), k -> new ArrayList<>());
                modules.add(m);
                JarInputStream jarIn = new JarInputStream(new FileInputStream(f));
                ZipEntry entry = jarIn.getNextEntry();
                if (entry != null) {
                    do {
                        if (entry.getName().endsWith(".class") && !entry.getName().equals("module-info.class")) {
                            String className = entry.getName();
                            className = className.replaceAll("/", ".");
                            className = className.substring(0, className.length() - 6);

                            byte[] hash = Utils.computeHash(jarIn);

                            m.addClass(className, hash);
                        }
                    } while ((entry = jarIn.getNextEntry()) != null);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Stream<Map.Entry<String, List<Module>>> stream() {
        return moduleMap.entrySet().stream();
    }
}