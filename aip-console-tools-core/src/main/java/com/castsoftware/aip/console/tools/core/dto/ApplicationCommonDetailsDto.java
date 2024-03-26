package com.castsoftware.aip.console.tools.core.dto;

import com.castsoftware.aip.console.tools.core.dto.jobs.JobType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class ApplicationCommonDetailsDto {

    private String name;
    private String guid;
    private String domainGuid;
    private String domainName;
    private LocalDateTime latestActionDate;
    private boolean imagingOnly;
    private String size;
    private long nbLoc;
    private Long numberOfObjects;
    private Long numberOfLinks;
    private int nbOfAlerts;
    private Set<JobType> latestJobs;

}
