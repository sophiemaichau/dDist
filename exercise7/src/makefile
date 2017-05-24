
JFLAGS = -g
JC = javac
JVM = java
FILE=
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

LINUX = xterm -e "make run1"

LINUX_CONFIG = $(shell $(LINUX))

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

default: classes

classes: $(CLASSES:.java=.class)

run1: classes
	$(JVM) DistributedTextEditor

run2: classes
	$(JVM) DistributedTextEditor

run3: classes
	$(JVM) DistributedTextEditor

run4: classes
	$(JVM) DistributedTextEditor

clean:
	$(RM) *.class
	$(RM) Utilities/*.class

all:
	@echo $(LINUX_CONFIG)
