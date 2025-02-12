/*
 * Copyright 2022 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.qsync.project;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;
import static java.util.Arrays.stream;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.idea.blaze.common.Context;
import com.google.idea.blaze.common.Label;
import com.google.idea.blaze.common.PrintOutput;
import com.google.idea.blaze.common.RuleKinds;
import com.google.idea.blaze.qsync.project.ProjectTarget.SourceType;
import com.google.idea.blaze.qsync.query.PackageSet;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * The build graph of all the rules that make up the project.
 *
 * <p>This class is immutable. A new instance of it will be created every time there is any change
 * to the project structure.
 */
@AutoValue
public abstract class BuildGraphData {

  /** A map from target to file on disk for all source files */
  public abstract ImmutableMap<Label, Location> locations();

  /** A set of all the BUILD files */
  public abstract PackageSet packages();

  /** A map from a file path to its target */
  abstract ImmutableMap<Path, Label> fileToTarget();

  /** All dependencies external to this project */
  public abstract ImmutableSet<Label> projectDeps();

  public abstract TargetTree allTargets();

  /** Mapping of in-project targets to {@link ProjectTarget}s */
  public abstract ImmutableMap<Label, ProjectTarget> targetMap();

  /**
   * All in-project targets with a direct compile or runtime dependency on a specified target, which
   * may be external.
   */
  @Memoized
  ImmutableMultimap<Label, Label> reverseDeps() {
    ImmutableMultimap.Builder<Label, Label> map = ImmutableMultimap.builder();
    for (ProjectTarget t : targetMap().values()) {
      for (Label dep : t.deps()) {
        map.put(dep, t.label());
      }
      for (Label runtimeDep : t.runtimeDeps()) {
        map.put(runtimeDep, t.label());
      }
    }
    return map.build();
  }

  /**
   * Calculates the set of direct reverse dependencies for a set of targets (including the targets
   * themselves).
   */
  public ImmutableSet<Label> getSameLanguageTargetsDependingOn(Set<Label> targets) {
    ImmutableMultimap<Label, Label> rdeps = reverseDeps();
    ImmutableSet.Builder<Label> directRdeps = ImmutableSet.builder();
    directRdeps.addAll(targets);
    for (Label target : targets) {
      ImmutableSet<QuerySyncLanguage> targetLanguages = targetMap().get(target).languages();
      // filter the rdeps based on the languages, removing those that don't have a common
      // language. This ensures we don't follow reverse deps of (e.g.) a java target depending on
      // a cc target.
      rdeps.get(target).stream()
          .filter(d -> !Collections.disjoint(targetMap().get(d).languages(), targetLanguages))
          .forEach(directRdeps::add);
    }
    return directRdeps.build();
  }

  /**
   * Returns all in project targets that depend on the source file at {@code sourcePath} via an
   * in-project dependency chain. Used to determine possible test targets for a given file.
   *
   * <p>If project target A depends on external target B, and external target B depends on project
   * target C, target A is *not* included in {@code getReverseDeps} for a source file in target C.
   */
  public Collection<ProjectTarget> getReverseDepsForSource(Path sourcePath) {

    ImmutableSet<Label> targetOwners = getTargetOwners(sourcePath);

    if (targetOwners == null || targetOwners.isEmpty()) {
      return ImmutableList.of();
    }

    Queue<Label> toVisit = Queues.newArrayDeque(targetOwners);
    Set<Label> visited = Sets.newHashSet();

    while (!toVisit.isEmpty()) {
      Label next = toVisit.remove();
      if (visited.add(next)) {
        toVisit.addAll(reverseDeps().get(next));
      }
    }

    return visited.stream()
        .map(label -> targetMap().get(label))
        .filter(Objects::nonNull)
        .collect(toImmutableList());
  }

  public ImmutableSet<Path> getTargetSources(Label target, SourceType... types) {
    return Optional.ofNullable(targetMap().get(target)).stream()
        .map(ProjectTarget::sourceLabels)
        .flatMap(m -> stream(types).map(m::get))
        .flatMap(Set::stream)
        .map(locations()::get)
        .filter(Objects::nonNull) // filter out generated sources
        .map(l -> l.file)
        .collect(toImmutableSet());
  }

