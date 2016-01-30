package com.excelian.mache.jmeter.mongo.knownkeys;

import com.excelian.mache.builder.storage.ConnectionContext;
import com.excelian.mache.core.Mache;
import com.excelian.mache.core.MacheLoader;
import com.excelian.mache.core.SchemaOptions;
import com.excelian.mache.jmeter.mongo.AbstractMongoSamplerClient;
import com.excelian.mache.jmeter.mongo.MongoTestEntity;
import com.mongodb.ServerAddress;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.util.List;
import java.util.Map;

import static com.excelian.mache.builder.MacheBuilder.mache;
import static com.excelian.mache.guava.GuavaMacheProvisioner.guava;
import static com.excelian.mache.mongo.builder.MongoDBProvisioner.mongoConnectionContext;
import static com.excelian.mache.mongo.builder.MongoDBProvisioner.mongodb;

/**
 * JMeter test that measures reading directly from the backing Mongo store.
 */
public class ReadFromDB extends AbstractMongoSamplerClient {

    private MacheLoader<String, MongoTestEntity> db;

    @Override
    public void setupTest(JavaSamplerContext context) {
        getLogger().info("ReadFromDB.setupTest");

        final Map<String, String> mapParams = extractParameters(context);

        try {
            final Mache<String, MongoTestEntity> mache = mache(String.class, MongoTestEntity.class)
                .cachedBy(guava())
                .storedIn(mongodb()
                    .withSeeds(new ServerAddress(mapParams.get("mongo.server.ip.address"), 27017))
                    .withDatabase(mapParams.get("keyspace.name"))
                    .withSchemaOptions(SchemaOptions.CREATE_SCHEMA_IF_NEEDED)
                    .build()).withNoMessaging().macheUp();
            db = mache.getCacheLoader();
            db.create();

        } catch (Exception e) {
            getLogger().error("Error connecting to cassandra", e);
        }
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        if (db != null) {
            db.close();
        }
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        final SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            readDocumentFromDb(extractParameters(context));
            result.sampleEnd();
            result.setSuccessful(true);
        } catch (Exception e) {
            result.sampleEnd();
            result.setSuccessful(false);
            getLogger().error("Error running test", e);
            result.setResponseMessage(e.toString());
        }
        return result;
    }

    private void readDocumentFromDb(final Map<String, String> params) throws Exception {
        final String docNumber = params.get("entity.keyNo");
        final String key = "document_" + docNumber;
        db.load(key);
    }
}
