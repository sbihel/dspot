package fr.inria.diversify.mutant.pit;

import org.apache.commons.io.FileUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 1/4/17
 */
public class PitResultParser {

    public static List<PitResult> parseAndDelete(String pathToDirectoryResults) {
        if (!new File(pathToDirectoryResults).exists()) {
            return null;
        }
        final File[] files = new File(pathToDirectoryResults).listFiles();
        if (files == null) {
            return null;
        }
        File directoryReportPit = files[0];
        if (!directoryReportPit.exists()) {
            return null;
        }
        File fileResults = new File(directoryReportPit.getPath() + "/mutations.csv");
        FileInputStream fileResultsXml;
        try {
            fileResultsXml = new FileInputStream(directoryReportPit.getPath() + "/mutations.xml");
        } catch (FileNotFoundException e) {
            return null;
        }
        final List<PitResult> results = PitResultParser.parse(fileResults, fileResultsXml);
        try {
            FileUtils.deleteDirectory(directoryReportPit);
        } catch (IOException e) {
            // ignored
        }
        return results;
    }

    public static List<PitResult> parse(File fileResults, FileInputStream fileResultsXml) {
        final List<PitResult> results = new ArrayList<>();

        XMLInputFactory xmlInFact = XMLInputFactory.newInstance();
        final XMLStreamReader reader;
        try {
            reader = xmlInFact.createXMLStreamReader(fileResultsXml);
        } catch (XMLStreamException e) {
            return null;
        }

        try (BufferedReader buffer = new BufferedReader(new FileReader(fileResults))) {
            buffer.lines().forEach(line -> {
                String[] splittedLines = line.split(",");
                if (splittedLines.length == 7) {
                    PitResult.State state;
                    try {
                        state = PitResult.State.valueOf(splittedLines[5]);
                    } catch (Exception e) {
                        state = PitResult.State.NO_COVERAGE;
                    }
                    String fullQualifiedNameOfMutatedClass = splittedLines[1];
                    String fullQualifiedNameMutantOperator = splittedLines[2];
                    String fullQualifiedNameMethod;
                    String fullQualifiedNameClass;
                    if ("none".equals(splittedLines[6])) {
                        fullQualifiedNameMethod = "none";
                        fullQualifiedNameClass = "none";
                    } else {
                        final String[] nameOfTheKiller = splittedLines[6].split("\\(");
                        if (nameOfTheKiller.length > 1) {
                            fullQualifiedNameMethod = nameOfTheKiller[0];
                            fullQualifiedNameClass = nameOfTheKiller[1].substring(0, nameOfTheKiller[1].length() - 1);
                        } else {
                            fullQualifiedNameMethod = "none";
                            fullQualifiedNameClass = nameOfTheKiller[0].substring(0, nameOfTheKiller[0].length() / 2);
                        }
                    }
                    int lineNumber = Integer.parseInt(splittedLines[4]);
                    String location = splittedLines[3];

                    int eventType = 0;
                    try {
                        eventType = reader.next();
                    } catch (XMLStreamException e) {
                        //
                    }
                    while (eventType != XMLStreamConstants.START_ELEMENT || !("description".equals(reader.getLocalName()))) {
                        try {
                            eventType = reader.next();
                        } catch (XMLStreamException e) {
                            //
                        }
                    }
                    try {
                        reader.next();
                    } catch (XMLStreamException e) {
                        //
                    }
                    String mutantDescription = reader.getText();

                    results.add(new PitResult(fullQualifiedNameOfMutatedClass, state, fullQualifiedNameMutantOperator, fullQualifiedNameMethod, fullQualifiedNameClass, lineNumber, location, mutantDescription));
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (reader != null) {
            try {
                reader.close();
            } catch (XMLStreamException e) {
                //
            }
        }

        return results;
    }
}
