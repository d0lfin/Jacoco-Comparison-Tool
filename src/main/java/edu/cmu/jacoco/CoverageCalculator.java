package edu.cmu.jacoco;

import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IPackageCoverage;

import java.util.*;

class CoverageCalculator {

    private Visitor visitor = (packageName, className, coverageInfo) -> {};

    /**
     * Return coverage tree
     *
     *     coverage info for bundle
     *     |__ total covered branches in all files
     *     |__ total branches in all files
     *     |__ childs (packages coverages)
     *         |__ package coverage
     *         |__ ...
     *         |__ package coverage
     *             |__ total covered branches of package
     *             |__ total branches of package
     *             |__ childs (package classes coverages)
     *                 |__ class coverage
     *                 |__ ...
     *                 |__ class coverage
     *                     |__ total covered branches of class
     *                     |__ total branches of class
     *                     |__ childs (empty)
     *
     */
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

                ICounter counter = classCoverage.getLineCounter();
                final int total = counter.getTotalCount();
                final int covered = counter.getCoveredCount();

                classCoverageInfo.totalBranches = total;
                classCoverageInfo.coveredBranches = covered;

                packageCoverageInfo.totalBranches += total;
                packageCoverageInfo.coveredBranches += covered;

                totalCoverageInfo.totalBranches += total;
                totalCoverageInfo.coveredBranches += covered;

                visitor.visit(packageName, className, classCoverageInfo);
            }

        }

        return totalCoverageInfo;
    }

    void setCoverageVisitor(Visitor visitor) {
        this.visitor = visitor;
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

    interface Visitor {
        void visit(String packageName, String className, CoverageInfo coverageInfo);
    }
}