  public ImmutableSet<ProjectTarget> targetsForKind(String kind) {
    return targetMap().values().stream()
        .filter(t -> t.kind().equals(kind))
        .collect(toImmutableSet());
  }

  @Override
  public final String toString() {
    // The default autovalue toString() implementation can result in a very large string which
    // chokes the debugger.
    return getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this));
  }

  public static Builder builder() {
    return new AutoValue_BuildGraphData.Builder();
  }

  @VisibleForTesting
  public static final BuildGraphData EMPTY =
      builder().projectDeps(ImmutableSet.of()).packages(PackageSet.EMPTY).build();

  /** Builder for {@link BuildGraphData}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract ImmutableMap.Builder<Label, Location> locationsBuilder();

    public abstract ImmutableMap.Builder<Path, Label> fileToTargetBuilder();

    public abstract ImmutableMap.Builder<Label, ProjectTarget> targetMapBuilder();

    public abstract Builder projectDeps(Set<Label> value);

    public abstract TargetTree.Builder allTargetsBuilder();

    public abstract Builder packages(PackageSet value);

    abstract BuildGraphData autoBuild();

    public final BuildGraphData build() {
      BuildGraphData result = autoBuild();
      // these are memoized, but we choose to pay the cost of building it now so that it's done at
      // sync time rather than later on.
      ImmutableSetMultimap<Label, Label> unused = result.sourceOwners();
      ImmutableMultimap<Label, Label> unused2 = result.reverseDeps();
      return result;
    }
  }

  /** Represents a location on a file. */
  public static class Location {

    private static final Pattern PATTERN = Pattern.compile("(.*):(\\d+):(\\d+)");

    public final Path file; // Relative to workspace root
    public final int row;
    public final int column;

    /**
     * @param location A location as provided by bazel, i.e. {@code path/to/file:lineno:columnno}
     */
    public Location(String location) {
      Matcher matcher = PATTERN.matcher(location);
      Preconditions.checkArgument(matcher.matches(), "Location not recognized: %s", location);
      file = Path.of(matcher.group(1));
      Preconditions.checkState(
          !file.startsWith("/"),
          "Filename starts with /: ensure that "
              + "`--relative_locations=true` was specified in the query invocation.");
      row = Integer.parseInt(matcher.group(2));
      column = Integer.parseInt(matcher.group(3));
    }
  }

  private final LoadingCache<Label, ImmutableSet<Label>> transitiveDeps =
      CacheBuilder.newBuilder()
          .build(CacheLoader.from(this::calculateTransitiveExternalDependencies));

  public ImmutableSet<Label> getTransitiveExternalDependencies(Label target) {
    return transitiveDeps.getUnchecked(target);
  }

  private ImmutableSet<Label> calculateTransitiveExternalDependencies(Label target) {
    ImmutableSet.Builder<Label> builder = ImmutableSet.builder();

    // Targets with cyclic dependencies will not build, but the query does not check for cycles
    Set<Label> visited = Sets.newHashSet();
    Deque<Label> toVisit = Queues.newArrayDeque();
    toVisit.add(target);

    while (!toVisit.isEmpty()) {
      Label nextLabel = toVisit.poll();
      if (visited.add(nextLabel)) {
        // targetMap only contains in-project target labels.
        if (!targetMap().containsKey(nextLabel)) {
          builder.add(nextLabel);
        } else {
          if (projectDeps().contains(nextLabel)) {
            builder.add(nextLabel);
          }
          toVisit.addAll(targetMap().get(nextLabel).deps());
        }
      }
    }
    return Sets.intersection(builder.build(), projectDeps()).immutableCopy();
  }

  @Memoized
  public ImmutableSetMultimap<Label, Label> sourceOwners() {
    return targetMap().values().stream()
        .flatMap(
            t -> t.sourceLabels().values().stream().map(src -> new SimpleEntry<>(src, t.label())))
        .collect(toImmutableSetMultimap(e -> e.getKey(), e -> e.getValue()));
  }

  @Nullable
  public ImmutableSet<Label> getTargetOwners(Path path) {
    Label syncTarget = fileToTarget().get(path);
    return sourceOwners().get(syncTarget);
  }

  /**
   * @deprecated Choosing a target based on the number of deps it has is not a good strategy, as we
   *     could end up selecting one that doesn't build in the current config. Allow the user to
   *     choose, or require the projects source -> target mapping to be unambiguous instead.
   */
  @Deprecated
  @Nullable
  public Label selectLabelWithLeastDeps(Collection<Label> candidates) {
    return candidates.stream()
        .min(Comparator.comparingInt(label -> targetMap().get(label).deps().size()))
        .orElse(null);
  }

  @VisibleForTesting
  @Nullable
  ImmutableSet<Label> getFileDependencies(Path path) {
    ImmutableSet<Label> targets = getTargetOwners(path);
    if (targets == null) {
      return null;
    }
    return targets.stream()
        .map(this::getTransitiveExternalDependencies)
        .flatMap(Set::stream)
        .collect(toImmutableSet());
  }

  /** A set of all the targets that show up in java rules 'src' attributes */
  @Memoized
  public ImmutableSet<Label> javaSources() {
    return sourcesByRuleKindAndType(RuleKinds::isJava, SourceType.REGULAR);
  }

  /** Returns a list of all the java source files of the project, relative to the workspace root. */
  public List<Path> getJavaSourceFiles() {
    return pathListFromLabels(javaSources());
  }

  /**
   * Returns a list of all the proto source files of the project, relative to the workspace root.
   */
  @Memoized
  public List<Path> getProtoSourceFiles() {
    return getSourceFilesByRuleKindAndType(RuleKinds::isProtoSource, SourceType.REGULAR);
  }

  /** Returns a list of all the cc source files of the project, relative to the workspace root. */
  @Memoized
  public List<Path> getCcSourceFiles() {
    return getSourceFilesByRuleKindAndType(RuleKinds::isCc, SourceType.REGULAR);
  }

  public List<Path> getSourceFilesByRuleKindAndType(
      Predicate<String> ruleKindPredicate, SourceType... sourceTypes) {
    return pathListFromLabels(sourcesByRuleKindAndType(ruleKindPredicate, sourceTypes));
  }

  private ImmutableSet<Label> sourcesByRuleKindAndType(
      Predicate<String> ruleKindPredicate, SourceType... sourceTypes) {
    return targetMap().values().stream()
        .filter(t -> ruleKindPredicate.test(t.kind()))
        .map(ProjectTarget::sourceLabels)
        .flatMap(srcs -> stream(sourceTypes).map(srcs::get))
        .flatMap(Set::stream)
        .collect(toImmutableSet());
  }

  private List<Path> pathListFromLabels(Collection<Label> labels) {
    List<Path> paths = new ArrayList<>();
    for (Label src : labels) {
      Location location = locations().get(src);
      if (location == null) {
        continue;
      }
      paths.add(location.file);
    }
    return paths;
  }

  public ImmutableSet<Path> getAllSourceFiles() {
    return fileToTarget().keySet();
  }

  /**
   * Returns a list of regular (java/kt) source files owned by an Android target, relative to the
   * workspace root.
   */
  public List<Path> getAndroidSourceFiles() {
    return getSourceFilesByRuleKindAndType(RuleKinds::isAndroid, SourceType.REGULAR);
  }

  public List<Path> getAndroidResourceFiles() {
    return getSourceFilesByRuleKindAndType(RuleKinds::isAndroid, SourceType.ANDROID_RESOURCES);
  }

  /** Returns a list of custom_package fields that used by current project. */
  public ImmutableSet<String> getAllCustomPackages() {
    return targetMap().values().stream()
        .map(ProjectTarget::customPackage)
        .flatMap(Optional::stream)
        .collect(toImmutableSet());
  }

  public ImmutableSet<DependencyTrackingBehavior> getDependencyTrackingBehaviors(Label target) {
    if (!targetMap().containsKey(target)) {
      return ImmutableSet.of();
    }
    return targetMap().get(target).languages().stream()
        .map(l -> l.dependencyTrackingBehavior)
        .collect(toImmutableSet());
  }

  /**
   * Returns the list of project targets related to the given workspace file.
   *
   * @param context Context
   * @param workspaceRelativePath Workspace relative file path to find targets for. This may be a
   *     source file, directory or BUILD file.
   * @return Corresponding project targets. For a source file, this is the targets that build that
   *     file. For a BUILD file, it's the set or targets defined in that file. For a directory, it's
   *     the set of all targets defined in all build packages within the directory (recursively).
   */
  public TargetsToBuild getProjectTargets(Context<?> context, Path workspaceRelativePath) {
    if (workspaceRelativePath.endsWith("BUILD")) {
      Path packagePath = workspaceRelativePath.getParent();
      return TargetsToBuild.targetGroup(allTargets().get(packagePath));
    } else {
      TargetTree targets = allTargets().getSubpackages(workspaceRelativePath);
      if (!targets.isEmpty()) {
        // this will only be non-empty for directories
        return TargetsToBuild.targetGroup(targets.toLabelSet());
      }
    }
    // Now a build file or a directory containing packages.
    if (getAllSourceFiles().contains(workspaceRelativePath)) {
      ImmutableSet<Label> targetOwner = getTargetOwners(workspaceRelativePath);
      if (!targetOwner.isEmpty()) {
        return TargetsToBuild.forSourceFile(targetOwner, workspaceRelativePath);
      }
    } else {
      context.output(
          PrintOutput.error("Can't find any supported targets for %s", workspaceRelativePath));
      context.output(
          PrintOutput.error(
              "If this is a newly added supported rule, please re-sync your project."));
      context.setHasWarnings();
    }
    return TargetsToBuild.NONE;
  }

  /**
   * Returns the set of targets that would need to be built in order to enable analysis for a
   * project target.
   *
   * @param projectTarget A project target.
   * @return The set of targets that need to be built. For a {@link
   *     DependencyTrackingBehavior#EXTERNAL_DEPENDENCIES} target this will be the set of external
   *     dependenceis; for a {@link DependencyTrackingBehavior#SELF} target this will be the target
   *     itself.
   */
  public ImmutableSet<Label> getExternalDepsToBuildFor(Label projectTarget) {
    ImmutableSet<DependencyTrackingBehavior> depTracking =
        getDependencyTrackingBehaviors(projectTarget);

    ImmutableSet.Builder<Label> deps = ImmutableSet.builder();
    for (DependencyTrackingBehavior behavior : depTracking) {
      switch (behavior) {
        case EXTERNAL_DEPENDENCIES:
          deps.addAll(getTransitiveExternalDependencies(projectTarget));
          break;
        case SELF:
          // For C/C++, we don't need to build external deps, but we do need to extract
          // compilation information for the target itself.
          deps.add(projectTarget);
          break;
      }
    }
    return deps.build();
  }

  /**
   * Returns the set of {@link ProjectTarget#languages() target languages} for a set of project
   * targets.
   */
  public ImmutableSet<QuerySyncLanguage> getTargetLanguages(ImmutableSet<Label> targets) {
    return targets.stream()
        .map(targetMap()::get)
        .map(ProjectTarget::languages)
        .reduce((a, b) -> Sets.union(a, b).immutableCopy())
        .orElse(ImmutableSet.of());
  }

  /**
   * Calculates the {@link RequestedTargets} for a project target.
   *
   * @return Requested targets. The {@link RequestedTargets#buildTargets} will match the parameter
   *     given; the {@link RequestedTargets#expectedDependencyTargets} will be determined by the
   *     {@link #getDependencyTrackingBehaviors(Label)} of the targets given.
   */
  public Optional<RequestedTargets> computeRequestedTargets(Set<Label> projectTargets) {
    ImmutableSet<Label> externalDeps =
        projectTargets.stream()
            .filter(
                t ->
                    getDependencyTrackingBehaviors(t).stream()
                        .anyMatch(b -> b.shouldIncludeExternalDependencies))
            .flatMap(t -> getTransitiveExternalDependencies(t).stream())
            .collect(toImmutableSet());

    return Optional.of(new RequestedTargets(ImmutableSet.copyOf(projectTargets), externalDeps));
  }
}
