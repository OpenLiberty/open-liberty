/**
 *
 */
package jsonb.cdi.web;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

@ApplicationScoped
public class JsonbProducer {

    @Produces
    @ApplicationScoped
    public Jsonb produceJsonb() {
        System.out.println("JsonbProducer.produceJsonb() invoked");
        return JsonbBuilder.create();
    }

}
