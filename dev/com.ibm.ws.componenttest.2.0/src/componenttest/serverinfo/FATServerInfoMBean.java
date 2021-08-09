/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.serverinfo;

import java.util.Set;

public interface FATServerInfoMBean {
    /**
     * A String representing the {@link javax.management.ObjectName} that this MBean maps to.
     */
    public final static String OBJECT_NAME = "WebSphereFAT:name=ServerInfo";

    public Set<String> getInstalledFeatures();
}
