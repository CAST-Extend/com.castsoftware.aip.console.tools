package com.castsoftware.aip.console.tools.core.dto.upload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChunkedUploadMetadataRequest {
    private long chunkSize;
}
