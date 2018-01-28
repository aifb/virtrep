/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.marmotta.platform.ldp.webservices;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.IBayesInferrer;
import org.eclipse.recommenders.jayes.inference.jtree.JunctionTreeAlgorithm;

import edu.kit.aifb.step.api.SemanticStateBasedResource;
import edu.kit.aifb.step.resources.*;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.lang3.StringUtils;

import org.apache.marmotta.commons.http.ContentType;
import org.apache.marmotta.commons.http.MarmottaHttpUtils;
import org.apache.marmotta.commons.vocabulary.LDP;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.apache.marmotta.platform.core.api.exporter.ExportService;
import org.apache.marmotta.platform.core.api.io.MarmottaIOService;
import org.apache.marmotta.platform.core.api.triplestore.SesameService;
import org.apache.marmotta.platform.core.events.SesameStartupEvent;
import org.apache.marmotta.platform.ldp.api.LdpBinaryStoreService;
import org.apache.marmotta.platform.ldp.api.LdpService;
import org.apache.marmotta.platform.ldp.api.Preference;
import org.apache.marmotta.platform.ldp.exceptions.IncompatibleResourceTypeException;
import org.apache.marmotta.platform.ldp.exceptions.InvalidInteractionModelException;
import org.apache.marmotta.platform.ldp.exceptions.InvalidModificationException;
import org.apache.marmotta.platform.ldp.patch.InvalidPatchDocumentException;
import org.apache.marmotta.ldpath.parser.ParseException;
import org.apache.marmotta.platform.ldp.patch.parser.RdfPatchParser;
import org.apache.marmotta.platform.ldp.util.AbstractResourceUriGenerator;
import org.apache.marmotta.platform.ldp.util.LdpUtils;
import org.apache.marmotta.platform.ldp.util.RandomUriGenerator;
import org.apache.marmotta.platform.ldp.util.SlugUriGenerator;

import org.jboss.resteasy.spi.NoLogWebApplicationException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.openrdf.rio.helpers.StatementCollector;
import org.semanticweb.yars.nx.BNode;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Nodes;
//import org.semanticweb.yars.nx.Resource;
//import org.semanticweb.yars.nx.namespace.OWL;
//import org.semanticweb.yars.nx.namespace.XSD;
//import org.semanticweb.yars.nx.namespace.LDP;
//import org.semanticweb.yars.turtle.TurtleParseException;
//import org.semanticweb.yars.turtle.TurtleParser;
import org.semanticweb.yars.nx.namespace.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Service.State;

//import com.jayway.restassured.builder.ResponseBuilder;

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
import edu.kit.aifb.step.vocabs.DBO;
import edu.kit.aifb.step.vocabs.HYDRA;
import edu.kit.aifb.step.vocabs.MEXCORE;
import edu.kit.aifb.step.vocabs.STEP;
import edu.kit.aifb.step.wrapper.Utils;
import info.aduna.iteration.CloseableIteration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.swing.plaf.synth.SynthTextPaneUI;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.sun.management.OperatingSystemMXBean;

/**
 * Linked Data Platform web services.
 *
 * @see <a href="http://www.w3.org/TR/ldp/">http://www.w3.org/TR/ldp/</a>
 *
 * @author Sergio Fernández
 * @author Jakob Frank
 */
@ApplicationScoped
@Path(LdpWebService.PATH + "{local:.*}")
public class LdpWebService {

	public static final String PATH = "/ldp"; // TODO: at some point this will be root ('/') in marmotta
	public static final String LDP_SERVER_CONSTRAINTS = "http://wiki.apache.org/marmotta/LDPImplementationReport/2014-09-16";

	public static final String LINK_REL_DESCRIBEDBY = "describedby";
	public static final String LINK_REL_CONSTRAINEDBY = "http://www.w3.org/ns/ldp#constrainedBy";
	public static final String LINK_REL_CONTENT = "content";
	public static final String LINK_REL_META = "meta";
	public static final String LINK_REL_TYPE = "type";
	public static final String LINK_PARAM_ANCHOR = "anchor";
	public static final String HTTP_HEADER_SLUG = "Slug";
	public static final String HTTP_HEADER_ACCEPT_POST = "Accept-Post";
	public static final String HTTP_HEADER_ACCEPT_PATCH = "Accept-Patch";
	public static final String HTTP_HEADER_PREFER = "Prefer";
	public static final String HTTP_HEADER_PREFERENCE_APPLIED = "Preference-Applied";
	public static final String HTTP_METHOD_PATCH = "PATCH";

	public static final String PROGRAM_TRIPLE = "<http://coordinate> <http://x> \"1\" . "
			+ "<http://coordinate> <http://y> \"2\" . " + "<http://coordinate> <http://z> \"3\" . ";
	public static final String QUERY_CONSTRUCT_SPO = "CONSTRUCT { ?s ?p ?o . } WHERE { ?s ?p ?o . }";

	private static final Logger log = LoggerFactory.getLogger(LdpWebService.class);

	// counter for some statistics
	public static int numberOfIntegrationRequests;

	public static Map<Node, String> interactionPatterns = new HashMap<Node, String>();

	@Inject
	private ConfigurationService configurationService;

	@Inject
	private LdpService ldpService;

	@Inject
	private ExportService exportService;

	@Inject
	private SesameService sesameService;

	@Inject
	private MarmottaIOService ioService;

	@Inject
	private LdpBinaryStoreService binaryStore;

	// @Inject
	// private VirtualResource virtualResource;

	private final List<ContentType> producedRdfTypes;
	private final org.openrdf.model.Resource ldpContext = ValueFactoryImpl.getInstance().createURI(LDP.NAMESPACE);

	public LdpWebService() {
		producedRdfTypes = new ArrayList<>();

		final List<RDFFormat> availableWriters = LdpUtils
				.filterAvailableWriters(LdpService.SERVER_PREFERED_RDF_FORMATS);
		for (RDFFormat format : RDFWriterRegistry.getInstance().getKeys()) {
			final String primaryQ;
			final int idx = availableWriters.indexOf(format);
			if (idx < 0) {
				// not a prefered format
				primaryQ = ";q=0.5";
			} else {
				// a prefered format
				primaryQ = String.format(Locale.ENGLISH, ";q=%.1f", Math.max(1.0 - (idx * 0.1), 0.55));
			}

			final String secondaryQ = ";q=0.3";
			final List<String> mimeTypes = format.getMIMETypes();
			for (int i = 0; i < mimeTypes.size(); i++) {
				final String mime = mimeTypes.get(i);
				if (i == 0) {
					// first mimetype is the default
					producedRdfTypes.add(MarmottaHttpUtils.parseContentType(mime + primaryQ));
				} else {
					producedRdfTypes.add(MarmottaHttpUtils.parseContentType(mime + secondaryQ));
				}
			}
		}
		Collections.sort(producedRdfTypes);

		log.debug("Available RDF Serializer: {}", producedRdfTypes);

		// Including the FLSWrapper into a Hash Map
		// Marcel
		interactionPatterns.put(STEP.BayesNode, "BayesNode");
		interactionPatterns.put(STEP.FLSVisitourAPI, "edu.kit.aifb.step.resources.FlsVisitourCallContainer");

	}

	protected void initialize(@Observes SesameStartupEvent event) {
		log.info("Starting up LDP WebService Endpoint");
		String root = UriBuilder.fromUri(configurationService.getBaseUri()).path(LdpWebService.PATH).build()
				.toASCIIString();
		try {
			final RepositoryConnection conn = sesameService.getConnection();
			try {
				conn.begin();
				ldpService.init(conn, conn.getValueFactory().createURI(root));
				log.debug("Created LDP root container <{}>", root);
				conn.commit();
			} finally {
				conn.close();
			}
		} catch (RepositoryException e) {
			log.error("Error creating LDP root container <{}>: {}", root, e.getMessage(), e);
		}
	}

	@GET
	public Response GET(@Context final UriInfo uriInfo,
			@HeaderParam(HttpHeaders.ACCEPT) @DefaultValue(MediaType.WILDCARD) String type,
			@HeaderParam(HTTP_HEADER_PREFER) PreferHeader preferHeader) throws RepositoryException {

		final String resource = ldpService.getResourceUri(uriInfo);
		final RepositoryConnection conn = sesameService.getConnection();
		conn.begin();

		try {
			if (isCallInstanze(resource, conn)) {

				//TODO make FlsWrapper request

				FlsVisitourCallInstance callInstance = new FlsVisitourCallInstance(
						"http://localhost:8080/marmotta/ldp/FLS-1", resource.substring(resource.lastIndexOf("/")+1));

				if (type.contains(MediaType.TEXT_PLAIN.toLowerCase())
						|| type.contains(MediaType.TEXT_HTML.toLowerCase())) {
					String graph = "";
					Iterable<Node[]> nodes = callInstance.read();
					Iterator<Node[]> iter = nodes.iterator();
					while (iter.hasNext()) {
						Node[] node = iter.next();
						graph += node[0].getLabel() + " " + node[1].getLabel() + " " + node[2].getLabel() + "\n";
					}

					conn.commit();
					return Response.status(Status.OK)
							.entity("the fls visitour REST web service for specific calls: " + graph)
							.variants(Utils.getVariants()).build();

				} else {

					conn.commit();
					return Response.ok().entity(new GenericEntity<Iterable<Node[]>>(callInstance.read()) {
					}).variants(Utils.getVariants()).build();

				}
			} else if ( isVirtualResource(resource, conn) ) {


				RDFFormat format = RDFFormat.TURTLE;
				try {
					format = RDFFormat.forMIMEType(MarmottaHttpUtils.parseAcceptHeader(type).get(0).getMime());
				} catch (Exception e) {

				}
				Resource service = getContainer(resource, conn);
				return executeLinkedDataWebService(conn, service, Response.ok(), null, format).build();

			}	
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RepositoryException(e);
		} finally {
			conn.close();
		}


		log.debug("GET to LDPR <{}>", resource);
		return buildGetResponse(resource, MarmottaHttpUtils.parseAcceptHeader(type), preferHeader).build();
	}

