package com.samsung.ds.hbase.configuration;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;

import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class HbaseClientConfig {

    @Bean
    public Connection connect() throws IOException {
        org.apache.hadoop.conf.Configuration conf = HBaseConfiguration.create();
        HBaseConfiguration.create();
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        conf.set("hbase.zookeeper.quorum", "192.28.186.253");
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        conf.set("mapreduce.output.testoutputformat.separator", "^");
        conf.setBoolean("mapreduce.map.output.compress", true);
        conf.set("mapreduce.map.output.compress.codec", "org.apache.hadoop.io.compress.DefaultCodec");
        conf.setBoolean("mapreduce.output.fileoutputformat.compress", true);
        conf.set("mapreduce.output.fileoutputformat.compress.codec", "org.apache.hadoop.io.compress.DefaultCodec");

        return ConnectionFactory.createConnection(conf);
    }
}
