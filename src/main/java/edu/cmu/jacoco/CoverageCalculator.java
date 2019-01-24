package edu.cmu.jacoco;

import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IPackageCoverage;

import java.util.*;

class CoverageCalculator {

    CoverageInfo getInfo(IBundleCoverage bundleCoverage) {
        Collection<IPackageCoverage> packagesCoverage = bundleCoverage.getPackages();
        CoverageInfo totalCoverageInfo = new CoverageInfo();

        for (IPackageCoverage packageCoverage: packagesCoverage) {
            String packageName = packageCoverage.getName();
            Collection<IClassCoverage> classesCoverage = packageCoverage.getClasses();
            CoverageInfo packageCoverageInfo = new CoverageInfo();

            totalCoverageInfo.childs.put(packageName, packageCoverageInfo);

            for (IClassCoverage classCoverage: classesCoverage) {
                String className = classCoverage.getName();
                CoverageInfo classCoverageInfo = new CoverageInfo();

                packageCoverageInfo.childs.put(className, classCoverageInfo);

                final int total = classCoverage.getBranchCounter().getTotalCount();
                final int covered = classCoverage.getBranchCounter().getCoveredCount();

                classCoverageInfo.totalBranches = total;
                classCoverageInfo.coveredBranches = covered;

                packageCoverageInfo.totalBranches += total;
                packageCoverageInfo.coveredBranches += covered;

                totalCoverageInfo.totalBranches += total;
                totalCoverageInfo.coveredBranches += covered;
            }

        }

        return totalCoverageInfo;
    }


    static class CoverageInfo {
        private int totalBranches = 0;
        private int coveredBranches = 0;

        private Map<String, CoverageInfo> childs = new HashMap<>();

        int getTotalBranches() {
            return totalBranches;
        }

        int getCoveredBranches() {
            return coveredBranches;
        }

        Map<String, CoverageInfo> getChilds() {
            return Collections.unmodifiableMap(childs);
        }
    }
}
