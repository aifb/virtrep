#!/bin/bash
curl -XPOST -v -H "Slug: jaws" -H "Content-Type: text/turtle" -d "<> a <http://step.aifb.kit.edu/VirtualResource> ; <http://www.w3.org/2000/01/rdf-schema#label> \"jaws\"^^<http://www.w3.org/2001/XMLSchema#string> ." --user admin:pass123 http://km.aifb.kit.edu/services/bader4/marmotta/ldp/JawsContainer/
