package edu.kit.aifb.step.linkeddatawebservice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.inject.Inject;
import javax.swing.AbstractSpinnerModel;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.http.MethodNotSupportedException;
import org.apache.marmotta.commons.vocabulary.LDP;
import org.apache.marmotta.platform.ldp.api.LdpBinaryStoreService;
//import org.apache.marmotta.commons.vocabulary.LDP;
//import org.apache.marmotta.ldpath.parser.ParseException;
//import org.jboss.resteasy.spi.NoLogWebApplicationException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.semanticweb.yars.nx.BNode;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Nodes;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.namespace.RDFS;

import edu.kit.aifb.datafu.Binding;
import edu.kit.aifb.datafu.ConstructQuery;
import edu.kit.aifb.datafu.Origin;
import edu.kit.aifb.datafu.Program;
import edu.kit.aifb.datafu.consumer.impl.BindingConsumerCollection;
import edu.kit.aifb.datafu.engine.EvaluateProgram;
import edu.kit.aifb.datafu.io.origins.InternalOrigin;
import edu.kit.aifb.datafu.io.sinks.BindingConsumerSink;
import edu.kit.aifb.datafu.parser.ProgramConsumerImpl;
import edu.kit.aifb.datafu.parser.QueryConsumerImpl;
import edu.kit.aifb.datafu.parser.notation3.Notation3Parser;
import edu.kit.aifb.datafu.parser.sparql.SparqlParser;
import edu.kit.aifb.datafu.planning.EvaluateProgramConfig;
import edu.kit.aifb.datafu.planning.EvaluateProgramGenerator;
import edu.kit.aifb.step.api.AbstractSemanticStateBasedResource;
import edu.kit.aifb.step.api.SemanticStateBasedResource;
import edu.kit.aifb.step.vocabs.STEP;



public class LinkedDataWebService extends AbstractSemanticStateBasedResource implements SemanticStateBasedResource {
	
	public LinkedDataWebService(String baseUri, RepositoryConnection connection, LdpBinaryStoreService store) {
		super(baseUri, connection, store);
	}
	
	public List<Resource> contains() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	public String create(Iterable<Node[]> arg0) throws RemoteException, MissingArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean delete() throws RemoteException, MethodNotSupportedException {
		// TODO Auto-generated method stub
		return false;
	}

	public Iterable<Node[]> read() throws RemoteException {
		org.openrdf.model.Resource service;
		try {
			service = getContainer(baseUri, connection);
		
		RepositoryResult<Statement> contains_triples = connection.getStatements(service, LDP.contains, null, false);
		

		URI program = null;
		URI query = null;
		while (contains_triples.hasNext()) {
			RepositoryResult<Statement> programs = connection.getStatements(
					service,
					new URIImpl(STEP.hasProgram.getLabel()), null, true, new org.openrdf.model.Resource[0]);

			if (programs.hasNext()) {
				String program_uri = programs.next().getObject().stringValue();
				if (program_uri.endsWith(".bin") || program_uri.endsWith(".bin/")) {
					program = new URIImpl(program_uri);
				} else if (program_uri.endsWith("/")) {
					program = new URIImpl(program_uri.substring(0, program_uri.length() - 2) + ".bin");
				} else {
					program = new URIImpl(program_uri + ".bin");
				}
			}


			RepositoryResult<Statement> queries = connection.getStatements(
					service,
					new URIImpl(STEP.hasQuery.getLabel()), 
					null, true, new org.openrdf.model.Resource[0]);

			if (queries.hasNext()) {
				String query_uri = queries.next().getObject().stringValue();
				if (query_uri.endsWith(".bin") || query_uri.endsWith(".bin/")) {
					query = new URIImpl(query_uri);
				} else if (query_uri.endsWith("/")) {
					query = new URIImpl(query_uri.substring(0, query_uri.length() - 2) + ".bin");
				} else {
					query = new URIImpl(query_uri + ".bin");
				}
			}

			contains_triples.next();
		}

		if (program == null || query == null)
			throw new RepositoryException("program and/or query resoruce not found");

		// get Program as file
		// OutputStream program_data = new ByteArrayOutputStream();
		//InputStream program_data = binaryStore.read(program);
		//InputStream query_data = binaryStore.read(query);
		// ldpService.exportBinaryResource(connection, program, program_data);
		// if (programs.hasNext()) {
		// do nothing yet
		// handle multiple programs with same WebService
		// }

		// return Response.ok(
		// new GenericEntity<Iterable<Node[]>>(
		// executeWebService(service, postBody, "",
		// programs.next().getObject().stringValue()) ) { }
		// );
		
			return executeLinkedDataWebService(program.stringValue(), query.stringValue());
		} catch (RepositoryException e) {
			List<Node[]> error = new ArrayList<Node[]>();
			e.printStackTrace();
			//log.error("sparql.ParseException: ", e);
			error.add(new Node[] { new BNode("error"), RDFS.LABEL, new Literal("failed!") });
			error.add(new Node[] { new BNode("error"), RDFS.COMMENT,
					new Literal("sparql.ParseException: " + e.getMessage()) });
			return error;
		}
 
	}

