#!/usr/bin/env python

import os
import subprocess

projects = ['android_teleop_video']
android_version = 'android-17'
other_libs = ['android_gingerbread_mr1', 'android_honeycomb_mr2']
properties_fname = 'project.properties'

cwd = os.path.dirname(__file__)
android_core_path = subprocess.check_output(['rosstack', 'find', 'android_core']).strip()
# ant/Android requires the path to be relative for some reason
android_core_relpath = os.path.relpath(android_core_path, cwd)

for p in projects:
    p_path = os.path.join(cwd, p)
    prop_path = os.path.join(p_path, properties_fname)
    if os.path.exists(prop_path):
        os.unlink(prop_path)
    for l in other_libs:
        l_path = os.path.join('..', android_core_relpath, l)
        cmd = ['android', 'update', 'project', '--path', p_path, '--target', android_version, '--library', l_path]
        subprocess.check_call(cmd)
