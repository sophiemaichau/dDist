
JFLAGS = -g
JC = javac
JVM = java
FILE=
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

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

run: classes
	$(JVM) $(MAIN)

clean:
	$(RM) *.class
	$(RM) Utilities/*.class

milo: xterm -e "make run"
