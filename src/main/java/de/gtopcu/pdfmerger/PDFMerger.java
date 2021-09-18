package de.gtopcu.pdfmerger;

import com.google.common.collect.Sets;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PDFMerger {
  private static final String RESULT_FILE_NAME_FORMAT = "%s.pdf";
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

  private static final String ARGUMENT_RESULT_NAME = "--resultname=";
  private static final String ARGUMENT_SOURCE_ROOT = "--sourceroot=";

  private static final Set<String> FILE_EXTENSIONS =
      Sets.newHashSet(Collections.singletonList(".pdf"));

  public static void main(String[] args) {
    final String result = determineResultFileName(args);
    final String sourceRoot = determineSourceRoot(args);
    final Path sourceRootPath = Paths.get(sourceRoot);
    if (Files.notExists(sourceRootPath)) {
      System.err.println("The given path does not exist");
      return;
    }
    try {
      final List<Path> paths = availableFiles(sourceRootPath);
      if (paths.isEmpty()) {
        System.err.println("There is no mergeable file");
        return;
      }
      mergeAllPortableDocumentFiles(paths, Paths.get(sourceRoot, result));
    } catch (Throwable throwable) {
      throwable.printStackTrace();
    }
  }

  private static void mergeAllPortableDocumentFiles(List<Path> paths, Path destinaionFileName)
      throws IOException {
    final PDFMergerUtility merger = new PDFMergerUtility();
    merger.setDestinationFileName(destinaionFileName.toString());
    for (Path path : paths) {
      final File file = path.toFile();
      final String name = file.getName();
      merger.addSource(path.toFile());
      System.out.println("[PROCESSING] merged file " + name);
      merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());
    }
  }

  /**
   * Lists all available files as a {@link Path} representation sorted by their names
   *
   * @param source where the files should come from
   * @return all available files
   * @throws IOException if an I/O error occurs when opening the directory
   */
  private static List<Path> availableFiles(Path source) throws IOException {
    return Files.list(source)
        .filter(checkForValidity())
        .sorted(Path::compareTo)
        .collect(Collectors.toList());
  }

  /**
   * Checks the given {@link Path}'s name for available extensions
   *
   * @return Predicate which decides if the given path is valid or not
   */
  private static Predicate<Path> checkForValidity() {
    return path ->
        FILE_EXTENSIONS.stream()
            .anyMatch(ext -> path.getFileName().toString().toLowerCase().endsWith(ext));
  }

  /**
   * Finds out what the name of the produced file should be
   *
   * @param args given to the program before being executed
   * @return name found out by the given args
   */
  private static String determineResultFileName(String[] args) {
    return String.format(
        RESULT_FILE_NAME_FORMAT,
        determineProgramArgumentValue(
            args, ARGUMENT_RESULT_NAME, DATE_TIME_FORMATTER.format(LocalDateTime.now())));
  }

  /**
   * Searches for the sourceroot where the mergeable files are taken from
   *
   * @param args given to the program before being executed
   * @return sourceroot found out by the given args
   */
  private static String determineSourceRoot(String[] args) {
    return determineProgramArgumentValue(
        args, ARGUMENT_SOURCE_ROOT, System.getProperty("user.dir"));
  }

  /**
   * Searches for a given argument value by a specific key
   *
   * @param args given to the program before being executed
   * @param key for which a value the program is searching for
   * @param defaultValue if there is no value for the given key, this value will be returned
   * @return either the found or defaultValue
   */
  private static String determineProgramArgumentValue(
      String[] args, String key, String defaultValue) {
    return Arrays.stream(args)
        .map(String::toLowerCase)
        .filter(value -> value.startsWith(key))
        .findFirst()
        .map(value -> value.substring(key.length()))
        .orElse(defaultValue);
  }
}
