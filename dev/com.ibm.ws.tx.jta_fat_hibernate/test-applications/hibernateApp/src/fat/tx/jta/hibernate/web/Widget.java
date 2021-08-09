package fat.tx.jta.hibernate.web;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "Widget")
public class Widget {

    @Id
    public int id;

    public Widget() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
