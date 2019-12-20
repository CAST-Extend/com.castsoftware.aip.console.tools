package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.exceptions.UploadException;

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
     * Calls AIP Console API to create an upload and upload the provide InputStream content.
     * <p>
     * Same as calling {@link #uploadAndExtractInputStream(String, String, long, InputStream, boolean)}
     * but will check AIP Console API information to select whether it should be extracted or not.
     * <p>
     * To control the extract parameter, please use {@link #uploadAndExtractInputStream(String, String, long, InputStream, boolean)}
     *
     * @param appGuid  The application GUID
     * @param fileName The name of the file
     * @param fileSize The size of the file
     * @param content  The input stream with the content
     * @return True if the upload was successful, false otherwise
     * @throws UploadException If any issue occurs while communicating with AIP Console, or reading the file content
     */
    boolean uploadInputStream(String appGuid, String fileName, long fileSize, InputStream content)
            throws UploadException;

    /**
     * Calls AIP Console API to create an upload, upload the provided InputStream content and extract the content
     * if the extract parameter is true
     *
     * @param appGuid  The application GUID
     * @param fileName The name of the file
     * @param fileSize The size of the file
     * @param content  The input stream with the content
     * @param extract  Whether or not the content should be extracted in AIP Console (depends on package path parameter)
     * @return True if the upload was successful, false otherwise
     * @throws UploadException If any issue occurs while communicating with AIP Console, or reading the file content
     */
    boolean uploadAndExtractInputStream(String appGuid, String fileName, long fileSize, InputStream content, boolean extract)
            throws UploadException;
}
