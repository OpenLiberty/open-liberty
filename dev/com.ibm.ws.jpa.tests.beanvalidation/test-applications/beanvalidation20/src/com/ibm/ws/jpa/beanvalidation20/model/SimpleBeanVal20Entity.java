package com.ibm.ws.jpa.beanvalidation20.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;
import javax.validation.constraints.Email;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;

@Entity
public class SimpleBeanVal20Entity {

    @Id
    private int id;

    @Version
    private int version;

    @Email(message = "SimpleBeanVal20Entity.email is not well formed")
    @NotNull(message = "SimpleBeanVal20Entity.email is null")
    private String email;

    @Future(message = "SimpleBeanVal20Entity.futureInstant must be in the future")
    private java.time.Instant futureInstant;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public java.time.Instant getFutureInstant() {
        return futureInstant;
    }

    public void setFutureInstant(java.time.Instant futureInstant) {
        this.futureInstant = futureInstant;
    }
}
