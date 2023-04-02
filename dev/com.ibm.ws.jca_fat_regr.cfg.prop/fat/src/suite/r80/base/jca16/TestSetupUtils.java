/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package suite.r80.base.jca16;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Class <CODE>TestSetupUtils</CODE> provides utility methods used
 * to help set up resources needed for tests
 */
public class TestSetupUtils {

    public static final String raDir = "test-resourceadapters/adapter.regr/resources/META-INF/";

    private static JavaArchive resourceAdapterEjs_jar;

    public static JavaArchive getTraAnnEjsResourceAdapter_jar() {
        if (resourceAdapterEjs_jar == null) {
            resourceAdapterEjs_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapter.jar");
            resourceAdapterEjs_jar.addPackages(true, "com.ibm.ejs.ras");
            Filter<ArchivePath> packageFilter = new Filter<ArchivePath>() {
                @Override
                public boolean include(final ArchivePath object) {
                    final String currentPath = object.get();

                    boolean included = !currentPath.contains("com/ibm/tra/ann");

                    //System.out.println("ResourceAdapterEjs included: " + included + " packageFilter object name: " + currentPath);
                    return included;

                }
            };
            resourceAdapterEjs_jar.addPackages(true, packageFilter, "com.ibm.tra");
        }

        return resourceAdapterEjs_jar;
    }
}
