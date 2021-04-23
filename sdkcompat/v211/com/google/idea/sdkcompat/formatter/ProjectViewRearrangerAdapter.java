package com.google.idea.sdkcompat.formatter;

import com.intellij.psi.codeStyle.arrangement.Rearranger;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.arrangement.ArrangementSettings;
import com.intellij.psi.codeStyle.arrangement.ArrangementEntry;
import com.intellij.openapi.util.Pair;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public abstract class ProjectViewRearrangerAdapter<T extends ArrangementEntry> implements Rearranger<T> {

    // #api211: Wildcard generics introduced in 2021.1
    @Override
    public List<T> parse(PsiElement root, @Nullable Document document, Collection<? extends TextRange> ranges, ArrangementSettings settings) {
        return doParse(root, document, ranges, settings);
    }

    public abstract List<T> doParse(
            PsiElement root,
            @Nullable Document document,
            Collection<? extends TextRange> ranges,
            ArrangementSettings settings);

    @Nullable
    @Override
    public Pair<T, List<T>> parseWithNew(
            PsiElement root,
            @Nullable Document document,
            Collection<? extends TextRange> ranges,
            PsiElement element,
            ArrangementSettings settings) {
        return doParseWithNew(root, document, ranges, element, settings);
    }

    public abstract Pair<T, List<T>> doParseWithNew(
            PsiElement root,
            @Nullable Document document,
            Collection<? extends TextRange> ranges,
            PsiElement element,
            ArrangementSettings settings);
}
