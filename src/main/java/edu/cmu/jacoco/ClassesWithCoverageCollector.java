package edu.cmu.jacoco;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ClassesWithCoverageCollector implements CoverageCalculator.Visitor {
    final Map<String, Set<String>> classesWithCoverage = new HashMap<>();

    @Override
    public void visit(String packageName, String className, CoverageCalculator.CoverageInfo coverageInfo) {
        if (coverageInfo.getCoveredBranches() == 0) {
            return;
        }
        if (!classesWithCoverage.containsKey(packageName)) {
            classesWithCoverage.put(packageName, new HashSet<>());
        }
        classesWithCoverage.get(packageName).add(className);
    }
}
