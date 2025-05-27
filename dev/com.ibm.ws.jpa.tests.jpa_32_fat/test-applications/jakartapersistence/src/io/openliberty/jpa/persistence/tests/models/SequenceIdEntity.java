package io.openliberty.jpa.persistence.tests.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

@Entity
public class SequenceIdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;

    public static SequenceIdEntity of(String name) {
        SequenceIdEntity uuidIdEntity = new SequenceIdEntity();
        uuidIdEntity.setName(name);
        return uuidIdEntity;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
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
