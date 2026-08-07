/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.library.user;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.library.Library;

@Component(service = {})
public class LibraryUser {

    private final ClassLoadingService classLoadingService;
    @Activate
    public LibraryUser(@Reference ClassLoadingService cls) {
        this.classLoadingService = cls;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    public void setLibrary(Library l) {
        if (!"testCommonLib".equals(l.id())) {
            return;
        }
        ClassLoader cl1 = classLoadingService.getSharedLibraryClassLoader(l);
        ClassLoader cl2 = classLoadingService.getSharedLibraryClassLoader(l);
        if (cl1 == cl2) {
            System.out.println("TEST_SYNC getSharedLibraryClassLoader - SUCCESS: " + cl1);
        } else {
            System.out.println("TEST_SYNC getSharedLibraryClassLoader - FAILED: cl1=" + cl1 + " cl2=" + cl2);
        }
        // add the loader to system properties to be checked by the application
        System.getProperties().put("test.library.user.loader", cl1);
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                ClassLoader cl3 = classLoadingService.getSharedLibraryClassLoader(l);
                if (cl1 == cl3) {
                    System.out.println("TEST_ASYNC getSharedLibraryClassLoader - SUCCESS: " + cl1);
                } else {
                    System.out.println("TEST_ASYNC getSharedLibraryClassLoader - FAILED: cl1=" + cl1 + " cl3=" + cl3);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Getting classloader again").start();
    }
    public void unsetLibrary(Library l) {
        // do nothing
    }
}
