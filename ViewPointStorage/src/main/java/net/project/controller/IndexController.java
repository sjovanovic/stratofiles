package net.project.controller;

import com.google.common.collect.ImmutableMap;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.CqlResult;
import com.netflix.astyanax.recipes.storage.CassandraChunkedStorageProvider;
import com.netflix.astyanax.recipes.storage.ChunkedStorage;
import com.netflix.astyanax.recipes.storage.ChunkedStorageProvider;
import com.netflix.astyanax.recipes.storage.ObjectMetadata;
import com.netflix.astyanax.serializers.ByteSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Default controller
 */
public class IndexController extends Controller implements ControllerInterface {


    AstyanaxContext<Keyspace> context;
    String clusterName = "Test Cluster";
    String keyspaceName = "KeyspaceName";
    String columnFamilyName = "storage4";
    Keyspace keyspace;

	// default method for all unmatched URL-s
	public void IndexAction(){
		//render("header");
        echo("Setup: GET /setup \nUsage: \nPOST /upload?file=example.png ---binary data --- \nGET /download?file=example.png");
	}

    public void SetupAction(){
        setItUp();

        ColumnFamily<String, String> STORAGE_CF =
                new ColumnFamily<String, String>(columnFamilyName,
                        StringSerializer.get(), StringSerializer.get(), ByteSerializer.get());
        try {
            keyspace.createColumnFamily(STORAGE_CF, null);
            /*
            keyspace.createColumnFamily(STORAGE_CF, ImmutableMap.<String, Object>builder()
                    .put("comparator", "UTF8Type")
                    .put("key_validation_class", "UTF8Type")
                    .put("default_validation_class", "BytesType")
                    .build()
            );
            */
            echo("Setup complete.");
        } catch (ConnectionException e) {
            e.printStackTrace();
            echo("Setup complete.");
        }
        //execCql("CREATE KEYSPACE IF NOT EXISTS "+keyspaceName+" WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };");
        //execCql("CREATE COLUMN FAMILY IF NOT EXISTS storage WITH comparator = UTF8Type AND key_validation_class=UTF8Type AND default_validation_class = BytesType");
    }


    public void UploadAction(){
        setItUp();
        Map<String, String> query;
        try {
            query = splitQuery(this.request.getQueryString());
            String fileName; // this.request.getParameter("file");
            if(query.containsKey("file")){
                fileName = query.get("file");
                ChunkedStorageProvider provider
                        = new CassandraChunkedStorageProvider(
                        keyspace,
                        columnFamilyName);
                try {
                    InputStream inputStream = this.request.getInputStream();
                    ObjectMetadata meta = ChunkedStorage.newWriter(provider, fileName, inputStream)
                            .withChunkSize(0x1000)    // Optional chunk size to override
                                    // the default for this provider
                            .withConcurrencyLevel(8)  // Optional. Upload chunks in 8 threads
                                    //.withTtl(60)              // Optional TTL for the entire object
                            .call();

                    echo("File is uploaded. " + meta.getAttributes());
                } catch (IOException e) {
                    echo("Upload failed: "+e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    echo("Upload failed: "+e.getMessage());
                    e.printStackTrace();
                }
            }else{
                echo("Missing 'file' query parameter.");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void DownloadAction(){
        String fileName = this.request.getParameter("file");

        setItUp();
        ChunkedStorageProvider provider = new CassandraChunkedStorageProvider(keyspace, columnFamilyName);

        // For this example we create a byte array output stream, which requires us to first read
        // the object size.   You don't need to do this if you are reading into a FileOutputStream
        ObjectMetadata meta = null;
        try {
            meta = ChunkedStorage.newInfoReader(provider, fileName).call();
        } catch (Exception e) {
            echo("Download failed: "+e.getMessage());
            e.printStackTrace();
        }
        // get the size of the file in bytes
        int contentLength = 0;
        try{
            contentLength = meta.getObjectSize().intValue();
        }catch (Exception e) {
            echo("Download failed: "+e.getMessage());
            e.printStackTrace();
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream(contentLength);

        // Read the file
        try {
            meta = ChunkedStorage.newReader(provider, fileName, os)
                    .withBatchSize(11)       // Randomize fetching blocks within a batch.
                    //.withRetryPolicy(new ExponentialBackoffWithRetry(250,20))
                            // Retry policy for when a chunk isn't available.
                            //  This helps implement retries in a cross region
                            //  setup where replication may be slow
                    .withConcurrencyLevel(2) // Download chunks in 2 threads.  Be careful here.
                            //  Too many client + too many thread = Cassandra not happy
                    .call();

            // write file to output
            this.response.setContentLength(contentLength);
            os.writeTo(this.response.getOutputStream());
        } catch (Exception e) {
            echo("Download failed: "+e.getMessage());
            e.printStackTrace();
        }
    }

    private void setItUp() {
        if(context == null){
            context = new AstyanaxContext.Builder()
                    .forCluster(clusterName)
                    .forKeyspace(keyspaceName)
                    .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                            .setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE)
                    )
                    .withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("MyConnectionPool")
                            .setPort(9160)
                            .setMaxConnsPerHost(1)
                            .setSeeds("127.0.0.1:9160")
                    )
                    .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                            .setCqlVersion("3.4.0")
                            .setTargetCassandraVersion("3.2.1"))
                    .withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
                    .buildKeyspace(ThriftFamilyFactory.getInstance());

            context.start();
            keyspace = context.getClient();

            try {
                keyspace.createKeyspaceIfNotExists(ImmutableMap.<String, Object>builder()
                        .put("strategy_options", ImmutableMap.<String, Object>builder()
                                .put("replication_factor", "1")
                                .build())
                        .put("strategy_class", "SimpleStrategy")
                        .build()
                );
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
        }
    }

    private OperationResult execCql(String cql){
        // CREATE COLUMN FAMILY IF NOT EXISTS storage WITH comparator = UTF8Type AND key_validation_class=UTF8Type
        setItUp();  // maybe not needed
        ColumnFamily<String, String> STORAGE_CF = new ColumnFamily<String, String>(columnFamilyName, StringSerializer.get(), StringSerializer.get(), ByteSerializer.get());
        OperationResult<CqlResult<String, String>> result;
        try {
            result = keyspace
                    .prepareQuery(STORAGE_CF)
                    .withCql(cql)
                    .execute();
            return result;
        } catch (ConnectionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

}
