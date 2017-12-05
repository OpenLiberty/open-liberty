/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jpaappcli.client;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

public class JPAApplicationClient {
    @PersistenceUnit(unitName = "JPAPU_RL")
    private static EntityManagerFactory emf;

    public static void main(String[] args) {

        System.out.println("emf=" + emf);
    }
}
