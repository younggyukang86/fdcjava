package com.samsung.ds.hbase.utils;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import java.io.IOException;

public class HbaseClient {
    public final Connection connection;
    private static HbaseClient instance = null;

    private HbaseClient() throws IOException {
        ConfigProperties configProperties = ConfigProperties.getInstance();
        String zkPort = configProperties.getValue("hbase.config.zk.port");
        String zkQuorum = configProperties.getValue("hbase.config.zk.quorum");
        String zkZnode = configProperties.getValue("hbase.config.zk.znode");

        org.apache.hadoop.conf.Configuration conf = HBaseConfiguration.create();
        HBaseConfiguration.create();
        conf.set("hbase.zookeeper.property.clientPort", zkPort);
        conf.set("hbase.zookeeper.quorum", "192.28.186.253");
        conf.set("hbase.zookeeper.quorum", zkQuorum);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        conf.set("zookeeper.znode.parent", zkZnode);
        conf.set("mapreduce.output.testoutputformat.separator", "^");
        conf.setBoolean("mapreduce.map.output.compress", true);
        conf.set("mapreduce.map.output.compress.codec", "org.apache.hadoop.io.compress.DefaultCodec");
        conf.setBoolean("mapreduce.output.fileoutputformat.compress", true);
        conf.set("mapreduce.output.fileoutputformat.compress.codec", "org.apache.hadoop.io.compress.DefaultCodec");

        connection = ConnectionFactory.createConnection(conf);
    }

    public synchronized static HbaseClient getInstance() throws IOException {
        if (instance == null) {
            instance = new HbaseClient();
        }

        return instance;
    }
}
