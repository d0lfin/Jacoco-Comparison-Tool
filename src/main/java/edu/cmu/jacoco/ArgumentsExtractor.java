package edu.cmu.jacoco;

import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class ArgumentsExtractor {

    private static final String SOURCES = "sources";
    private static final String REPORT = "report";
    private static final String CLASSES = "classes";
    private static final String ROOT = "root";
    private static final String FIRST = "first";
    private static final String SECOND = "second";
    private static final String TITLES = "titles";

    private final Options options = new Options();

    ArgumentsExtractor() {
        options.addOption(create(SOURCES, "The directory containing the sources files", false));
        options.addOption(create(REPORT, "The directory that the generated report will be written to"));
        options.addOption(create(CLASSES, "The directory containing the classes files", false));
        options.addOption(create(ROOT, "The directory containing the classes files", false));
        options.addOption(create(FIRST, "The paths to the Jacoco execution files, coma separated"));
        options.addOption(create(SECOND, "The paths to the Jacoco execution files, coma separated"));
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
        CommandLine line = parser.parse(options, fix(args));

        String sources = line.getOptionValue(SOURCES);
        String classes = line.getOptionValue(CLASSES);

        return new Arguments(
                sources == null ? Collections.emptyList() : Arrays.asList(sources.split(",")),
                classes == null ? Collections.emptyList() : Arrays.asList(classes.split(",")),
                line.getOptionValue(REPORT),
                line.getOptionValue(ROOT),
                Arrays.asList(line.getOptionValue(FIRST).split(",")),
                Arrays.asList(line.getOptionValue(SECOND).split(",")),
                Arrays.asList(line.getOptionValue(TITLES).split(","))
        );
    }

    private String[] fix(String[] args) {
        List<String> fixedArgs = new ArrayList<String>() {
            @Override
            public boolean add(String o) {
                return super.add(o.replace("\"", ""));
            }

            @Override
            public String set(int index, String element) {
                return super.set(index, element.replace("\"", ""));
            }
        };

        boolean listStarted = false;
        for (String arg: args) {
            if (arg.startsWith("\"") && arg.endsWith("\"")) {
                fixedArgs.add(arg);
                continue;
            }

            if (arg.startsWith("\"")) {
                listStarted = true;
                fixedArgs.add(arg);
                continue;
            }

            if (!listStarted) {
                fixedArgs.add(arg);
                continue;
            }

            if (arg.endsWith("\"")) {
                listStarted = false;
            }

            int elements = fixedArgs.size() - 1;
            String lastArg = fixedArgs.get(elements);
            fixedArgs.set(elements, lastArg + " " + arg);
        }

        return fixedArgs.toArray(new String[0]);
    }

    static class Arguments {
        final List<String> sources;
        final List<String> classes;
        final String report;
        final String root;
        final List<String> first;
        final List<String> second;
        final List<String> titles;

        Arguments(List<String> src,
                  List<String> classes,
                  String report,
                  String root,
                  List<String> first,
                  List<String> second,
                  List<String> titles) {
            this.sources = src;
            this.classes = classes;
            this.report = report;
            this.root = root;
            this.first = first;
            this.second = second;
            this.titles = titles;
        }
    }
}
