TARGET=SerialDemo
ifeq ($(OS),Windows_NT)
	SERP=;
else
	SERP=:
endif

all:
	javac -cp jSerialComm-1.3.11.jar "$(TARGET).java"
	
run:all
	java -cp "jSerialComm-1.3.11.jar$(SERP)." "$(TARGET)"

clean:
	rm -rf *.class
