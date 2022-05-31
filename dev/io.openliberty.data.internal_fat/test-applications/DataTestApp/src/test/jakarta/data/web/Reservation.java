/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 *
 */
public class Reservation {
    public String host;

    public Set<String> invitees;

    public String location;

    public long meetingID; // TODO autogenerate?

    public OffsetDateTime start;

    public OffsetDateTime stop;
}
