#! /bin/bash
ROSJAVA_VERSION="c655a7f4f79a"
ANDROIDCORE_VERSION="2cd488729f61"

checkstatus() {
	BUILDSTATUS=`cat rosjava_build.log | grep "BUILD FAILED"`
	if [ -z "$BUILDSTATUS" ]; then
		echo "Build successfull!"
	else
		echo "ROSJava compile failed! See "`pwd`"/rosjava_build.log for details." >&2
		exit 1
	fi
}

# Check ROS installation
echo -e "\nChecking ROS install..."
hash roscore 2>/dev/null || { echo >&2 "ROS isn't installed, aborting!"; exit 1; }
ROSDIR=`which roscore`
ROSDIR=`echo $ROSDIR | awk 'BEGIN{FS="/";OFS="/";}{for(i = 1; i < NF-1; i++) printf "%s/",$i; print ""}'`
echo -e "ROS is installed at $ROSDIR\n\n"

# Check Android SDK installation
echo -e "\nChecking Android SDK install..."
ANDROID=`which android`
ROSDIR=`echo $ANDROID | awk 'BEGIN{FS="/";OFS="/";}{for(i = 1; i < NF-1; i++) printf "%s/",$i; print ""}'`
if [ -z "$ANDROID" ]; then
	echo -e "Android SDK is not installed! Can not proceed without the SDK.\nSee http://developer.android.com/sdk/index.html for details.\n" >&2
	exit 1
else
	echo "Android SDK installed at $ANDROID"
fi

# Check available SDK versions
echo "Checking installed SDK versions..."
AVAILABLE=`android list targets | grep "API level: " | sed -n -e 's/^.*: //p'`
echo -e "Installed versions:\n$AVAILABLE"
VTEN=`echo $AVAILABLE | grep 10`
VTHIRTEEN=`echo $AVAILABLE | grep 13`
if [ -z "$VTEN" -o -z "$VTHIRTEEN" ]; then
	echo "Must have Android SDK version 10 and 13 installed!" >&2
	exit 1
fi 

# Install required dependencies
echo -e "\nInstalling ROSJava dependencies..."
sudo apt-get install -y ant python-pip mercurial openjdk-6-jre openjdk-6-jdk # > /dev/null
# UNSURE IF THESE LINES ARE NECESSARY:
sudo pip install --upgrade rosinstall #> /dev/null
#sudo pip install --upgrade sphinx Pygments > /dev/null

# If ROS_WORKSPACE exists and is a directory, use that as the install directory
if [[ ! -z "$ROS_WORKSPACE" && -d "$ROS_WORKSPACE" ]]; then
	echo -e "\nDetected an existing ros workspace at $ROS_WORKSPACE\nUsing existing workspace as install directory."
else
	# Request installation directory, create if it doesn't exist
	read -e -p "Enter an installation directory: " INSTALLDIR
	if [ -d "$INSTALLDIR" ]; then
		echo "That directory already exists!"
	else
		echo "Creading directory $INSTALLDIR"
		mkdir -p $INSTALLDIR
	fi

	# Create ROS workspace, add setup script to .bashrc
	echo "Setting up ROS workspace"
	cd $INSTALLDIR
	rosws init &> /dev/null
	export ROS_WORKSPACE=$INSTALLDIR
	echo "export ROS_WORKSPACE=$INSTALLDIR" >> ~/.bashrc
	echo "Added 'export ROS_WORKSPACE=$INSTALLDIR' to your .bashrc."
fi

rosws $ROSDIR"/.rosinstall" &> /dev/null
echo "Downloading ROSJava..."
rosws merge http://rosjava.googlecode.com/hg/.rosinstall >& /dev/null
rosws set rosjava_core --version="$ROSJAVA_VERSION" -y >& /dev/null
rosws update >& /dev/null
source setup.bash
echo "source "`pwd`"/setup.bash" >> ~/.bashrc

# Build ROSJava
cd rosjava_core
echo "$ ./gradlew install" > rosjava_build.log
./gradlew install >> rosjava_build.log
checkstatus

# Build eclipse projects
echo "$ ./gradlew eclipse" >> rosjava_build.log
./gradlew eclipse >> rosjava_build.log
checkstatus

# Let's skip building the documentation for now...

# Download android_core
echo "Downloading android_core..."
cd ..

rosws merge http://android.rosjava.googlecode.com/hg/.rosinstall >& /dev/null
rosws set android_core --version="$ANDROIDCORE_VERSION" -y >& /dev/null
rosws update
source setup.bash
cd google
echo "Building Google prerequisite..."
echo "$ ./gradlew install" > rosjava_build.log
./gradlew install >> rosjava_build.log
checkstatus

# Build the android_core library
echo "Building android_core..."
cd ../android_core
android update project --path ./android_honeycomb_mr2/ --target android-13
echo "$ ./gradlew debug" > rosjava_build.log
./gradlew debug >> rosjava_build.log
checkstatus

echo "Complete!"
