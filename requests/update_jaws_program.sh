#!/bin/bash
curl -XPUT -v  -H "If-Match: W/\"1517179071000\""  --data-binary "@program_jaws_2.n3" --user admin:pass123 http://km.aifb.kit.edu/services/bader4/marmotta/ldp/JawsContainer/program/
