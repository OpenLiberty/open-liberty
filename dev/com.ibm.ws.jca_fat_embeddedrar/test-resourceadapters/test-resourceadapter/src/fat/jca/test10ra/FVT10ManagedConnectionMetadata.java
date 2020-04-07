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
package fat.jca.test10ra;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;

public class FVT10ManagedConnectionMetadata implements ManagedConnectionMetaData {

    private String userName;

    public FVT10ManagedConnectionMetadata(String user) {
        this.userName = user;
    }

    @Override
    public String getEISProductName() throws ResourceException {
        return "TEST EIS";
    }

    @Override
    public String getEISProductVersion() throws ResourceException {
        return "1.0";
    }

    @Override
    public int getMaxConnections() throws ResourceException {
        return 0;
    }

    @Override
    public String getUserName() throws ResourceException {
        return userName;
    }

}
