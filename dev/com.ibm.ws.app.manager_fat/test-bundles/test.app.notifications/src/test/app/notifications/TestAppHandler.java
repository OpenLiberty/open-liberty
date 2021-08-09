/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.app.notifications;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;
import com.ibm.wsspi.application.handler.ApplicationTypeSupported;
import com.ibm.wsspi.application.lifecycle.ApplicationStartBarrier;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.factory.contributor.ArtifactContainerFactoryContributor;

@Component(name = "test.app.notifications",
           service = { ApplicationHandler.class, RuntimeUpdateListener.class, ApplicationTypeSupported.class, ArtifactContainerFactoryContributor.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM", "type=tan", "handlesType=java.io.File" })
public class TestAppHandler implements ApplicationHandler<String>, RuntimeUpdateListener, ApplicationTypeSupported, ArtifactContainerFactoryContributor {
    private static final String RETURN_NULL = "returnNull";
    private static final String RETURN_FAILED = "returnFailed";
    private static final String RETURN_FALSE = "returnFalse";

    private FutureMonitor futureMonitor;
    private AppInstallsCalledCompletionListener installsCalledListener;
    private final List<String> installingApps = Collections.synchronizedList(new ArrayList<String>());

    @Reference
    protected void setFutureMonitor(FutureMonitor futureMonitor) {
        this.futureMonitor = futureMonitor;
        this.installsCalledListener = new AppInstallsCalledCompletionListener(this.futureMonitor);
    }

    protected void unsetFutureMonitor(FutureMonitor futureMonfitor) {
        // nothing;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.application.handler.ApplicationHandler#install(com.ibm.wsspi.application.handler.ApplicationInformation)
     */
    @Override
    public Future<Boolean> install(final ApplicationInformation<String> applicationInformation) {
        if (RETURN_NULL.equals(applicationInformation.getName())) {
            return futureMonitor.createFutureWithResult((Boolean) null);
        }
        if (RETURN_FAILED.equals(applicationInformation.getName())) {
            return futureMonitor.createFutureWithResult(Boolean.class, new RuntimeException("TEST FAILURE"));
        }
        if (RETURN_FALSE.equals(applicationInformation.getName())) {
            return futureMonitor.createFutureWithResult(Boolean.FALSE);
        }
        applicationInformation.setHandlerInfo(applicationInformation.getName());
        boolean firstApp = false;
        synchronized (installingApps) {
            installingApps.add(applicationInformation.getHandlerInfo());
            firstApp = installingApps.size() == 1;
        }
        final Future<Boolean> installFuture = futureMonitor.createFuture(Boolean.class);
        futureMonitor.onCompletion(installsCalledListener.getInstallsCompleteFuture(), new CompletionListener<Void>() {
            @Override
            public void successfulCompletion(Future<Void> future, Void result) {
                reportStarting();
                futureMonitor.setResult(installFuture, Boolean.TRUE);
            }

            @Override
            public void failedCompletion(Future<Void> future, Throwable t) {
                reportStarting();
                futureMonitor.setResult(installFuture, t);
            }

            private void reportStarting() {
                synchronized (installingApps) {
                    StringBuilder output = new StringBuilder();
                    if (!installingApps.isEmpty()) {
                        for (String app : installingApps) {
                            if (output.length() > 0) {
                                output.append(", ");
                            }
                            output.append(app);
                        }
                        installingApps.clear();
                        output.insert(0, "CURRENT INSTALLING APPS: ");
                        System.out.println(output);
                    }
                }
                System.out.println("STARTING APP: " + applicationInformation.getHandlerInfo());
            }
        });
        System.out.println("INSTALLING APP: " + applicationInformation.getHandlerInfo());
        if (firstApp) {
            System.out.println("FIRST APP: " + applicationInformation.getHandlerInfo());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
        return installFuture;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.application.handler.ApplicationHandler#uninstall(com.ibm.wsspi.application.handler.ApplicationInformation)
     */
    @Override
    public Future<Boolean> uninstall(ApplicationInformation<String> applicationInformation) {
        return futureMonitor.createFutureWithResult(Boolean.TRUE);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.application.handler.ApplicationHandler#setUpApplicationMonitoring(com.ibm.wsspi.application.handler.ApplicationInformation)
     */
    @Override
    public ApplicationMonitoringInformation setUpApplicationMonitoring(ApplicationInformation<String> applicationInformation) {
        // do nothing
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.runtime.update.RuntimeUpdateListener#notificationCreated(com.ibm.ws.runtime.update.RuntimeUpdateManager, com.ibm.ws.runtime.update.RuntimeUpdateNotification)
     */
    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        if (RuntimeUpdateNotification.APPLICATIONS_INSTALL_CALLED.equals(notification.getName())) {
            installsCalledListener.createInstallsCompleteFuture();
            notification.onCompletion(installsCalledListener);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.artifact.factory.contributor.ArtifactContainerFactoryContributor#createContainer(java.io.File, java.lang.Object)
     */
    @Override
    public ArtifactContainer createContainer(File workAreaCacheDir, Object o) {
        if (o instanceof File) {
            final File f = (File) o;
            if (!f.getName().endsWith(".tan") && !f.getName().endsWith(".nha")) {
                return null;
            }
            return new ArtifactContainer() {

                @Override
                public String getName() {
                    return f.getName();
                }

                @Override
                public ArtifactContainer getRoot() {
                    return this;
                }

                @Override
                public String getPhysicalPath() {
                    return f.getAbsolutePath();
                }

                @Override
                public String getPath() {
                    return f.getPath();
                }

                @Override
                public ArtifactContainer getEnclosingContainer() {
                    return null;
                }

                @Override
                public Iterator<ArtifactEntry> iterator() {
                    return Collections.<ArtifactEntry> emptyList().iterator();
                }

                @Override
                public void useFastMode() {
                }

                @Override
                public void stopUsingFastMode() {
                }

                @Override
                public boolean isRoot() {
                    return true;
                }

                @Override
                public Collection<URL> getURLs() {
                    return Collections.emptyList();
                }

                @Override
                public ArtifactEntry getEntryInEnclosingContainer() {
                    return null;
                }

                @Override
                public ArtifactEntry getEntry(String pathAndName) {
                    return null;
                }

                @Override
                public ArtifactNotifier getArtifactNotifier() {
                    return new ArtifactNotifier() {

                        @Override
                        public boolean setNotificationOptions(long interval, boolean useMBean) {
                            return false;
                        }

                        @Override
                        public boolean removeListener(ArtifactListener listenerToRemove) {
                            return false;
                        }

                        @Override
                        public boolean registerForNotifications(ArtifactNotification targets, ArtifactListener callbackObject) throws IllegalArgumentException {
                            return false;
                        }
                    };
                }
            };
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.artifact.factory.contributor.ArtifactContainerFactoryContributor#createContainer(java.io.File, com.ibm.wsspi.artifact.ArtifactContainer,
     * com.ibm.wsspi.artifact.ArtifactEntry, java.lang.Object)
     */
    @Override
    public ArtifactContainer createContainer(File workAreaCacheDir, ArtifactContainer parent, ArtifactEntry entry, Object o) {
        return null;
    }

    @Reference
    private void setApplicationStartBarrier(ApplicationStartBarrier applicationStartBarrier) {
    }
}
