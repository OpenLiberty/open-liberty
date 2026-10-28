/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.v1_1.web;

import static jakarta.data.repository.By.ID;

import java.util.List;
import java.util.Optional;

import jakarta.data.repository.By;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Fetching;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;
import jakarta.data.repository.stateful.Detach;
import jakarta.data.repository.stateful.Persist;

/**
 * Repository for an entity with multiple levels of embeddables and a
 * collection attribute on the embeddables that can be used to test the
 * Fetching annotation.
 * TODO switch to stateless repository once Hibernate bug 20997 is fixed
 * https://hibernate.atlassian.net/browse/HHH-20997
 */
@Repository(dataStore = "MyDataStore")
public interface Mailbox extends DataRepository<Mail, Long> {
    @Detach
    void detach(Mail mail);

    @Delete
    void empty();

    @Persist
    void send(Mail mail);

    @Find
    @Fetching("sender.address.lines")
    @Select("sender.address.lines")
    Optional<List<String>> senderLines(@By(ID) long id);

    @Find
    @Fetching("Recipient.address_lines")
    @Fetching("SENDER.ADDRESS.LINES")
    Optional<Mail> withBothLines(@By(ID) long id);

    @Find
    @Fetching("recipient_address_lines")
    Optional<Mail> withRecipientLines(@By(ID) long id);
}
