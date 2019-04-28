package org.dstadler.github.util;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;

public class VersionComparator implements Comparator<String> {
    private final static Pattern SIMPLE_VERSION = Pattern.compile("[0-9.][0-9.]+[0-9.]");
    private final static Pattern BETA_VERSION = Pattern.compile("[0-9.][0-9.]+[0-9.]-(?:beta|BETA|SNAPSHOT|bbn)\\d*(-\\d*)?");

    public static class Version implements Comparable<Version> {
        public int major;
        public int minor;
        public int revision;
        public boolean beta;
        public boolean snapshot;
        public boolean other;
        public int betaSnapshotOther;

        public static Version parse(String version) {
            Version v = new Version();

            // sort these as highest version for now
            if("other".equals(version)) {
                v.major = 0;
                return v;
            }

            String[] parts = version.split("[-.]");
            Preconditions.checkState(parts.length > 1 && parts.length < 5, "Need a version with 2 to 4 parts, but had %s resulting from %s", Arrays.toString(parts), version);

            v.major = Integer.parseInt(parts[0]);
            v.minor = Integer.parseInt(parts[1]);
            if(parts.length > 2) {
                handleBetaSnapshotOrRevision(v, parts[2]);
            }
            if(parts.length > 3) {
                if(!v.beta && !v.snapshot && !v.other) {
                    handleBetaSnapshotOrRevision(v, parts[3]);
                } else if (v.betaSnapshotOther == 0) {
                    v.betaSnapshotOther = Integer.parseInt(parts[3]);
                }
            }

            return v;
        }

        private static void handleBetaSnapshotOrRevision(Version v, String part) {
            if (isBeta(part)) {
                v.beta = true;
                part = StringUtils.removeStartIgnoreCase(part, "beta");
                if(StringUtils.isNotEmpty(part)) {
                    v.betaSnapshotOther = Integer.parseInt(part);
                }
            } else if (isSnapshot(part)) {
                v.snapshot = true;
                part = StringUtils.removeStartIgnoreCase(part, "snapshot");
                if(StringUtils.isNotEmpty(part)) {
                    v.betaSnapshotOther = Integer.parseInt(part);
                }
            } else {
                //noinspection UnusedCatchParameter
                try {
                    v.revision = Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    v.other = true;
                }
            }
        }

        private static boolean isBeta(CharSequence part) {
            return StringUtils.startsWithIgnoreCase(part, "BETA");
        }

        private static boolean isSnapshot(CharSequence part) {
            return StringUtils.startsWithIgnoreCase(part, "SNAPSHOT");
        }

        @Override
        public int compareTo(Version o) {
            if(major != o.major) {
                return Integer.compare(major, o.major);
            }
            if(minor != o.minor) {
                return Integer.compare(minor, o.minor);
            }
            if(revision != o.revision) {
                return Integer.compare(revision, o.revision);
            }
            if(beta && !o.beta) {
                if(o.snapshot || o.other) {
                    return Integer.compare(betaSnapshotOther, o.betaSnapshotOther);
                } else {
                    return -1*Integer.compare(betaSnapshotOther, o.betaSnapshotOther);
                }
            }
            if(!beta && o.beta) {
                if(snapshot || other) {
                    return Integer.compare(betaSnapshotOther, o.betaSnapshotOther);
                } else {
                    return -1*Integer.compare(betaSnapshotOther, o.betaSnapshotOther);
                }
            }
            if(snapshot && !o.snapshot) {
                if(o.other) {
                    return Integer.compare(betaSnapshotOther, o.betaSnapshotOther);
                } else {
                    if(betaSnapshotOther == o.betaSnapshotOther) {
                        return -1;
                    }

                    return -1*Integer.compare(betaSnapshotOther, o.betaSnapshotOther);
                }
            }
            if(!snapshot && o.snapshot) {
                if(other) {
                    return Integer.compare(betaSnapshotOther, o.betaSnapshotOther);
                } else {
                    if(betaSnapshotOther == o.betaSnapshotOther) {
                        return 1;
                    }

                    return -1*Integer.compare(betaSnapshotOther, o.betaSnapshotOther);
                }
            }

            return Integer.compare(betaSnapshotOther, o.betaSnapshotOther);
        }

        @Override
        public String toString() {
            return "Version{" +
                    "major=" + major +
                    ", minor=" + minor +
                    ", revision=" + revision +
                    ", beta=" + beta +
                    ", snapshot=" + snapshot +
                    ", other=" + other +
                    ", betaSnapshotOther=" + betaSnapshotOther +
                    '}';
        }
    }

    @SuppressWarnings("ObjectInstantiationInEqualsHashCode")
    @Override
    public final int compare(String var1, String var2) {
        boolean simple1 = var1 != null && SIMPLE_VERSION.matcher(var1).matches();
        boolean simple2 = var2 != null && SIMPLE_VERSION.matcher(var2).matches();

        // check for beta/snapshot-versions
        boolean beta1 = var1 != null && BETA_VERSION.matcher(var1).matches();
        boolean beta2 = var2 != null && BETA_VERSION.matcher(var2).matches();

        if(("other".equals(var1) || simple1 || beta1) && ("other".equals(var2) || simple2 || beta2)) {
            Version v1 = Version.parse(var1);
            Version v2 = Version.parse(var2);

            int ret = v1.compareTo(v2);

            // if the parsed version is equal, we still want to compare the string
            // itself as there can be subtle differences, e.g. "3.9" and "3.9.0" and
            // we still want to keep these versions apart in sorted sets
            if(ret == 0) {
                return var1.compareTo(var2);
            }

            return ret;
        }

        // finally resort to normal textual comparison
        if(var1 == null && var2 == null) {
            return 0;
        }
        if(var1 == null) {
            return -1;
        }
        if(var2 == null) {
            return 1;
        }
        return var1.compareTo(var2);
    }
}
