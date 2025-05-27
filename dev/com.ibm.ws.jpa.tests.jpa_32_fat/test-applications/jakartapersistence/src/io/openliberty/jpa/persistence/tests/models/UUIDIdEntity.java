package io.openliberty.jpa.persistence.tests.models;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
public class UUIDIdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    public static UUIDIdEntity of(String name) {
        UUIDIdEntity uuidIdEntity = new UUIDIdEntity();
        uuidIdEntity.setName(name);
        return uuidIdEntity;
    }

    /**
     * @return the id
     */
    public UUID getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    @PrePersist
    private void prePersist() {
        if (this.id == null) {
            throw new IllegalStateException("Inside '@PrePersist', 'ID' is null");
        }
    }

}
