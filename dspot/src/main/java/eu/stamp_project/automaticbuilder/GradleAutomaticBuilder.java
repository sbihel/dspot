package eu.stamp_project.automaticbuilder;

import java.io.*;

import java.nio.file.*;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.stream.Collectors;

import eu.stamp_project.mutant.pit.GradlePitTaskAndOptions;
import eu.stamp_project.utils.DSpotUtils;
import eu.stamp_project.utils.sosiefier.InputConfiguration;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtType;

import static eu.stamp_project.mutant.pit.GradlePitTaskAndOptions.*;

/**
 * Created by Daniele Gagliardi
 * daniele.gagliardi@eng.it
 * on 18/07/17.
 */
public class GradleAutomaticBuilder implements AutomaticBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradleAutomaticBuilder.class);

    private static final String JAVA_PROJECT_CLASSPATH = "gjp_cp"; // Gradle Java Project classpath file

    private static final String CLASSPATH_SEPARATOR = System.getProperty("path.separator");

    private static final String GRADLE_BUILD_FILE_BAK_SUFFIX = ".orig";

    private static final String GRADLE_BUILD_FILE = "build.gradle";

    private static final String NEW_LINE = System.getProperty("line.separator");

    private InputConfiguration configuration;

    GradleAutomaticBuilder(InputConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void compile(String pathToRootOfProject) {
        runTasks(pathToRootOfProject,"clean", "compileJava", "test");
    }

    @Override
    public String buildClasspath(String pathToRootOfProject) {
        try {
            final File classpathFile = new File(pathToRootOfProject + File.separator + JAVA_PROJECT_CLASSPATH);
            if (!classpathFile.exists()) {
                LOGGER.info("Classpath file for Gradle project doesn't exist, starting to build it...");

                LOGGER.info("Injecting  Gradle task to print project classpath on stdout...");
                injectPrintClasspathTask(pathToRootOfProject);
                LOGGER.info("Retrieving project classpath...");
                byte[] taskOutput = cleanClasspath(runTasks(pathToRootOfProject,"printClasspath4DSpot"));
                LOGGER.info("Writing project classpath on file " + JAVA_PROJECT_CLASSPATH +"...");
                FileOutputStream fos = new FileOutputStream(pathToRootOfProject + File.separator + JAVA_PROJECT_CLASSPATH);
                fos.write(taskOutput);
                fos.close();
                resetOriginalGradleBuildFile(pathToRootOfProject);
            }
            try (BufferedReader buffer = new BufferedReader(new FileReader(classpathFile))) {
                return buffer.lines().collect(Collectors.joining());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reset() {
        //TODO Maybe we should change one time the the gradle and reset it at the end of the process
    }

    @Override
    public void runPit(String pathToRootOfProject) {
        runPit(pathToRootOfProject, null);
    }

    @Override
    public void runPit(String pathToRootOfProject, CtType<?>... testClasses) {
        try {
            LOGGER.info("Injecting  Gradle task to run Pit...");
            injectPitTask(pathToRootOfProject, testClasses);

            LOGGER.info("Running Pit...");

            runTasks(pathToRootOfProject, CMD_PIT_MUTATION_COVERAGE);

            resetOriginalGradleBuildFile(pathToRootOfProject);

        } catch (Exception ignored) {

        }
    }

    protected byte[] runTasks(String pathToRootOfProject, String... tasks) {
        ProjectConnection connection = GradleConnector.newConnector().forProjectDirectory(new File(pathToRootOfProject)).connect();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            BuildLauncher build = connection.newBuild();
            build.forTasks(tasks);
            build.setStandardOutput(outputStream);
            build.setStandardError(outputStream);
            build.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            connection.close();
        }
        return outputStream.toByteArray();
    }

    private byte[] cleanClasspath(byte[] taskOutput) {

        LOGGER.info("Retrieved task output:" + NEW_LINE);
        LOGGER.info(new String(taskOutput));
        LOGGER.info("" + NEW_LINE + " Extracting project classpath from task output...");
        StringBuilder sb = new StringBuilder();
        String cleanCP = new String(taskOutput);
        String classPathPattern = "([\\/a-z0-9\\.\\-]*\\.jar|[\\/a-z0-9\\.\\-]*\\.zip)";

        Pattern p = Pattern.compile(classPathPattern);

        Matcher m = p.matcher(cleanCP);
        while (m.find()) {
            sb.append(m.group());
            sb.append(CLASSPATH_SEPARATOR);
        }
        LOGGER.info("Project classpath from task output:" + NEW_LINE + " ");
        LOGGER.info(sb.toString() + "" + NEW_LINE + " ");
        return sb.toString().getBytes();
    }

    private void injectPrintClasspathTask(String pathToRootOfProject) throws IOException {

        String originalGradleBuildFilename = pathToRootOfProject + File.separator + GRADLE_BUILD_FILE;
        File gradleBuildFile = new File(originalGradleBuildFilename);
        makeBackup(gradleBuildFile);

        String printClasspathTask = getPrintClasspathTask();

        Files.write(Paths.get(originalGradleBuildFilename), printClasspathTask.getBytes(), StandardOpenOption.APPEND);

        LOGGER.info("Injected following Gradle task in the Gradle build file " + originalGradleBuildFilename + ":" + NEW_LINE + " ");
        LOGGER.info(printClasspathTask);
    }

    private void injectPitTask(String pathToRootOfProject) throws IOException {
        injectPitTask(pathToRootOfProject, null);
    }

    private void injectPitTask(String pathToRootOfProject, CtType<?>... testClasses) throws IOException {

        String originalGradleBuildFilename = pathToRootOfProject + File.separator + GRADLE_BUILD_FILE;
        File gradleBuildFile = new File(originalGradleBuildFilename);
        makeBackup(gradleBuildFile);

        String pitTask = getPitTask(testClasses);

        Files.write(Paths.get(originalGradleBuildFilename), pitTask.getBytes(), StandardOpenOption.APPEND);

        LOGGER.info("Injected following Gradle task in the Gradle build file " + originalGradleBuildFilename + ":" + NEW_LINE + " ");
        LOGGER.info(pitTask);
    }

    private void makeBackup(File gradleBuildFile) throws IOException {
        if (gradleBuildFile.exists()) {
            LOGGER.info("Found original Gradle build file, making backup copy...");
            String originalGradleBuildFilename = gradleBuildFile.getPath();
            String backedUpGradleBuildFilename = originalGradleBuildFilename + GRADLE_BUILD_FILE_BAK_SUFFIX;
            Path from = Paths.get(originalGradleBuildFilename);
            Path to = Paths.get(backedUpGradleBuildFilename);
            //overwrite existing file, if exists
            CopyOption[] options = new CopyOption[]{
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES
            };
            Files.copy(from, to, options);
            LOGGER.info("Original Gradle build file backed-up as " + backedUpGradleBuildFilename + ".");
        }
    }

    private void resetOriginalGradleBuildFile(String pathToRootOfProject) {

        LOGGER.info("Restoring original Gradle build file...");

        String modifiedGradleBuildFilename = pathToRootOfProject + File.separator + GRADLE_BUILD_FILE;
        String originalGradleBuildFilename = modifiedGradleBuildFilename + GRADLE_BUILD_FILE_BAK_SUFFIX;

        File originalGradleBuildFile = new File(originalGradleBuildFilename);
        if (originalGradleBuildFile.exists()) {
            File modifiedGradleBuildFile = new File(modifiedGradleBuildFilename);
            LOGGER.info("Deleting modified (with injected task) Gradle build file...");
            modifiedGradleBuildFile.delete();
            LOGGER.info("Renaming original Gradle build file from " + originalGradleBuildFilename + " to " + modifiedGradleBuildFilename + "...");
            originalGradleBuildFile.renameTo(new File(pathToRootOfProject + File.separator + GRADLE_BUILD_FILE));
        }
    }

    private String getPrintClasspathTask() {
        return NEW_LINE + NEW_LINE + "task printClasspath4DSpot {" + NEW_LINE +
                "    doLast {" + NEW_LINE +
                "        configurations.testRuntime.each { println it }" + NEW_LINE +
                "    }" + NEW_LINE +
                "}";
    }

    private String getPitTask() {
        return getPitTask(null);
    }


    private String getPitTask(CtType<?>... testClasses) {
        return getPitTaskConfiguration() + getPitTaskOptions(testClasses);
    }

    private String getPitTaskConfiguration() {
        return NEW_LINE + NEW_LINE + "buildscript {" + NEW_LINE +
                "    repositories {" + NEW_LINE +
                (descartesMode ? "        mavenLocal()" : "") + NEW_LINE +
                "        maven {" + NEW_LINE + " " +
                "            url \"https://plugins.gradle.org/m2/\"" + NEW_LINE +
                "        }" + NEW_LINE +
                NEW_LINE +
                "    }" + NEW_LINE +
                NEW_LINE +
                "    configurations.maybeCreate(\"pitest\")" + NEW_LINE +
                NEW_LINE +
                "    dependencies {" + NEW_LINE +
                "       classpath 'info.solidsoft.gradle.pitest:gradle-pitest-plugin:1.1.11'" + NEW_LINE +
                (descartesMode ? "       pitest 'eu.stamp_project.stamp:descartes:0.1-SNAPSHOT'" : "") + NEW_LINE +
                "    }" + NEW_LINE +
                "}" + NEW_LINE +
                NEW_LINE +
                "apply plugin: 'info.solidsoft.pitest'" + NEW_LINE;
    }

    private String getPitTaskOptions(CtType<?>... testClasses) {
        return  NEW_LINE + NEW_LINE + "pitest {" + NEW_LINE +
                "    " + OPT_TARGET_CLASSES + "['" + configuration.getProperty("filter") + "']" + NEW_LINE +
                "    " + OPT_WITH_HISTORY + "true" + NEW_LINE +
                "    " + OPT_VALUE_REPORT_DIR + NEW_LINE +
                "    " + OPT_VALUE_FORMAT + NEW_LINE +
                (configuration.getProperty(PROPERTY_VALUE_TIMEOUT) != null ?
                        "    " + PROPERTY_VALUE_TIMEOUT + " = " + configuration.getProperty(PROPERTY_VALUE_TIMEOUT) : "") + NEW_LINE +
                (configuration.getProperty(PROPERTY_VALUE_JVM_ARGS) != null ?
                        "    " + PROPERTY_VALUE_JVM_ARGS + " = " + configuration.getProperty(PROPERTY_VALUE_JVM_ARGS) : "") + NEW_LINE +
                (testClasses != null ? "    " + OPT_TARGET_TESTS + "['" + Arrays.stream(testClasses).map(DSpotUtils::ctTypeToFullQualifiedName).collect(Collectors.joining(",")) + "']": "") + NEW_LINE +
                (configuration.getProperty(PROPERTY_ADDITIONAL_CP_ELEMENTS) != null ?
                        "    " + OPT_ADDITIONAL_CP_ELEMENTS + "['" + configuration.getProperty(PROPERTY_ADDITIONAL_CP_ELEMENTS) + "']":"") + NEW_LINE +
                (descartesMode ? "    " + OPT_MUTATION_ENGINE + NEW_LINE + "    " + getDescartesMutators() :
                        "    " + OPT_MUTATORS + (evosuiteMode ?
                                VALUE_MUTATORS_EVOSUITE : VALUE_MUTATORS_ALL)) + NEW_LINE +
                (configuration.getProperty(PROPERTY_EXCLUDED_CLASSES) != null ?
                        "    " + OPT_EXCLUDED_CLASSES +  "['" + configuration.getProperty(PROPERTY_EXCLUDED_CLASSES) + "']":"") + NEW_LINE +
                "}" + NEW_LINE;
    }


    private String getDescartesMutators() {
        return "mutators = " + configuration.getProperty("descartesMutators");
    }

    @Override
    public String getOutputDirectoryPit() {
        return GradlePitTaskAndOptions.OUTPUT_DIRECTORY_PIT;
    }
}
