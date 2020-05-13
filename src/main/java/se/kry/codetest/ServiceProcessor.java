/**
 * Manage Service Entity.
 */
package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ServiceProcessor {
    public static final String INSERT_QUERY = "INSERT INTO service VALUES(?,?,?,datetime());";
    public static final String SELECT_ALL_QUERY = "SELECT * FROM SERVICE;";
    public static final String UPDATE_STATUS_QUERY = "UPDATE SERVICE SET STATUS=? WHERE URL=?;";
    public static final String DELETE_QUERY = "DELETE FROM SERVICE WHERE URL = ?;";

    private LinkedList<JsonArray> queryQueue = new LinkedList<>();
    public int CACHE_SIZE = 100;

    private DBConnector dbConnector;

    public ServiceProcessor(DBConnector connector) {
        dbConnector = connector;
    }

    /**
     * Return list of services.
     * @return Services List
     */
    public Future<ResultSet> getServicesList() {
        Promise<ResultSet> resultSetFuture = Promise.promise();
        dbConnector.query(ServiceProcessor.SELECT_ALL_QUERY)
                .onSuccess(result -> resultSetFuture.complete(result))
                .onFailure(event -> resultSetFuture.fail(event));
        return resultSetFuture.future();
    }

    /**
     * Persist the service.
     * @param name
     * @param url
     * @return status of the operation.
     */
    public Future<String> saveService(String name, String url) {
        JsonArray params = new JsonArray().add(name).add(url).add("Unknown");
        Promise<String> result = Promise.promise();
        dbConnector.query(ServiceProcessor.INSERT_QUERY, params)
                .onSuccess(resultSet -> result.complete("Successfully added the service."))
                .onFailure(event -> result.fail("Failed to add the service."));
        return result.future();
    }

    /**
     * Update service status.
     * @param url
     * @param status
     * @return Status of the update operation.
     */
    public Future<String> updateServiceStatus(String url, String status) {
        JsonArray params = new JsonArray().add(status).add(url);
        Promise<String> result = Promise.promise();
        dbConnector.updateQuery(ServiceProcessor.UPDATE_STATUS_QUERY, params)
                .onSuccess(event -> result.complete("Successfully updated the service status:" + url))
                .onFailure(event -> result.fail("Failed to update the service status." + url));
        return result.future();
    }

    /**
     * Update service status batch based on the configured cache Size.
     * When cache is full, a batch request will be processed.
     * @param url
     * @param status
     * @return
     */
    public Future<String> updateServiceStatusBatch(String url, String status) {
        JsonArray param = new JsonArray().add(status).add(url);
        queryQueue.add(param);
        Promise<String> result = Promise.promise();
        if (queryQueue.size() >= CACHE_SIZE) {
            List<JsonArray> batchParams = new ArrayList<>();
            for (int i = 0; i < CACHE_SIZE; i++) {
                batchParams.add(queryQueue.removeFirst());
            }
            dbConnector.updateBatchQuery(ServiceProcessor.UPDATE_STATUS_QUERY, batchParams)
                    .onSuccess(resultSet -> {
                        result.complete("Successfully updated the service status - " + resultSet.getUpdated());
                    })
                    .onFailure(event -> result.fail("Failed to update the service status."));
        }
        return result.future();
    }

    /**
     * Delete the services from the given servicesList.
     * @param servicesList - List of Url strings.
     * @return Status of the operation.
     */
    public Future<String> deleteServices(List servicesList) {
        List<JsonArray> params = new ArrayList<>();
        servicesList.stream().forEach(o -> {
            params.add(new JsonArray().add(o.toString()));
        });
        Promise<String> result = Promise.promise();
        dbConnector.updateBatchQuery(ServiceProcessor.DELETE_QUERY, params)
                .onSuccess(updateResult -> {
                    if (updateResult.getUpdated() > 0) {
                        result.complete("Successfully deleted the selected services.");
                    } else {
                        result.complete("0 rows are deleted.");
                    }
                })
                .onFailure(event -> result.fail("Failed to delete the service."));
        return result.future();
    }
}