	@HEAD
	public Response HEAD(@Context final UriInfo uriInfo,
			@HeaderParam(HttpHeaders.ACCEPT) @DefaultValue(MediaType.WILDCARD) String type,
			@HeaderParam(HTTP_HEADER_PREFER) PreferHeader preferHeader) throws RepositoryException {
		numberOfIntegrationRequests++;
		final String resource = ldpService.getResourceUri(uriInfo);
		log.debug("HEAD to LDPR <{}>", resource);
		return buildGetResponse(resource, MarmottaHttpUtils.parseAcceptHeader(type), preferHeader).entity(null).build();
	}

	private Response.ResponseBuilder buildGetResponse(final String resource, List<ContentType> acceptedContentTypes,
			PreferHeader preferHeader) throws RepositoryException {
		log.trace("LDPR requested media type {}", acceptedContentTypes);
		final RepositoryConnection conn = sesameService.getConnection();
		try {
			conn.begin();

			log.trace("Checking existence of {}", resource);
			if (!ldpService.exists(conn, resource)) {
				log.debug("{} does not exist", resource);
				final Response.ResponseBuilder resp;
				if (ldpService.isReusedURI(conn, resource)) {
					resp = createResponse(conn, Response.Status.GONE, resource);
				} else {
					resp = createResponse(conn, Response.Status.NOT_FOUND, resource);
				}
				conn.rollback();
				return resp;
			} else {
				log.trace("{} exists, continuing", resource);

				/*
				 * sba
				 */
				updateStatistics(resource);
			}

			// Content-Neg
			if (ldpService.isNonRdfSourceResource(conn, resource)) {
				log.trace("<{}> is marked as LDP-NR", resource);
				// LDP-NR
				final ContentType realType = MarmottaHttpUtils.parseContentType(ldpService.getMimeType(conn, resource));
				if (realType == null) {
					log.debug("<{}> has no format information - try some magic...");
					final ContentType rdfContentType = MarmottaHttpUtils.bestContentType(producedRdfTypes,
							acceptedContentTypes);
					if (MarmottaHttpUtils.bestContentType(MarmottaHttpUtils.parseAcceptHeader("*/*"),
							acceptedContentTypes) != null) {
						log.trace(
								"Unknown type of LDP-NR <{}> is compatible with wildcard - sending back LDP-NR without Content-Type",
								resource);
						// Client will accept anything, send back LDP-NR
						final Response.ResponseBuilder resp = buildGetResponseBinaryResource(conn, resource,
								preferHeader);
						conn.commit();
						return resp;
					} else if (rdfContentType == null) {
						log.trace(
								"LDP-NR <{}> has no type information, sending HTTP 409 with hint for wildcard 'Accept: */*'",
								resource);
						// Client does not look for a RDF Serialisation, send back 409 Conflict.
						log.debug(
								"No corresponding LDP-RS found for <{}>, sending HTTP 409 with hint for wildcard 'Accept: */*'",
								resource);
						final Response.ResponseBuilder resp = build406Response(conn, resource,
								Collections.<ContentType>emptyList());
						conn.commit();
						return resp;
					} else {
						log.debug("Client is asking for a RDF-Serialisation of LDP-NS <{}>, sending meta-data",
								resource);
						final Response.ResponseBuilder resp = buildGetResponseSourceResource(conn, resource,
								Rio.getWriterFormatForMIMEType(rdfContentType.getMime(), RDFFormat.TURTLE),
								preferHeader);
						conn.commit();
						return resp;
					}
				} else if (MarmottaHttpUtils.bestContentType(Collections.singletonList(realType),
						acceptedContentTypes) == null) {
					log.trace("Client-accepted types {} do not include <{}>-s available type {} - trying some magic...",
							acceptedContentTypes, resource, realType);
					// requested types do not match the real type - maybe an rdf-type is accepted?
					final ContentType rdfContentType = MarmottaHttpUtils.bestContentType(producedRdfTypes,
							acceptedContentTypes);
					if (rdfContentType == null) {
						log.debug("Can't send <{}> ({}) in any of the accepted formats: {}, sending 406", resource,
								realType, acceptedContentTypes);
						final Response.ResponseBuilder resp = build406Response(conn, resource,
								Collections.singletonList(realType));
						conn.commit();
						return resp;
					} else {
						log.debug("Client is asking for a RDF-Serialisation of LDP-NS <{}>, sending meta-data",
								resource);
						final Response.ResponseBuilder resp = buildGetResponseSourceResource(conn, resource,
								Rio.getWriterFormatForMIMEType(rdfContentType.getMime(), RDFFormat.TURTLE),
								preferHeader);
						conn.commit();
						return resp;
					}
				} else {
					final Response.ResponseBuilder resp = buildGetResponseBinaryResource(conn, resource, preferHeader);
					conn.commit();
					return resp;
				}
			} else {
				// Requested Resource is a LDP-RS
				final ContentType bestType = MarmottaHttpUtils.bestContentType(producedRdfTypes, acceptedContentTypes);
				if (bestType == null) {
					log.trace("Available formats {} do not match any of the requested formats {} for <{}>, sending 406",
							producedRdfTypes, acceptedContentTypes, resource);
					final Response.ResponseBuilder resp = build406Response(conn, resource, producedRdfTypes);
					conn.commit();
					return resp;
				} else {
					final Response.ResponseBuilder resp = buildGetResponseSourceResource(conn, resource,
							Rio.getWriterFormatForMIMEType(bestType.getMime(), RDFFormat.TURTLE), preferHeader);
					conn.commit();
					return resp;
				}
			}
		} catch (final Throwable t) {
			conn.rollback();
			throw t;
		} finally {
			conn.close();
		}
	}

	private Response.ResponseBuilder build406Response(RepositoryConnection connection, String resource,
			List<ContentType> availableContentTypes) throws RepositoryException {
		final Response.ResponseBuilder response = createResponse(connection, Response.Status.NOT_ACCEPTABLE, resource);
		if (availableContentTypes.isEmpty()) {
			response.entity(String.format("%s is not available in the requested format%n", resource));
		} else {
			response.entity(String.format("%s is only available in the following formats: %s%n", resource,
					availableContentTypes));
		}
		// Sec. 4.2.2.2
		return addOptionsHeader(connection, resource, response);
	}

	private Response.ResponseBuilder buildGetResponseBinaryResource(RepositoryConnection connection,
			final String resource, PreferHeader preferHeader) throws RepositoryException {
		final String realType = ldpService.getMimeType(connection, resource);
		log.debug("Building response for LDP-NR <{}> with format {}", resource, realType);
		final Preference preference = LdpUtils.parsePreferHeader(preferHeader);
		final StreamingOutput entity = new StreamingOutput() {
			@Override
			public void write(OutputStream out) throws IOException, WebApplicationException {
				try {
					final RepositoryConnection outputConn = sesameService.getConnection();
					try {
						outputConn.begin();
						ldpService.exportBinaryResource(outputConn, resource, out);
						outputConn.commit();
					} catch (RepositoryException | IOException e) {
						outputConn.rollback();
						throw new WebApplicationException(e,
								createResponse(Response.status(Response.Status.INTERNAL_SERVER_ERROR)).entity(e)
								.build());
					} finally {
						outputConn.close();
					}
				} catch (RepositoryException e) {
					throw new WebApplicationException(e,
							createResponse(Response.status(Response.Status.INTERNAL_SERVER_ERROR)).entity(e).build());
				}
			}
		};
		// Sec. 4.2.2.2
		final Response.ResponseBuilder resp = addOptionsHeader(connection, resource,
				createResponse(connection, Response.Status.OK, resource).entity(entity).type(realType));
		if (preferHeader != null) {
			if (preference.isMinimal()) {
				resp.status(Response.Status.NO_CONTENT).entity(null).header(HTTP_HEADER_PREFERENCE_APPLIED,
						PreferHeader.fromPrefer(preferHeader).parameters(null).build());
			}
		}
		return resp;
	}

