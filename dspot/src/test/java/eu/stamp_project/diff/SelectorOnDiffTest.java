package eu.stamp_project.diff;

import eu.stamp_project.AbstractTest;
import eu.stamp_project.Main;
import org.junit.Test;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 19/04/18
 */
public class SelectorOnDiffTest extends AbstractTest {

    @Override
    public String getPathToPropertiesFile() {
        return "src/test/resources/multiple-pom/deep-pom-modules.properties";
    }

    @Test
    public void test() throws Exception {
        Main.verbose = true;
//        System.out.println(SelectorOnDiff.findTestClassesAccordingToADiff(Utils.getInputConfiguration()));
        Main.verbose = false;
    }
}
