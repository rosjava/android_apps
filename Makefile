PROJECTS = android_teleop_video
BUILD_CMD = ant debug
CLEAN_CMD = ant clean
LOAD_CMD = adb -d install -r

all:
	$(foreach p,$(PROJECTS),cd $(p) && $(BUILD_CMD))

clean:
	$(foreach p,$(PROJECTS),cd $(p) && $(CLEAN_CMD))

load:
	$(foreach p,$(PROJECTS),$(LOAD_CMD) $(p)/bin/*-debug.apk)
