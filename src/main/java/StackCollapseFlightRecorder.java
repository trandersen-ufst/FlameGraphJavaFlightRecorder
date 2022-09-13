
// If compiler fails here, you need an OpenJDK Java 17 or later.

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adapted from <a href="https://github.com/billybong/JavaFlames/blob/main/JavaFlames.java">https://github.com/billybong/JavaFlames/blob/main/JavaFlames.java</a>
 */

public class StackCollapseFlightRecorder {

    public static final String JDK_EXECUTION_SAMPLE = "jdk.ExecutionSample";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            exit(1, "expected jfr input file as argument");
        }
        var jfrFile = Paths.get(args[0]);
        if (!Files.exists(jfrFile)) {
            exit(2, jfrFile + " not found.");
        }
        for (var line : produceFlameGraphLog(jfrFile).toList()) {
            System.out.print(line + "\n"); // Ensure Unix endings.
        }
    }

    private static void exit(int code, String message) {
        System.err.println(message);
        System.exit(code);
    }


    public static Stream<String> produceFlameGraphLog(final Path jfrRecording) throws IOException {
        var recordingFile = new RecordingFile(jfrRecording);
        return extractEvents(recordingFile)
                .filter(it -> JDK_EXECUTION_SAMPLE.equals(it.getEventType().getName()))
                // fold frames into a single semicolon separated string
                .map(event -> collapseFrames(event.getStackTrace().getFrames()))
                // collapse identical frames into "frames count". (uniq -c)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream().map(e -> "%s %d".formatted(e.getKey(), e.getValue()))
                .sorted()
                .onClose(io(recordingFile::close));
    }

    public static String collapseFrames(List<RecordedFrame> frames) {
        /*
        var methodNames = new ArrayDeque<String>(frames.size());
        for (var frame : frames) {
            final RecordedMethod method = frame.getMethod();
            methodNames.addFirst("%s::%s".formatted(method.getType().getName(), method.getName()));
        }
        return String.join(";", methodNames);

         */

        // The string needs to be reversed.  Can it be done efficiently with streams?

        var l1 = frames.stream()
                .map(frame -> frame.getMethod())
                .map(m -> "%s::%s".formatted(m.getType().getName(), m.getName()))
                .toList();
        var l2 = new ArrayList<>(l1);
        Collections.reverse(l2);
        return String.join(";", l2);
    }

    public static Stream<RecordedEvent> extractEvents(RecordingFile recordingFile) {
        return Stream.generate(() ->
                recordingFile.hasMoreEvents() ?
                        io(recordingFile::readEvent).get() :
                        null
        ).takeWhile(Objects::nonNull);
    }

    // Helpers for dealing with checked IOException's in lambdas

    @FunctionalInterface
    interface IORunnable {
        void run() throws IOException;
    }

    /*
        @FunctionalInterface
        interface IOConsumer<T> {
            void apply(T input) throws IOException;
        }
    */
    @FunctionalInterface
    interface IOSupplier<T> {
        T get() throws IOException;
    }

    private static <T> Supplier<T> io(IOSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    /*
    private static <T> Consumer<T> io(IOConsumer<T> consumer) {
        return t -> {
            try {
                consumer.apply(t);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

     */

    private static Runnable io(IORunnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
}
