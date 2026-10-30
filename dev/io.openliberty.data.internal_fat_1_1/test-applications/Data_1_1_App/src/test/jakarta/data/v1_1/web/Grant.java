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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * An entity representing a financial grant awarded by a Giver to a Grantee.
 *
 * This entity is designed to test:
 * <ul>
 * <li>The {@code Fetching} annotation: the {@code giver} association is lazy
 * and can be eagerly loaded per query via
 * {@code @Fetching("giver")}.</li>
 * <li>QueryOptions with entityGraph: the {@code recipient} association is lazy
 * and can be eagerly loaded via the named entity graph
 * {@code "Grant.withRecipient"}.</li>
 * <li>A third lazy attribute that is left lazy: the {@code reportingDates}
 * element collection is lazy by default and is not fetched by either
 * of the above mechanisms, allowing tests to verify it stays
 * uninitialized.</li>
 * </ul>
 */
@Entity
@NamedEntityGraph(name = "Grant.withRecipient",
                  attributeNodes = @NamedAttributeNode("recipient"))
@Table(name = "Grants") // GRANT is a reserved SQL keyword
public class Grant {

    @Column(nullable = false)
    public int amount; // in USD

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "giver_id",
                nullable = false,
                foreignKey = @ForeignKey(name = "FK_Grant_giver"))
    public Giver giver;

    @Id
    public long id;

    @Column(nullable = false)
    @Version
    public LocalDateTime lastUpdated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id",
                nullable = false,
                foreignKey = @ForeignKey(name = "FK_Grant_recipient"))
    public Grantee recipient;

    /**
     * Scheduled reporting due-dates for this grant. This collection is lazy
     * by default and is not fetched when only {@code @Fetching("giver")} or
     * the {@code "Grant.withRecipient"} entity graph is applied, making it
     * suitable for testing that a third lazy attribute remains uninitialized.
     */
    @ElementCollection
    @CollectionTable //
    (name = "Grant_reportingDates",
     joinColumns = @JoinColumn //
     (name = "Grant_id",
      foreignKey = @ForeignKey //
      (name = "FK_Grant_reportingDates",
       foreignKeyDefinition = //
       "FOREIGN KEY (Grant_id) REFERENCES Grants(id) ON DELETE CASCADE")))
    @Column(name = "reportingDate", nullable = false)
    @OrderColumn
    public List<LocalDate> reportingDates;

    @Column(nullable = false)
    public boolean renewable;

    @Column(nullable = false)
    public String title;

    public static Grant of(long id,
                           String title,
                           int amount,
                           boolean renewable,
                           Giver giver,
                           Grantee recipient,
                           List<LocalDate> reportingDates) {
        Grant g = new Grant();
        g.id = id;
        g.title = title;
        g.amount = amount;
        g.renewable = renewable;
        g.giver = giver;
        g.recipient = recipient;
        g.reportingDates = reportingDates;
        return g;
    }

    /**
     * Textual description. For example:
     *
     * Grant#1 "STEM Outreach Initiative" $50000.00 renewable
     */
    @Override
    public String toString() {
        return new StringBuilder("Grant#")
                        .append(id)
                        .append(" \"")
                        .append(title)
                        .append("\" $")
                        .append(amount)
                        .append(renewable ? " renewable" : " non-renewable")
                        .toString();
    }
}
