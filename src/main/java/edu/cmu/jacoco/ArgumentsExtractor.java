package edu.cmu.jacoco;

import org.apache.commons.cli.*;

public class ArgumentsExtractor {

    private static CommandLine line;

    static boolean extractArguments(String[] args) {
        CommandLineParser parser = new BasicParser();

        Options options = new Options();
        boolean valid = true;

        options.addOption( OptionBuilder.withLongOpt( "source" )
                .withDescription( "The directory containing the SOURCE files" )
                .hasArg()
                .create() );
        options.addOption( OptionBuilder.withLongOpt( "report" )
                .withDescription( "The directory that the generated REPORTs will be written to" )
                .hasArg()
                .create() );
        options.addOption( OptionBuilder.withLongOpt( "package" )
                .withDescription( "The packages that the reports will be genrated for" )
                .hasArg()
                .create() );

        options.addOption( OptionBuilder.withLongOpt( "exec" )
                .withDescription( "The name of the Jacoco execution files" )
                .hasArg()
                .create() );

        options.addOption( OptionBuilder.withLongOpt( "title" )
                .withDescription( "The title of the test suites in the coverage report" )
                .hasArg()
                .create() );


        try {
            // parse the command line arguments
            line = parser.parse( options, args );

            if( !line.hasOption( "source" )) {
                System.out.println("You need to specify the source directory");
                valid = false;
            }

            if( !line.hasOption("report")) {
                System.out.println( "You need to specify the report directory");
                valid = false;
            }

            if( !line.hasOption( "exec" ) ) {
                System.out.println("You need to specify the name of the exec files.");
                valid = false;
            }


        }
        catch( ParseException exp ) {
            System.out.println( "Unexpected exception:" + exp.getMessage() );
            valid = false;
        }

        return valid;
    }

    static String getOptionValue(String option) {
        if (line.hasOption(option)) {
            return line.getOptionValue(option);
        }
        else {
            return new String();
        }
    }

    static String[] getOptionValues(String option, String separator) {
        if (line.hasOption(option)) {
            return line.getOptionValue(option).split(separator);
        }
        else {
            return new String[0];
        }
    }
}
