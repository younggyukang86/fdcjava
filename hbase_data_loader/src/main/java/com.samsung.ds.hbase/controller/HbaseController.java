package com.samsung.ds.hbase.controller;

import com.jcraft.jsch.IO;
import javassist.bytecode.ByteArray;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;

import org.apache.hadoop.hbase.util.Bytes;
import org.codehaus.jackson.map.Serializers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xerial.snappy.Snappy;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hbase")
@Slf4j
public class HbaseController {

    @Autowired
    Connection connection;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Value("${hbase.trace.table-name}")
    String hbaseTableName;

    @Value("${hbase.trace.sample-query-2}")
    String sampleQuery;

    @Value("${hbase.trace.ouput-path}")
    String outputPath;

    @Value("${hbase.trace.ref-value}")
    String refValueQualifier;

    @GetMapping("/getTable")
    public ResponseEntity getTable(@RequestParam(defaultValue = "1", required = true) String num,
                                   @RequestParam(defaultValue = "1", required = true) String rowCount) throws IOException {
        Timestamp totalStart = new Timestamp(System.currentTimeMillis());
        long totalCount = 0L;
        int numberOfThreads = Integer.valueOf(num);
        int rowKeySetCount = Integer.valueOf(rowCount);

        // get rowkeys from mes
        List<String> rowKeys = getRowKeys();
        List<String> tmpRowKeys = new ArrayList<>();
        for (int i = 0; i < rowKeySetCount; i++) {
            tmpRowKeys.addAll(rowKeys);
        }
        List<List<String>> partRowKeys = new ArrayList<>();
        int idx1 = (int) Math.ceil((double) tmpRowKeys.size() / numberOfThreads);
        log.info("idx1 :{}", idx1);
        for (int i = 1; i <= numberOfThreads; i++) {
            List<String> result = new ArrayList<>();
            int idx2 = idx1 * i > tmpRowKeys.size() ? tmpRowKeys.size() : idx1 * i;
            log.info("idx2 :{}", idx2);
            for (int j = (i-1) * idx1; j < idx2; j++) {
                result.add(tmpRowKeys.get(j));
            }
            partRowKeys.add(result);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Callable<Object>> callableTasks = new ArrayList();
        for (int i = 0; i < numberOfThreads; i++) {
            int idx = 1;
            Callable<Object> callableTask = () -> {
                return decompressTest(partRowKeys.get(idx));
            };
            callableTasks.add(callableTask);
        }

        try {
            List<Future<Object>> futures = executor.invokeAll(callableTasks);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (Future<Object> future : futures) {
                List<String> datas = (List<String>) future.get();
                for (String data : datas) {
                    baos.write(data.getBytes());
                }
            }
            // write csv file
            File file = new File("/opt/jsm_trace_test", String.format("trace_data_write_%d.csv", System.currentTimeMillis()));
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            Timestamp writeStart = new Timestamp(System.currentTimeMillis());
            try {
                IOUtils.copy(is, new FileOutputStream(file));
                is.close();
                log.info("[CSV FILE] create csv file!!!!");
            } catch (Exception e) {
                // TODO
                    throw new RuntimeException("CSV 파일 생성을 실패하였습니다." + e);
            }
            Timestamp writeEnd = new Timestamp(System.currentTimeMillis());
            log.info("[WRITE] START TIME : {}", writeStart);
            log.info("[WRITE] START TIME : {}", writeEnd);

        } catch (Exception e) {
            log.error("[ERROR] EXECUTOR ERROR :{}", e);
        } finally {
            executor.shutdown();
        }
        log.info("[TOTAL] START TIME :{}", totalStart);
        log.info("[TOTAL] END TIME :{}", new Timestamp(System.currentTimeMillis()));

        return ResponseEntity.ok(totalCount);
    }

    @GetMapping("/getTableDistributeFile")
    public ResponseEntity getTableDistributeFile(@RequestParam(defaultValue = "1", required = true) String num) throws IOException {
        Timestamp totalStart = new Timestamp(System.currentTimeMillis());
        long totalCount = 0L;
        int numberOfThreads = Integer.valueOf(num);
        // get rowkeys from mes
        List<String> rowKeys = getRowKeys();

        List<List<String>> partRowKeys = new ArrayList<>();
        int idx1 = (int) Math.ceil((double) rowKeys.size() / numberOfThreads);
        log.info("idx1 :{}", idx1);
        for (int i = 1; i <= numberOfThreads; i++) {
          List<String> result = new ArrayList<>();
          int idx2 = idx1 * i > rowKeys.size() ? rowKeys.size() : idx1 * i;
          log.info("idx2 : {}", idx2);
          for (int j = (i-1) * idx1; j < idx2; j++) {
              result.add(rowKeys.get(j));
          }
          partRowKeys.add(result);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Callable<Object>> callableTasks = new ArrayList();
        for (int i = 0; i < numberOfThreads; i++) {
            int idx = i;
            Callable<Object> callableTask = () -> {
                return decompressTestDistributeFile(partRowKeys.get(idx), idx);
            };
            callableTasks.add(callableTask);
        }
        try {
            List<Future<Object>> futures = executor.invokeAll(callableTasks);
        } catch (Exception e) {
            log.error("[ERROR] EXECUTOR ERROR : {}", e);
        } finally {
            executor.shutdown();
        }
        log.info("[TOTAL] START TIME :{}", totalStart);
        log.info("[TOTAL] END TIME :{}", new Timestamp(System.currentTimeMillis()));

        return ResponseEntity.ok(totalCount);
    }

    private long decompressTestDistributeFile(List<String> rowKeys, int threadNum) throws IOException {
        Table table = getHbaseTable();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long rowNum = 0;
        for(String rowKey : rowKeys) {
            Result r = getResultFromHbase(rowKey, table);
            for (Cell cell : r.listCells()) {
                String qualifier = hbaseQualifierReplace(new String(cell.getQualifierArray()));
                String columnFamily = new String(cell.getFamilyArray());
                log.info("[HBASE COLUMN INFO] column family : {}, column qualfier : {}", columnFamily, qualifier);
                String refValue = hbaseQualifierReplace(getRefValueFromResult(r));
                if (!qualifier.equals(refValueQualifier)) {
                    // uncompressed trace data
                    String data = new String(Snappy.uncompress(Base64.decodeBase64(new String(cell.getFamilyArray()))));
                    // unnest trace data
                    String[] datas = data.split(",");
                    List<String> c3 = splitData(datas[2]);
                    List<String> c4 = splitData(datas[3]);
                    List<String> c5 = splitData(datas[4]);
                    List<String> c6 = splitData(datas[5]);
                    List<String> c7 = splitData(datas[6]);
                    List<String> c8 = splitData(datas[7]);

                    for (int i=0; i < c3.size(); i++) {
                        String traceData = String.format("%s,%s,%s,%s,%s,%s,%s,%s\n", datas[0], datas[1], c3.get(i), c4.get(i), c5.get(i), c6.get(i), c7.get(i), c8.get(i));
                        String completeTraceData = String.format("%d,%s,%s,%s", rowNum, refValue, qualifier, traceData);
                        rowNum++;
                    }
                }
            }
        }

        File file = new File("/data1/jsm_trace_test", String.format("trace_data_write_%d.csv", System.currentTimeMillis()));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        try {
            IOUtils.copy(is, new FileOutputStream(file));
            is.close();
            log.info("[CSV FILE] create csv file!!!!");
        } catch(Exception e) {
            // TODO
            throw new RuntimeException("CSV 파일 생성을 실패하였습니다." + e);
        }

        return rowNum;
    }

    private List<String> decompressTest(List<String> rowKeys) throws IOException {
        Table table = getHbaseTable();
        List<String> traceDatas = new ArrayList<>();
        long rowNum = 0;

        for (String rowKey : rowKeys) {
            Result r = getResultFromHbase(rowKey, table);
            for(Cell cell : r.listCells()) {
                String qualifier = hbaseQualifierReplace(new String(cell.getQualifierArray()));
                String columnFamily = new String(cell.getFamilyArray());
                log.info("[HBASE COLUMN INFO] column family :{}, column qualifier : {}", columnFamily, qualifier);
                String refValue = hbaseQualifierReplace(getRefValueFromResult(r));
                if (!qualifier.equals(refValueQualifier)) {
                    // uncompressed trace data
                    String data = new String(Snappy.uncompress(Base64.decodeBase64(new String(cell.getValueArray()))));
                    // unnest trace data
                    String[] datas = data.split(",");
                    List<String> c3 = splitData(datas[2]);
                    List<String> c4 = splitData(datas[3]);
                    List<String> c5 = splitData(datas[4]);
                    List<String> c6 = splitData(datas[5]);
                    List<String> c7 = splitData(datas[6]);
                    List<String> c8 = splitData(datas[7]);

                    for (int i = 0; i < c3.size(); i++) {
                        String traceData = String.format("%s,%s,%s,%s,%s,%s,%s,%s\n", datas[0], datas[1], c3.get(i), c4.get(i), c5.get(i), c6.get(i), c7.get(i), c8.get(i));
                        traceDatas.add(String.format("%d,%s,%s,%s", rowNum, refValue, qualifier, traceData));
                        rowNum++;
                    }
                }
            }
        }
        return traceDatas;
    }

    private void appendDataToFile(File outputFile, ByteArrayOutputStream baos) throws IOException {
        FileOutputStream fos = new FileOutputStream(outputFile, true);
        fos.write(baos.toByteArray());
        baos.flush();
        fos.close();
    }

    private  File createOutputFile(int threadNum) throws IOException {
        File file = new File(outputPath, String.format("trace_data_write_%d_%d.csv", System.currentTimeMillis(), threadNum));
        file.createNewFile();
        return file;
    }

    private List<String> splitData(String data) {
        return Arrays.asList(data.split("\\|"));
    }

    private String getRefValueFromResult(Result r) {
        return new String(r.listCells().stream().filter(i -> new String(i.getQualifierArray()).equals(refValueQualifier)).collect(Collectors.toList()).get(0).getValueArray());
    }

    private String hbaseQualifierReplace(String qualifier) {
        return qualifier.replace("^", ",");
    }

    private Result getResultFromHbase(String rowKey, Table table) throws IOException {
        Get g = new Get(Bytes.toBytes(rowKey));
        return table.get(g);
    }

    private Table getHbaseTable() throws IOException {
        TableName tableName = TableName.valueOf(hbaseTableName);
        return connection.getTable(tableName);
    }

    private List<String> getRowKeys() {
        List<Map<String, Object>> rowKeys = jdbcTemplate.queryForList(sampleQuery);
        List<String> result = new ArrayList<>();

        for (Map<String, Object> rowKey : rowKeys) {
            result.add(rowKey.get("record_key").toString());
        }

        return result;
    }
}
