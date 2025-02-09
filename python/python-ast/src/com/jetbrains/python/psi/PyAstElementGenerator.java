package com.jetbrains.python.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.ast.PyAstExpressionStatement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@ApiStatus.Experimental
public class PyAstElementGenerator {
  public static PyAstElementGenerator getInstance(Project project) {
    return project.getService(PyAstElementGenerator.class);
  }

  private final Project myProject;

  protected PyAstElementGenerator(Project project) {
    myProject = project;
  }

  static final int[] FROM_ROOT = new int[]{0};

  @NotNull
  public <T> T createFromText(LanguageLevel langLevel, Class<T> aClass, final String text) {
    return createFromText(langLevel, aClass, text, FROM_ROOT);
  }

  @NotNull
  public <T> T createPhysicalFromText(LanguageLevel langLevel, Class<T> aClass, final String text) {
    return createFromText(langLevel, aClass, text, FROM_ROOT, true);
  }

  /**
   * Creates an arbitrary PSI element from text, by creating a bigger construction and then cutting the proper subelement.
   * Will produce all kinds of exceptions if the path or class would not match the PSI tree.
   *
   * @param langLevel the language level to use for parsing the text
   * @param aClass    class of the PSI element; may be an interface not descending from PsiElement, as long as target node can be cast to it
   * @param text      text to parse
   * @param path      a sequence of numbers, each telling which child to select at current tree level; 0 means first child, etc.
   * @return the newly created PSI element
   */
  @NotNull
  public <T> T createFromText(LanguageLevel langLevel, Class<T> aClass, final String text, final int[] path) {
    return createFromText(langLevel, aClass, text, path, false);
  }

  @NotNull
  private <T> T createFromText(LanguageLevel langLevel, Class<T> aClass, final String text, final int[] path, boolean physical) {
    PsiElement ret = createDummyFile(langLevel, text, physical);
    for (int skip : path) {
      if (ret != null) {
        ret = ret.getFirstChild();
        for (int i = 0; i < skip; i += 1) {
          if (ret != null) {
            ret = ret.getNextSibling();
          }
          else {
            ret = null;
            break;
          }
        }
      }
      else {
        break;
      }
    }
    if (ret == null) {
      throw new IllegalArgumentException("Can't find element matching path " + Arrays.toString(path) + " in text '" + text + "'");
    }
    if (aClass.isInstance(ret)) {
      //noinspection unchecked
      return (T)ret;
    }
    else {
      throw new IllegalArgumentException("Can't create an element of type " + aClass + " from text '" + text + "', got " + ret.getClass() + " instead");
    }
  }

  public PsiFile createDummyFile(LanguageLevel langLevel, String contents) {
    return createDummyFile(langLevel, contents, false);
  }

  /**
   * @return name used for {@link #createDummyFile(LanguageLevel, String)}
   */
  @NotNull
  public static String getDummyFileName() {
    return "dummy." + PythonFileType.INSTANCE.getDefaultExtension();
  }

  /**
   * TODO: Use {@link PsiFileFactory} instead?
   */
  private PsiFile createDummyFile(LanguageLevel langLevel, String contents, boolean physical) {
    final PsiFileFactory factory = PsiFileFactory.getInstance(myProject);
    final String name = getDummyFileName();
    final LightVirtualFile virtualFile = new LightVirtualFile(name, PythonFileType.INSTANCE, contents);
    specifyFileLanguageLevel(virtualFile, langLevel);
    final PsiFile psiFile = ((PsiFileFactoryImpl)factory).trySetupPsiForFile(virtualFile, PythonLanguage.getInstance(), physical, true);
    assert psiFile != null;
    return psiFile;
  }

  public PyAstExpressionStatement createDocstring(String content) {
    return createFromText(LanguageLevel.getDefault(),
                          PyAstExpressionStatement.class, content + "\n");
  }

  protected void specifyFileLanguageLevel(@NotNull VirtualFile virtualFile, @Nullable LanguageLevel langLevel) { }
}
