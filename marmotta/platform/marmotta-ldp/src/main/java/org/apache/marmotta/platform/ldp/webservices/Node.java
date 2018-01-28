package org.apache.marmotta.platform.ldp.webservices;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class Node implements Serializable{
	
	double[] probabilities;
	String[] outcomes;
	List<Node> parents;
	String name;
	
	public Node(String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	public void setProbabilities(double... probabilities){
		this.probabilities = probabilities;
	}
	
	public double[] getProbabilities(){
		return probabilities;
	}
	
	public void setOutcomes(String... outcomes){
		this.outcomes = outcomes;
	}
	public String[] getOutcomes(){
		return outcomes;
	}
	
	public void setParents(List<Node> parents){
		this.parents = parents;
	}
	public List<Node> getParents(){
		return parents;
	}
	
}
