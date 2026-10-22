package com.ticketing.attachment.service;

import com.ticketing.common.api.BusinessException;
import io.minio.*;
import java.io.ByteArrayInputStream;
import org.springframework.stereotype.Component;

@Component
public class MinioObjectStorage implements ObjectStorage {
    private final MinioClient client;
    public MinioObjectStorage(MinioClient client) { this.client = client; }
    @Override public void put(String bucket, String key, byte[] bytes, String type) {
        try { client.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                .stream(new ByteArrayInputStream(bytes), bytes.length, -1).contentType(type).build()); }
        catch (Exception ex) { throw unavailable(); }
    }
    @Override public byte[] read(String bucket, String key) {
        try (var in = client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            byte[] result = in.readNBytes(5 * 1024 * 1024 + 1);
            if (result.length > 5 * 1024 * 1024) throw unavailable();
            return result;
        } catch (Exception ex) { throw unavailable(); }
    }
    @Override public void verify(String bucket, String key) {
        try { client.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build()); }
        catch (Exception ex) { throw unavailable(); }
    }
    @Override public void delete(String bucket, String key) {
        try { client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build()); }
        catch (Exception ex) { throw unavailable(); }
    }
    private BusinessException unavailable() { return new BusinessException(503, "STORAGE_UNAVAILABLE", "图片存储暂不可用，请稍后重试"); }
}
