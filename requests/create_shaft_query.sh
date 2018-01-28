#!/bin/bash
curl -XPOST -H "Slug: query"  --data-binary "query_shaft.rq" --user admin:pass123 http://km.aifb.kit.edu/services/bader4/marmotta/ldp/ShaftContainer/
