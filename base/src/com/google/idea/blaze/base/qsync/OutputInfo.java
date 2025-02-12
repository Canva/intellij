/*
 * Copyright 2023 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.base.qsync;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.idea.blaze.base.command.buildresult.OutputArtifact;
import com.google.idea.blaze.common.Label;
import com.google.idea.blaze.common.vcs.VcsState;
import com.google.idea.blaze.qsync.java.JavaTargetInfo.JavaArtifacts;
import com.google.idea.blaze.qsync.java.cc.CcCompilationInfoOuterClass.CcCompilationInfo;
import java.util.Optional;

/** A data class that collecting and converting output group artifacts. */
@AutoValue
public abstract class OutputInfo {

  @VisibleForTesting
  public static final OutputInfo EMPTY =
      create(
          GroupedOutputArtifacts.EMPTY,
          ImmutableSet.of(),
          ImmutableSet.of(),
          ImmutableSet.of(),
          0,
          Optional.empty());

  /** Returns the proto containing details of artifacts per target produced by the aspect. */
  public abstract ImmutableSet<JavaArtifacts> getArtifactInfo();

  public abstract ImmutableSet<CcCompilationInfo> getCcCompilationInfo();

  public abstract GroupedOutputArtifacts getOutputGroups();

  public abstract ImmutableSet<Label> getTargetsWithErrors();

  public abstract int getExitCode();

  /**
   * The state of the VCS from the build that produced this output. May be absent if the bazel
   * instance of VCS state do not support this.
   */
  public abstract Optional<VcsState> getVcsState();

  public ImmutableList<OutputArtifact> get(OutputGroup group) {
    return getOutputGroups().get(group);
  }

  public ImmutableList<OutputArtifact> getJars() {
    return getOutputGroups().get(OutputGroup.JARS);
  }

  public ImmutableList<OutputArtifact> getAars() {
    return getOutputGroups().get(OutputGroup.AARS);
  }

  public ImmutableList<OutputArtifact> getGeneratedSources() {
    return getOutputGroups().get(OutputGroup.GENSRCS);
  }

  public boolean isEmpty() {
    return getOutputGroups().isEmpty();
  }

  @VisibleForTesting
  public abstract Builder toBuilder();

  @VisibleForTesting
  public static Builder builder() {
    return EMPTY.toBuilder();
  }

  public static OutputInfo create(
      GroupedOutputArtifacts allArtifacts,
      ImmutableSet<JavaArtifacts> artifacts,
      ImmutableSet<CcCompilationInfo> ccInfo,
      ImmutableSet<Label> targetsWithErrors,
      int exitCode,
      Optional<VcsState> vcsState) {
    return new AutoValue_OutputInfo.Builder()
        .setArtifactInfo(artifacts)
        .setCcCompilationInfo(ccInfo)
        .setOutputGroups(allArtifacts)
        .setTargetsWithErrors(targetsWithErrors)
        .setExitCode(exitCode)
        .setVcsState(vcsState)
        .build();
  }

  /** Builder for {@link OutputInfo}. */
  @VisibleForTesting
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setArtifactInfo(ImmutableSet<JavaArtifacts> value);

    public abstract Builder setArtifactInfo(JavaArtifacts... values);

    public abstract Builder setCcCompilationInfo(ImmutableSet<CcCompilationInfo> value);

    public abstract Builder setOutputGroups(GroupedOutputArtifacts artifcts);

    public abstract Builder setTargetsWithErrors(ImmutableSet<Label> value);

    public abstract Builder setTargetsWithErrors(Label... values);

    public abstract Builder setExitCode(int value);

    public abstract Builder setVcsState(Optional<VcsState> value);

    public abstract OutputInfo build();
  }
}
