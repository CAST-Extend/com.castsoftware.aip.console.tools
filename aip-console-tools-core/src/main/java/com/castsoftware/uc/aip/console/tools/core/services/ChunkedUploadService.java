package com.castsoftware.uc.aip.console.tools.core.services;

import com.castsoftware.uc.aip.console.tools.core.exceptions.UploadException;

public interface ChunkedUploadService {

    /**
     * Calls AIP Console API to create an upload and upload the file.
     *
     * @param appGuid The application GUID to use to upload the file
     * @param zipFile An absolute file path for the file to upload
     * @return True if the upload was successful, false otherwise
     * @throws UploadException if the upload was not completed successfully
     */
    boolean uploadFile(String appGuid, String zipFile) throws UploadException;
}
