package io.openliberty.jpa.data.tests.models;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
//For recreating the issue, remove the comment below which contains @NamedQuery
// @NamedQuery(name = "TEST_OLGH_29319", query = "FROM Annuity WHERE annuityHolderId = :holderId")
public class Annuity {

    @Id
    @GeneratedValue
    private UUID id;

    private String annuityHolderId;

    private double amount;

    @Version
    private long version;

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAnnuityHolderId() {
        return annuityHolderId;
    }

    public void setAnnuityHolderId(String annuityHolderId) {
        this.annuityHolderId = annuityHolderId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    // Convenience method for creating an instance
    public static Annuity of(String annuityHolderId, double amount) {
        Annuity annuity = new Annuity();
        annuity.annuityHolderId = annuityHolderId;
        annuity.amount = amount;
        return annuity;
    }
}

