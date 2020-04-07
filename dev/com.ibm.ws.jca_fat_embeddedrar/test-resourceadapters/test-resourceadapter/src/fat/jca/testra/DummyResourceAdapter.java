/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.testra;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.jms.Destination;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;

import fat.jca.dll.TestWrapper;

public class DummyResourceAdapter implements ResourceAdapter {

    BootstrapContext ctx;
    WorkManager workManager;
    private static final ConcurrentLinkedQueue<WorkManager> workManagersForAllInstances = new ConcurrentLinkedQueue<WorkManager>();

    Timer jcaTimer;

    int delay;

    int period;

    String userName;

    String password;

    String nativeLib;

    @Override
    public void endpointActivation(MessageEndpointFactory mef,
                                   ActivationSpec as) throws ResourceException {
        DummyActivationSpec das = (DummyActivationSpec) as;
        final String destinationName = das.getDestination();
        final MessageEndpointFactory mef1 = mef;
        try {
            jcaTimer = ctx.createTimer();
        } catch (UnavailableException e) {
            e.printStackTrace(System.out);
        }
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Destination destination = DummyME.destinations.get(destinationName);
                if (destination instanceof javax.jms.Queue) {
                    DummyQueue dQ = (DummyQueue) destination;
                    DummyMessage message = dQ.internalQueue.peek();
                    if (message != null) {
                        try {
                            MessageEndpoint me = mef1.createEndpoint(null);
                            javax.jms.MessageListener endpoint = (javax.jms.MessageListener) me;
                            endpoint.onMessage(message);
                            dQ.internalQueue.remove(message);
                        } catch (Exception e) {
                            e.printStackTrace(System.out);
                        }
                    }
                }
            }
        };
        jcaTimer.schedule(task, delay, period);
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory mef,
                                     ActivationSpec as) {
        jcaTimer.cancel();
    }

    public static WorkManager[] getWorkManagersForAllStartedInstances() {
        return workManagersForAllInstances.toArray(new WorkManager[workManagersForAllInstances.size()]);
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] arg0) throws ResourceException {
        return null;
    }

    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        this.ctx = ctx;
        workManager = ctx.getWorkManager();
        workManagersForAllInstances.add(workManager);
        // Load a class from a jar in the rar file
        try {
            this.getClass().getClassLoader().loadClass("fat.jca.embeddedresourceadapter.jar1.FVTTestJar1Access");
            this.getClass().getClassLoader().loadClass("fat.jca.embeddedresourceadapter.jar2.FVTTestJar2Access");
            System.out.print("WAS able to load class FVTTestJar1Access");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.print("Was NOT able to load class FVTTestJar1Access");
            System.out.println("FAT Bundle NOT Started.");
            throw new ResourceAdapterInternalException(e);
        }

        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        System.out.println("os.name: " + os);
        System.out.println("os.arch: " + arch);

        if (os.contains("win") || os.contains("linux")) {
            String libName;
            if (arch.contains("x86") || arch.contains("amd")) {
                libName = arch.contains("64") ? nativeLib : nativeLib + "32";
            } else {
                libName = null;
            }

            if (libName != null) {
                System.out.println("platform specific lib name: " + System.mapLibraryName(libName));
                System.loadLibrary(libName);
                TestWrapper wrapper = new TestWrapper();
                // EmbeddedJCATest looks for this message.
                String echoValue = wrapper.echo("Loaded Native Library " + libName);
                System.out.println(echoValue);
            }
        }
    }

    @Override
    public void stop() {
        workManagersForAllInstances.remove(workManager);
        workManager = null;
        ctx = null;
    }

    /**
     * @return the delay
     */
    public int getDelay() {
        return delay;
    }

    /**
     * @param delay
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    /**
     * @return the period
     */
    public int getPeriod() {
        return period;
    }

    /**
     * @param period
     */
    public void setPeriod(int period) {
        this.period = period;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return the nativeLib
     */
    public String getNativeLib() {
        return nativeLib;
    }

    /**
     * @param userName
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @param nativeLib
     */
    public void setNativeLib(String nativeLib) {
        this.nativeLib = nativeLib;
    }

}
