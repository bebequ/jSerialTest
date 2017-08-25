TARGET=SerialDemo

all:
	javac -cp jSerialComm-1.3.11.jar "$(TARGET).java"
	
run:all
	java -cp jSerialComm-1.3.11.jar:. "$(TARGET)"	

clean:
	rm -rf "$(TARGET).class"
