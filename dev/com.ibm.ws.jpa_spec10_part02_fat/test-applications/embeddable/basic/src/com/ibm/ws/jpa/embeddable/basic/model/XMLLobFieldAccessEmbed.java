package com.ibm.ws.jpa.embeddable.basic.model;

@SuppressWarnings("serial")
public class XMLLobFieldAccessEmbed implements java.io.Serializable {

    private String clobValueFA;

    public XMLLobFieldAccessEmbed() {
    }

    public XMLLobFieldAccessEmbed(String clobValueFA) {
        this.clobValueFA = clobValueFA;
    }

    public String getClobValueFA() {
        return this.clobValueFA;
    }

    public void setClobValueFA(String clobValueFA) {
        this.clobValueFA = clobValueFA;
    }

    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLLobFieldAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    public String toString() {
        return "clobValueFA=" + clobValueFA;
    }

}
