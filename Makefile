NAME := app.Main

# JARs
CORE := C:\Program Files\Processing\app\resources\core\library\core-4.4.4.jar
OPENCV := C:\Users\tecfa\OneDrive\Documents\Processing\libraries\opencv_processing\library\*
VIDEO := c:\Users\tecfa\OneDrive\Documents\Processing\libraries\video\library\*

#jdk
JAVA_HOME := C:\Program Files\Java\jdk-17\bin


# classpath
COMPILE_CP := lib\*;$(CORE);$(OPENCV);$(VIDEO)
RUN_CP := bin;lib\*;$(CORE);$(OPENCV);$(VIDEO)

# java compiler and runtime
JAVAC = $(JAVA_HOME)/javac 
JAVA = $(JAVA_HOME)/java

# DIRECTORY
SRC_DIR := src
BIN_DIR := bin
$(BIN_DIR):
	@if not exist "$(BIN_DIR)" mkdir "$(BIN_DIR)"

SRC := src/app/Main.java \
	   src/app/AppState.java \
	   src/app/Calibration.java \
	   src/app/CamCapture.java \
	   src/app/LiveSegment.java \
	   src/app/Marker.java \
	   src/app/MarkerButton.java \
	   src/app/MarkerManager.java \
	   src/app/ProjectorWindow.java \
	   src/app/SegmentManager.java \
	   src/app/StaticSegment.java \
	   src/app/TrackedMarker.java \
	   src/app/Triangle.java \
	   src/app/TriangleManager \
	   src/app/UtilsUI.java

all: $(BIN_DIR) 
	"$(JAVAC)" -cp "$(COMPILE_CP)" -d "$(BIN_DIR)" $(SRC)

run: all
	"$(JAVA)" -cp "$(RUN_CP)" $(NAME)

clean:
	@if exist "$(BIN_DIR)" rmdir /S /Q "$(BIN_DIR)"

.PHONY: all run clean