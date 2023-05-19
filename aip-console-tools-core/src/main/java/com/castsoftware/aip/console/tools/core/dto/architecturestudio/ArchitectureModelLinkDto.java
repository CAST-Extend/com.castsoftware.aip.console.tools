package com.castsoftware.aip.console.tools.core.dto.architecturestudio;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArchitectureModelLinkDto {

    private int callerSubsetId;
    private String callerLayer;
    private int callerId;
    private String callerName;
    private String callerType;
    private String callerFullName;
    private String callerPath;
    private int calleeSubsetId;
    private String calleeLayer;
    private int calleeId;
    private String calleeName;
    private String calleeType;
    private String calleeFullName;
    private String calleePath;
    private int linkId;
    private String linkType;
    private boolean internal;
    private boolean isDynamic;
    private String dynamicInfo;

}
