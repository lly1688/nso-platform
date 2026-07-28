package com.nso.business.file;

import java.io.IOException;
import java.io.InputStream;

public interface ObjectStoragePort {
    StoredObject put(String objectName, String contentType, long size, InputStream inputStream) throws IOException;
    InputStream get(String objectKey) throws IOException;
    record StoredObject(String objectKey, String sha256) { }
}
