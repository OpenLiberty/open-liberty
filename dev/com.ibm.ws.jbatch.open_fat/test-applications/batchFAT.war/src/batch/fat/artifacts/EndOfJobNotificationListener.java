/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.artifacts;

import javax.batch.api.listener.AbstractJobListener;

/**
 *
 */
public class EndOfJobNotificationListener extends AbstractJobListener {

    /**
     * Notifies this class object so that anyone
     * can take this into consideration with polling (e.g. only start polling when
     * notified of this).
     * 
     * IMPORTANT: This is only going to work with someone with access to this classloader!
     * E.g. the FAT client couldn't do this but a servlet could potentially.
     */
    @Override
    public void afterJob() throws Exception {

        Object lock = EndOfJobNotificationListener.class;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

}
