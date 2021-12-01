package com.ibm.ws.jpa.overridepersistencecontext.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class SimpleOverridePersistenceContextEntity {

    @Id
    private int id;

    @Version
    private int version;

    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
