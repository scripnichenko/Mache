package com.excelian.mache.mongo.builder;

import com.excelian.mache.builder.StorageProvisioner;
import com.excelian.mache.builder.storage.ConnectionContext;
import com.excelian.mache.core.MacheLoader;
import com.excelian.mache.core.SchemaOptions;
import com.excelian.mache.mongo.MongoDBCacheLoader;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.springframework.data.mongodb.core.CollectionOptions;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * {@link StorageProvisioner} implementation for Mongo DB.
 */
public class MongoDBProvisioner implements StorageProvisioner {

    private final MongoConnectionContext connectionContext;
    private final List<MongoCredential> mongoCredentials;
    private final MongoClientOptions clientOptions;
    private final String database;
    private final SchemaOptions schemaOptions;
    private final CollectionOptions collectionOptions;

    private MongoDBProvisioner(MongoConnectionContext connectionContext, List<MongoCredential> credentials,
                               MongoClientOptions clientOptions, String database, SchemaOptions schemaOptions,
                               CollectionOptions collectionOptions) {

        this.connectionContext = connectionContext;
        this.mongoCredentials = credentials;
        this.clientOptions = clientOptions;
        this.database = database;
        this.schemaOptions = schemaOptions;
        this.collectionOptions = collectionOptions;
    }


    public static SeedsListBuilder mongodb() {
        return seeds -> database -> {
            final MongoConnectionContext mongoConnectionContext = MongoConnectionContext.getInstance(seeds);
            return new MongoDBProvisionerBuilder(mongoConnectionContext, database);
        };
    }


    public static MongoConnectionContext mongoConnectionContext(ServerAddress... seeds) {
        return MongoConnectionContext.getInstance(seeds);
    }

    @Override
    public <K, V> MacheLoader<K, V> getCacheLoader(Class<K> keyType, Class<V> valueType) {
        return new MongoDBCacheLoader<>(keyType, valueType, connectionContext, mongoCredentials, clientOptions, database,
                schemaOptions, collectionOptions);
    }

    /**
     * Forces seeds to be provided.
     */
    public interface SeedsListBuilder {
        DatabaseNameBuilder withSeeds(ServerAddress... seeds);
    }

    /**
     * Forces database name to be provided.
     */
    public interface DatabaseNameBuilder {
        MongoDBProvisionerBuilder withDatabase(String database);
    }

    /**
     * A builder with defaults for a Mongo DB cluster.
     */
    public static class MongoDBProvisionerBuilder {
        private final MongoConnectionContext connectionContext;
        private final String database;
        private List<MongoCredential> mongoCredentials = Collections.emptyList();
        private MongoClientOptions mongoClientOptions = MongoClientOptions.builder().build();
        private SchemaOptions schemaOptions = SchemaOptions.USE_EXISTING_SCHEMA;
        private CollectionOptions collectionOptions = null;

        private MongoDBProvisionerBuilder(MongoConnectionContext connectionContext, String database) {
            this.connectionContext = connectionContext;
            this.database = database;
        }

        public MongoDBProvisionerBuilder withMongoCredentials(List<MongoCredential> mongoCredentials) {
            this.mongoCredentials = mongoCredentials;
            return this;
        }

        public MongoDBProvisionerBuilder withMongoClientOptions(MongoClientOptions mongoClientOptions) {
            this.mongoClientOptions = mongoClientOptions;
            return this;
        }

        public MongoDBProvisionerBuilder withSchemaOptions(SchemaOptions schemaOptions) {
            this.schemaOptions = schemaOptions;
            return this;
        }

        public MongoDBProvisionerBuilder withCollectionOptions(CollectionOptions collectionOptions) {
            this.collectionOptions = collectionOptions;
            return this;

        }

        public MongoDBProvisioner build() {
            return new MongoDBProvisioner(connectionContext, mongoCredentials, mongoClientOptions, database, schemaOptions,
                    collectionOptions);
        }
    }
}
