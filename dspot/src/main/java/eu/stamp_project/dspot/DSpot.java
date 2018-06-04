package eu.stamp_project.dspot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.stamp_project.Main;
import eu.stamp_project.automaticbuilder.AutomaticBuilder;
import eu.stamp_project.automaticbuilder.AutomaticBuilderFactory;
import eu.stamp_project.dspot.amplifier.Amplifier;
import eu.stamp_project.dspot.selector.CloverCoverageSelector;
import eu.stamp_project.dspot.selector.TestSelector;
import eu.stamp_project.testrunner.EntryPoint;
import eu.stamp_project.utils.AmplificationChecker;
import eu.stamp_project.utils.AmplificationHelper;
import eu.stamp_project.utils.AmplificationLog;
import eu.stamp_project.utils.Counter;
import eu.stamp_project.utils.DSpotUtils;
import eu.stamp_project.utils.Initializer;
import eu.stamp_project.utils.compilation.DSpotCompiler;
import eu.stamp_project.utils.json.ClassTimeJSON;
import eu.stamp_project.utils.json.ProjectTimeJSON;
import eu.stamp_project.utils.sosiefier.InputConfiguration;
import eu.stamp_project.utils.sosiefier.InputProgram;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.stamp_project.utils.AmplificationHelper.PATH_SEPARATOR;

/**
 * User: Simon
 * Date: 08/06/15
 * Time: 17:36
 */
public class DSpot {

    private static final Logger LOGGER = LoggerFactory.getLogger(DSpot.class);

    private List<Amplifier> amplifiers;

    private int numberOfIterations;

    private TestSelector testSelector;

    public InputProgram inputProgram;

    private InputConfiguration inputConfiguration;

    private DSpotCompiler compiler;

    private ProjectTimeJSON projectTimeJSON;

    public DSpot(InputConfiguration inputConfiguration) throws Exception {
        this(inputConfiguration, 3, Collections.emptyList(), new CloverCoverageSelector());
    }

    public DSpot(InputConfiguration configuration, int numberOfIterations) throws Exception {
        this(configuration, numberOfIterations, Collections.emptyList());
    }

    public DSpot(InputConfiguration configuration, TestSelector testSelector) throws Exception {
        this(configuration, 3, Collections.emptyList(), testSelector);
    }

    public DSpot(InputConfiguration configuration, int iteration, TestSelector testSelector) throws Exception {
        this(configuration, iteration, Collections.emptyList(), testSelector);
    }

    public DSpot(InputConfiguration configuration, List<Amplifier> amplifiers) throws Exception {
        this(configuration, 3, amplifiers);
    }

    public DSpot(InputConfiguration inputConfiguration, int numberOfIterations, List<Amplifier> amplifiers) throws Exception {
        this(inputConfiguration, numberOfIterations, amplifiers, new CloverCoverageSelector());
    }

