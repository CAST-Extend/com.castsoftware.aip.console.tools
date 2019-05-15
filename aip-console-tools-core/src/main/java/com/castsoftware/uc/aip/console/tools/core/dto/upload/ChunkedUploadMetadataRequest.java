package com.castsoftware.uc.aip.console.tools.core.dto.upload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChunkedUploadMetadataRequest {
    private long chunkSize;
}
