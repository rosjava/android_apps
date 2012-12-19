PROJECTS = android_teleop_video foobar
BUILD_CMD = ant debug
CONF_CMD = tools/configure.py
CLEAN_CMD = ant clean
LOAD_CMD = adb -d install -r

all:
	$(foreach p,$(PROJECTS),cd $(p); $(BUILD_CMD); cd ..;)

conf:
	$(foreach p,$(PROJECTS),$(CONF_CMD) $(p);)

clean:
	$(foreach p,$(PROJECTS),cd $(p); $(CLEAN_CMD); cd ..;)

load:
	$(foreach p,$(PROJECTS),$(LOAD_CMD) $(p)/bin/*-debug.apk;)
