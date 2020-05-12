package com.ibm.ws.jpa.embeddable.basic.model;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.Lob;

@Embeddable
@Access(AccessType.FIELD)
@SuppressWarnings("serial")
public class LobFieldAccessEmbed implements java.io.Serializable {

    @Lob
    private String clobValueFA;

    public LobFieldAccessEmbed() {
    }

    public LobFieldAccessEmbed(String clobValueFA) {
        this.clobValueFA = clobValueFA;
    }

    public String getClobValueFA() {
        return this.clobValueFA;
    }

    public void setClobValueFA(String clobValueFA) {
        this.clobValueFA = clobValueFA;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof LobFieldAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "clobValueFA=" + clobValueFA;
    }

}
