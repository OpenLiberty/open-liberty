/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejbbasic;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class StaticUserRepository {

    private static final Map<String, User> nameUserMap = new HashMap<String, User>();

    static {
        nameUserMap.put("Arugal", new User("Arugal", new Date(System.currentTimeMillis())));
        nameUserMap.put("Illidan Stormrage", new User("Illidan Stormrage", new Date(System.currentTimeMillis() - 24 * 3600 * 1000 * 15L)));
        nameUserMap.put("Hakkar", new User("Hakkar", new Date(System.currentTimeMillis() - 24 * 3600 * 1000 * 30L)));
    }

    public static User getUser(String userName) throws UserNotFoundException {
        User user = nameUserMap.get(userName);
        if (user == null) {
            throw new UserNotFoundException(userName);
        }
        return user;
    }

    public static User[] listUsers() {
        return nameUserMap.values().toArray(new User[] {});
    }
}
