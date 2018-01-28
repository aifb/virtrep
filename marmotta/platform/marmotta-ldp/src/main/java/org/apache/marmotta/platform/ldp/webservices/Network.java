package org.apache.marmotta.platform.ldp.webservices;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Network implements Serializable{

	List<Node> Nodes = new ArrayList<Node>();
	
	
	public Node addNode(String name){
		Nodes.add(new Node(name));
		return Nodes.get(Nodes.size()-1);
	}
}
