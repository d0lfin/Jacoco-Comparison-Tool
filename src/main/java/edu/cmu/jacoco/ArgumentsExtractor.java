package edu.cmu.jacoco;

import org.apache.commons.cli.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class ArgumentsExtractor {

    private static final String SOURCES = "sources";
    private static final String REPORT = "report";
    private static final String CLASSES = "classes";
    private static final String ROOT = "root";
    private static final String EXEC = "exec";
    private static final String TITLES = "titles";

    private final Options options = new Options();

    ArgumentsExtractor() {
        options.addOption(create(SOURCES, "The directory containing the sources files", false));
        options.addOption(create(REPORT, "The directory that the generated report will be written to"));
        options.addOption(create(CLASSES, "The directory containing the classes files", false));
        options.addOption(create(ROOT, "The directory containing the classes files", false));
        options.addOption(create(EXEC, "The paths to the Jacoco execution files, coma separated"));
        options.addOption(create(TITLES, "The titles of the test suites in the coverage report, coma separated"));
    }

    private Option create(String name, String description) {
        return create(name, description, true);
    }

    private Option create(String name, String description, boolean required) {
        return OptionBuilder.withLongOpt(name)
                .withDescription(description)
                .hasArg()
                .isRequired(required)
                .create();
    }

    Arguments extractArguments(String[] args) throws ParseException {
        CommandLineParser parser = new BasicParser();
        CommandLine line = parser.parse(options, args);

        String sources = line.getOptionValue(SOURCES);
        String classes = line.getOptionValue(CLASSES);

        return new Arguments(
                sources == null ? Collections.emptyList() : Arrays.asList(sources.split(",")),
                classes == null ? Collections.emptyList() : Arrays.asList(classes.split(",")),
                line.getOptionValue(REPORT),
                line.getOptionValue(ROOT),
                Arrays.asList(line.getOptionValue(EXEC).split(",")),
                Arrays.asList(line.getOptionValue(TITLES).split(","))
        );
    }

    static class Arguments {
        final List<String> sources;
        final List<String> classes;
        final String report;
        final String root;
        final List<String> exec;
        final List<String> titles;

        Arguments(List<String> src,
                  List<String> classes,
                  String report,
                  String root,
                  List<String> exec,
                  List<String> titles) {
            this.sources = src;
            this.classes = classes;
            this.report = report;
            this.root = root;
            this.exec = exec;
            this.titles = titles;
        }
    }
}
