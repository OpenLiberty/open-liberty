/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.standalone.rest.client.clientinterfaces;

import java.util.List;

import javax.ws.rs.GET;

import io.openliberty.standalone.rest.client.entities.Pet;

public interface PetService {

    @GET
    List<Pet> allPets();
}