package com.google.idea.sdkcompat.python;

import com.jetbrains.python.psi.LanguageLevel;

import org.jetbrains.annotations.NotNull;

/**
 * #api202: LanguageLevel.getMajorVersion and .getMinorVersion method added in 2020.3
 * .getVersion was removed in 2021.1
 */
public class LanguageLevelDelegate {
  private LanguageLevel delegate;

  public LanguageLevelDelegate(LanguageLevel delegate) {
    this.delegate = delegate;
  }

  /**
   * #api202: LanguageLevel.getMajorVersion and .getMinorVersion method added in 2020.3
   * .getVersion was removed in 2021.1
   */
  public int getVersion() {
    return (this.delegate.getMajorVersion() * 100) + this.delegate.getMinorVersion();
  }
}