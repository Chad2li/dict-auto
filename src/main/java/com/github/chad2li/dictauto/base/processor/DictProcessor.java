package com.github.chad2li.dictauto.base.processor;

import com.github.chad2li.dictauto.base.annotation.DictId;
import com.github.chad2li.dictauto.base.dto.DictItemDto;
import com.github.chad2li.dictauto.base.util.Log;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.Set;
import java.util.StringJoiner;

/**
 * @author chad
 * @date 2022/5/13 22:52
 * @since 1 create by chad
 */
@SupportedAnnotationTypes("com.github.chad2li.dictauto.base.annotation.DictId")
public class DictProcessor extends AbstractProcessor {

    private Messager messager;

    private Elements elementUtils;
    private Filer filer;
    private JavacTrees javacTrees;
    private TreeMaker treeMaker;
    private Names names;

    public static final String TRACKER_CLASS = DictItemDto.class.getSimpleName();

    public static final String TRACKER_PACKAGE = DictItemDto.class.getPackage().getName();

    public DictProcessor() {
        Log.write("constructor");
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.messager = env.getMessager();
        this.elementUtils = processingEnv.getElementUtils();
        this.filer = processingEnv.getFiler();

        this.javacTrees = JavacTrees.instance(env);
        Context content = ((JavacProcessingEnvironment) env).getContext();
        this.treeMaker = TreeMaker.instance(content);
        this.names = Names.instance(content);
        Log.write("init");
    }

    /**
     * 至少为8
     *
     * @return 版本
     * @date 2022/5/13 23:21
     * @author chad
     * @since 1 by chad at 2022/5/13
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        SourceVersion latest = SourceVersion.latest();
        if (latest.compareTo(SourceVersion.RELEASE_8) > 0) {
            return latest;
        }
        return SourceVersion.RELEASE_8;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Log.write("process start");
        boolean result = false;
        try {
            result = tryProcess4(annotations, roundEnv);
        } catch (Throwable t) {
            Log.write(t);
        }
        Log.write("process end");
        return result;
    }

    /**
     *
     * @param annotations
     * @param env
     * @return
     */
    private boolean tryProcess4(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Set<? extends Element> dicts = env.getElementsAnnotatedWith(DictId.class);
        Log.write("process size: " + dicts.size());
        dicts.forEach(d -> {
            String varName = d.getSimpleName().toString();
            String varPrefix = varName.substring(0, varName.indexOf("DictId"));
            JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) this.javacTrees.getTree(d);
            TypeElement userDemo = (TypeElement) d.getEnclosingElement();
            Log.write("\tprocess item: " + userDemo.getSimpleName().toString());
            JCTree tree = this.javacTrees.getTree(userDemo);
            tree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
//                    List<JCTree.JCVariableDecl> dictDecl = new ArrayList<>();
                    StringJoiner sj = new StringJoiner("\n");

                    addImportInfo(userDemo);

                    // todo 处理 list 类型
                    JCTree.JCVariableDecl dictDecl = DictProcessor.this.treeMaker.VarDef(
                            DictProcessor.this.treeMaker.Modifiers(Flags.PRIVATE)
                            , DictProcessor.this.names.fromString(varPrefix + "DictItem")
                            , DictProcessor.this.treeMaker.Ident(DictProcessor.this.names.fromString(TRACKER_CLASS))
                            , null
                    );

                    sj.add(varPrefix + "DictItem");
                    jcClassDecl.defs = jcClassDecl.defs.append(dictDecl);

                    Log.write(sj.toString());

                    super.visitClassDef(jcClassDecl);
                }
            });
        });
        return true;
    }

    /**
     * 给{@code class}文件增加 {@code import} 语句
     *
     * @param element class
     * @date 2022/5/19 12:19
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    private void addImportInfo(Element element) {
        TreePath treePath = javacTrees.getPath(element);
        Tree leaf = treePath.getLeaf();
        if (treePath.getCompilationUnit() instanceof JCTree.JCCompilationUnit && leaf instanceof JCTree) {
            JCTree.JCCompilationUnit jccu = (JCTree.JCCompilationUnit) treePath.getCompilationUnit();

            for (JCTree jcTree : jccu.getImports()) {
                if (jcTree != null && jcTree instanceof JCTree.JCImport) {
                    JCTree.JCImport jcImport = (JCTree.JCImport) jcTree;
                    if (jcImport.qualid != null && jcImport.qualid instanceof JCTree.JCFieldAccess) {
                        JCTree.JCFieldAccess jcFieldAccess = (JCTree.JCFieldAccess) jcImport.qualid;
                        try {
                            if (TRACKER_PACKAGE.equals(jcFieldAccess.selected.toString()) && TRACKER_CLASS.equals(jcFieldAccess.name.toString())) {
                                return;
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            java.util.List<JCTree> trees = new ArrayList<>();
            trees.addAll(jccu.defs);
            JCTree.JCIdent ident = treeMaker.Ident(names.fromString(TRACKER_PACKAGE));
            JCTree.JCImport jcImport = treeMaker.Import(treeMaker.Select(
                    ident, names.fromString(TRACKER_CLASS)), false);
            if (!trees.contains(jcImport)) {
                trees.add(0, jcImport);
            }
            jccu.defs = List.from(trees);
        }
    }
}
