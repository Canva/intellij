/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package com.google.idea.sdkcompat.vcs;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.Change.Type;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.VcsUser;
import com.intellij.vcs.log.impl.VcsChangesLazilyParsedDetails;
import com.intellij.vcs.log.impl.VcsFileStatusInfo;
import java.util.List;
import java.util.function.BiFunction;
import javax.annotation.Nullable;

/** #api182: adapter for changes to VcsChangesLazilyParsedDetails in 2018.3. */
public abstract class VcsChangesLazilyParsedDetailsAdapter<V extends VcsRevisionNumber>
    extends VcsChangesLazilyParsedDetails {

  protected VcsChangesLazilyParsedDetailsAdapter(
      Project project,
      VirtualFile root,
      Hash hash,
      List<Hash> parentsHashes,
      V vcsRevisionNumber,
      String subject,
      String commitMessage,
      VcsUser author,
      long time,
      List<List<VcsFileStatusInfo>> reportedChanges,
      ChangesParser changesParser) {
    super(
        project,
        hash,
        parentsHashes,
        time,
        root,
        subject,
        author,
        commitMessage,
        author,
        time,
        reportedChanges,
        changesParser);
  }

  protected abstract List<V> getParents(V revision);

  protected abstract Change createChange(
      Project project,
      VirtualFile root,
      @Nullable String fileBefore,
      @Nullable V revisionBefore,
      @Nullable String fileAfter,
      V revisionAfter,
      FileStatus aStatus);

  protected abstract FileStatus renamedFileStatus();

  private class UnparsedChanges extends VcsChangesLazilyParsedDetails.UnparsedChanges {
    private UnparsedChanges(
        Project project,
        List<List<VcsFileStatusInfo>> changesOutput,
        BiFunction<List<VcsFileStatusInfo>,Integer,List<Change>> parser) {
      super(project, changesOutput, parser);
    }
  }
}
