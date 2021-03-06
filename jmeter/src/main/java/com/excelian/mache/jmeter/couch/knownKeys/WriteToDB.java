package com.excelian.mache.jmeter.couch.knownkeys;

import com.excelian.mache.core.MacheLoader;
import com.excelian.mache.core.SchemaOptions;
import com.excelian.mache.couchbase.builder.CouchbaseProvisioner;
import com.excelian.mache.jmeter.couch.AbstractCouchSamplerClient;
import com.excelian.mache.jmeter.couch.CouchTestEntity;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.util.Map;

import static com.couchbase.client.java.cluster.DefaultBucketSettings.builder;
import static com.excelian.mache.couchbase.builder.CouchbaseProvisioner.couchbase;

/**
 * JMeter test that measures writing directly to the Cassandra backing store.
 */
public class WriteToDB extends AbstractCouchSamplerClient {
    private MacheLoader<String, CouchTestEntity> db;

    @Override
    public void setupTest(JavaSamplerContext context) {
        getLogger().info(getClass().getName() + ".setupTest");

        final Map<String, String> mapParams = extractParameters(context);

        try {
            final String keySpace = mapParams.get("keyspace.name");
            final String couchServer = mapParams.get("couch.server.ip.address");
            final CouchbaseProvisioner<String, CouchTestEntity> provisioner = couchbase()
                .withBucketSettings(builder().name(keySpace).quota(150).build())
                .withNodes(couchServer)
                .withSchemaOptions(SchemaOptions.CREATE_SCHEMA_IF_NEEDED)
                .build();
            db = provisioner.getCacheLoader(String.class, CouchTestEntity.class);

            db.create();// ensure we are connected and schema exists

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
            writeDocumentToDbWithNewData(extractParameters(context));
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

    private void writeDocumentToDbWithNewData(final Map<String, String> params) {

        final String docNumber = params.get("entity.keyNo");
        final String entityValue = params.get("entity.value");
        final String key = "document_" + docNumber;
        final String value = (entityValue.equals("CURRENTTIME")) ? key + "_" + System.currentTimeMillis() : entityValue;

        getLogger().info("Writing to db key=" + key);
        db.put(key, new CouchTestEntity(key, value));
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = super.getDefaultParameters();

        defaultParameters.addArgument("entity.value", "CURRENTTIME");
        return defaultParameters;
    }
}