	public Iterable<Node[]> readDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	public SemanticStateBasedResource retrieve(String arg0) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean update(Iterable<Node[]> arg0) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}
	
	




	/**
	 * @author sba
	 * 
	 *         Integrate Linked Data-Fu into Marmotta
	 * 
	 * @param resource
	 * @param rb
	 * @param postBody
	 * @param query
	 * @param program_resource
	 * @param query_resource 
	 * @param format
	 * @return
	 * @throws IllegalArgumentException
	 */
	private Iterable<Node[]> executeLinkedDataWebService(String program_resource, String query_resource) throws IllegalArgumentException {

		ValueFactory factory = ValueFactoryImpl.getInstance();
		List<Node[]> results = new ArrayList<Node[]>();

		/*
		 * Linked Data-Fu execution
		 */

		try {
			LdpBinaryStoreService binaryStore = (LdpBinaryStoreService) store;
			// OutputStream program_data = new ByteArrayOutputStream();
			InputStream program_data = binaryStore.read(new URIImpl(program_resource));
			InputStream query_data = binaryStore.read(new URIImpl(query_resource));
			// ldpService.exportBinaryResource(connection, program, program_data);

			/*
			 * Generate a Program Object
			 */
			Origin program_origin = new InternalOrigin("programOriginTriple");
			ProgramConsumerImpl programConsumer = new ProgramConsumerImpl(program_origin);

			Notation3Parser notation3Parser = new Notation3Parser(program_data);
			notation3Parser.parse(programConsumer, program_origin);
			Program program = programConsumer.getProgram(program_origin);

			/*
			 * Generate a Graph Object
			 */
			// TurtleParser turtleParser = new TurtleParser(input_nodes,
			// Charset.defaultCharset(), new java.net.URI( resource.stringValue() ) );
			RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
			org.openrdf.model.Graph myGraph = new org.openrdf.model.impl.GraphImpl();
			StatementCollector collector = new StatementCollector(myGraph);
			rdfParser.setRDFHandler(collector);

			List<org.semanticweb.yars.nx.Node[]> input_nodes = new LinkedList<org.semanticweb.yars.nx.Node[]>();

			myGraph.forEach(s -> {
				org.semanticweb.yars.nx.Node[] node = { new org.semanticweb.yars.nx.Resource(s.getSubject().toString()),
						new org.semanticweb.yars.nx.Resource(s.getSubject().toString()),
						new org.semanticweb.yars.nx.Resource(s.getSubject().toString()) };
				//log.warn("Input Nodes: " + node[0] + " " + node[1] + " " + node[2]);
				input_nodes.add(node);
			});

			/*
			 * Register a Query
			 */
			QueryConsumerImpl qc = new QueryConsumerImpl(new InternalOrigin("query_consumer_1"));
			//String s = new String("CONSTRUCT { ?s ?p ?o . } WHERE { ?s ?p ?o . }");
			//SparqlParser sp = new SparqlParser(new StringReader(s));
			SparqlParser sp = new SparqlParser(new InputStreamReader(query_data));

			sp.parse(qc, new InternalOrigin("SparqlQuery"));
			ConstructQuery sq = qc.getConstructQueries().iterator().next();

			BindingConsumerCollection bc = new BindingConsumerCollection();
			BindingConsumerSink sink = new BindingConsumerSink(bc);

			program.registerConstructQuery(sq, sink);

			/*
			 * Create an EvaluateProgram Object
			 */
			EvaluateProgramConfig config = new EvaluateProgramConfig();
			EvaluateProgramGenerator ep = new EvaluateProgramGenerator(program, config);
			EvaluateProgram epg = ep.getEvaluateProgram();

			/*
			 * Evaluate the Program
			 */
			epg.start();

			epg.awaitIdleAndFinish();

			epg.shutdown();

			for (Binding binding : bc.getCollection()) {

				Nodes nodes = binding.getNodes();
				Node[] node = nodes.getNodeArray();

				String subj_string = node[0].toString().replace("<", "").replace(">", "").replace("\"", "");
				org.openrdf.model.Resource subject = null;

				if (subj_string.startsWith("_")) {

					// is BlankNode
					subject = factory.createBNode(subj_string.replace("_:", ""));

				} else {

					subject = factory.createURI(subj_string);

				}

				String predicate_string = node[1].toString().replace("<", "").replace(">", "").replace("\"", "");
				URI predicate = factory.createURI(predicate_string);

				// String object_string = node[2].toString().replace("<", "").replace(">",
				// "").replace("\"", "");
				// try {
				//
				//
				// Value object = factory.createURI( object_string );
				results.add(node);
				//
				//
				//
				// } catch (IllegalArgumentException e) {
				//
				// Value object = factory.createLiteral( object_string );
				// results.add( factory.createStatement(subject, predicate, object) );
				//
				// }

			}

		} catch (edu.kit.aifb.datafu.parser.sparql.ParseException e) {
			e.printStackTrace();
			//log.error("sparql.ParseException: ", e);
			results.add(new Node[] { new BNode("error"), RDFS.LABEL, new Literal("failed!") });
			results.add(new Node[] { new BNode("error"), RDFS.COMMENT,
					new Literal("sparql.ParseException: " + e.getMessage()) });
		} catch (edu.kit.aifb.datafu.parser.notation3.ParseException e) {
			e.printStackTrace();
			//log.error("notation3.ParseException: ", e);
			results.add(new Node[] { new BNode("error"), RDFS.LABEL, new Literal("failed!") });
			results.add(new Node[] { new BNode("error"), RDFS.COMMENT,
					new Literal("notation3.ParseException: " + e.getMessage()) });
		} catch (InterruptedException e) {
			e.printStackTrace();
			//log.error("InterruptedException: ", e);
			results.add(new Node[] { new BNode("error"), RDFS.LABEL, new Literal("failed!") });
			results.add(new Node[] { new BNode("error"), RDFS.COMMENT,
					new Literal("InterruptedException: " + e.getMessage()) });
		} catch (IOException e) {
			e.printStackTrace();
			//log.error("IOException: ", e);
			results.add(new Node[] { new BNode("error"), RDFS.LABEL, new Literal("failed!") });
			results.add(new Node[] { new BNode("error"), RDFS.COMMENT, new Literal("IOException: " + e.getMessage()) });
		}

		return results;

	}
	
	public org.openrdf.model.Resource getContainer(String child, RepositoryConnection conn) throws RepositoryException {

		//child = child.substring(0, child.lastIndexOf("/"));

		RepositoryResult<Statement> result = conn.getStatements(null,
				ValueFactoryImpl.getInstance().createURI(LDP.contains.stringValue()),
				ValueFactoryImpl.getInstance().createURI(child)
				, true);

		if (result.hasNext()) {
			return	result.next().getSubject();
		} else {

			throw new RepositoryException("No such resurce.");

		}
	}

}
