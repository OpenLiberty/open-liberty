/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.persistra.resourceadapter;

import java.util.Date;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;
import javax.resource.spi.AdministeredObject;

/**
 * Non-serializable trigger that is provided by a resource adapter.
 */
@AdministeredObject
public class RATrigger implements Trigger {
    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        return lastExecution == null ? taskScheduledTime : null; // run once immediately
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }
}