    public DSpot(InputConfiguration inputConfiguration,
                 int numberOfIterations,
                 List<Amplifier> amplifiers,
                 TestSelector testSelector) throws Exception {

        Initializer.initialize(inputConfiguration);
        this.inputConfiguration = inputConfiguration;
        this.inputProgram = inputConfiguration.getInputProgram();

        AutomaticBuilder builder = AutomaticBuilderFactory.getAutomaticBuilder(inputConfiguration);
        String dependencies = builder.buildClasspath(this.inputConfiguration.getAbsolutePathToProjectRoot());

        if (inputConfiguration.getProperty("additionalClasspathElements") != null) {
            dependencies += PATH_SEPARATOR + new File(this.inputConfiguration.getAbsolutePathToProjectRoot()
                    + inputConfiguration.getProperty("additionalClasspathElements")).getAbsolutePath();
        }

        this.compiler = DSpotCompiler.createDSpotCompiler(this.inputConfiguration, dependencies);
        this.inputConfiguration.setFactory(compiler.getLauncher().getFactory());
        this.amplifiers = new ArrayList<>(amplifiers);
        this.numberOfIterations = numberOfIterations;
        this.testSelector = testSelector;
        this.testSelector.init(this.inputConfiguration);

        final String[] splittedPath = this.inputConfiguration.getAbsolutePathToProjectRoot().split("/");
        final File projectJsonFile = new File(this.inputConfiguration.getOutputDirectory() +
                "/" + splittedPath[splittedPath.length - 1] + ".json");
        if (projectJsonFile.exists()) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            this.projectTimeJSON = gson.fromJson(new FileReader(projectJsonFile), ProjectTimeJSON.class);
        } else {
            this.projectTimeJSON = new ProjectTimeJSON(splittedPath[splittedPath.length - 1]);
        }
        if (Main.useWorkingDirectory) {
            EntryPoint.workingDirectory = new File(this.inputProgram.getProgramDir());
        }
    }

    public void addAmplifier(Amplifier amplifier) {
        this.amplifiers.add(amplifier);
    }

    public List<CtType> amplifyAllTests() {
        return this.amplifyAllTests(this.inputConfiguration.getFactory().Class().getAll().stream()
                .filter(ctClass -> !ctClass.getModifiers().contains(ModifierKind.ABSTRACT))
                .filter(ctClass ->
                        ctClass.getMethods().stream()
                                .anyMatch(AmplificationChecker::isTest))
                .collect(Collectors.toList()));
    }

    public List<CtType> amplifyAllTestsNames(List<String> fullQualifiedNameTestClasses) {
        return fullQualifiedNameTestClasses.stream()
                .flatMap(fullQualifiedNameTestClass -> this.amplifyTest(fullQualifiedNameTestClass).stream())
                .collect(Collectors.toList());
    }

    public List<CtType> amplifyAllTests(List<CtType> testClasses) {
        final List<CtType> amplifiedTestClasses = testClasses.stream()
                .filter(this.isExcluded)
                .map(this::amplifyTest)
                .collect(Collectors.toList());
        writeTimeJson();
        return amplifiedTestClasses;
    }

    public List<CtType> amplifyTest(String targetTestClasses) {
        if (!targetTestClasses.contains("\\")) {
            targetTestClasses = targetTestClasses.replaceAll("\\.", "\\\\\\.").replaceAll("\\*", ".*");
        }
        Pattern pattern = Pattern.compile(targetTestClasses);
        return this.compiler.getFactory().Class().getAll().stream()
                .filter(ctType -> pattern.matcher(ctType.getQualifiedName()).matches())
                .filter(ctClass ->
                        ctClass.getMethods().stream()
                                .anyMatch(AmplificationChecker::isTest))
                .filter(this.isExcluded)
                .map(this::amplifyTest)
                .collect(Collectors.toList());
    }

    public CtType amplifyTest(CtType test) {
        return this.amplifyTest(test, AmplificationHelper.getAllTest(test));
    }

    public CtType amplifyTest(String fullQualifiedName, List<String> methods) {
        final CtType<?> testClass = this.compiler.getLauncher().getFactory().Type().get(fullQualifiedName);
        return amplifyTest(testClass, methods.stream()
                .map(methodName -> testClass.getMethodsByName(methodName).get(0))
                .filter(AmplificationChecker::isTest)
                .collect(Collectors.toList()));
    }

    public CtType amplifyTest(CtType test, List<CtMethod<?>> methods) {
        try {
            test = AmplificationHelper.convertToJUnit4(test, this.inputConfiguration, this.inputProgram);
            Counter.reset();
            AmplificationLog.reset();
            Amplification testAmplification = new Amplification(this.inputConfiguration, this.amplifiers, this.testSelector, this.compiler);
            final List<CtMethod<?>> filteredTestCases = this.filterTestCases(methods);
            long time = System.currentTimeMillis();
            testAmplification.amplification(test, filteredTestCases, numberOfIterations);
            final long elapsedTime = System.currentTimeMillis() - time;
            LOGGER.info("elapsedTime {}", elapsedTime);
            this.projectTimeJSON.add(new ClassTimeJSON(test.getQualifiedName(), elapsedTime));
            final CtType clone = test.clone();
            test.getPackage().addType(clone);
            CtType<?> amplification = AmplificationHelper.createAmplifiedTest(testSelector.getAmplifiedTestCases(), clone, testSelector.getMinimizer());
            testSelector.report();

            Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().disableHtmlEscaping().create();
            final File fileAmpLog = new File(this.inputConfiguration.getOutputDirectory() + "/" +
                    test.getQualifiedName() + "_amp_log.json");
            HashMap<String, ArrayList<AmplificationLog.ActionLog>> newMap = new HashMap<>();
            AmplificationLog.getKeySet().forEach(methodKey ->
                    newMap.put(methodKey,
                            AmplificationLog.getAmplifications(methodKey)));
            try (FileWriter writer = new FileWriter(fileAmpLog, false)) {
                writer.write(gson.toJson(newMap));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            final File outputDirectory = new File(inputConfiguration.getOutputDirectory());
            LOGGER.info("Print {} with {} amplified test cases in {}", amplification.getSimpleName(),
                    testSelector.getAmplifiedTestCases().size(), this.inputConfiguration.getOutputDirectory());
            DSpotUtils.printAmplifiedTestClass(amplification, outputDirectory);
            FileUtils.cleanDirectory(compiler.getSourceOutputDirectory());
            try {
                String pathToDotClass = compiler.getBinaryOutputDirectory().getAbsolutePath() + "/" +
                        test.getQualifiedName().replaceAll("\\.", "/") + ".class";
                FileUtils.forceDelete(new File(pathToDotClass));
            } catch (IOException ignored) {
                //ignored
            }
            writeTimeJson();
            return amplification;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<CtMethod<?>> filterTestCases(List<CtMethod<?>> testMethods) {
        if (this.inputConfiguration.getProperty("excludedTestCases") == null) {
            return testMethods;
        } else {
            final List<String> excludedTestCases = Arrays.stream(this.inputConfiguration.getProperty("excludedTestCases").split(",")).collect(Collectors.toList());
            return testMethods.stream()
                    .filter(ctMethod ->
                            excludedTestCases.isEmpty() ||
                                    !excludedTestCases.contains(ctMethod.getSimpleName())
                    ).collect(Collectors.toList());
        }
    }

    @Deprecated
    public InputProgram getInputProgram() {
        return inputProgram;
    }

    public InputConfiguration getInputConfiguration() {
        return this.inputConfiguration;
    }

    private final Predicate<CtType> isExcluded = ctType ->
            this.inputConfiguration.getProperty("excludedClasses") == null ||
                    Arrays.stream(this.inputConfiguration.getProperty("excludedClasses").split(","))
                            .map(Pattern::compile)
                            .map(pattern -> pattern.matcher(ctType.getQualifiedName()))
                            .noneMatch(Matcher::matches);

    private void writeTimeJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final File file = new File(this.inputConfiguration.getOutputDirectory() +
                "/" + this.projectTimeJSON.projectName + ".json");
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(gson.toJson(this.projectTimeJSON));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
