
JFLAGS = -g
JC = javac
JVM = java
FILE=
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

LINUX_CONFIG3 = $(shell xterm -e "make run1" & xterm -e "make run2" & xterm -e "make run3")

LINUX_CONFIG4 = $(shell xterm -e "make run1" & xterm -e "make run2" & xterm -e "make run3" & xterm -e "make run4")

CLASSES = \
	AbstractClient.java \
	AbstractServer.java \
    ConcreteServer.java \
    ConcreteClient.java \
    ConnectionHandler.java \
    ConnectionList.java \
    DistributedTextEditor.java \
    DocumentEventCapturer.java \
    ElectionStrategy.java \
    OldestFirstElectionStrategy.java \
    RedirectServer.java \
    RemoteList.java \
    Utilities/MessageWrapper.java \
    Utilities/MyTextEvent.java \
    Utilities/Pair.java \
    Utilities/RedirectEvent.java \
    Utilities/TextCopyEvent.java \
    Utilities/TextInsertEvent.java \
    Utilities/TextRemoveEvent.java \
    Utilities/UpdateViewEvent.java

MAIN = DistributedTextEditor

default: clean classes

classes: $(CLASSES:.java=.class)

run1: classes
	$(JVM) DistributedTextEditor 20 0

run2: classes
	$(JVM) DistributedTextEditor 600 0

run3: classes
	$(JVM) DistributedTextEditor 1200 0

run4: classes
	$(JVM) DistributedTextEditor 200 500

clean:
	$(RM) *.class
	$(RM) Utilities/*.class

three: default
	@echo $(LINUX_CONFIG3)

four: default
	@echo $(LINUX_CONFIG4)

open: default
	@echo $(shell java DistributedTextEditor 4)