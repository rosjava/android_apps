#!/usr/bin/env python

from __future__ import print_function
import shutil
import os
import sys
import tempfile

USAGE = 'create-app.py <AppName>'
TEMPLATE = os.path.abspath(os.path.join(os.path.dirname(__file__), 'app_template'))

def parse_args(argv):
    if len(argv) != 2:
        print(USAGE)
        sys.exit(1)
    args = {}
    args['name'] = argv[1]
    return args


def go(argv):
    args = parse_args(argv)
    camel = args['name']
    lower = camel.lower()
    direct = os.path.abspath(lower)

    remaps = {'APPNAME_CAMEL': camel, 'APPNAME_LOWER': lower}

    print('Creating app %s in %s...'%(camel, direct))
    if os.path.exists(direct):
        print('Error: %s already exists'%(direct))
    shutil.copytree(TEMPLATE, direct)

    for d in os.walk(direct):
        for f in d[2]: 
            path = os.path.join(d[0], f)
            if os.path.isfile(path):
                for k,v in remaps.iteritems():
                    # Substitute in file content
                    tmp = tempfile.NamedTemporaryFile(delete=False)
                    inp = open(path, 'r').read()
                    tmp.write(inp.replace('@%s@'%(k), v))
                    tmp.close()
                    os.rename(tmp.name, path)
        # Substitute in file names
        for k,v in remaps.iteritems():
            if os.path.basename(d[0]) == k:
                os.rename(d[0], os.path.join(os.path.dirname(d[0]),v))

if __name__ == '__main__':
    go(sys.argv)
