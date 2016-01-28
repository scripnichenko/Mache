package com.excelian.mache.mongo;

import com.excelian.mache.core.MacheLoader;
import com.excelian.mache.core.SchemaOptions;
import com.excelian.mache.mongo.builder.MongoConnectionContext;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Loads Mongo data as JSON.
 *
 * @param <K> Key
 * @param <V> Value
 * @implNote This does not use Spring and will directly interface with Mongo
 *           TODO move to mongo V3 api, the older API is deprecated but will work
 *           TODO remove unused generic arguments, some references will break if changed ar present
 */
public class MongoDBJsonCacheLoader<K, V> implements MacheLoader<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBJsonCacheLoader.class);
    private final List<MongoCredential> credentials;
    private final MongoClientOptions clientOptions;
    private final SchemaOptions schemaOptions;
    private final String database;
    private MongoConnectionContext mongoConnectionContext;

    private MongoClient mongoClient;

    /**
     * TODO To be refactored.
     *
     * @param mongoConnectionContext A
     * @param credentials            A
     * @param clientOptions          A
     * @param database               A
     * @param schemaOptions          A
     */
    public MongoDBJsonCacheLoader(MongoConnectionContext mongoConnectionContext,
                                  List<MongoCredential> credentials,
                                  MongoClientOptions clientOptions,
                                  String database, SchemaOptions schemaOptions) {
        this.mongoConnectionContext = mongoConnectionContext;
        this.credentials = credentials;
        this.clientOptions = clientOptions;
        this.schemaOptions = schemaOptions;
        this.database = database.replace("-", "_").replace(" ", "_").replace(":", "_");
    }

    @Override
    public String getName() {
        return String.class.getSimpleName();
    }

    @Override
    public void create() {
        if (mongoClient == null) {
            synchronized (this) {
                if (mongoClient == null) {
                    mongoClient = connect();
                }
            }
        }
    }

    @Override
    public void put(String key, String value) {
        LOG.trace("Saving to mongo key={}, newValue={}", key, value);
        DBObject dbObject = (DBObject) JSON.parse(value);
        DBObject keyObject = new BasicDBObject();
        keyObject.put("_id", key);
        DB database = mongoClient.getDB(this.database); // TODO, cache DB object
        DBCollection collection = database.getCollection(this.database);// TODO, how do we get map name?
        collection.update(keyObject, dbObject, true, false);
    }

    @Override
    public void remove(String key) {
        LOG.trace("Removing from mongo key={}", key);
        DB database = mongoClient.getDB(this.database);
        DBCollection collection = database.getCollection(this.database);
        BasicDBObject query = new BasicDBObject();
        query.put("_id", key);
        collection.remove(query);
    }

    @Override
    public String load(String key) {
        DB database = mongoClient.getDB(this.database);
        DBCollection collection = database.getCollection(this.database);

        BasicDBObject query = new BasicDBObject();
        query.put("_id", key);
        DBObject result = collection.findOne(query);
        if (result == null) {
            return null;
        } else {
            String value = result.toString();
            LOG.trace("Loading from mongo by key {} - result {}", key, value);
            return value;
        }
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            synchronized (this) {
                if (mongoClient != null) {
                    if (schemaOptions.shouldDropSchema()) {
                        mongoClient.dropDatabase(database);
                        LOG.info("Dropped database {}", database);
                    }
                    mongoClient.close();
                    mongoClient = null;
                }
            }
        }
    }

    private MongoClient connect() {
        return new MongoClient(mongoConnectionContext.getConnection(this), credentials, clientOptions);
    }

    @Override
    public String toString() {
        return "MongoDBJsonCacheLoader{"
            + "credentials=" + credentials
            + ", clientOptions=" + clientOptions
            + ", mongoClient=" + mongoClient
            + ", mongoConnectionContext=" + mongoConnectionContext
            + ", schemaOptions=" + schemaOptions
            + ", database='" + database + '\''
            + '}';
    }
}