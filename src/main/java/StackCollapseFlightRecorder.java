// If compiler fails here, you need an OpenJDK Java 17 or later.

import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

/**
 * Adapted from
 * <a href="https://github.com/billybong/JavaFlames/blob/main/JavaFlames.java">https://github.com/billybong/JavaFlames/blob/main/JavaFlames.java</a>
 */

public class StackCollapseFlightRecorder {

    public static final String JDK_EXECUTION_SAMPLE = "jdk.ExecutionSample";

    public static List<String> collapseFlightRecorderEvents(final Path jfrRecordingPath) throws IOException {

        // Streams rapidly becomes quite complex.  Considering how to improve readability.

        try (var jfr = new RecordingFile(jfrRecordingPath)) {

            var eventTypes = new ArrayList<>(jfr.readEventTypes().stream()
                    .map(EventType::getName)
                    .toList());
            eventTypes.sort(Comparator.naturalOrder());

            Map<String, Long> frameStringCountMap = Stream
                    // convert JFR "hasMoreEvents? + get" to stream.
                    .generate(() -> jfr.hasMoreEvents() ? io(jfr::readEvent).get() : null)
                    .takeWhile(Objects::nonNull)

                    .filter(it -> JDK_EXECUTION_SAMPLE.equals(it.getEventType().getName()))

                    // fold reversed frames into a single semicolon separated string

                    .map(event -> reverseList(event.getStackTrace().getFrames()).stream()
                            .map(f -> "%s::%s".formatted(
                                    f.getMethod().getType().getName(),
                                    f.getMethod().getName())
                            )
                            .collect(joining(";"))
                    )
                    // ORDER BY name GROUP BY name
                    .collect(groupingBy(identity(), counting()));

            return frameStringCountMap
                    // convert "frames-string"->count into "frames-string -> count".
                    .entrySet().stream().map(e -> "%s %d".formatted(e.getKey(), e.getValue()))
                    .sorted()
                    .toList();
        }
    }

    private static void exit(int code, String message) {
        System.err.println(message);
        System.exit(code);
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

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            exit(1, "expected jfr input file as argument");
        }
        var jfrFile = Paths.get(args[0]);
        if (!Files.exists(jfrFile)) {
            exit(2, jfrFile + " not found.");
        }
        for (var line : collapseFlightRecorderEvents(jfrFile)) {
            System.out.print(line + "\n"); // Ensure Unix endings.
        }
    }

    // Helpers for dealing with checked IOException's in lambdas

    private static <T> List<T> reverseList(List<T> frames) {
        var l = new ArrayList<>(frames);
        Collections.reverse(l);
        return l;
    }

    @FunctionalInterface
    interface IOSupplier<T> {
        T get() throws IOException;
    }
}
