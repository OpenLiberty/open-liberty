/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.queuing.internal;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;

/**
 * This guy is a singleton object that manages the attaching and detaching from
 * a client's shared memory area.  We must be careful when attaching/detaching
 * since a dup attach or a premature detach can cause an ABEND.  This class
 * ensures that all shared memory areas are safely attached and detached as
 * appropriate.
 * 
 * This class maintains a mapping of shared memory area tokens (Longs representing
 * native addresses) to "state" objects (AtomicLongs) which monitor the attach,
 * access, and detach of shared memory areas.  The state object is basically a
 * use count, with a few special states, which are:
 *   state == -1: The shared memory area is detached (this is the initial state value)
 *   state == 0: A detach is pending since there are no more callers accessing the area
 *   state > 0: Indicates the number of callers accessing the area
 *   
 * Before a shared memory area is attached it has state == -1 (detached). Once
 * attached, the state is set to 1 (whoever attached it is the first connection
 * to access the area).  Each new connection increments the state by 1.  When
 * the connection closes the state is decremented.
 * 
 * Other callers like read/write temporarily obtain access (increment the state)
 * for only the duration of the native method where access is needed.  These callers 
 * then immediately release access (decrement the state).
 * 
 * So an area's state remains > 0 so long as there are still connections using it.
 * Once the last connection exits, the state drops to 0, which triggers the detach.
 * The detach, if successful, sets the state to -1 (the detached state).  A subsequent
 * call to attach will re-attach to the area.
 * 
 * This class uses compare-and-swap on the state object for lock-free access. 
 * The actual attach and detach functions are also wrapped with synchronization blocks
 * to ensure atomicity and consistency. 
 */
public class SharedMemoryAttachmentManager {

    /**
     * A mapping of client shared memory user token (used to access client's shared memory)
     * to an AtomicLong indicating the state of our attachment to that shared memory area.
     * 
     * If state == -1, then we have detached.
     * If state == 0, then a detach is pending.
     * if state > 0, then it indicates the "use count" for the shared memory -- roughly
     * this means the number of threads currently accessing the memory.
     * 
     */
    private final ConcurrentHashMap<Long, AtomicLong> stateTokenMap = new ConcurrentHashMap<Long, AtomicLong>();
    
    /**
     * Handles callouts to do the actual attach/detach.
     */
    private NativeRequestHandler nativeRequestHandler;

    /**
     * 
     */
    public SharedMemoryAttachmentManager(NativeRequestHandler nativeRequestHandler) {
        this.nativeRequestHandler = nativeRequestHandler;
    }
    
    /**
     * This method ensures that 1 and only 1 StateToken (AtomicLong) object is ever created for
     * the given sharedMemoryToken.
     * 
     * We're protecting against the scenario where two connections from the same client (using
     * the same sharedMemoryToken) arrive at the same time and both try to connect back to the
     * client's shared memory. In order for the synchronization in obtainOrCreate to work
     * right, they both must be using the same StateToken object for that sharedMemoryToken.
     * 
     * If the StateToken doesn't exist yet, create it, initialize it to -1 ("detached"), and add
     * it to the map atomically.
     * 
     * @param sharedMemoryToken - uniquely identifies the shared memory area
     * 
     * @return the state token for the given shared memory area
     */
    protected AtomicLong getStateToken(long sharedMemoryToken) {
        AtomicLong stateToken = stateTokenMap.get(sharedMemoryToken);

        if (stateToken == null) {
            stateTokenMap.putIfAbsent(sharedMemoryToken, new AtomicLong(-1));
            stateToken = stateTokenMap.get(sharedMemoryToken);
        }

        return stateToken;
    }

    /**
     * Obtain access to the shared memory area, or attach to the shared memory
     * area if we haven't yet or if we've previously detached.
     * 
     * @throws IOException for attach failures
     * @throws IllegalStateException if really unexpected things happen.
     */
    public void obtainAccessOrAttach(NativeWorkRequest connectWorkRequest) throws IOException {

        AtomicLong stateToken = getStateToken(connectWorkRequest.getSharedMemoryToken());

        while (true) {
            long currentValue = stateToken.get();

            if (currentValue <= 0) {
                // Either we've detached or are about to.  

                if (attach(stateToken, connectWorkRequest)) {
                    // Succesfully attached.
                    return;

                } else {
                    // State changed. Most likely some other thread completed the attach.
                    // Loop around and run thru the logic again.
                }

            } else {
                // Already attached.  Just increment the use count.
                if (obtainAccess(stateToken)) {
                    return;
                } else {
                    // Couldn't obtain. Perhaps another thread just detached. 
                    // Loop around and run thru the logic again.
                }
            }
        }
    }

