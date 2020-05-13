package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

public class BackgroundPoller {

    private static String FAIL = "FAIL";
    private static String OK = "OK";
    private Vertx vertx;
    private ServiceProcessor serviceProcessor;

    public BackgroundPoller(Vertx vertx, ServiceProcessor serviceProcessor) {
        this.vertx = vertx;
        this.serviceProcessor = serviceProcessor;
    }

    /**
     * Poll all the services available.
     */
    public void pollServices() {
        serviceProcessor.getServicesList().onComplete(event -> {
            if (event.succeeded()) {
                for (JsonObject row : event.result().getRows()) {
                    String site = row.getString("url");
                    try {
                        //A get request is send with timeout of 5000 sec,
                        //and update the service status based on the response.
                        WebClient.create(vertx)
                                .getAbs(site)
                                .timeout(5000)
                                .send(ar -> {
                                    if (ar.succeeded()) {
                                        serviceProcessor.updateServiceStatus(site, OK);
                                    } else {
                                        serviceProcessor.updateServiceStatus(site, FAIL);
                                    }
                                });
                    } catch (Exception ex) {
                        System.err.println(ex.getMessage());
                        serviceProcessor.updateServiceStatus(site, FAIL);
                    }
                }
            }
        });
    }
}
