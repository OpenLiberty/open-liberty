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
package bean;

/**
 *
 */
import java.util.Date;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class InLibJarBean {
    private final Date created;

    public InLibJarBean() {
        created = new Date();
    }

    public String getCreated() {
        return created.toString();
    }

    @Override
    public String toString() {
        return "created in " + created;
    }
}
