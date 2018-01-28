package edu.kit.aifb.ldbwebservice;


import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.semanticweb.yars.nx.Resource;


public class STEP {
	
	public static final String NAMESPACE = "http://step.aifb.kit.edu/";

    /** {@code step} **/
    public static final String PREFIX = "step";

	public static final Resource LinkedDataWebService;
	public static final Resource BayesService;
	public static final Resource StartAPI;
	public static final Resource Output;
	public static final Resource Target;
	public static final Resource BayesNode;
	public static final Resource VirtualResource;
	public static final Resource FLSVisitourAPI;
	
	public static final Resource hasWebService;
	public static final Resource hasProgram;
	public static final Resource hasValue;
	public static final Resource hasModel;
	public static final Resource hasResult;
	public static final Resource hasOutput;
	public static final Resource hasBayesNode;
	public static final Resource hasProbabilities;
	public static final Resource hasParents;
	public static final Resource hasName;
	public static final Resource hasStartAPI;
	public static final Resource hasStatistics;
	public static final Resource numberOfRequests;
	public static final Resource interactionPattern;

	

	
    static {
        ValueFactory factory = ValueFactoryImpl.getInstance();
        
        // Classes:

        LinkedDataWebService = new Resource(STEP.NAMESPACE + "LinkedDataWebService");
        BayesService = new Resource(STEP.NAMESPACE + "BayesService");
        StartAPI =  new Resource(STEP.NAMESPACE + "StartAPI");
        Output = new Resource(STEP.NAMESPACE + "Output");
        Target = new Resource(STEP.NAMESPACE + "Target");
        BayesNode = new Resource(STEP.NAMESPACE + "BayesNode");
        VirtualResource = new Resource(STEP.NAMESPACE + "VirtualResource");
        FLSVisitourAPI = new Resource(STEP.NAMESPACE + "FLSVisitourAPI");
        
        
        // Predicates:
        
        hasWebService = new Resource(STEP.NAMESPACE + "hasWebService");
        hasProgram = new Resource(STEP.NAMESPACE + "hasProgram");
        hasValue = new Resource(STEP.NAMESPACE + "hasValue");
        hasModel = new Resource(STEP.NAMESPACE + "hasModel");
        hasOutput = new Resource(STEP.NAMESPACE + "hasOutput");
        hasBayesNode = new Resource(STEP.NAMESPACE + "hasBayesNode");
        hasResult = new Resource(STEP.NAMESPACE + "hasResult");
        hasStartAPI = new Resource(STEP.NAMESPACE + "hasStartAPI");
        hasProbabilities = new Resource(STEP.NAMESPACE + "hasProbabilities");
        hasParents = new Resource(STEP.NAMESPACE + "hasParents");
        hasName = new Resource(STEP.NAMESPACE + "hasName");
        hasStatistics = new Resource(STEP.NAMESPACE + "hasStatistics");
        numberOfRequests = new Resource(STEP.NAMESPACE + "numberOfRequests");
        interactionPattern = new Resource(STEP.NAMESPACE + "interactionPattern");

    }

}
