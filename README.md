This repository is for adapting the ideas from
<https://github.com/billybong/JavaFlames>
(which created HTML flame graphs on the fly) about
converting Java Flight Recorder data into something
that <https://github.com/brendangregg/FlameGraph> understands
to get SVG flame graphs.

Usage:

    java .... saved.jfr | flamegraph.pl > saved.svg

(flamegraph.pl is from FlameGraph repository)

To get a flight recorder recording:

    java -XX:StartFlightRecording=duration=1h,settings=profile,filename=saved.jfr ...

See <https://docs.oracle.com/en/java/javase/11/tools/java.html#GUID-4856361B-8BFD-4964-AE84-121F5F6CF111> for 
a list of options.  Search for StartFlightRecording for those influencing HOW data is saved.
Search for FlightRecorderOptions for those influencing WHAT data is saved.a

To get a flight recorder recording from a Maven invocation, set MAVEN_OPTS first.

Windows CMD.exe:

    set MAVEN_OPTS=-XX:StartFlightRecording=duration=1h,filename=saved.jfr

Linux/MacOS:

    export MAVEN_OPTS=-XX:StartFlightRecording=duration=1h,filename=saved.jfr

(see <https://access.redhat.com/documentation/en-us/openjdk/17/html-single/using_jdk_flight_recorder_with_openjdk/index#starting-jdk-flight-recorder> for details)

To actually convert the collapsed data to a flamegraph run something like this in the
FlameGraph repository

```shell
./flamegraph.pl < /mnt/c/Users/W31473/git/FlameGraphJavaFlightRecorder/saved.collapsed  > /mnt/c/Users/W31473/git/FlameGraphJavaFlightRecorder/saved.svg
```

/ravn 2022-09-16


