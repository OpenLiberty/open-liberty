/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.KernelUtils;
import com.ibm.ws.kernel.feature.internal.RefreshBundlesListener;
import com.ibm.ws.kernel.feature.internal.StartLevelFrameworkListener;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 *
 */
public class TestUtils {

    static Field bundleRepository = null;

    public static void clearBundleRepositoryRegistry() throws Exception {
        if (bundleRepository == null) {
            bundleRepository = BundleRepositoryRegistry.class.getDeclaredField("repositoryHolders");
            bundleRepository.setAccessible(true);
        }
        ((Map<?, ?>) bundleRepository.get(null)).clear();
    }

    /** Replace the location of the kernel lib directory (usually calculated) */
    public static void setKernelUtilsBootstrapLibDir(File bootLibDir) throws Exception {
        KernelUtils.setBootStrapLibDir(bootLibDir);
    }

    /** Replace the location of the utils install directory (usually calculated) */
    public static void setUtilsInstallDir(File installDir) throws Exception {
        Utils.setInstallDir(installDir);
    }

    /**
     * Return an input stream containing a valid feature manifest
     *
     * @param symbolicNameString flattened symbolic name string. e.g. com.ibm.websphere.dummy;visibility:=protected
     * @param subsystemContentString flattened subsystem content string (including leading whitespace and
     *            middle line endings if wrapped): notexist1;location:="lib/notexist1"
     * @return InputStream that can be used to construct a SubsystemDefinition
     */
    public static InputStream createValidFeatureManifestStream(String symbolicNameString, String subsystemContentString) {
        return createValidFeatureManifestStream(null, symbolicNameString, subsystemContentString);
    }

    /**
     * Return an input stream containing a valid feature manifest
     *
     * @param shortName - the short name for the feature.
     * @param symbolicNameString flattened symbolic name string. e.g. com.ibm.websphere.dummy;visibility:=protected
     * @param subsystemContentString flattened subsystem content string (including leading whitespace and
     *            middle line endings if wrapped): notexist1;location:="lib/notexist1"
     * @return InputStream that can be used to construct a SubsystemDefinition
     */
    public static InputStream createValidFeatureManifestStream(String shortName, String symbolicNameString, String subsystemContentString) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0\n");
        writer.write("Subsystem-SymbolicName: " + symbolicNameString + "\n");
        if (shortName != null)
            writer.write("IBM-ShortName: " + shortName + "\n");
        writer.write("Subsystem-Type: osgi.subsystem.feature \n");
        writer.write("Subsystem-Version: 1.0.0 \n");
        writer.write("IBM-Feature-Version: 2 \n");
        writer.write("Subsystem-Content: " + subsystemContentString + "\n");
        writer.flush();

        return new ByteArrayInputStream(out.toByteArray());
    }

    /** Trivial interface that groups Bundle & BundleStartLevel so mock can push through the adapt method */
    public static interface TestBundleStartLevel extends Bundle, BundleStartLevel {}

    /** Trivial interface that groups Bundle & BundleRevision so mock can push through the adapt method */
    public static interface TestBundleRevision extends Bundle, BundleRevision {}

    public static final class TestFrameworkStartLevel implements FrameworkStartLevel {
        @Override
        public Bundle getBundle() {
            return null;
        }

        @Override
        public int getStartLevel() {
            return 0;
        }

        @Override
        public void setStartLevel(int startlevel, FrameworkListener... listeners) {
            try {
                Method m = StartLevelFrameworkListener.class.getDeclaredMethod("levelReached", boolean.class);
                m.setAccessible(true);

                for (FrameworkListener l : listeners) {
                    if (l instanceof StartLevelFrameworkListener) {
                        StartLevelFrameworkListener sl = (StartLevelFrameworkListener) l;
                        m.invoke(sl, false);
                    }
                }
            } catch (Exception t) {
                throw new RuntimeException("Unexpected exception invoking StartLevelFrameworkListener.levelReached: " + t.getMessage(), t);
            }
        }

        @Override
        public int getInitialBundleStartLevel() {
            return 2;
        }

        @Override
        public void setInitialBundleStartLevel(int startlevel) {}
    }

    public static final class TestFrameworkWiring implements FrameworkWiring {
        @Override
        public Bundle getBundle() {
            return null;
        }

        @Override
        public void refreshBundles(Collection<Bundle> bundles, FrameworkListener... listeners) {
            try {
                Method m = RefreshBundlesListener.class.getDeclaredMethod("finish", boolean.class);
                m.setAccessible(true);

                for (FrameworkListener l : listeners) {
                    if (l instanceof RefreshBundlesListener) {
                        RefreshBundlesListener rl = (RefreshBundlesListener) l;
                        m.invoke(rl, false);
                    }
                }
            } catch (Exception t) {
                throw new RuntimeException("Unexpected exception invoking RefreshBundlesListener.finish: " + t.getMessage(), t);
            }
        }

        @Override
        public boolean resolveBundles(Collection<Bundle> bundles) {
            return false;
        }

        @Override
        public Collection<Bundle> getRemovalPendingBundles() {
            return null;
        }

        @Override
        public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles) {
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.framework.wiring.FrameworkWiring#findProviders(org.osgi.resource.Requirement)
         */
        @Override
        public Collection<BundleCapability> findProviders(Requirement requirement) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    public static void recursiveClean(final WsResource fileToRemove) {
        recursiveClean(new File(fileToRemove.toExternalURI()));
    }

    public static void recursiveClean(final File fileToRemove) {
        if (fileToRemove == null)
            return;

        if (!fileToRemove.exists())
            return;

        if (fileToRemove.isDirectory()) {
            File[] files = fileToRemove.listFiles();
            for (File file : files) {
                if (file.isDirectory())
                    recursiveClean(file);
                else
                    file.delete();
            }
        }

        fileToRemove.delete();
    }
}