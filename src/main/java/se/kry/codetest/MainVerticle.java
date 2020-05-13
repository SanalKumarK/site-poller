package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MainVerticle extends AbstractVerticle {

    private static final String urlRegEx = "((http?|https|ftp|file)://)((W|w){3}.)?[a-zA-Z0-9]+\\.[a-zA-Z]+[:\\d]*[/=\\w\\?]*";
    private DBConnector connector;
    private BackgroundPoller poller;
    private ServiceProcessor serviceProcessor;

    @Override
    public void start(Future<Void> startFuture) {
        connector = new DBConnector(vertx);
        serviceProcessor = new ServiceProcessor(connector);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        setRoutes(router);
        triggerPolling();

        vertx
                .createHttpServer()
                .requestHandler(router)
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        System.out.println("KRY code test service started");
                        startFuture.complete();
                    } else {
                        startFuture.fail(result.cause());
                    }
                });
    }

    /**
     * Trigger the polling, operation with an interval of 1 min.
     */
    private void triggerPolling() {
        poller = new BackgroundPoller(vertx, serviceProcessor);
        vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices());
    }

    private void setRoutes(Router router) {
        router.route("/*").handler(StaticHandler.create());

        router.get("/service").handler(req -> {
            serviceProcessor.getServicesList().onComplete(event -> {
                if (event.succeeded()) {
                    //reload services map.
                    req.response()
                            .putHeader("content-type", "application/json")
                            .end(new JsonArray(event.result().getRows()).encode());
                } else {
                    req.response()
                            .putHeader("content-type", "application/json")
                            .end(new JsonArray().encode());
                }
            });
        });
        router.post("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            String msg = validateInput(jsonBody);
            if (msg != null) {
                //If validation failed, return the reason for the failure.
                req.response()
                        .putHeader("content-type", "text/plain")
                        .end(msg);
                return;
            }
            //save the service, and return the result.
            serviceProcessor.saveService(jsonBody.getString("name"),
                    jsonBody.getString("url")).onComplete(result -> {
                req.response()
                        .putHeader("content-type", "text/plain")
                        .end(result.result());
            });
        });
        router.delete("/service").handler(req -> {
            //Process delete service request.
            JsonArray servicesList = req.getBodyAsJsonArray();
            if (servicesList == null || servicesList.isEmpty()) {
                req.response()
                        .putHeader("content-type", "text/plain")
                        .end("Invalid list of services.");
                return;
            }
            serviceProcessor.deleteServices(servicesList.getList()).onComplete(event -> {
                req.response()
                        .putHeader("content-type", "text/plain")
                        .end(event.result());
            });
        });
    }

    private String validateInput(JsonObject jsonBody) {
        String error = null;
        String name = jsonBody.getString("name");
        String url = jsonBody.getString("url");
        if (name == null || name.isEmpty()) {
            error = "Please provide a valid service name.\n";
        }
        if (url == null || url.isEmpty() || !url.matches(urlRegEx)) {
            if (error != null) {
                error = error + "Please provide a valid URL.";
            } else {
                error = "Please provide a valid URL.";
            }
        }
        return error;
    }
}