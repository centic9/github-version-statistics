package org.dstadler.github.upgrade;

/**
 * Enumeration for the different results when looking at a project
 */
enum UpgradeStatus {
    NotAccessible,
    UnknownBuildSystem,
    BuildFailed,
    BuildSucceeded,
    CanUpgrade,
    UpgradeFailed,
    UpgradePossible,
}
