package com.excelian.mache.jmeter.cassandra;

import com.datastax.driver.core.Cluster;
import com.excelian.mache.cassandra.DefaultCassandraConfig;
import com.excelian.mache.core.SchemaOptions;
import com.excelian.mache.jmeter.MacheAbstractJavaSamplerClient;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import com.excelian.mache.cassandra.CassandraCacheLoader;

import java.util.Map;

public class ReadFromCassandra extends MacheAbstractJavaSamplerClient {

    private CassandraCacheLoader<String, CassandraTestEntity> db;

    @Override
    public void setupTest(JavaSamplerContext context) {
        getLogger().info("ReadFromCassandra.setupTest");

        Map<String, String> mapParams = extractParameters(context);
        String keySpace = mapParams.get("keyspace.name");

        try {
            final DefaultCassandraConfig config = new DefaultCassandraConfig();
            Cluster cluster = CassandraCacheLoader.connect(
                mapParams.get("server.ip.address"),
                mapParams.get("cluster.name"),
                9042, config);
            db = new CassandraCacheLoader<>(CassandraTestEntity.class, cluster,
                SchemaOptions.CREATESCHEMAIFNEEDED, keySpace, config);
            db.create();//ensure we are connected and schema exists
        } catch (Exception e) {
            getLogger().error("Error connecting to cassandra", e);
        }
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        if (db != null) db.close();
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        Map<String, String> mapParams = extractParameters(context);
        SampleResult result = new SampleResult();
        boolean success = false;

        result.sampleStart();

        try {
            String keyValue = mapParams.get("entity.key");
            CassandraTestEntity entity = db.load(keyValue);

            if (entity == null) {
                throw new Exception("No data found in db for key value of " + keyValue);
            }

            result.setResponseMessage("Read " + entity.pkString + " from database");
            success = true;
        } catch (Exception e) {
            setupResultForError(result, e);
            return result;
        }

        result.sampleEnd();
        result.setSuccessful(success);
        return result;
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument("keyspace.name", "JMeterReadThrough");
        defaultParameters.addArgument("server.ip.address", "10.28.1.140");
        defaultParameters.addArgument("cluster.name", "BluePrint");
        defaultParameters.addArgument("entity.key", "K1");
        return defaultParameters;
    }

}