	private Response.ResponseBuilder buildGetResponseSourceResource(RepositoryConnection conn, final String resource,
			final RDFFormat format, final PreferHeader preferHeader) throws RepositoryException {
		// Deliver all triples from the <subject> context.
		log.debug("Building response for LDP-RS <{}> with RDF format {}", resource, format.getDefaultMIMEType());
		final Preference preference = LdpUtils.parsePreferHeader(preferHeader);
		final StreamingOutput entity = new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				try {
					final RepositoryConnection outputConn = sesameService.getConnection();
					try {
						outputConn.begin();
						ldpService.exportResource(outputConn, resource, output, format, preference);
						outputConn.commit();
					} catch (RDFHandlerException e) {
						outputConn.rollback();
						throw new NoLogWebApplicationException(e,
								createResponse(Response.status(Response.Status.INTERNAL_SERVER_ERROR))
								.entity(e.getMessage()).build());
					} catch (final Throwable t) {
						outputConn.rollback();
						throw t;
					} finally {
						outputConn.close();
					}
				} catch (RepositoryException e) {
					throw new WebApplicationException(e,
							createResponse(Response.status(Response.Status.INTERNAL_SERVER_ERROR)).entity(e).build());
				}
			}
		};
		// Sec. 4.2.2.2
		final Response.ResponseBuilder resp = addOptionsHeader(conn, resource,
				createResponse(conn, Response.Status.OK, resource).entity(entity).type(format.getDefaultMIMEType()));
		if (preference != null) {
			if (preference.isMinimal()) {
				resp.status(Response.Status.NO_CONTENT).entity(null);
			}
			resp.header(HTTP_HEADER_PREFERENCE_APPLIED, PreferHeader.fromPrefer(preferHeader).parameters(null).build());
		}
		return resp;
	}

	/**********************************************************************************************************************
	 * LDP Post Request.
	 *
	 * @see <a href=
	 *      "https://dvcs.w3.org/hg/ldpwg/raw-file/default/ldp.html#ldpr-HTTP_POST">5.4
	 *      LDP-R POST</a>
	 * @see <a href=
	 *      "https://dvcs.w3.org/hg/ldpwg/raw-file/default/ldp.html#ldpc-HTTP_POST">6.4
	 *      LDP-C POST</a>
	 **********************************************************************************************************************/
	@POST
	public Response POST(@Context UriInfo uriInfo, @HeaderParam(HTTP_HEADER_SLUG) String slug,
			@HeaderParam(HttpHeaders.LINK) List<Link> linkHeaders,
			@HeaderParam(HttpHeaders.ACCEPT) @DefaultValue(MediaType.WILDCARD) String accept_type, InputStream postBody,
			@HeaderParam(HttpHeaders.CONTENT_TYPE) MediaType type) throws RepositoryException, URISyntaxException {
		// @POST
		// public Response POST(@Context UriInfo uriInfo, @HeaderParam(HTTP_HEADER_SLUG)
		// String slug,
		// @HeaderParam(HttpHeaders.LINK) List<Link> linkHeaders,
		// Iterable<Node[]> postBody, @HeaderParam(HttpHeaders.CONTENT_TYPE) MediaType
		// type)
		// throws RepositoryException {

		//numberOfIntegrationRequests++;

		final String container = ldpService.getResourceUri(uriInfo);
		log.debug("POST to LDPC <{}>", container);

		RDFFormat format = RDFFormat.TURTLE;
		try {
			format = RDFFormat.forMIMEType(MarmottaHttpUtils.parseAcceptHeader(accept_type).get(0).getMime());
		} catch (Exception e) {

		}

		final RepositoryConnection conn = sesameService.getConnection();
		try {
			conn.begin();

			if (!ldpService.exists(conn, container)) {
				final Response.ResponseBuilder resp;
				if (ldpService.isReusedURI(conn, container)) {
					log.debug("<{}> has been deleted, can't POST to it!", container);
					resp = createResponse(conn, Response.Status.GONE, container);
				} else {
					log.debug("<{}> does not exists, can't POST to it!", container);
					resp = createResponse(conn, Response.Status.NOT_FOUND, container);
				}
				conn.rollback();
				return resp.build();
			}
			/*
			 * sba:
			 */
			else {

				updateStatistics(container);
			}

			/*
			 * sba:
			 */
			if ( ldpService.isStartAPI(conn, container) ) {
				log.debug("<{}> exists and is a LinkedDataWebService, so this triggers the service", container);

				// RepositoryResult<Statement> statements = conn.getStatements(
				// ValueFactoryImpl.getInstance().createURI(resource),
				// ValueFactoryImpl.getInstance().createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
				// null, true, new Resource[0]);

				final Response.ResponseBuilder resp = createWebServiceResponse(conn, 200, container, postBody, format);

				log.debug("Invoking Web Service for <{}> successful", container);
				conn.commit();
				return resp.build();

			}

			if (ldpService.isVirtualResource(conn, container)) {

				// =============================================================================================
				//
				// is a FLSResource
				//
				// =============================================================================================
				RepositoryResult<Statement> neededPatterns = conn.getStatements(
						ValueFactoryImpl.getInstance().createURI(ldpService.getResourceUri(uriInfo)),
						ValueFactoryImpl.getInstance().createURI(STEP.interactionPattern.getLabel()), null, true);

				while (neededPatterns.hasNext()) {
					Statement neededClass = neededPatterns.next();
					try {

						final String resource = ldpService.getResourceUri(uriInfo);
						final Response.ResponseBuilder response = createResponse(conn, Response.Status.CREATED,
								resource);
						Class cls_Test = Class.forName(interactionPatterns
								.get(new org.semanticweb.yars.nx.Resource(neededClass.getObject().toString())));
						Constructor co = cls_Test.getConstructor(String.class);
						SemanticStateBasedResource res = (SemanticStateBasedResource) co.newInstance(resource);
						res.create(postBodyToIterableNode(postBody, new URIImpl(resource)));

						conn.commit();
						return response.build();

					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InstantiationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			// Check that the target container supports the LDPC Interaction Model
			final LdpService.InteractionModel containerModel = ldpService.getInteractionModel(conn, container);
			if (containerModel != LdpService.InteractionModel.LDPC) {
				final Response.ResponseBuilder response = createResponse(conn, Response.Status.METHOD_NOT_ALLOWED,
						container);
				conn.commit();
				return response
						.entity(String.format("%s only supports %s Interaction Model", container, containerModel))
						.build();
			}

			// Get the LDP-Interaction Model (Sec. 5.2.3.4 and Sec. 4.2.1.4)
			final LdpService.InteractionModel ldpInteractionModel = ldpService.getInteractionModel(linkHeaders);

			if (ldpService.isNonRdfSourceResource(conn, container)) {
				log.info("POSTing to a NonRdfSource is not allowed ({})", container);
				final Response.ResponseBuilder response = createResponse(conn, Response.Status.METHOD_NOT_ALLOWED,
						container).entity("POST to NonRdfSource is not allowed\n");
				conn.commit();
				return response.build();
			}

			final AbstractResourceUriGenerator uriGenerator;
			if (StringUtils.isBlank(slug)) {
				/* Sec. 5.2.3.8) */
				uriGenerator = new RandomUriGenerator(ldpService, container, conn);
			} else {
				// Honor client wishes from Slug-header (Sec. 5.2.3.10)
				// http://www.ietf.org/rfc/rfc5023.txt
				log.trace("Slug-Header is '{}'", slug);
				uriGenerator = new SlugUriGenerator(ldpService, container, slug, conn);
			}

			final String newResource = uriGenerator.generateResourceUri();

			log.debug("POST to <{}> will create new LDP-R <{}>", container, newResource);
			// connection is closed by buildPostResponse
			return buildPostResponse(conn, container, newResource, ldpInteractionModel, postBody, type);
			// return buildPostResponse(conn, container, newResource, ldpInteractionModel,
			// null, type);
		} catch (InvalidInteractionModelException e) {
			log.debug("POST with invalid interaction model <{}> to <{}>", e.getHref(), container);
			final Response.ResponseBuilder response = createResponse(conn, Response.Status.BAD_REQUEST, container);
			conn.commit();
			return response.entity(e.getMessage()).build();
		} catch (IllegalArgumentException e) {
			log.debug("POST with invalid body content: {}", container);
			final Response.ResponseBuilder response = createResponse(conn, Response.Status.BAD_REQUEST, container);
			conn.commit();
			return response.entity(e.getMessage()).build();
		} catch (MissingArgumentException e) {
			log.debug("POST with invalid body content: {}", container);
			final Response.ResponseBuilder response = createResponse(conn, Response.Status.BAD_REQUEST, container);
			conn.commit();
			return response.entity(e.getMessage()).build();
		} catch (RemoteException e) {
			log.debug("POST with invalid body content: {}", container);
			final Response.ResponseBuilder response = createResponse(conn, Response.Status.BAD_REQUEST, container);
			conn.commit();
			return response.entity(e.getMessage()).build();
		} catch (InvocationTargetException e) {
			log.debug("POST with invalid body content: {}", container);
			final Response.ResponseBuilder response = createResponse(conn, Response.Status.BAD_REQUEST, container);
			conn.commit();
			return response.entity(e.getMessage()).build();
		} catch (IllegalAccessException e) {
			log.debug("POST with invalid body content: {}", container);
			final Response.ResponseBuilder response = createResponse(conn, Response.Status.BAD_REQUEST, container);
			conn.commit();
			return response.entity(e.getMessage()).build();
		} catch (final Throwable t) {
			conn.rollback();
			throw t;
		} finally {
			conn.close();
		}
	}

	/**
	 * @param connection
	 *            the RepositoryConnection (with active transaction) to read extra
	 *            data from. WILL BE COMMITTED OR ROLLBACKED
	 * @throws RepositoryException
	 */
	private Response buildPostResponse(RepositoryConnection connection, String container, String newResource,
			LdpService.InteractionModel interactionModel, InputStream requestBody, MediaType type)
					throws RepositoryException {
		final String mimeType = LdpUtils.getMimeType(type);
		// checking if resource (container) exists is done later in the service
		try {
			String location = ldpService.addResource(connection, container, newResource, interactionModel, mimeType,
					requestBody);
			final Response.ResponseBuilder response = createResponse(connection, Response.Status.CREATED, container)
					.location(java.net.URI.create(location));
			if (newResource.compareTo(location) != 0) {
				response.links(
						Link.fromUri(newResource).rel(LINK_REL_DESCRIBEDBY).param(LINK_PARAM_ANCHOR, location).build());
			}
			connection.commit();
			return response.build();
		} catch (IOException | RDFParseException e) {
			final Response.ResponseBuilder resp = createResponse(connection, Response.Status.BAD_REQUEST, container)
					.entity(e.getClass().getSimpleName() + ": " + e.getMessage());
			connection.rollback();
			return resp.build();
		} catch (UnsupportedRDFormatException e) {
			final Response.ResponseBuilder resp = createResponse(connection, Response.Status.UNSUPPORTED_MEDIA_TYPE,
					container).entity(e);
			connection.rollback();
			return resp.build();
		}
	}

	/*************************************************************************************************************
	 * 
	 * Handle PUT (Sec. 4.2.4, Sec. 5.2.4)
	 * 
	 *************************************************************************************************************/
	@PUT
	public Response PUT(@Context UriInfo uriInfo, @Context Request request,
			@HeaderParam(HttpHeaders.LINK) List<Link> linkHeaders, @HeaderParam(HttpHeaders.IF_MATCH) EntityTag eTag,
			@HeaderParam(HttpHeaders.CONTENT_TYPE) MediaType type, InputStream postBody)
					throws RepositoryException, IOException, InvalidModificationException, RDFParseException,
					IncompatibleResourceTypeException, URISyntaxException {

		//numberOfIntegrationRequests++;

		final String resource = ldpService.getResourceUri(uriInfo);
		log.debug("PUT to <{}>", resource);

		final RepositoryConnection conn = sesameService.getConnection();
		try {
			conn.begin();

			final String mimeType = LdpUtils.getMimeType(type);
			final Response.ResponseBuilder resp;
			final String newResource; // NOTE: newResource == resource for now, this might change in the future
			if (ldpService.exists(conn, resource)) {

				log.debug("<{}> exists and is a DataResource, so this is an UPDATE", resource);

				/*
				 * sba:
				 */
				updateStatistics(resource);

				if (eTag == null) {
					// check for If-Match header (ETag) -> 428 Precondition Required (Sec. 4.2.4.5)
					log.trace("No If-Match header, but that's a MUST");
					resp = createResponse(conn, 428, resource);
					conn.rollback();
					return resp.build();
				} else {
					// check ETag -> 412 Precondition Failed (Sec. 4.2.4.5)
					log.trace("Checking If-Match: {}", eTag);
					EntityTag hasTag = ldpService.generateETag(conn, resource);
					if (!eTag.equals(hasTag)) {
						log.trace("If-Match header did not match, expected {}", hasTag);
						resp = createResponse(conn, Response.Status.PRECONDITION_FAILED, resource);
						conn.rollback();
						return resp.build();
					}
				}

				newResource = ldpService.updateResource(conn, resource, postBody, mimeType);
				log.debug("PUT update for <{}> successful", newResource);
				resp = createResponse(conn, Response.Status.OK, resource);
				conn.commit();
				return resp.build();

			} else if (ldpService.isReusedURI(conn, resource)) {
				log.debug("<{}> has been deleted, we should not re-use the URI!", resource);
				resp = createResponse(conn, Response.Status.GONE, resource);
				conn.commit();
				return resp.build();
			} else {
				log.debug("<{}> does not exist, so this is a CREATE", resource);
				// LDP servers may allow resource creation using PUT (Sec. 4.2.4.6)

				final String container = LdpUtils.getContainer(resource);
				try {
					// Check that the target container supports the LDPC Interaction Model
					final LdpService.InteractionModel containerModel = ldpService.getInteractionModel(conn, container);
					if (containerModel != LdpService.InteractionModel.LDPC) {
						final Response.ResponseBuilder response = createResponse(conn,
								Response.Status.METHOD_NOT_ALLOWED, container);
						conn.commit();
						return response.entity(
								String.format("%s only supports %s Interaction Model", container, containerModel))
								.build();
					}

					// Get the LDP-Interaction Model (Sec. 5.2.3.4 and Sec. 4.2.1.4)
					final LdpService.InteractionModel ldpInteractionModel = ldpService.getInteractionModel(linkHeaders);

					// connection is closed by buildPostResponse
					return buildPostResponse(conn, container, resource, ldpInteractionModel, postBody, type);
				} catch (InvalidInteractionModelException e) {
					log.debug("PUT with invalid interaction model <{}> to <{}>", e.getHref(), container);
					final Response.ResponseBuilder response = createResponse(conn, Response.Status.BAD_REQUEST,
							container);
					conn.commit();
					return response.entity(e.getMessage()).build();
				}
			}
		} catch (IOException | RDFParseException e) {
			final Response.ResponseBuilder resp = createResponse(conn, Response.Status.BAD_REQUEST, resource)
					.entity(e.getClass().getSimpleName() + ": " + e.getMessage());
			conn.rollback();
			return resp.build();
		} catch (InvalidModificationException | IncompatibleResourceTypeException e) {
			final Response.ResponseBuilder resp = createResponse(conn, Response.Status.CONFLICT, resource)
					.entity(e.getClass().getSimpleName() + ": " + e.getMessage());
			conn.rollback();
			return resp.build();
		} catch (final Throwable t) {
			conn.rollback();
			throw t;
		} finally {
			conn.close();
		}
	}

	/**
	 * Handle delete (Sec. 4.2.5, Sec. 5.2.5)
	 */
	@DELETE
	public Response DELETE(@Context UriInfo uriInfo) throws RepositoryException {

		//numberOfIntegrationRequests++;

		final String resource = ldpService.getResourceUri(uriInfo);
		log.debug("DELETE to <{}>", resource);

		final RepositoryConnection con = sesameService.getConnection();
		try {
			con.begin();

			if (!ldpService.exists(con, resource)) {
				final Response.ResponseBuilder resp;
				if (ldpService.isReusedURI(con, resource)) {
					resp = createResponse(con, Response.Status.GONE, resource);
				} else {
					resp = createResponse(con, Response.Status.NOT_FOUND, resource);
				}
				con.rollback();
				return resp.build();
			}

			ldpService.deleteResource(con, resource);
			final Response.ResponseBuilder resp = createResponse(con, Response.Status.NO_CONTENT, resource);
			con.commit();
			return resp.build();
		} catch (final Throwable e) {
			log.error("Error deleting LDP-R: {}: {}", resource, e.getMessage());
			con.rollback();
			throw e;
		} finally {
			con.close();
		}

	}

	@PATCH
	public Response PATCH(@Context UriInfo uriInfo, @HeaderParam(HttpHeaders.IF_MATCH) EntityTag eTag,
			@HeaderParam(HttpHeaders.CONTENT_TYPE) MediaType type, InputStream postBody) throws RepositoryException {




		final String resource = ldpService.getResourceUri(uriInfo);
		log.debug("PATCH to <{}>", resource);

		final RepositoryConnection con = sesameService.getConnection();
		try {
			con.begin();

			if (!ldpService.exists(con, resource)) {
				final Response.ResponseBuilder resp;
				if (ldpService.isReusedURI(con, resource)) {
					resp = createResponse(con, Response.Status.GONE, resource);
				} else {
					resp = createResponse(con, Response.Status.NOT_FOUND, resource);
				}
				con.rollback();
				return resp.build();
			}
			/*
			 * sba:
			 */
			else {
				updateStatistics(resource);
			}

			if (eTag != null) {
				// check ETag if present
				log.trace("Checking If-Match: {}", eTag);
				EntityTag hasTag = ldpService.generateETag(con, resource);
				if (!eTag.equals(hasTag)) {
					log.trace("If-Match header did not match, expected {}", hasTag);
					final Response.ResponseBuilder resp = createResponse(con, Response.Status.PRECONDITION_FAILED,
							resource);
					con.rollback();
					return resp.build();
				}
			}

			// Check for the supported mime-type
			if (!type.toString().equals(RdfPatchParser.MIME_TYPE)) {
				log.trace("Incompatible Content-Type for PATCH: {}", type);
				final Response.ResponseBuilder resp = createResponse(con, Response.Status.UNSUPPORTED_MEDIA_TYPE,
						resource).entity("Unknown Content-Type: " + type + "\n");
				con.rollback();
				return resp.build();
			}

			try {
				ldpService.patchResource(con, resource, postBody, false);
				final Response.ResponseBuilder resp = createResponse(con, Response.Status.NO_CONTENT, resource);
				con.commit();
				return resp.build();
			} catch (ParseException | InvalidPatchDocumentException e) {
				final Response.ResponseBuilder resp = createResponse(con, Response.Status.BAD_REQUEST, resource)
						.entity(e.getMessage() + "\n");
				con.rollback();
				return resp.build();
			} catch (InvalidModificationException e) {
				final Response.ResponseBuilder resp = createResponse(con, 422, resource).entity(e.getMessage() + "\n");
				con.rollback();
				return resp.build();
			}

		} catch (final Throwable t) {
			con.rollback();
			throw t;
		} finally {
			con.close();
		}
	}

	/**
	 * Handle OPTIONS (Sec. 4.2.8, Sec. 5.2.8)
	 */
	@OPTIONS
	public Response OPTIONS(@Context final UriInfo uriInfo) throws RepositoryException {

		numberOfIntegrationRequests++;

		final String resource = ldpService.getResourceUri(uriInfo);
		log.debug("OPTIONS to <{}>", resource);

		final RepositoryConnection con = sesameService.getConnection();
		try {
			con.begin();

			if (!ldpService.exists(con, resource)) {
				final Response.ResponseBuilder resp;
				if (ldpService.isReusedURI(con, resource)) {
					resp = createResponse(con, Response.Status.GONE, resource);
				} else {
					resp = createResponse(con, Response.Status.NOT_FOUND, resource);
				}
				con.rollback();
				return resp.build();
			}
			/*
			 * sba:
			 */
			else {
				updateStatistics(resource);
			}

			Response.ResponseBuilder builder = createResponse(con, Response.Status.OK, resource);

			addOptionsHeader(con, resource, builder);

			con.commit();
			return builder.build();
		} catch (final Throwable t) {
			con.rollback();
			throw t;
		} finally {
			con.close();
		}

	}

	private Response.ResponseBuilder addOptionsHeader(RepositoryConnection connection, String resource,
			Response.ResponseBuilder builder) throws RepositoryException {
		log.debug("Adding required LDP Headers (OPTIONS, GET); see Sec. 8.2.8 and Sec. 4.2.2.2");
		if (ldpService.isNonRdfSourceResource(connection, resource)) {
			// Sec. 4.2.8.2
			log.trace("<{}> is an LDP-NR: GET, HEAD, PUT and OPTIONS allowed", resource);
			builder.allow(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.PUT, HttpMethod.OPTIONS);
		} else if (ldpService.isRdfSourceResource(connection, resource)) {
			if (ldpService.getInteractionModel(connection, resource) == LdpService.InteractionModel.LDPR) {
				log.trace("<{}> is a LDP-RS (LDPR interaction model): GET, HEAD, PUT, PATCH and OPTIONS allowed",
						resource);
				// Sec. 4.2.8.2
				builder.allow(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.PUT, HTTP_METHOD_PATCH, HttpMethod.OPTIONS);
			} else {
				// Sec. 4.2.8.2
				log.trace("<{}> is a LDP-RS (LDPC interaction model): GET, HEAD, POST, PUT, PATCH and OPTIONS allowed",
						resource);
				builder.allow(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.PUT, HTTP_METHOD_PATCH,
						HttpMethod.OPTIONS);
				// Sec. 4.2.3 / Sec. 5.2.3
				builder.header(HTTP_HEADER_ACCEPT_POST, LdpUtils.getAcceptPostHeader("*/*"));
			}
			// Sec. 4.2.7.1
			builder.header(HTTP_HEADER_ACCEPT_PATCH, RdfPatchParser.MIME_TYPE);
		}

		return builder;
	}

	/**
	 * Add all the default headers specified in LDP to the Response
	 *
	 * @param connection
	 *            the RepositoryConnection (with active transaction) to read extra
	 *            data from
	 * @param status
	 *            the StatusCode
	 * @param resource
	 *            the iri/uri/url of the resource
	 * @return the provided ResponseBuilder for chaining
	 */
	protected Response.ResponseBuilder createResponse(RepositoryConnection connection, Response.Status status,
			String resource) throws RepositoryException {
		return createResponse(connection, status.getStatusCode(), resource);
	}

	/**
	 * Add all the default headers specified in LDP to the Response
	 *
	 * @param connection
	 *            the RepositoryConnection (with active transaction) to read extra
	 *            data from
	 * @param status
	 *            the status code
	 * @param resource
	 *            the uri/url of the resource
	 * @return the provided ResponseBuilder for chaining
	 */
	protected Response.ResponseBuilder createResponse(RepositoryConnection connection, int status, String resource)
			throws RepositoryException {
		return createResponse(connection, Response.status(status), resource);
	}

	/**
	 * Add all the default headers specified in LDP to the Response
	 *
	 * @param connection
	 *            the RepositoryConnection (with active transaction) to read extra
	 *            data from
	 * @param rb
	 *            the ResponseBuilder
	 * @param resource
	 *            the uri/url of the resource
	 * @return the provided ResponseBuilder for chaining
	 */
	protected Response.ResponseBuilder createResponse(RepositoryConnection connection, Response.ResponseBuilder rb,
			String resource) throws RepositoryException {
		createResponse(rb);

		if (ldpService.exists(connection, resource)) {
			// Link rel='type' (Sec. 4.2.1.4, 5.2.1.4)
			List<Statement> statements = ldpService.getLdpTypes(connection, resource);
			for (Statement stmt : statements) {
				Value o = stmt.getObject();
				if (o instanceof URI && o.stringValue().startsWith(LDP.NAMESPACE)) {
					rb.link(o.stringValue(), LINK_REL_TYPE);
				}
			}

			final URI rdfSource = ldpService.getRdfSourceForNonRdfSource(connection, resource);
			if (rdfSource != null) {
				// Sec. 5.2.8.1 and 5.2.3.12
				rb.link(rdfSource.stringValue(), LINK_REL_DESCRIBEDBY);
				// This is not covered by the Spec, but is very convenient to have
				rb.link(rdfSource.stringValue(), LINK_REL_META);
			}
			final URI nonRdfSource = ldpService.getNonRdfSourceForRdfSource(connection, resource);
			if (nonRdfSource != null) {
				// This is not covered by the Spec, but is very convenient to have
				rb.link(nonRdfSource.stringValue(), LINK_REL_CONTENT);
			}

			// ETag (Sec. 4.2.1.3)
			rb.tag(ldpService.generateETag(connection, resource));

			// Last modified date
			rb.lastModified(ldpService.getLastModified(connection, resource));
		}

		return rb;
	}

	/**
	 * Add the non-resource related headers specified in LDP to the provided
	 * ResponseBuilder
	 * 
	 * @param rb
	 *            the ResponseBuilder to decorate
	 * @return the updated ResponseBuilder for chaining
	 */
	protected Response.ResponseBuilder createResponse(Response.ResponseBuilder rb) {
		// Link rel='http://www.w3.org/ns/ldp#constrainedBy' (Sec. 4.2.1.6)
		rb.link(LDP_SERVER_CONSTRAINTS, LINK_REL_CONSTRAINEDBY);

		return rb;
	}

	/**
	 * Add all the default headers specified in LDP to the Response
	 *
	 * @param connection
	 *            the RepositoryConnection (with active transaction) to read extra
	 *            data from
	 * @param status
	 *            the StatusCode
	 * @param resource
	 *            the iri/uri/url of the resource
	 * @return the provided ResponseBuilder for chaining
	 */
	protected Response.ResponseBuilder createWebServiceResponse(RepositoryConnection connection, Response.Status status,
			String resource, InputStream input_nodes, RDFFormat format) throws RepositoryException {
		return createWebServiceResponse(connection, status.getStatusCode(), resource, input_nodes, format);
	}

	protected Response.ResponseBuilder createWebServiceResponse(RepositoryConnection connection, int status,
			String resource, InputStream postBody, RDFFormat format) throws RepositoryException {
		return createWebServiceResponse(connection, Response.status(status), resource, postBody, format);
	}

	/**
	 * 
	 *
	 * @param connection
	 *            the RepositoryConnection (with active transaction) to read extra
	 *            data from
	 * @param rb
	 *            the ResponseBuilder
	 * @param resource
	 *            the uri/url of the resource
	 * @return the provided ResponseBuilder for chaining
	 */
	protected Response.ResponseBuilder createWebServiceResponse(RepositoryConnection connection,
			Response.ResponseBuilder rb, String resource, InputStream postBody, RDFFormat format)
					throws RepositoryException, IllegalArgumentException {

		rb = createWebServiceResponse(rb);

		if (ldpService.exists(connection, resource)) {

			try {

				RepositoryResult<Statement> service_descriptions = connection.getStatements(null,
						ValueFactoryImpl.getInstance().createURI(HYDRA.entrypoint.getLabel()),
						ValueFactoryImpl.getInstance().createURI(resource), true, new org.openrdf.model.Resource[0]);

				if (!service_descriptions.hasNext()) {
					log.debug("Could not find any connected service description to <{}>", resource);
					return rb.status(Response.Status.EXPECTATION_FAILED)
							.entity("Could not find any connected service descriptions!");
				}

				URI service_description = cleanURI((URI) service_descriptions.next().getSubject());
				if (service_descriptions.hasNext()) {
					// do nothing yet
					// to do: handle multiple services with same startAPI
				}

				RepositoryResult<Statement> services = connection.getStatements(null, LDP.contains, service_description,
						true, new org.openrdf.model.Resource[0]);
				if (!services.hasNext()) {
					log.debug("Could not find any connected service to <{}>", service_description);
					return rb.status(Response.Status.EXPECTATION_FAILED)
							.entity("Could not find any connected service!");
				}
				URI service = cleanURI((URI) services.next().getSubject());

				// =============================================================================================
				//
				// is a BayesService
				//
				// =============================================================================================

				if (connection.hasStatement(service, RDF.TYPE,
						ValueFactoryImpl.getInstance().createURI(STEP.BayesService.getLabel()), true)) {
					// is BayesService

					RepositoryResult<Statement> models = connection.getStatements(service,
							ValueFactoryImpl.getInstance().createURI(STEP.hasModel.getLabel()), null, true,
							new org.openrdf.model.Resource[0]);
					log.warn("models enthaelt folgenden Inhalt", models);

					// return Response.ok(
					// new GenericEntity<Iterable<Node[]>>(
					// executeBayesschesModel(service, rb, postBody, models, format) ) { }
					// );

					return executeBayesschesModel(service, rb, connection, postBody, models, format);
				}

				else  {

					RepositoryResult<Statement> contains_triples = connection.getStatements(service, LDP.contains, null, false);
					if (!contains_triples.hasNext()) {
						log.warn("Could not find any connected service to <{}>", resource);
						return rb.status(Response.Status.EXPECTATION_FAILED)
								.entity("Could not find any connected program!");
					}
					// =============================================================================================
					//
					// is a LinkedDataWebService
					//
					// =============================================================================================

					return executeLinkedDataWebService(connection, service, rb, postBody, format);
				}

			} catch (RepositoryException e) {
				return rb.status(Response.Status.EXPECTATION_FAILED)
						.entity("Necessary preconditions (N3-Program, Query) missing.");
			} catch (ClassNotFoundException e1) {
				log.error(e1.getMessage());
				e1.printStackTrace();
			} catch (IOException e1) {
				log.error(e1.getMessage());
				e1.printStackTrace();
			}

		}

		return rb;
	}

	/**
	 * returns an URI without an ending slash
	 * 
	 * @param uri
	 * @return
	 */
	private URI cleanURI(URI uri) {
		String str = uri.toString();

		if (str.endsWith("/")) {
			String clean_uri = str.substring(0, str.length() - 1);
			return new URIImpl(clean_uri);
		} else {
			return uri;
		}
	}

	private Response.ResponseBuilder executeBayesschesModel(URI resource, Response.ResponseBuilder rb,
			RepositoryConnection connection, InputStream postBody, RepositoryResult<Statement> models, RDFFormat format)
					throws IllegalArgumentException, RepositoryException, IOException, ClassNotFoundException {
		/*
		 * resource is BaysscherService program_data ist InputSteam from the program
		 * 
		 */
		log.warn("Start BayesNet Service");

		List<Node[]> results = new LinkedList<Node[]>();

		ValueFactory factory = ValueFactoryImpl.getInstance();

		Network original = new Network();

		double[] beliefs = null;

		URI object = factory.createURI(models.next().getObject().stringValue());

		RepositoryResult<Statement> bayesNodes = connection.getStatements(object,
				ValueFactoryImpl.getInstance().createURI(STEP.hasBayesNode.getLabel()), null, true, new Resource[0]);

		/*
		 * org.apache.marmotta.platform.ldp.webservices.Node a =
		 * original.addNode("<http://step.aifb.kit.edu/a>"); a.setOutcomes("true",
		 * "false"); a.setProbabilities(0.2, 0.8);
		 * 
		 * org.apache.marmotta.platform.ldp.webservices.Node b =
		 * original.addNode("<http://step.aifb.kit.edu/b>"); b.setOutcomes("one", "two",
		 * "three"); b.setParents(Arrays.asList(a));
		 * 
		 * log.warn("Start BayesNet Servicetester"); b.setProbabilities( 0.1, 0.4, 0.5,
		 * // a == true 0.3, 0.4, 0.3 // a == false );
		 * 
		 * org.apache.marmotta.platform.ldp.webservices.Node c =
		 * original.addNode("<http://step.aifb.kit.edu/c>"); c.setOutcomes("true",
		 * "false"); c.setParents(Arrays.asList(a, b)); c.setProbabilities( // a == true
		 * 0.1, 0.9, // b == one 0.0, 1.0, // b == two 0.5, 0.5, // b == three // a ==
		 * false 0.2, 0.8, // b == one 0.0, 1.0, // b == two 0.7, 0.3 // b == three );
		 */

		log.warn("Continuing BayesNet Service with " + original.toString());

		BayesNet net = new BayesNet();

		while (bayesNodes.hasNext()) {

			// for(org.apache.marmotta.platform.ldp.webservices.Node node: original.Nodes) {
			// log.warn("node: " + node.name);
			// BayesNode transfer = net.createNode(node.name);
			// log.warn("node outcome: " + node.getOutcomes());
			// transfer.addOutcomes(node.getOutcomes());
			// }

			// for(org.apache.marmotta.platform.ldp.webservices.Node node: original.Nodes){
			// List<BayesNode> eltern = new ArrayList<BayesNode>();
			// BayesNode transfer = net.getNode(node.name);

			URI uri = new URIImpl(bayesNodes.next().getObject().stringValue());

			RepositoryResult<Statement> names = connection.getStatements(uri,
					ValueFactoryImpl.getInstance().createURI(STEP.hasName.getLabel()), null, true, new Resource[0]);

			String name = names.next().getObject().stringValue();

			BayesNode transfer = net.createNode(name);

			RepositoryResult<Statement> outcomes = connection.getStatements(uri,
					ValueFactoryImpl.getInstance().createURI(STEP.hasOutput.getLabel()), null, true, new Resource[0]);

			String[] output = (outcomes.next().getObject().stringValue()).split("_");
			transfer.addOutcomes(output);

		}

		RepositoryResult<Statement> bayesNodes2 = connection.getStatements(object,
				ValueFactoryImpl.getInstance().createURI(STEP.hasBayesNode.getLabel()), null, true, new Resource[0]);

		while (bayesNodes2.hasNext()) {

			URIImpl uri = new URIImpl(bayesNodes2.next().getObject().stringValue());

			RepositoryResult<Statement> parents = connection.getStatements(uri,
					ValueFactoryImpl.getInstance().createURI(STEP.hasParents.getLabel()), null, true, new Resource[0]);
			RepositoryResult<Statement> name = connection.getStatements(uri,
					ValueFactoryImpl.getInstance().createURI(STEP.hasName.getLabel()), null, true, new Resource[0]);

			BayesNode transfer = net.getNode(name.next().getObject().stringValue());

			if (parents.hasNext()) {
				String[] parTransfer = (parents.next().getObject().stringValue()).split(" ");

				List<BayesNode> eltern = new ArrayList<BayesNode>();
				for (String parent : parTransfer) {
					eltern.add(net.getNode(parent));
				}
				transfer.setParents(eltern);
			}

			RepositoryResult<Statement> probabilities = connection.getStatements(uri,
					ValueFactoryImpl.getInstance().createURI(STEP.hasProbabilities.getLabel()), null, true,
					new Resource[0]);

			String[] probTransfer = (probabilities.next().getObject().stringValue()).split(", ");

			double[] prob = new double[probTransfer.length];
			for (int i = 0; i < prob.length; i++) {
				prob[i] = Double.parseDouble(probTransfer[i]);
			}

			transfer.setProbabilities(prob);
		}

		log.warn("Network " + net);
		IBayesInferrer inferer = new JunctionTreeAlgorithm();
		inferer.setNetwork(net);

		Map<BayesNode, String> evidence = new HashMap<BayesNode, String>();

		// TurtleParser turtleParser = new TurtleParser(input_nodes,
		// Charset.defaultCharset(), new java.net.URI( resource.stringValue() ) );
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		org.openrdf.model.Graph myGraph = new org.openrdf.model.impl.GraphImpl();
		StatementCollector collector = new StatementCollector(myGraph);
		rdfParser.setRDFHandler(collector);

		try {
			rdfParser.parse(postBody, resource.stringValue());
		} catch (RDFParseException | RDFHandlerException e) {

			List<Node[]> error = new LinkedList<Node[]>();
			error.add(new org.semanticweb.yars.nx.Node[] { new org.semanticweb.yars.nx.BNode("You"), RDFS.LABEL,
					new org.semanticweb.yars.nx.Literal("failed!") });
		}

		List<org.semanticweb.yars.nx.Node[]> input_nodes = new LinkedList<org.semanticweb.yars.nx.Node[]>();

		myGraph.forEach(s -> {
			try {

				if (s.getObject() instanceof org.openrdf.model.Resource) {
					Node[] node = { new org.semanticweb.yars.nx.Resource(s.getSubject().toString()),
							new org.semanticweb.yars.nx.Resource(s.getPredicate().toString()),
							new org.semanticweb.yars.nx.Resource(s.getObject().toString()) };
					log.warn("Input Nodes: " + node[0] + " " + node[1] + " " + node[2]);
					input_nodes.add(node);
				} else {
					org.semanticweb.yars.nx.Node[] node = {
							new org.semanticweb.yars.nx.Resource(s.getSubject().toString()),
							new org.semanticweb.yars.nx.Resource(s.getPredicate().toString()),
							new org.semanticweb.yars.nx.Literal(s.getObject().stringValue()) };
					log.warn("Input Nodes: " + node[0] + " " + node[1] + " " + node[2]);
					input_nodes.add(node);
				}
			} catch (ClassCastException e) {
				org.semanticweb.yars.nx.Node[] node = { new org.semanticweb.yars.nx.Resource(s.getSubject().toString()),
						new org.semanticweb.yars.nx.Resource(s.getPredicate().toString()),
						new org.semanticweb.yars.nx.Literal(s.getObject().stringValue()) };
				log.warn("Input Nodes: " + node[0] + " " + node[1] + " " + node[2]);
				input_nodes.add(node);
			}
		});

		// only entities of type STEP.BayesNode are regarded
		HashMap<Node, List<Node[]>> relevant_nodes = new HashMap<Node, List<Node[]>>();
		for (Node[] node : input_nodes) {
			if (node[2].equals(STEP.BayesNode)) {
				if (!relevant_nodes.containsKey(node[0])) {
					List<Node[]> subgraph = new ArrayList<Node[]>();
					subgraph.add(node);
					relevant_nodes.put(node[0], subgraph);
				}
			}
		}

		// add Literal ?y mit ?x step:hasOutput ?y
		for (Node[] node : input_nodes) {
			if (relevant_nodes.containsKey(node[0]) && node[1].equals(STEP.hasOutput)) {
				try {
					evidence.put(net.getNode(node[0].toString()),
							((org.semanticweb.yars.nx.Literal) node[2]).getLabel());
				} catch (Exception e) {
					log.error("Could not configure the BayesNet Engine accordingly: ", e);
					e.printStackTrace();
				}
			}
		}

		// all input nodes are read, know get output nodes .
		for (Node[] node : input_nodes) {
			if (node[2].equals(STEP.Target)) {
				inferer.setEvidence(evidence);

				beliefs = inferer.getBeliefs(net.getNode(node[0].toString()));
				List<String> classes = net.getNode(node[0].toString()).getOutcomes();
				Iterator<String> classes_iterator = classes.iterator();

				for (double ergebnis : beliefs) {

					org.semanticweb.yars.nx.Resource classes_node = new org.semanticweb.yars.nx.Resource(
							"#" + classes_iterator.next());
					results.add(new Node[] { node[0], STEP.hasOutput, classes_node });
					results.add(new Node[] { classes_node, STEP.hasResult, new Literal(String.valueOf(ergebnis)) });
				}
			}
		}

		final StreamingOutput entity = new StreamingOutput() {

			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				try {
					ldpService.writeResource(resource, results, output, format);
				} catch (RDFHandlerException e) {
					throw new NoLogWebApplicationException(e,
							createResponse(Response.status(Response.Status.INTERNAL_SERVER_ERROR))
							.entity(e.getMessage()).build());
				} catch (final Throwable t) {
					throw t;
				}
			}
		};

		return Response.status(Status.OK).entity(entity);
	}





	/**
	 * @author sba
	 * 
	 * @param service
	 * @param rb
	 * @param postBody
	 * @param format
	 * @return
	 * @throws RepositoryException 
	 */
	private ResponseBuilder executeLinkedDataWebService(RepositoryConnection connection, Resource service, ResponseBuilder rb, InputStream postBody,
			RDFFormat format) throws RepositoryException {

		RepositoryResult<Statement> contains_triples = connection.getStatements(service, LDP.contains, null, false);


		URI program = null;
		URI query = null;
		while (contains_triples.hasNext()) {
			RepositoryResult<Statement> programs = connection.getStatements(
					new URIImpl(contains_triples.next().getObject().stringValue()),
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
				break;
			}


			RepositoryResult<Statement> queries = connection.getStatements(
					new URIImpl(contains_triples.next().getObject().stringValue()),
					new URIImpl(STEP.hasQuery.getLabel()), null, true, new org.openrdf.model.Resource[0]);

			if (queries.hasNext()) {
				String query_uri = queries.next().getObject().stringValue();
				if (query_uri.endsWith(".bin") || query_uri.endsWith(".bin/")) {
					query = new URIImpl(query_uri);
				} else if (query_uri.endsWith("/")) {
					query = new URIImpl(query_uri.substring(0, query_uri.length() - 2) + ".bin");
				} else {
					query = new URIImpl(query_uri + ".bin");
				}
				break;
			}
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
		return executeLinkedDataWebService(service, rb, postBody, "", program.stringValue(), query.stringValue(), format);

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
	private ResponseBuilder executeLinkedDataWebService(Resource resource, ResponseBuilder rb, InputStream postBody,
			String query, String program_resource, String query_resource, RDFFormat format) throws IllegalArgumentException {

		ValueFactory factory = ValueFactoryImpl.getInstance();
		List<Node[]> results = new ArrayList<Node[]>();

		/*
		 * Linked Data-Fu execution
		 */

		try {

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

			try {
				if (postBody != null)
					rdfParser.parse(postBody, resource.stringValue());
			} catch (RDFParseException | RDFHandlerException e) {

				List<Node[]> error = new LinkedList<Node[]>();
				error.add(new org.semanticweb.yars.nx.Node[] { new org.semanticweb.yars.nx.BNode("You"), RDFS.LABEL,
						new org.semanticweb.yars.nx.Literal("failed!") });
			} catch (IOException e) {
				log.error("Parsing incoming data failed: ", e);
				e.printStackTrace();
			}

			List<org.semanticweb.yars.nx.Node[]> input_nodes = new LinkedList<org.semanticweb.yars.nx.Node[]>();

			myGraph.forEach(s -> {
				org.semanticweb.yars.nx.Node[] node = { new org.semanticweb.yars.nx.Resource(s.getSubject().toString()),
						new org.semanticweb.yars.nx.Resource(s.getSubject().toString()),
						new org.semanticweb.yars.nx.Resource(s.getSubject().toString()) };
				log.warn("Input Nodes: " + node[0] + " " + node[1] + " " + node[2]);
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
			log.error("sparql.ParseException: ", e);
			results.add(new Node[] { new BNode("error"), RDFS.LABEL, new Literal("failed!") });
			results.add(new Node[] { new BNode("error"), RDFS.COMMENT,
					new Literal("sparql.ParseException: " + e.getMessage()) });
		} catch (edu.kit.aifb.datafu.parser.notation3.ParseException e) {
			e.printStackTrace();
			log.error("notation3.ParseException: ", e);
			results.add(new Node[] { new BNode("error"), RDFS.LABEL, new Literal("failed!") });
			results.add(new Node[] { new BNode("error"), RDFS.COMMENT,
					new Literal("notation3.ParseException: " + e.getMessage()) });
		} catch (InterruptedException e) {
			e.printStackTrace();
			log.error("InterruptedException: ", e);
			results.add(new Node[] { new BNode("error"), RDFS.LABEL, new Literal("failed!") });
			results.add(new Node[] { new BNode("error"), RDFS.COMMENT,
					new Literal("InterruptedException: " + e.getMessage()) });
		} catch (IOException e) {
			e.printStackTrace();
			log.error("IOException: ", e);
			results.add(new Node[] { new BNode("error"), RDFS.LABEL, new Literal("failed!") });
			results.add(new Node[] { new BNode("error"), RDFS.COMMENT, new Literal("IOException: " + e.getMessage()) });
		}

		final StreamingOutput entity = new StreamingOutput() {

			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				try {
					ldpService.writeResource(resource, results, output, format);
				} catch (RDFHandlerException e) {
					throw new NoLogWebApplicationException(e,
							createResponse(Response.status(Response.Status.INTERNAL_SERVER_ERROR))
							.entity(e.getMessage()).build());
				} catch (final Throwable t) {
					throw t;
				}
			}
		};

		numberOfIntegrationRequests++;
		return Response.status(Status.OK).entity(entity);

	}

	protected Response.ResponseBuilder createWebServiceResponse(Response.ResponseBuilder rb) {
		// Link rel='http://www.w3.org/ns/ldp#constrainedBy' (Sec. 4.2.1.6)
		rb.link(LDP_SERVER_CONSTRAINTS, LINK_REL_CONSTRAINEDBY);

		return rb;
	}

	public String getStringFromInputStream(InputStream stream) {
		String pro = "";
		Scanner scanner = new Scanner(stream, "UTF-8");
		while (scanner.hasNextLine()) {
			pro += scanner.nextLine() + "\n";
		}
		scanner.close();
		return pro;
	}

	/**
	 * @author sba
	 * 
	 * @return
	 * @throws ParseException
	 * @throws edu.kit.aifb.datafu.parser.notation3.ParseException
	 */
	public static Program getProgramTriple()
			throws ParseException, edu.kit.aifb.datafu.parser.notation3.ParseException {
		Origin origin = new InternalOrigin("programOriginTriple");
		ProgramConsumerImpl programConsumer = new ProgramConsumerImpl(origin);
		Notation3Parser notation3Parser = new Notation3Parser(new ByteArrayInputStream(PROGRAM_TRIPLE.getBytes()));
		notation3Parser.parse(programConsumer, origin);
		return programConsumer.getProgram(origin);
	}

	/**
	 * @author sba
	 * 
	 * @param resource
	 */
	private void updateStatistics(String resource) {

		try {
			RepositoryConnection connection = sesameService.getConnection();
			ValueFactory factory = ValueFactoryImpl.getInstance();

			URI subject = factory.createURI(resource);
			// URI predicate = factory.createURI(STEP.numberOfRequests.getLabel());

			connection.begin();
			// CloseableIteration<Statement, RepositoryException> ldpStatements =
			// connection.getStatements(subject, predicate, null, false, ldpContext);

			@SuppressWarnings("restriction")
			OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory
			.getOperatingSystemMXBean();
			@SuppressWarnings("restriction")
			org.openrdf.model.Literal cpuLoad = factory.createLiteral(operatingSystemMXBean.getProcessCpuLoad());

			org.openrdf.model.Literal memory = factory
					.createLiteral(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

			// Statement updatedCounterStatistics = factory.createStatement(subject,
			// predicate, factory.createLiteral(1));
			// Statement updatedMemoryStatistics = factory.createStatement(subject,
			// predicate, memory);
			// Statement updatedCpuStatistics = factory.createStatement(subject, predicate,
			// cpuLoad);

			// sba: new and better statistics with unique properties
			URI cpu = factory.createURI(MEXCORE.cpu.getLabel());
			URI hasMemory = factory.createURI(MEXCORE.memory.getLabel());
			URI numberOfRequests = factory.createURI(STEP.numberOfRequests.getLabel());

			Statement updatedCounterStatistics2 = factory.createStatement(subject, numberOfRequests,
					factory.createLiteral(1));
			Statement updatedMemoryStatistics2 = factory.createStatement(subject, hasMemory, memory);
			Statement updatedCpuStatistics2 = factory.createStatement(subject, cpu, cpuLoad);

			CloseableIteration<Statement, RepositoryException> ldpStatements2 = connection.getStatements(subject, cpu,
					null, false, ldpContext);
			connection.remove(ldpStatements2, ldpContext);
			ldpStatements2 = connection.getStatements(subject, hasMemory, null, false, ldpContext);
			connection.remove(ldpStatements2, ldpContext);
			ldpStatements2 = connection.getStatements(subject, numberOfRequests, null, false, ldpContext);
			// connection.remove(ldpStatements2, ldpContext);

			while (ldpStatements2.hasNext()) {
				Statement statement = ldpStatements2.next();

				if (statement.getObject() instanceof org.openrdf.model.Literal) {
					org.openrdf.model.Literal literal = (org.openrdf.model.Literal) statement.getObject();

					if (literal.getDatatype().stringValue().equalsIgnoreCase(XMLSchema.INT.stringValue())) {
						try {
							int number = literal.intValue();
							number++;
							// updatedCounterStatistics = factory.createStatement(subject, predicate,
							// factory.createLiteral(number));
							updatedCounterStatistics2 = factory.createStatement(subject, numberOfRequests,
									factory.createLiteral(number));
							break;
						} catch (NumberFormatException e) {
						}
					}
				}
			}

			ldpStatements2 = connection.getStatements(subject, numberOfRequests, null, false, ldpContext);
			connection.remove(ldpStatements2, ldpContext);

			connection.commit();

			connection.begin();
			List<Statement> statements = new LinkedList<Statement>();
			// statements.add(updatedCounterStatistics);
			// statements.add(updatedMemoryStatistics);
			// statements.add(updatedCpuStatistics);

			// sba: new and better statistics with unique properties
			statements.add(updatedCounterStatistics2);
			statements.add(updatedMemoryStatistics2);
			statements.add(updatedCpuStatistics2);

			connection.add(statements, ldpContext);
			connection.commit();

		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public List<Node[]> postBodyToIterableNode(InputStream postBody, URI resource) {
		Map<BayesNode, String> evidence = new HashMap<BayesNode, String>();

		// TurtleParser turtleParser = new TurtleParser(input_nodes,
		// Charset.defaultCharset(), new java.net.URI( resource.stringValue() ) );
		RDFParser rdfParser = Rio.createParser(RDFFormat.JSONLD);
		org.openrdf.model.Graph myGraph = new org.openrdf.model.impl.GraphImpl();
		StatementCollector collector = new StatementCollector(myGraph);
		rdfParser.setRDFHandler(collector);

		try {
			rdfParser.parse(postBody, resource.stringValue());
		} catch (RDFParseException | RDFHandlerException | IOException e) {

			List<Node[]> error = new LinkedList<Node[]>();
			error.add(new org.semanticweb.yars.nx.Node[] { new org.semanticweb.yars.nx.BNode("You"), RDFS.LABEL,
					new org.semanticweb.yars.nx.Literal("failed!") });
		}

		List<org.semanticweb.yars.nx.Node[]> input_nodes = new LinkedList<org.semanticweb.yars.nx.Node[]>();

		myGraph.forEach(s -> {
			try {

				if (s.getObject() instanceof org.openrdf.model.Resource) {
					Node[] node = { new org.semanticweb.yars.nx.Resource(s.getSubject().toString()),
							new org.semanticweb.yars.nx.Resource(s.getPredicate().toString()),
							new org.semanticweb.yars.nx.Resource(s.getObject().toString()) };
					log.warn("Input Nodes: " + node[0] + " " + node[1] + " " + node[2]);
					input_nodes.add(node);
				} else {
					org.semanticweb.yars.nx.Node[] node = {
							new org.semanticweb.yars.nx.Resource(s.getSubject().toString()),
							new org.semanticweb.yars.nx.Resource(s.getPredicate().toString()),
							new org.semanticweb.yars.nx.Literal(s.getObject().stringValue()) };
					log.warn("Input Nodes: " + node[0] + " " + node[1] + " " + node[2]);
					input_nodes.add(node);
				}
			} catch (ClassCastException e) {
				org.semanticweb.yars.nx.Node[] node = { new org.semanticweb.yars.nx.Resource(s.getSubject().toString()),
						new org.semanticweb.yars.nx.Resource(s.getPredicate().toString()),
						new org.semanticweb.yars.nx.Literal(s.getObject().stringValue()) };
				log.warn("Input Nodes: " + node[0] + " " + node[1] + " " + node[2]);
				input_nodes.add(node);
			}
		});

		return input_nodes;
	}



	public boolean isCallInstanze(String uriInfo, RepositoryConnection conn) {

		uriInfo = uriInfo.substring(0, uriInfo.lastIndexOf("/"));

		try {
			return conn.hasStatement(ValueFactoryImpl.getInstance().createURI(uriInfo),
					ValueFactoryImpl.getInstance().createURI(STEP.interactionPattern.getLabel()),
					ValueFactoryImpl.getInstance().createURI(STEP.FLSVisitourAPI.getLabel()), true);
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			return false;
		}
	}


	/**
	 * @author sba
	 * 
	 * @param uriInfo
	 * @param conn
	 * @return
	 */
	public boolean isVirtualResource(String uriInfo, RepositoryConnection conn) {

		uriInfo = uriInfo.substring(0, uriInfo.lastIndexOf("/"));

		try {

			if (conn.hasStatement(
					getContainer(uriInfo, conn),
					ValueFactoryImpl.getInstance().createURI(RDF.TYPE.stringValue()),
					ValueFactoryImpl.getInstance().createURI(STEP.VirtualResource.getLabel()),
					true)) {
				return true;
			} else {

				return false;
			}

		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			return false;
		}	
	}

	public Resource getContainer(String child, RepositoryConnection conn) throws RepositoryException {

		child = child.substring(0, child.lastIndexOf("/"));

		RepositoryResult<Statement> result = conn.getStatements(null,
				ValueFactoryImpl.getInstance().createURI(LDP.contains.stringValue()),
				ValueFactoryImpl.getInstance().createURI(child)
				, true);

		if (result.hasNext()) {
			return	result.next().getSubject();
		} else {

			return null;

		}
	}

}
