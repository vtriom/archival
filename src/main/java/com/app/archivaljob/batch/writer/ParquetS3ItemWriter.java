package com.app.archivaljob.batch.writer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.apache.parquet.io.OutputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ParquetS3ItemWriter implements ItemWriter<Map<String, Object>>, StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ParquetS3ItemWriter.class);

    @Value("${archival.parquet.output-dir}")
    private String outputDir;
    @Value("${archival.s3.bucket}")
    private String s3Bucket;
    @Value("${archival.s3.region}")
    private String s3Region;
    @Value("${archival.s3.access-key}")
    private String s3AccessKey;
    @Value("${archival.s3.secret-key}")
    private String s3SecretKey;
    @Value("${archival.s3.base-path}")
    private String s3BasePath;
    @Value("${archival.skip-s3-upload:false}")
    private boolean skipS3Upload;

    private StepExecution stepExecution;
    private File parquetFile;
    private Schema avroSchema;
    private ParquetWriter<GenericRecord> writer;
    private List<String> columnNames;
    private boolean writerInitialized = false;
    private int recordCount = 0;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        String jobId = String.valueOf(stepExecution.getJobExecution().getJobId());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = "archival-" + jobId + "-" + timestamp + ".parquet";
        try {
            Files.createDirectories(Paths.get(outputDir));
        } catch (IOException e) {
            log.error("[Batch] Failed to create output directory: {}", outputDir, e);
            throw new RuntimeException("Failed to create output directory", e);
        }
        parquetFile = Paths.get(outputDir, fileName).toFile();
    }

    @Override
    public void write(Chunk<? extends Map<String, Object>> chunk) throws Exception {
        if (!writerInitialized) {
            // Collect all unique keys from the first chunk
            java.util.Set<String> allKeys = new java.util.LinkedHashSet<>();
            java.util.Map<String, Object> typeSamples = new java.util.HashMap<>();
            for (Map<String, Object> record : chunk) {
                for (Map.Entry<String, Object> entry : record.entrySet()) {
                    allKeys.add(entry.getKey());
                    // Prefer non-null sample for type inference
                    if (!typeSamples.containsKey(entry.getKey()) || typeSamples.get(entry.getKey()) == null) {
                        typeSamples.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            columnNames = new ArrayList<>(allKeys);
            StringBuilder schemaBuilder = new StringBuilder();
            schemaBuilder.append("{\"type\":\"record\",\"name\":\"Row\",\"fields\":[");
            for (Iterator<String> it = columnNames.iterator(); it.hasNext(); ) {
                String col = it.next();
                Object value = typeSamples.get(col);
                String avroField = inferAvroField(col, value);
                schemaBuilder.append(avroField);
                if (it.hasNext()) schemaBuilder.append(",");
            }
            schemaBuilder.append("]}");
            String schemaJson = schemaBuilder.toString();
            log.info("[Batch] Avro schema used for Parquet: {}", schemaJson);
            avroSchema = new Schema.Parser().parse(schemaJson);

            // Re-initialize writer with new schema
            if (writer != null) writer.close();
            // Delete the file if it exists before creating the new writer
            if (parquetFile.exists()) {
                log.warn("[Batch] Parquet file already exists, deleting: {}", parquetFile.getAbsolutePath());
                try {
                    if (!parquetFile.delete()) {
                        throw new IOException("Failed to delete existing Parquet file: " + parquetFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    log.error("[Batch] Could not delete existing Parquet file", e);
                    throw new RuntimeException("Failed to delete existing Parquet file: " + parquetFile.getAbsolutePath(), e);
                }
            }
            Configuration conf = new Configuration();
            OutputFile outputFile = HadoopOutputFile.fromPath(new Path(parquetFile.getAbsolutePath()), conf);
            writer = AvroParquetWriter.<GenericRecord>builder(outputFile)
                    .withSchema(avroSchema)
                    .withConf(conf)
                    .build();
            log.info("[Batch] Parquet writer initialized at {}", parquetFile.getAbsolutePath());
            writerInitialized = true;
        }

        for (Map<String, Object> item : chunk) {
            GenericRecord record = new GenericData.Record(avroSchema);
            for (String col : columnNames) {
                Object value = item.getOrDefault(col, null);
                // Convert LocalDate/LocalDateTime/Date to appropriate Avro value
                if (value instanceof LocalDate) {
                    value = (int) ((LocalDate) value).toEpochDay();
                } else if (value instanceof LocalDateTime) {
                    value = ((LocalDateTime) value).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                } else if (value instanceof Date) {
                    value = ((Date) value).getTime();
                }
                record.put(col, value);
            }
            writer.write(record);
            recordCount++;
        }
    }

    // Helper method to infer Avro type from Java object
    private String inferAvroType(Object value) {
        if (value == null) return "string"; // default to string if null
        if (value instanceof Integer) return "int";
        if (value instanceof Long) return "long";
        if (value instanceof Double || value instanceof Float) return "double";
        if (value instanceof Boolean) return "boolean";
        return "string";
    }

    // Helper method to infer Avro field definition from Java object
    private String inferAvroField(String col, Object value) {
        if (value instanceof LocalDate) {
            return "{\"name\":\"" + col + "\",\"type\":[\"null\",{\"type\":\"int\",\"logicalType\":\"date\"}]}";
        } else if (value instanceof LocalDateTime || value instanceof Date) {
            return "{\"name\":\"" + col + "\",\"type\":[\"null\",{\"type\":\"long\",\"logicalType\":\"timestamp-millis\"}]}";
        } else {
            String avroType = inferAvroType(value);
            return "{\"name\":\"" + col + "\",\"type\":[\"null\",\"" + avroType + "\"]}";
        }
    }

    // Do not use @Override here, as afterStep is not part of ItemWriter
    public ExitStatus afterStep(StepExecution stepExecution) {
        try {
            if (writer != null) {
                try {
                    writer.close();
                    log.info("[Batch] Parquet file closed: {}", parquetFile.getAbsolutePath());
                    log.info("[Batch] Total records written to Parquet: {}", recordCount);
                } catch (IOException e) {
                    log.error("[Batch] Failed to close Parquet writer", e);
                    return ExitStatus.FAILED;
                }
            }
        } catch (Exception e) {
            log.error("[Batch] Unexpected error during Parquet writer closure", e);
            return ExitStatus.FAILED;
        }
        // Upload to S3 (unless skipping)
        if (skipS3Upload) {
            log.info("[Batch] Skipping S3 upload as per configuration.");
            return ExitStatus.COMPLETED;
        }
        try {
            log.info("[Batch] Uploading Parquet file to S3: bucket={}, key={}", s3Bucket, s3BasePath + parquetFile.getName());
            uploadToS3(parquetFile);
            log.info("[Batch] Parquet file uploaded to S3 successfully.");
        } catch (Exception e) {
            log.error("[Batch] Failed to upload Parquet file to S3", e);
            return ExitStatus.FAILED;
        }
        return ExitStatus.COMPLETED;
    }

    private void uploadToS3(File file) {
        S3Client s3Client = S3Client.builder()
                .region(Region.of(s3Region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3AccessKey, s3SecretKey)))
                .build();
        String key = s3BasePath + file.getName();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(key)
                .build();
        s3Client.putObject(putObjectRequest, file.toPath());
    }
} 