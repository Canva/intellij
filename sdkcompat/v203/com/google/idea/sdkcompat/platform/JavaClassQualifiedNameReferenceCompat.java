package com.google.idea.sdkcompat.platform;

import com.intellij.patterns.PatternConditionPlus;
import com.intellij.util.ProcessingContext;
import com.intellij.util.PairProcessor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.PsiElementBase;

public abstract class JavaClassQualifiedNameReferenceCompat {

    /** wildcard generics added in 2021.1 */
    private static PatternCondition<PsiElementBase> nameCondition(final ElementPattern<?> pattern) {
        return new PatternConditionPlusCompat<PsiElementBase, String>("_withPsiName", pattern) {
            @Override
            public boolean processValues(
                    PsiElementBase t,
                    ProcessingContext context,
                    PairProcessor<String, ProcessingContext> processor) {
                return processor.process(t.getName(), context);
            }
        };
    }
}
