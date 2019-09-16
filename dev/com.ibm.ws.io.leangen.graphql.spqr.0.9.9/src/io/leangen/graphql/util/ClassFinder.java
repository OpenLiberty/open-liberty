package io.leangen.graphql.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enables discovery of classes that extend or implement a given class.
 * Instances maintain a cache of search results and should be reused for better performance.
 * The cache is thread-safe but allows for multiple search requests to go through if they arrive at exactly the same time.
 * This class operates in best-effort manner and only logs (never rethrows) any exceptions that occur during the search or class loading.
 */
public class ClassFinder {

    public final static Predicate<ClassInfo> CONCRETE = info -> !info.isAbstract() && !info.isInterface();

    public final static Predicate<ClassInfo> NON_IGNORED = info ->
            info.getAnnotations().stream().noneMatch(ann -> ann.getName().equals(Ignore.class.getName()));

    public final static Predicate<ClassInfo> PUBLIC = ClassInfo::isPublic;

    public final static Predicate<ClassInfo> ALL = info -> true;

    public static final Logger log = LoggerFactory.getLogger(ClassFinder.class);

    private final Map<String, ScanResult> cache = new ConcurrentHashMap<>();

    /**
     * Searches for the implementations/subtypes of the given {@link AnnotatedType}. Only the matching classes are loaded.
     *
     * @param superType The type the implementations/subtypes of which are to be searched for
     * @param packages The packages to limit the search to
     *
     * @return A collection of {@link AnnotatedType}s discovered that implementation/extend {@code superType}
     */
    @SuppressWarnings("WeakerAccess")
    public List<AnnotatedType> findImplementations(AnnotatedType superType, Predicate<ClassInfo> filter, String... packages) {
        Class<?> rawType = ClassUtils.getRawType(superType.getType());
        return findImplementations(rawType, filter, packages).stream()
                .flatMap(raw -> ClassFinder.getExactSubType(superType, raw))
                .collect(Collectors.toList());
    }

    /**
     * Searches for the implementations/subtypes of the given class. Only the matching classes are loaded.
     *
     * @param superType The type the implementations/subtypes of which are to be searched for
     * @param packages The packages to limit the search to
     *
     * @return A collection of classes discovered that implementation/extend {@code superType}
     */
    public List<Class<?>> findImplementations(Class superType, Predicate<ClassInfo> filter, String... packages) {
        String[] scanPackages = Utils.emptyIfNull(packages);
        String cacheKey = Arrays.stream(scanPackages).sorted().collect(Collectors.joining());
        ScanResult scanResults = cache.computeIfAbsent(cacheKey, k -> new ClassGraph()
                .whitelistPackages(packages)
                .enableAllInfo()
                .initializeLoadedClasses()
                .scan());
        try {
            return scanResults.getAllClasses().stream()
                    .filter(impl -> superType.isInterface() ? impl.implementsInterface(superType.getName()) : impl.extendsSuperclass(superType.getName()))
                    .filter(filter == null ? info -> true : filter)
                    .flatMap(info -> loadClass(info, superType))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to auto discover the subtypes of " + superType.getName()
                    + ". Error encountered while scanning the classpath/modulepath.", e);
            return Collections.emptyList();
        }
    }

    private static Stream<AnnotatedType> getExactSubType(AnnotatedType superType, Class<?> subClass) {
        AnnotatedType subType = GenericTypeReflector.getExactSubType(superType, subClass);
        if (subType != null) {
            return Stream.of(subType);
        }
        log.warn("Auto discovered type " + subClass.getName() + " will be ignored " +
                "because the exact matching sub type of " + ClassUtils.toString(superType) + " could not be determined");
        return Stream.empty();
    }

    private static Stream<Class<?>> loadClass(ClassInfo classInfo, Class superType) {
        try {
            return Stream.of(Class.forName(classInfo.getName(), true, superType.getClassLoader()));
        } catch (ClassNotFoundException e) {
            log.warn(String.format("Auto discovered class %s could not be loaded using the same loader that loaded %s." +
                    " Trying other loaders... For details see %s",
                    classInfo.getName(), superType.getName(), Urls.Errors.IMPLEMENTATION_CLASS_LOADING_FAILED));
        }
        try {
            return Stream.of(classInfo.loadClass());
        } catch (Exception e) {
            log.error("Auto discovered type " + classInfo.getName() + " failed to load and will be ignored", e);
            return Stream.empty();
        }
    }

    public void close() {
        try {
            cache.values().forEach(ScanResult::close);
            cache.clear();
        } catch (Exception e) {
            log.warn(ScanResult.class.getName() + " did not close cleanly", e);
        }
    }
}