    /**
     * 
     * Note: this method synchronizes on the stateToken in order to coordinate with detach.
     *       Note that we're not using synchronization to manage the value of the AtomicLong,
     *       just to coordinate in case two threads are trying to do an attach and detach
     *       at the same time.
     *       
     * @return true if we successfully attached; false if we didn't try to attach because
     *         the stateToken changed.
     * 
     * @throws IOException for attach failures
     * @throws IllegalStateException if really unexpected things happen
     */
    private boolean attach(AtomicLong stateToken, NativeWorkRequest connectWorkRequest) throws IOException {

        synchronized (stateToken) {
            // Get the value, within synchronization.
            long currentValue = stateToken.get();

            if (currentValue <= 0) {

                if (currentValue == -1) {
                    // We've detached.  Re-attach.
                    nativeRequestHandler.connectToClientsSharedMemory(connectWorkRequest.getClientConnectionHandle().getBytes(),
                                                                      connectWorkRequest.getRequestSpecificParms());
                }

                long newValue = 1; // new use count
                if (stateToken.compareAndSet(currentValue, newValue)) {
                    // expected.
                    return true;
                } else {
                    // Really not expected.  Somehow the state changed during the 
                    // synchronized block, even tho readers should not be modifying 
                    // it since it's already in detached/pendingDetached state.
                    throw new IllegalStateException();
                }
            } else {
                // State changed.  Most likely some other thread completed the attach.
                // Loop around and run the logic again.
                return false;
            }
        }
    }

    /**
     * Obtain access to the shared memory associated with the given connectWorkRequest.
     * 
     * If access is granted, this method returns normally.  If not, it throws a
     * SharedMemoryAccessException.
     * 
     * @throws SharedMemoryAccessException if access could not be obtained.
     */
    public void obtainAccess(NativeWorkRequest connectWorkRequest) throws SharedMemoryAccessException {
        if ( !obtainAccess(getStateToken(connectWorkRequest.getSharedMemoryToken())) ) {
            throw new SharedMemoryAccessException("Could not obtain access to client's shared memory for this connection");
        }
    }
    
    /**
     * @return true if access to the memory was successfully obtained.
     */
    protected boolean obtainAccess(AtomicLong stateToken) {

        while (true) {
            long currentValue = stateToken.get();

            if (currentValue <= 0) {
                // The memory is detached or about to be.
                return false;

            } else {
                // Increment the useCount.
                long newValue = currentValue + 1;
                if (stateToken.compareAndSet(currentValue, newValue)) {
                    return true; // We're done.
                } else {
                    // State changed.  Loop around and try again.
                }
            }
        }
    }

    /**
     * Release access to the shared memory area associated with the given connectWorkRequest.
     * 
     * If the use count drops to 0, then try to detach from the shared memory area.
     * 
     * @throws IOException for detach failures.
     */
    public void releaseAccess(NativeWorkRequest connectWorkRequest) throws IOException {

        AtomicLong stateToken = getStateToken(connectWorkRequest.getSharedMemoryToken());

        while (true) {

            long currentValue = stateToken.get();

            // Decrement the useCount 
            long newValue = currentValue - 1;
            if (stateToken.compareAndSet(currentValue, newValue)) {
                break; // We're done.
            } else {
                // State changed.  Loop around and try again.
            }
        }

        // Check if we should detach
        if (stateToken.get() == 0) {
            detach(stateToken, connectWorkRequest);
        }
    }

    /**
     * Try to detach. The detach is done after synchronizing the stateToken and
     * checking that its value is still 0. If so, the detach is made and the stateToken
     * value will is set to -1.
     * 
     * Otherwise, if the stateToken value is not 0, then this method does nothing.
     * 
     * @throws IOException for detach failures.
     */
    private void detach(AtomicLong stateToken, NativeWorkRequest connectWorkRequest) throws IOException {

        synchronized (stateToken) {
            long currentValue = stateToken.get();

            if (currentValue == 0) {
                // The use count is 0 and we're in the synchronized block.
                // It should be safe to detach now.
                nativeRequestHandler.disconnectFromClientsSharedMemory(connectWorkRequest.getClientConnectionHandle().getBytes(),
                                                                       connectWorkRequest.getRequestSpecificParms());

                long newValue = -1; // "detached"
                if (stateToken.compareAndSet(currentValue, newValue)) {
                    // expected.
                    return;
                } else {
                    // Really not expected. 
                    // Somehow the state changed even tho the useCount is 0, which 
                    // means everyone (except connect) should have bailed out, and connect
                    // should have obtained the lock before making updates.
                    throw new IllegalStateException();
                }
            }
        }
    }
}
