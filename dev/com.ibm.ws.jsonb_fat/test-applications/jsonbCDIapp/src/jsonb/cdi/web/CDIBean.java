package jsonb.cdi.web;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;

@ApplicationScoped
public class CDIBean {

    @Inject
    Jsonb jsonb;

    public Jsonb getJsonb() {
        return jsonb;
    }

}
