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

                classCoverageInfo.totalLines = total;
                classCoverageInfo.coveredLines = covered;

                packageCoverageInfo.totalLines += total;
                packageCoverageInfo.coveredLines += covered;

                totalCoverageInfo.totalLines += total;
                totalCoverageInfo.coveredLines += covered;

                visitor.visit(packageName, className, classCoverageInfo);
            }

        }

        return totalCoverageInfo;
    }

    void setCoverageVisitor(Visitor visitor) {
        this.visitor = visitor;
    }


    static class CoverageInfo {
        private int totalLines = 0;
        private int coveredLines = 0;

        private Map<String, CoverageInfo> childs = new HashMap<>();

        int getTotalLines() {
            return totalLines;
        }

        int getCoveredLines() {
            return coveredLines;
        }

        Map<String, CoverageInfo> getChilds() {
            return Collections.unmodifiableMap(childs);
        }
    }

    interface Visitor {
        void visit(String packageName, String className, CoverageInfo coverageInfo);
    }
}
