package com.ibm.ws.jpa.beanvalidation.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

@Entity
public class SimpleBeanValEntity {

    @Id
    private int id;

    @Version
    private int version;

    @NotNull(message = "SimpleBeanValEntity.name is null")
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
