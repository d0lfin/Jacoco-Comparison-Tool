package edu.cmu.jacoco;

import org.apache.commons.cli.*;

import java.util.Arrays;
import java.util.List;

class ArgumentsExtractor {

    private static final String SOURCES = "sources";
    private static final String REPORT = "report";
    private static final String CLASSES = "classes";
    private static final String EXEC = "exec";
    private static final String TITLES = "titles";

    private final Options options = new Options();

    ArgumentsExtractor() {
        options.addOption(create(SOURCES, "The directory containing the sources files"));
        options.addOption(create(REPORT, "The directory that the generated report will be written to"));
        options.addOption(create(CLASSES, "The directory containing the classes files"));
        options.addOption(create(EXEC, "The paths to the Jacoco execution files, coma separated"));
        options.addOption(create(TITLES, "The titles of the test suites in the coverage report, coma separated"));
    }

    private Option create(String name, String description) {
        return OptionBuilder.withLongOpt(name)
                .withDescription(description)
                .hasArg()
                .isRequired()
                .create();
    }

    Arguments extractArguments(String[] args) throws ParseException {
        CommandLineParser parser = new BasicParser();
        CommandLine line = parser.parse(options, args);

        return new Arguments(
                line.getOptionValue(SOURCES),
                line.getOptionValue(CLASSES),
                line.getOptionValue(REPORT),
                Arrays.asList(line.getOptionValue(EXEC).split(",")),
                Arrays.asList(line.getOptionValue(TITLES).split(","))
        );
    }

    class Arguments {
        final String sources;
        final String classes;
        final String report;
        final List<String> exec;
        final List<String> titles;

        Arguments(String src, String classes, String report, List<String> exec, List<String> titles) {
            this.sources = src;
            this.classes = classes;
            this.report = report;
            this.exec = exec;
            this.titles = titles;
        }
    }
}
