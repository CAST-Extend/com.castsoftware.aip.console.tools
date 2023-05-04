package com.castsoftware.aip.console.tools.core.dto.jobs;

import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelLinkDto;

import java.util.Set;

public class CheckModelReportRequest {
    private String name;
    private Integer transactionId;
    private String description;
    private Integer metricId;
    private Set<ArchitectureModelLinkDto> links;

    public static CheckModelReportRequestBuilder builder() {
        return new CheckModelReportRequestBuilder();
    }

    public String getName() {
        return this.name;
    }

    public Integer getTransactionId() {
        return this.transactionId;
    }

    public String getDescription() {
        return this.description;
    }

    public Integer getMetricId() {
        return this.metricId;
    }

    public Set<ArchitectureModelLinkDto> getLinks() {
        return this.links;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setTransactionId(final Integer transactionId) {
        this.transactionId = transactionId;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setMetricId(final Integer metricId) {
        this.metricId = metricId;
    }

    public void setLinks(final Set<ArchitectureModelLinkDto> links) {
        this.links = links;
    }

    public CheckModelReportRequest() {
    }

    public CheckModelReportRequest(final String name, final Integer transactionId, final String description, final Integer metricId, final Set<ArchitectureModelLinkDto> links) {
        this.name = name;
        this.transactionId = transactionId;
        this.description = description;
        this.metricId = metricId;
        this.links = links;
    }

    public static class CheckModelReportRequestBuilder {
        private String name;
        private Integer transactionId;
        private String description;
        private Integer metricId;
        private Set<ArchitectureModelLinkDto> links;

        CheckModelReportRequestBuilder() {
        }

        public CheckModelReportRequestBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public CheckModelReportRequestBuilder transactionId(final Integer transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public CheckModelReportRequestBuilder description(final String description) {
            this.description = description;
            return this;
        }

        public CheckModelReportRequestBuilder metricId(final Integer metricId) {
            this.metricId = metricId;
            return this;
        }

        public CheckModelReportRequestBuilder links(final Set<ArchitectureModelLinkDto> links) {
            this.links = links;
            return this;
        }

        public CheckModelReportRequest build() {
            return new CheckModelReportRequest(this.name, this.transactionId, this.description, this.metricId, this.links);
        }

        public String toString() {
            return "CheckModelReportRequest.CheckModelReportRequestBuilder(name=" + this.name + ", transactionId=" + this.transactionId + ", description=" + this.description + ", metricId=" + this.metricId + ", links=" + this.links + ")";
        }
    }
}

