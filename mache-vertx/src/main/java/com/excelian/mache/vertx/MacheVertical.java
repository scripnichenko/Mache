package com.excelian.mache.vertx;

import com.excelian.mache.core.Mache;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A vertx vertical is synonymous to an Actor. This class will register the routes that we are interested in
 * and forward calls to a MacheInstanceCache
 * <p>
 * TODO threading/synchronisation
 */
public class MacheVertical {

    private static final Logger LOG = LoggerFactory.getLogger(MacheVertical.class);
    private final MacheInstanceCache instanceCache;

    public MacheVertical(MacheInstanceCache instanceCache) {
        this.instanceCache = instanceCache;
    }

    public void run() {
        // Entry point to the REST service
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        router.route().handler(StaticHandler.create());
        router.route("/map/*").handler(BodyHandler.create());

        router.exceptionHandler(this::handleException);

        router.route(HttpMethod.DELETE, "/map/:mapName")
                .handler(this::handleDeleteMap);
        router.route(HttpMethod.GET, "/map/:mapName/:key")
                .order(-1)
                .handler(this::handleGetMap); // avoid static handler interception
        router.route(HttpMethod.PUT, "/map/:mapName/:key")
                .handler(this::handlePutMap);

        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(8080, handler -> {
                    if (handler.succeeded()) {
                        System.out.println("http://localhost:8080/");
                    } else {
                        System.err.println("Failed to listen on port 8080");
                    }
                });
    }

    private void handleException(Throwable throwable) {
        LOG.error("Vertx server exception {}", throwable);
    }

    private void handleGetMap(RoutingContext req) {
        String mapName = req.request().getParam("mapName");
        String key = req.request().getParam("key");

        try {
            String value = instanceCache.getKey(mapName, key);
            req.response().end(value == null ? "" : value);
        } catch (Exception e) {
            // key not present
            req.response()
                    .setStatusCode(400)
                    .end("key not found");
        }
    }

    private void handleDeleteMap(RoutingContext req) {
        String mapName = req.request().getParam("mapName");

        try {
            Mache<String, String> old = instanceCache.deleteMap(mapName);
            if(old == null){
                req.response()
                        .setStatusCode(400)
                        .end("map does not exist");
            } else {
                req.response().end(String.format("removed %s map", mapName));
            }
        } catch (Exception e) {
            req.fail(500);
        }
    }

    private void handlePutMap(RoutingContext req) {
        String mapName = req.request().getParam("mapName");
        String key = req.request().getParam("key");
        String value = req.getBodyAsString();

        try {
            instanceCache.putKey(mapName, key, value);
            req.response().end(String.format("PUT %s %s", mapName, key));
        } catch (Exception e){
            req.fail(500);
        }
    }
}
