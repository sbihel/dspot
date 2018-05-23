package eu.stamp_project.dspot.selector.json.coverage;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 31/07/17
 */
public class TestCaseJSON {

	private final String name;
	private final String parentName;
	private final int nbAssertionAdded;
	private final int nbInputAdded;
	private final int instructionCovered;
	private final int instructionTotal;

	public TestCaseJSON(String name, String parentName, int nbAssertionAdded, int nbInputAdded, int instructionCovered, int instructionTotal) {
		this.name = name;
		this.parentName = (parentName == null)? "": parentName;
		this.nbAssertionAdded = nbAssertionAdded;
		this.nbInputAdded = nbInputAdded;
		this.instructionCovered = instructionCovered;
		this.instructionTotal = instructionTotal;
	}
}
