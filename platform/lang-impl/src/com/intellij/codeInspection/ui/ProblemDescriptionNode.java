/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.codeInspection.ui;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemDescriptorUtil;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.FactoryMap;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.codeInspection.ProblemDescriptorUtil.APPEND_LINE_NUMBER;
import static com.intellij.codeInspection.ProblemDescriptorUtil.TRIM_AT_TREE_END;

/**
 * @author max
 */
public class ProblemDescriptionNode extends CachedInspectionTreeNode implements RefElementAware {
  protected RefEntity myElement;
  private final CommonProblemDescriptor myDescriptor;
  protected final InspectionToolWrapper myToolWrapper;
  @NotNull
  protected final InspectionToolPresentation myPresentation;
  private final HighlightDisplayLevel myLevel;

  public ProblemDescriptionNode(RefEntity element,
                                CommonProblemDescriptor descriptor,
                                @NotNull InspectionToolWrapper toolWrapper,
                                @NotNull InspectionToolPresentation presentation) {
    super(descriptor);
    myElement = element;
    myDescriptor = descriptor;
    myToolWrapper = toolWrapper;
    myPresentation = presentation;
    final InspectionProfileImpl profile = (InspectionProfileImpl)presentation.getContext().getCurrentProfile();
    myLevel = descriptor instanceof ProblemDescriptor
              ? profile.getErrorLevel(HighlightDisplayKey.find(toolWrapper.getShortName()), ((ProblemDescriptor)descriptor).getStartElement())
              : profile.getTools(toolWrapper.getID(), element.getRefManager().getProject()).getLevel();
    init();
}

  @NotNull
  public InspectionToolWrapper getToolWrapper() {
    return myToolWrapper;
  }

  @Nullable
  public RefEntity getElement() {
    return myElement;
  }

  @Nullable
  public CommonProblemDescriptor getDescriptor() {
    return myDescriptor;
  }

  @Override
  public int getProblemCount() {
    return 1;
  }

  @Override
  public void visitProblemSeverities(FactoryMap<HighlightDisplayLevel, Integer> counter) {
    counter.put(myLevel, counter.get(myLevel) + 1);
  }

  @Override
  public boolean calculateIsValid() {
    if (myElement instanceof RefElement && !myElement.isValid()) return false;
    final CommonProblemDescriptor descriptor = getDescriptor();
    if (descriptor instanceof ProblemDescriptor) {
      final PsiElement psiElement = ((ProblemDescriptor)descriptor).getPsiElement();
      return psiElement != null && psiElement.isValid();
    }
    return true;
  }


  @Override
  public boolean isResolved(ExcludedInspectionTreeNodesManager manager) {
    return myElement instanceof RefElement && getPresentation().isProblemResolved(myElement, getDescriptor());
  }

  @Override
  public void ignoreElement(ExcludedInspectionTreeNodesManager manager) {
    InspectionToolPresentation presentation = getPresentation();
    presentation.ignoreCurrentElementProblem(getElement(), getDescriptor());
  }

  @Override
  public void amnesty(ExcludedInspectionTreeNodesManager manager) {
    InspectionToolPresentation presentation = getPresentation();
    presentation.amnesty(getElement());
  }

  @NotNull
  private InspectionToolPresentation getPresentation() {
    return myPresentation;
  }

  @Override
  public FileStatus getNodeStatus() {
    if (myElement instanceof RefElement){
      return getPresentation().getProblemStatus(myDescriptor);
    }
    return FileStatus.NOT_CHANGED;
  }

  @Override
  public String calculatePresentableName() {
    CommonProblemDescriptor descriptor = getDescriptor();
    if (descriptor == null) return "";
    PsiElement element = descriptor instanceof ProblemDescriptor ? ((ProblemDescriptor)descriptor).getPsiElement() : null;

    return XmlStringUtil.stripHtml(ProblemDescriptorUtil.renderDescriptionMessage(descriptor, element,
                                                                                  APPEND_LINE_NUMBER | TRIM_AT_TREE_END));
  }
}
