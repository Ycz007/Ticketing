package com.ticketing.attachment.service;

public interface ObjectStorage {
    void put(String bucket, String key, byte[] bytes, String mediaType);
    byte[] read(String bucket, String key);
    void verify(String bucket, String key);
    void delete(String bucket, String key);
}
