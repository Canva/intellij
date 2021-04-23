package com.google.idea.sdkcompat.formatter;

import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.ChangedRangesInfo;
import org.jetbrains.annotations.NotNull;
import com.intellij.util.IncorrectOperationException;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class CodeStyleManagerAdapter extends CodeStyleManager {

    // #api211: Wildcard generics introduced in 2021.1
    @Override
    public void reformatText(@NotNull PsiFile file, @NotNull Collection<? extends TextRange> ranges) throws IncorrectOperationException {
        doReformatText(file, ranges);
    }

    public abstract void doReformatText(@NotNull PsiFile file, @NotNull Collection<? extends TextRange> ranges) throws IncorrectOperationException;

    // #api211: Wildcard generics introduced in 2021.1
    @Override
    public void reformatTextWithContext(@NotNull PsiFile file, @NotNull Collection<? extends TextRange> ranges) throws IncorrectOperationException {
        doReformatTextWithContext(file, ranges);
    }

    public void doReformatTextWithContext(@NotNull PsiFile file, @NotNull Collection<? extends TextRange> ranges) throws IncorrectOperationException {
        List<TextRange> rangesList = new ArrayList<>(ranges);
        reformatTextWithContext(file, new ChangedRangesInfo(rangesList, null));
    }
}
