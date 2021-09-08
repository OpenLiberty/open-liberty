package com.ibm.ws.jpa.beanvalidation20.model;

public class SimpleBeanVal20XMLEntity {

    private int id;

    private int version;

    private String email;

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
