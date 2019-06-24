package com.castsoftware.uc.aip.console.tools.core.services;

import com.castsoftware.uc.aip.console.tools.core.exceptions.UploadException;

import java.io.File;
import java.io.InputStream;

public interface ChunkedUploadService {

    /**
     * Calls AIP Console API to create an upload and upload the file.
     *
     * @param appGuid The application GUID to use to upload the file
     * @param zipFile An absolute file path for the file to upload
     * @return True if the upload was successful, false otherwise
     * @throws UploadException if the upload was not completed successfully
     */
    boolean uploadFile(String appGuid, File zipFile) throws UploadException;

    /**
     * Calls AIP Console API to create an upload and upload the provided InputStream content
     *
     * @param appGuid  The applicationg GUID
     * @param fileName The name of the file
     * @param fileSize The size of the file
     * @param content  The input stream with the content
     * @return True if the upload was successful, false otherwise
     * @throws UploadException If any issue occurs whil communicating with AIP Console, or reading the file content
     */
    boolean uploadInputStream(String appGuid, String fileName, long fileSize, InputStream content)
            throws UploadException;
}
