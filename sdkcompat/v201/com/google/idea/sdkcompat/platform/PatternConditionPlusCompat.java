package com.google.idea.sdkcompat.platform;

import com.intellij.patterns.PatternConditionPlus;
import com.intellij.util.ProcessingContext;
import com.intellij.util.PairProcessor;
import com.intellij.patterns.ElementPattern;

public abstract class PatternConditionPlusCompat<Target, Value> extends PatternConditionPlus<Target, Value> {

    public PatternConditionPlusCompat(String methodName, final ElementPattern valuePattern) {
        super(methodName, valuePattern);
    }

    /** #api211: wildcard generics added in 2021.1 */
    @Override
    public boolean processValues(final Target t, final ProcessingContext context, final PairProcessor<Value, ProcessingContext> processor) {
        return doProcessValues(t, context, processor);
    }

    public abstract boolean doProcessValues(final Target t, final ProcessingContext context, final PairProcessor<? super Value, ? super ProcessingContext> processor);

}
