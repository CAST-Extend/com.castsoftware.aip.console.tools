package com.castsoftware.aip.console.tools.core.utils;

import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class VersionInformationTests {

    @Test
    public void testVersionInformation_NativeCall_NeitherBuildNorProductString() throws ApplicationServiceException {
        VersionInformation version = VersionInformation.fromString("2.5.2");
        assertThat(version.getMajor(), is(2));
        assertThat(version.getMinor(), is(5));
        assertThat(version.getMaintenance(), is(2));
    }

    @Test
    public void testVersionInformation_WithBuildAndProductString() throws ApplicationServiceException {
        VersionInformation version = VersionInformation.fromVersionString("2.6.0-RC-1636");

        assertNotNull(version);
        assertThat(version.getMajor(), is(2));
        assertThat(version.getMinor(), is(6));
        assertThat(version.getMaintenance(), is(0));
        assertThat(version.getBuild(), is(1636));
        assertThat(version.getProductVersionString(), is("RC"));
    }

    @Test
    public void testVersionInformation_NeitherBuildNorProductString() throws ApplicationServiceException {
        VersionInformation version = VersionInformation.fromVersionString("2.5.0");

        assertNotNull(version);
        assertThat(version.getMajor(), is(2));
        assertThat(version.getMinor(), is(5));
        assertThat(version.getMaintenance(), is(0));
    }

    @Test
    public void testVersionInformation_NoBuildWithProductString() throws ApplicationServiceException {
        VersionInformation version = VersionInformation.fromVersionString("2.4.9-funcrel");

        assertNotNull(version);
        assertThat(version.getMajor(), is(2));
        assertThat(version.getMinor(), is(4));
        assertThat(version.getMaintenance(), is(9));
    }
}
