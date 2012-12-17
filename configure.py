#!/usr/bin/env python

import os
import subprocess

projects = ['teleop_video']
android_version = 'android-17'
other_libs = ['android_gingerbread_mr1', 'android_honeycomb_mr2']

cwd = os.path.dirname(__file__)
android_core_path = subprocess.check_output(['rosstack', 'find', 'android_core']).strip()
# ant/Android requires the path to be relative for some reason
android_core_relpath = os.path.relpath(android_core_path, cwd)

for p in projects:
    p_path = os.path.join(cwd, p)
    for l in other_libs:
        l_path = os.path.join('..', android_core_relpath, l)
        cmd = ['android', 'update', 'project', '--path', p_path, '--target', android_version, '--library', l_path]
        subprocess.check_call(cmd)
