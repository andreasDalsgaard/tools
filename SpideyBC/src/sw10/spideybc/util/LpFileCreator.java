package sw10.spideybc.util;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class LpFileCreator {
	
	private File file;
	private FileWriter writer;
	
	/* Objective function */
	private ObjectiveFunction function;
	private ArrayList<String> objectives;
	
	/* Constraints */
	private ArrayList<String> flowContraints;
	private ArrayList<String> loopContraints;
	private ArrayList<String> allocationContraints;

	/* Possible objective functions */
	public enum ObjectiveFunction {
		MIN, MAX, EMPTY
	}	
	public LpFileCreator(String path, String name) throws IOException {
		file = new File(path, name);
		writer = new FileWriter(file, false);
		
		function = ObjectiveFunction.EMPTY;
		objectives = new ArrayList<String>();
		
		flowContraints = new ArrayList<String>();
		loopContraints = new ArrayList<String>();
		allocationContraints = new ArrayList<String>();
	}
	
	public LpFileCreator() throws IOException {
		this("./", "application.lp");
	}
	
	public void writeFile() throws IOException {
		/* Write objective */
		writer.write("/* objective function */\n");
		writer.write(function.toString().toLowerCase() + ":");
		for(String objective : objectives) {
			writer.write(" " + objective);
		}
		writer.write(";\n\n");
		
		/* Write all contraints */
		writeContraints("flow constraints", flowContraints);
		writer.write("\n");
		writeContraints("loop constraints", loopContraints);
		writer.write("\n");
		writeContraints("allocation constraints", allocationContraints);
		
		writer.close();
	}
	
	private void writeContraints(String name, ArrayList<String> constraints) throws IOException {
		if(constraints.size() != 0) {
			writer.write("/* " + name  + " */\n");
			for(String constraint : constraints) {
				writer.write(constraint + ";\n");
			}
		}
	}
	
	public void setObjectiveFunction(ObjectiveFunction function) {
		this.function = function;
	}
	
	public void addObjective(String objective) {
		objectives.add(objective);
	}
	
	public void addObjectives(String[] objectives) {
		for(String objective : objectives) {
			addObjective(objective);
		}
	}
	
	private void addContraint(String contraint, ArrayList<String> contraints) {
		contraints.add(contraint);
	}
	
	public void addFlowContraint(String name, String contraint) {
		addContraint(name + ": " + contraint, flowContraints);
	}
	
	public void addFlowContraint(String contraint) {
		addContraint(contraint, flowContraints);
	}
	
	public void addLoopContraint(String name, String contraint) {
		addContraint(name + ": " + contraint, loopContraints);
	}
	
	public void addLoopContraint(String contraint) {
		addContraint(contraint, loopContraints);
	}

	public void addAllocationContraint(String name, String contraint) {
		addContraint(name + ": " + contraint, allocationContraints);
	}
	
	public void addAllocationContraint(String contraint) {
		addContraint(contraint, allocationContraints);
	}
	
	public String getAbsolutePath() {
		return file.getAbsolutePath();
	}
}
