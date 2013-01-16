#!/usr/bin/env python

from __future__ import print_function
import os
import sys
import subprocess

android_version = 'android-17'
other_libs = ['android_gingerbread_mr1', 'android_honeycomb_mr2']
properties_fname = 'project.properties'

cwd = os.getcwd()
android_core_path = subprocess.check_output(['rosstack', 'find', 'android_core']).strip()
# ant/Android requires the path to be relative for some reason
android_core_relpath = os.path.relpath(android_core_path, cwd)

USAGE = 'configure.py [<proj_dir>]'

def parse_args(argv):
    dirs = []
    if len(argv) > 1:
        dirs = argv[1:]
    else:
        # Find directories that appear to be Android projects
        for f in os.listdir('.'):
            if os.path.isdir(f) and os.path.exists(os.path.join(f, 'AndroidManifest.xml')):
                dirs.append(f)
    return dirs

def go(argv):
    dirs = parse_args(argv)
    for p in dirs:
        print('Operating on: %s'%(p))
        p_path = os.path.join(cwd, p)
        prop_path = os.path.join(p_path, properties_fname)
        if os.path.exists(prop_path):
            os.unlink(prop_path)
        for l in other_libs:
            l_path = os.path.join('..', android_core_relpath, l)
            cmd = ['android', 'update', 'project', '--path', p_path, '--target', android_version, '--library', l_path]
            subprocess.check_call(cmd)

if __name__ == '__main__':
    go(sys.argv)
