package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.util.List;

public class DBConnector {

    private static final String DB_PATH = "poller.db";
    private final SQLClient client;

    public DBConnector(Vertx vertx) {
        JsonObject config = new JsonObject()
                .put("url", "jdbc:sqlite:" + DB_PATH)
                .put("driver_class", "org.sqlite.JDBC")
                .put("max_pool_size", 30);

        client = JDBCClient.createShared(vertx, config);
    }

    public Future<ResultSet> query(String query) {
        return query(query, new JsonArray());
    }

    /**
     * Query with parameters.
     * @param query
     * @param params
     * @return
     */
    public Future<ResultSet> query(String query, JsonArray params) {
        if (query == null || query.isEmpty()) {
            return Future.failedFuture("Query is null or empty");
        }
        if (!query.endsWith(";")) {
            query = query + ";";
        }

        Future<ResultSet> queryResultFuture = Future.future();

        client.queryWithParams(query, params, result -> {
            if (result.failed()) {
                System.err.println("Failed to perform the operation: " + result.cause());
                queryResultFuture.fail(result.cause());
            } else {
                queryResultFuture.complete(result.result());
            }
        });
        return queryResultFuture;
    }

    /**
     * Update operation for batch requests.
     * @param query
     * @param params
     * @return Future<UpdateResult>
     */
    public Future<UpdateResult> updateBatchQuery(final String query, List<JsonArray> params) {
        if (query == null || query.isEmpty()) {
            return Future.failedFuture("Query is null or empty");
        }

        Future<UpdateResult> queryResultFuture = Future.future();
        client.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.batchWithParams(query, params, result -> {
                    if (result.failed()) {
                        System.err.println("Failed to perform the operation: " + result.cause());
                        queryResultFuture.fail(result.cause());
                    } else {
                        queryResultFuture.complete(new UpdateResult()
                                .setUpdated(result.result().stream().mapToInt(Integer::intValue).sum()));
                    }
                    connection.close(close -> {
                        if (close.failed()) {
                            System.out.println("Failed to close the connection.");
                            queryResultFuture.fail(close.cause());
                        } else {
                            System.out.println("Connection is closed");
                        }
                    });
                });
            }
        });
        return queryResultFuture;
    }

    /**
     * Update query.
     * @param query
     * @param params
     * @return Future<UpdateResult>
     */
    public Future<UpdateResult> updateQuery(String query, JsonArray params) {
        if (query == null || query.isEmpty()) {
            return Future.failedFuture("Query is null or empty");
        }
        if (!query.endsWith(";")) {
            query = query + ";";
        }

        Future<UpdateResult> queryResultFuture = Future.future();

        client.updateWithParams(query, params, result -> {
            if (result.failed()) {
                System.err.println("Failed to perform the operation: " + result.cause());
                queryResultFuture.fail(result.cause());
            } else {
                queryResultFuture.complete(result.result());
            }
        });
        return queryResultFuture;
    }
}