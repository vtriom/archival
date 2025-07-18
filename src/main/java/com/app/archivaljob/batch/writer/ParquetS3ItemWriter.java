package com.app.archivaljob.batch.writer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
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

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        // TODO: Define Avro schema based on your data structure
        String schemaStr = "{\"type\":\"record\",\"name\":\"Row\",\"fields\":[{\"name\":\"data\",\"type\":\"string\"}]}";
        avroSchema = new Schema.Parser().parse(schemaStr);
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
        try {
            writer = AvroParquetWriter.<GenericRecord>builder(new Path(parquetFile.getAbsolutePath()))
                    .withSchema(avroSchema)
                    .build();
            log.info("[Batch] Parquet writer initialized at {}", parquetFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("[Batch] Failed to open Parquet writer", e);
            throw new RuntimeException("Failed to open Parquet writer", e);
        }
    }

    @Override
    public void write(Chunk<? extends Map<String, Object>> chunk) throws Exception {
        log.info("[Batch] Writing chunk of {} records to Parquet file", chunk.size());
        for (Map<String, Object> item : chunk) {
            // TODO: Map each field in the map to the Avro schema fields
            GenericRecord record = new GenericData.Record(avroSchema);
            // Example: if schema has a single 'data' field, serialize the map as a string
            record.put("data", item.toString());
            writer.write(record);
        }
    }

    // Do not use @Override here, as afterStep is not part of ItemWriter
    public ExitStatus afterStep(StepExecution stepExecution) {
        try {
            if (writer != null) {
                writer.close();
                log.info("[Batch] Parquet file closed: {}", parquetFile.getAbsolutePath());
            }
        } catch (IOException e) {
            log.error("[Batch] Failed to close Parquet writer", e);
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