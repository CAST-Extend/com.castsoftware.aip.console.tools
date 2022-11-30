package com.castsoftware.aip.console.tools.core.utils;

import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionInformation {

    /**
     * Value of a version compound stating that this compound is not defined.
     */
    public static final int UNDEFINED_NUMBER = -1;
    /**
     * Value of the build version compound stating that the version is an incremental.
     */
    public static final int INCREMENTAL_NUMBER = -2;

    /**
     * The precision granularity used to compare two versions. A version number may have up to 4 numbers separated by dots (e.g.
     * 7.2.213.1) representing the major number (7), the minor number (2), the maintenance number (213) and the build number (1).
     */
    public enum Precision {
        /**
         * Precision up to the 1st number of a version
         */
        MAJOR() {
            @Override
            int getValue(VersionInformation v) {
                return v.major;
            }

        },
        /**
         * Precision up to the 2nd number of a version
         */
        MINOR() {
            @Override
            int getValue(VersionInformation v) {
                return v.minor;
            }

        },
        /**
         * Precision up to the 3rd number of a version
         */
        MAINTENANCE() {
            @Override
            int getValue(VersionInformation v) {
                return v.maintenance;
            }

        },
        /**
         * Precision up to the 4th number of a version
         */
        BUILD() {
            @Override
            int getValue(VersionInformation v) {
                return v.build;
            }

            @Override
            protected int compare(int v1, int v2) {
                if (v1 != INCREMENTAL_NUMBER && v2 == INCREMENTAL_NUMBER) {
                    return -1;
                }

                if (v1 == INCREMENTAL_NUMBER && v2 != INCREMENTAL_NUMBER) {
                    return +1;
                }

                return super.compare(v1, v2);
            }

        };

        final int compare(VersionInformation vi1, VersionInformation vi2) {
            int value1 = getValue(vi1);
            int value2 = getValue(vi2);

            return compare(value1, value2);
        }

        protected int compare(int v1, int v2) {
            if ((v1 == UNDEFINED_NUMBER || v1 == 0) && (v2 == UNDEFINED_NUMBER || v2 == 0)) {
                return 0;
            }

            return v1 - v2;
        }

        abstract int getValue(VersionInformation v);
    }

    private static final String SEPARATOR = ".";

    /**
     * Used to assert constructor parameter coherence.
     *
     * @param major       The version major number.
     * @param minor       The version minor number.
     * @param maintenance The version maintenance number.
     * @param build       The version build number.
     */
    private static void assertVersionCoherence(int major, int minor, int maintenance, int build) throws ApplicationServiceException {
        // Only build number can be "inc"
        String versionString = toString(major, minor, maintenance, build);
        if (major < -1 || minor < -1 || maintenance < -1 || build < -2) {
            throw new ApplicationServiceException("The provided version doesn't match the required rule: " + versionString);
        }

        // Major is mandatory
        if (major == UNDEFINED_NUMBER || major == 0) {
            throw new ApplicationServiceException("The provided version has invalid MAJOR value: " + versionString);
        }

        if (minor == UNDEFINED_NUMBER && (maintenance != UNDEFINED_NUMBER || build != UNDEFINED_NUMBER)) {
            throw new ApplicationServiceException("The provided version has invalid MINOR value: " + versionString);
        }

        if (maintenance == UNDEFINED_NUMBER && build != UNDEFINED_NUMBER) {
            throw new ApplicationServiceException("The provided version has invalid MAINTENANCE value: " + versionString);
        }
    }

    static String toString(int major, int minor, int maintenance, int build) {
        StringBuilder sbMessage = new StringBuilder();
        sbMessage.append(major);
        sbMessage.append(SEPARATOR);
        sbMessage.append(minor);
        sbMessage.append(SEPARATOR);
        sbMessage.append(maintenance);
        sbMessage.append(SEPARATOR);
        sbMessage.append(build);
        sbMessage.append(SEPARATOR);
        return sbMessage.toString();
    }

    /**
     * Returns true if the two given version are compatible (i.e. equals) up to the specified precision.
     *
     * @param expected  The expect version. If null, nothing is expected so it is always compatible.
     * @param actual    The actual version. If null, it is never compatible except if nothing was expected.
     * @param precision The precision telling up to which level the version should be compared.
     * @return True if the two version are compatible or the expected is null. False otherwise.
     */
    public static boolean isCompatible(VersionInformation expected, VersionInformation actual, Precision precision) {
        if (expected == actual) {
            return true;
        }

        if (expected == null) {
            return true;
        }

        if (actual == null) {
            return false;
        }

        for (Precision p : Precision.values()) {
            if (p.compare(expected, actual) != 0) {
                return false;
            }
            if (precision == p) {
                return true;
            }
        }
        return true;
    }

    private final int major;
    private final int minor;
    private final int maintenance;
    private final int build;
    private final String productVersionString;

    /**
     * Constructor of a full version.
     *
     * @param major                The version major number.
     * @param minor                The version minor number.
     * @param maintenance          The version maintenance number.
     * @param build                The version build number.
     * @param productVersionString The product version (for Imaging and Security flats).
     */
    public VersionInformation(int major, int minor, int maintenance, int build, String productVersionString) throws ApplicationServiceException {
        assertVersionCoherence(major, minor, maintenance, build);
        this.major = major;
        this.minor = minor;
        this.maintenance = maintenance;
        this.build = build;
        this.productVersionString = productVersionString;
    }

    /**
     * Constructor of a full version.
     *
     * @param major       The version major number.
     * @param minor       The version minor number.
     * @param maintenance The version maintenance number.
     */
    public VersionInformation(int major, int minor, int maintenance) throws ApplicationServiceException {
        this(major, minor, maintenance, -1, String.join(".", Arrays.asList(new String[]{Integer.toString(major), Integer.toString(minor), "-1"})));
    }

    /**
     * Constructor of a version containing only a major and a minor numbers.
     *
     * @param major The version major number.
     * @param minor The version minor number.
     */
    public VersionInformation(int major, int minor) throws ApplicationServiceException {
        this(major, minor, UNDEFINED_NUMBER, UNDEFINED_NUMBER, String.join(".", Arrays.asList(new String[]{Integer.toString(major), Integer.toString(minor)})));
    }

    /**
     * Returns true if this version is strictly higher than the given version with the maximum precision.
     *
     * @param version The version to compare to.
     * @return True if this version is strictly higher than the given version. False otherwise.
     */
    public boolean isHigherThan(VersionInformation version) {
        return !isLowerThan(version, Precision.BUILD, true);
    }

    /**
     * Returns true if this version is strictly higher than the given version up to the given precision.
     *
     * @param version   The version to compare to.
     * @param precision The precision to compare up to.
     * @return True if this version is strictly higher than the given version. False otherwise.
     */
    public boolean isHigherThan(VersionInformation version, Precision precision) {
        return !isLowerThan(version, precision, true);
    }

    /**
     * Returns true if this version is higher than or equal to the given version with the maximum precision.
     *
     * @param version The version to compare to.
     * @return True if this version is higher than or equal to the given version. False otherwise.
     */
    public boolean isHigherThanOrEqual(VersionInformation version) {
        return !isLowerThan(version, Precision.BUILD, false);
    }

    /**
     * Returns true if this version is higher than or equal to the given version up to the given precision.
     *
     * @param version   The version to compare to.
     * @param precision The precision to compare up to.
     * @return True if this version is higher than or equal to the given version. False otherwise.
     */
    public boolean isHigherThanOrEqual(VersionInformation version, Precision precision) {
        return !isLowerThan(version, precision, false);
    }

    /**
     * Returns true if this version is strictly lower than the given version with the maximum precision.
     *
     * @param version The version to compare to.
     * @return True if this version is strictly lower than the given version. False otherwise.
     */
    public boolean isLowerThan(VersionInformation version) {
        return isLowerThan(version, Precision.BUILD, false);
    }

    /**
     * Returns true if this version is strictly lower than the given version up to the given precision.
     *
     * @param version   The version to compare to.
     * @param precision The precision to compare up to.
     * @return True if this version is strictly lower than the given version. False otherwise.
     */
    public boolean isLowerThan(VersionInformation version, Precision precision) {
        return isLowerThan(version, precision, false);
    }

    /**
     * Returns true if this version is lower than or equal to the given version with the maximum precision.
     *
     * @param version The version to compare to.
     * @return True if this version is lower than or equal to the given version. False otherwise.
     */
    public boolean isLowerThanOrEqual(VersionInformation version) {
        return isLowerThan(version, Precision.BUILD, true);
    }

    /**
     * Returns true if this version is lower than or equal to the given version up to the given precision.
     *
     * @param version   The version to compare to.
     * @param precision The precision to compare up to.
     * @return True if this version is lower than or equal to the given version. False otherwise.
     */
    public boolean isLowerThanOrEqual(VersionInformation version, Precision precision) {
        return isLowerThan(version, precision, true);
    }

    private boolean isLowerThan(VersionInformation version, Precision precision, boolean orEqual) {
        for (Precision p : Precision.values()) {
            int cmp = p.compare(this, version);
            if (cmp > 0) {
                return false;
            }
            if (cmp < 0) {
                return true;
            }
            if (precision == p) {
                break;
            }
        }
        return orEqual;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;

        for (Precision p : Precision.values()) {
            int value = p.getValue(this);

            // Undefined is considered as equals to 0 for comparison, so it is the same here
            if (value == UNDEFINED_NUMBER) {
                value = 0;
            }

            result = prime * result + value;
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VersionInformation && isEqual((VersionInformation) obj);
    }

    /**
     * @param v1 version information 1
     * @param v2 version information 2
     * @return true if v1 equals v2
     */
    public static boolean areEquals(VersionInformation v1, VersionInformation v2) {
        return v1 == v2 || (v1 != null && v2 != null && v1.isEqual(v2));
    }

    /**
     * @param that another version information
     * @return true if this version information equals to the provided one
     */
    public boolean isEqual(VersionInformation that) {
        return isEqual(that, null);
    }

    /**
     * Returns true if this version is equal to the given version up to the given precision.
     *
     * @param that      The version to compare to. Must not be null.
     * @param precision The precision to compare up to. If null, compare up to the highest precision.
     * @return True if this version is equal to the given version. False otherwise.
     */
    public boolean isEqual(VersionInformation that, Precision precision) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }

        for (Precision p : Precision.values()) {
            if (p.compare(this, that) != 0) {
                return false;
            }
            if (precision == p) {
                break;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return buildString(Precision.BUILD);
    }

    /**
     * Returns a string representing the version at maximum precision.
     *
     * @return The string representing the version.
     */
    public String buildString() {
        return buildString(null);
    }

    /**
     * Returns a string representing the version up to a given precision.
     *
     * @param precision The precision of the result string.
     * @return The string representing the version.
     */
    public String buildString(Precision precision) {
        if (precision == null) {
            precision = Precision.BUILD;
        }

        StringBuilder result = new StringBuilder();
        boolean isFirst = true;
        for (Precision p : Precision.values()) {
            if (precision != null && precision.compareTo(p) < 0) {
                break;
            }

            int value = p.getValue(this);
            if (value == UNDEFINED_NUMBER) {
                break;
            }

            if (isFirst) {
                isFirst = false;
            } else {
                result.append(SEPARATOR);
            }

            if (value == INCREMENTAL_NUMBER) {
                result.append("Inc");
            } else {
                result.append(value);
            }
        }

        return result.toString();
    }

    /**
     * Build a VersionInformation object from the given string.
     *
     * @param s The string representing a version.
     * @return The VersionInformation object representing the string.
     */
    public static VersionInformation fromString(String s) throws ApplicationServiceException {
        if (StringUtils.isEmpty(s)) {
            return null;
        }

        int[] pieces = new int[4];
        java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(s, SEPARATOR);
        for (int i = 0; i < 4; i++) {
            if (!tokenizer.hasMoreTokens()) {
                pieces[i] = UNDEFINED_NUMBER;
            } else {
                String revisionString = tokenizer.nextToken();
                if (revisionString.compareToIgnoreCase("inc") == 0) {
                    pieces[i] = INCREMENTAL_NUMBER;
                } else {
                    pieces[i] = Integer.decode(revisionString);
                }
            }
        }
        if (tokenizer.hasMoreTokens()) {
            throw new ApplicationServiceException("String contains unexpected token: " + s);
        }

        return new VersionInformation(pieces[0], pieces[1], pieces[2], pieces[3], s);
    }

    public static VersionInformation fromVersionString(String versionString) {
        //String regExp = "(?<revision>\\d+\\.\\d+\\.\\d+)[\\.|\\-]*(?<revisionString>[^\\.|\\-]+)[\\.|\\-]*(?<build>\\d*)";
        String regExp = "(?<revision>\\d+\\.\\d+\\.\\d+)[\\.|\\-]*(?<revisionString>[^\\.|\\-]+)*([\\.|\\-](?<build>\\d+))*";
        Pattern pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(versionString);
        String revision = "", revisionString = "", build = "";
        if (matcher.find()) {
            revision = matcher.group("revision");
            revisionString = matcher.group("revisionString");
            build = matcher.group("build");
        }

        try {
            int[] pieces = new int[4];
            java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(revision, SEPARATOR);
            for (int i = 0; i < 4; i++) {
                if (!tokenizer.hasMoreTokens()) {
                    pieces[i] = UNDEFINED_NUMBER;
                } else {
                    pieces[i] = Integer.decode(tokenizer.nextToken());
                }
            }
            if (StringUtils.isNotEmpty(build) && StringUtils.isNotEmpty(revisionString)) {
                return new VersionInformation(pieces[0], pieces[1], pieces[2], Integer.decode(build), revisionString);
            } else {
                return new VersionInformation(pieces[0], pieces[1], pieces[2]);
            }
        } catch (NumberFormatException | ApplicationServiceException e) {
            return null;
        }
    }

    /**
     * Compare two versions and return the highest one.
     *
     * @param version1 The first version to compare.
     * @param version2 The second version to compare.
     * @return The highest of the given versions.
     */
    public static VersionInformation getMax(VersionInformation version1, VersionInformation version2) {
        return version1.isLowerThan(version2, null) ? version2 : version1;
    }

    /**
     * Return the value of the major compound of the version.
     *
     * @return The major compound value or {@link VersionInformation#UNDEFINED_NUMBER} if not set
     */
    public int getMajor() {
        return major;
    }

    /**
     * Return the value of the minor compound of the version.
     *
     * @return The minor compound value or {@link VersionInformation#UNDEFINED_NUMBER} if not set
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Return the value of the maintenance compound of the version.
     *
     * @return The maintenance compound value or {@link VersionInformation#UNDEFINED_NUMBER} if not set
     */
    public int getMaintenance() {
        return maintenance;
    }

    /**
     * Return the value of the build compound of the version.
     *
     * @return The build compound value or {@link VersionInformation#UNDEFINED_NUMBER} if not set or
     * {@link VersionInformation#INCREMENTAL_NUMBER} if it is an incremental version
     */
    public int getBuild() {
        return build;
    }

    public String getProductVersionString() {
        return productVersionString;
    }

}
