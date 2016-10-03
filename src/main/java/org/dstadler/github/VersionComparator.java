package org.dstadler.github;

import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.regex.Pattern;

public class VersionComparator implements Comparator<String> {
    private final static Pattern SIMPLE_VERSION = Pattern.compile("[0-9.][0-9.]+[0-9.]");
    private final static Pattern BETA_VERSION = Pattern.compile("[0-9.][0-9.]+[0-9.]-beta\\d+");

    @Override
    public final int compare(String var1, String var2) {
        boolean simple1 = var1 != null && SIMPLE_VERSION.matcher(var1).matches();
        boolean simple2 = var2 != null && SIMPLE_VERSION.matcher(var2).matches();

        // check for beta-versions
        boolean beta1 = var1 != null && BETA_VERSION.matcher(var1).matches();
        boolean beta2 = var2 != null && BETA_VERSION.matcher(var2).matches();

        if((simple1 || beta1) && (simple2 || beta2)) {
            String[] parts1 = var1.split("[-.]");
            String[] parts2 = var2.split("[-.]");

            // check for non-beta-versions
            for (int i = 0; i < parts1.length && i < parts2.length; i++) {
                String part1 = parts1[i];
                String part2 = parts2[i];

                boolean betaPos1 = false;
                if(part1.startsWith("beta")) {
                    part1 = StringUtils.removeStart(part1, "beta");
                    betaPos1 = true;
                }

                boolean betaPos2 = false;
                if(part2.startsWith("beta")) {
                    part2 = StringUtils.removeStart(part2, "beta");
                    betaPos2 = true;
                }

                // handle cases of different beta-ness
                if (betaPos1 && !betaPos2) {
                    // one is beta, but the other not, if the non-beta has more elements, it is actually higher
                    if(parts1.length <= parts2.length) {
                        return -1;
                    }
                    // first is beta, second not, but they were equal up to now
                    // indicate that the second should come first
                    return 1;
                } else if (!betaPos1 && betaPos2) {
                    // one is beta, but the other not, if the non-beta has more elements, it is actually higher
                    if(parts1.length >= parts2.length) {
                        return 1;
                    }
                    // first is normal, second is beta, but they were equal up to now
                    // indicate that the first should come first
                    return -1;
                }

                int ret = Integer.compare(Integer.parseInt(part1), Integer.parseInt(part2));
                if (ret != 0) {
                    return ret;
                }
            }

            // at this point we had equal numbers for the number of items that both have in common
            if(parts1.length != parts2.length) {
                if (beta1 && !beta2) {
                    return -1;
                } else if (beta2 && !beta1) {
                    return 1;
                }

                // otherwise the longer one is a higher version, e.g. 3.10 and 3.10.1
                return parts1.length < parts2.length ? -1 : 1;
            }
        }

        // finally resort to normal textual comparison
        if(var1 == null && var2 == null) {
            return 0;
        } else if(var1 == null) {
            return -1;
        } else if(var2 == null) {
            return 1;
        }
        return var1.compareTo(var2);
    }
}
