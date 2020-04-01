/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ra;

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

public class DummyResourceAdapter implements ResourceAdapter {

    BootstrapContext ctx;
    WorkManager workManager;
    private static final ConcurrentLinkedQueue<WorkManager> workManagersForAllInstances = new ConcurrentLinkedQueue<WorkManager>();

    Timer jcaTimer;
    int delay;
    int period;
    String userName;
    String password;

    // Variables for test verification
    private static String ivVerifyString = "uninitialized_userName";
    private static Boolean canGetThirdPartyClass = null;

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

        // Check if we can load third party classes (for testClassSpaceRestriction)
        try {
            this.getClass().getClassLoader().loadClass("org.apache.commons.math.random.RandomGenerator");
            System.out.println("CAN get third-party class");
            canGetThirdPartyClass = true;
        } catch (ClassNotFoundException cnfe) {
            System.out.println("CAN NOT get third-party class.");
            canGetThirdPartyClass = false;
        }

        System.out.println("In DummyRA.start() username is: " + userName);
    }

    @Override
    public void stop() {
        workManagersForAllInstances.remove(workManager);
        workManager = null;
        ctx = null;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
        ivVerifyString = userName;
        System.out.println("Set username to: " + userName);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static boolean canGetThirdPartyClass() {
        return canGetThirdPartyClass.booleanValue();
    }

    public static String verifyUserName() {
        return ivVerifyString;
    }
}
