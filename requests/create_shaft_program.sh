#!/bin/bash
curl -XPOST -H "Slug: program"  --data-binary "program_shaft_1.n3" --user admin:pass123 http://km.aifb.kit.edu/services/bader4/marmotta/ldp/ShaftContainer/
