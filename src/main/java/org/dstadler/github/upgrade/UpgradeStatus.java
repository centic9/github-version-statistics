package org.dstadler.github.upgrade;

/**
 * Enumeration for the different results when looking at a project
 */
public enum UpgradeStatus {
    NotAccessible,
    UnknownBuildSystem,
    BuildFailed,
    BuildSucceeded,
    CanUpgrade,
    UpgradeFailed,
    UpgradePossible,
    NoStarsOrWatchers,
}
