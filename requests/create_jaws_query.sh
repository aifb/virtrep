#!/bin/bash
curl -XPOST -v -H "Slug: query"  --data-binary "@query_jaws.rq" --user admin:pass123 http://km.aifb.kit.edu/services/bader4/marmotta/ldp/JawsContainer/
