
JFLAGS = -g
JC = javac
JVM = java
FILE=
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

LINUX_CONFIG3 = $(shell xterm -e "make run1" & xterm -e "make run2" & xterm -e "make run3")

LINUX_CONFIG4 = $(shell xterm -e "make run1" & xterm -e "make run2" & xterm -e "make run3" & xterm -e "make run4")


LINUX_CONFIG5 = $(shell xterm -e "make run1" & xterm -e "make run2" & xterm -e "make run3" & xterm -e "make run4" & xterm -e "make run5")

MAIN = DistributedTextEditor

default: clean
	@echo $(shell javac *.java)

run1:
	@echo $(shell javac *.java)
	$(JVM) DistributedTextEditor 20 0

run2:
	@echo $(shell javac *.java)
	$(JVM) DistributedTextEditor 600 0

run3:
	@echo $(shell javac *.java)
	$(JVM) DistributedTextEditor 1200 500

run4:
	@echo $(shell javac *.java)
	$(JVM) DistributedTextEditor 20 500

run5:
	@echo $(shell javac *.java)
	$(JVM) DistributedTextEditor 600 500

clean:
	$(RM) *.class
	$(RM) Utilities/*.class

three: default
	@echo $(LINUX_CONFIG3)

four: default
	@echo $(LINUX_CONFIG4)

five: default
	@echo $(LINUX_CONFIG5)

open: default
	@echo $(shell java DistributedTextEditor)