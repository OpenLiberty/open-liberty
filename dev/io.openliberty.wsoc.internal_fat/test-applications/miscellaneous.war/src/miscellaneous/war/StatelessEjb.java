/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package miscellaneous.war;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class StatelessEjb implements StatelessInterface {

    String trackingString = "started";

    @Override
    public String getValue() {
        return trackingString;
    }

    @PostConstruct
    public void postConstructed() {
        trackingString = trackingString + MiscNoCDIInjectedServerEP.verify1;
    }

    @PreDestroy
    public void preDestroyed() {
        // hard to verify this in the test, but is uses the same mechanism as postContruct
        trackingString = trackingString + "...preDestroyed called";
    }

}
