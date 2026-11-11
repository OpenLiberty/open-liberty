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

import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

import org.eclipse.persistence.annotations.CascadeOnDelete;

/**
 * Entity with multiple levels of embeddables and a collection attribute
 * on the embeddables that can be used to test the Fetching annotation.
 *
 * Mail -> Recipient -> PostalAddress -> lines
 * Mail -> Sender -> PostalAddress -> lines
 */
@Entity
public class Mail {
    @Id
    public long id;

    @Column(nullable = false)
    public int postage; // cents

    @AttributeOverrides // avoids column name collision
    ({
       @AttributeOverride(name = "address.city",
                          column = @Column(name = "recipientCity")),
       @AttributeOverride(name = "address.stateCode",
                          column = @Column(name = "recipientStateCode")),
       @AttributeOverride(name = "address.zip",
                          column = @Column(name = "recipientZip"))
    })
    @Embedded
    public Recipient recipient;

    @AttributeOverrides // avoids column name collision
    ({
       @AttributeOverride(name = "address.city",
                          column = @Column(name = "senderCity")),
       @AttributeOverride(name = "address.stateCode",
                          column = @Column(name = "senderStateCode")),
       @AttributeOverride(name = "address.zip",
                          column = @Column(name = "senderZip"))
    })
    @Embedded
    public Sender sender;

    @Column(nullable = false)
    public float weight; // ounces

    @Embeddable
    public static class Recipient { //
        @Embedded //
        public RecipientAddress address;
    }

    @Embeddable
    public static class RecipientAddress {
        public String city;

        public String stateCode;

        public Integer zip;

        @ElementCollection
        @CascadeOnDelete // for EclipseLink
        @CollectionTable //
        (name = "RecipientLines",
         joinColumns = @JoinColumn //
         (name = "Mail_id",
          foreignKey = @ForeignKey //
          (name = "FK_RecipientLines_Mail_id",
           foreignKeyDefinition = """
                           FOREIGN KEY (Mail_id) REFERENCES Mail(id) ON DELETE CASCADE
                           """)))
        @OrderColumn // use list that allows fetch multiple, not Hibernate "bag"
        public List<String> lines;

        public static RecipientAddress of(String city,
                                          String stateCode,
                                          Integer zip,
                                          List<String> lines) {
            RecipientAddress address = new RecipientAddress();
            address.city = city;
            address.stateCode = stateCode;
            address.zip = zip;
            address.lines = lines;
            return address;
        }
    }

    @Embeddable
    public static class Sender { //
        @Embedded //
        public SenderAddress address;
    }

    @Embeddable
    public static class SenderAddress {
        public String city;

        public String stateCode;

        public Integer zip;

        @ElementCollection
        @CascadeOnDelete // for EclipseLink
        @CollectionTable //
        (name = "SenderLines",
         joinColumns = @JoinColumn //
         (name = "Mail_id",
          foreignKey = @ForeignKey //
          (name = "FK_SenderLines_Mail_id",
           foreignKeyDefinition = """
                           FOREIGN KEY (Mail_id) REFERENCES Mail(id) ON DELETE CASCADE
                           """)))
        @OrderColumn // use list that allows fetch multiple, not Hibernate "bag"
        public List<String> lines;

        public static SenderAddress of(String city,
                                       String stateCode,
                                       Integer zip,
                                       List<String> lines) {
            SenderAddress address = new SenderAddress();
            address.city = city;
            address.stateCode = stateCode;
            address.zip = zip;
            address.lines = lines;
            return address;
        }
    }

    public static Mail of(long id,
                          int postage,
                          float weight,
                          RecipientAddress recipientAddress,
                          SenderAddress senderAddress) {
        Mail mail = new Mail();
        mail.id = id;
        mail.postage = postage;
        mail.recipient = new Recipient();
        mail.recipient.address = recipientAddress;
        mail.sender = new Sender();
        mail.sender.address = senderAddress;
        mail.weight = weight;
        return mail;
    }

    /**
     * Textual description. For example:
     *
     * Mail#1 82¢ 0.25oz from: Sender Address Line 1 to: Recipient Address Line 1
     */
    @Override
    public String toString() {
        return new StringBuilder("Mail#")
                        .append(id)
                        .append(' ')
                        .append(postage)
                        .append("¢ ")
                        .append(weight)
                        .append("oz from: ")
                        .append(sender == null || //
                                sender.address == null || //
                                sender.address.lines == null || //
                                sender.address.lines.isEmpty() //
                                                ? "?" //
                                                : sender.address.lines.get(0))
                        .append(" to: ")
                        .append(recipient == null || //
                                recipient.address == null || //
                                recipient.address.lines == null || //
                                recipient.address.lines.isEmpty() //
                                                ? "?" //
                                                : recipient.address.lines.get(0))
                        .toString();
    }
}
