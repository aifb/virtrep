#!/bin/bash
curl -XPOST -v -H "Slug: program"  --data-binary "@program_jaws_1.n3" --user admin:pass123 http://km.aifb.kit.edu/services/bader4/marmotta/ldp/JawsContainer/
