package io.github.chad2li.dictauto.base.processor;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;
import io.github.chad2li.dictauto.base.annotation.DictId;
import io.github.chad2li.dictauto.base.cst.DictCst;
import io.github.chad2li.dictauto.base.dto.DictItemDto;
import io.github.chad2li.dictauto.base.util.Log;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.Set;

/**
 * @author chad
 * @date 2022/5/13 22:52
 * @since 1 create by chad
 */
@SupportedAnnotationTypes("io.github.chad2li.dictauto.base.annotation.DictId")
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

        Log.init(env.getMessager());
        Log.write("init");
    }

    /**
     * ?????????8
     *
     * @return ??????
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
        try {
            tryProcess4(annotations, roundEnv);
        } catch (Throwable t) {
            Log.write(t);
        }
        Log.write("process end");
        // false???????????? processor ?????????????????????
        return false;
    }

    /**
     * @param annotations
     * @param env
     * @return
     */
    private void tryProcess4(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Set<? extends Element> dicts = env.getElementsAnnotatedWith(DictId.class);
        Log.write("process size: " + dicts.size());
        dictFor:
        for (Element dictElement : dicts) {
            String varName = dictElement.getSimpleName().toString();
            String varPrefix = varName.substring(0, varName.indexOf(DictCst.FIELD_DICT_ID_SUFFIX));
            TypeElement clsElt = (TypeElement) dictElement.getEnclosingElement();
            String clsName = clsElt.getSimpleName().toString();
            Log.write("\tprocess item: " + clsName);
            JCTree tree = this.javacTrees.getTree(clsElt);

            // ????????????item???????????????
            String dictItemFieldName = varPrefix + DictCst.FIELD_DICT_ITEM_SUFFIX;
            java.util.List<VariableElement> fields = ElementFilter.fieldsIn(clsElt.getEnclosedElements());
            if (CollectionUtil.isNotEmpty(fields)) {
                // ????????????????????????????????????
                for (VariableElement field : fields) {
                    String fieldName = field.getSimpleName().toString();
                    if (dictItemFieldName.equalsIgnoreCase(fieldName)) {
                        Log.write("\t\texists " + clsName + "." + dictItemFieldName);
                        continue dictFor;
                    }
                }
            }

            String methodName = dictItemFieldName.substring(0, 1).toUpperCase() + dictItemFieldName.substring(1);
            String getter = "get" + methodName;
            String setter = "set" + methodName;
            Log.write("\t\tadd    " + clsName + "." + dictItemFieldName);
            tree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    // import
                    addImportInfo(clsElt);

                    // todo ?????? list ??????
                    JCTree.JCVariableDecl dictDecl = DictProcessor.this.treeMaker.VarDef(
                            DictProcessor.this.treeMaker.Modifiers(Flags.PRIVATE)
                            , DictProcessor.this.names.fromString(dictItemFieldName)
                            , DictProcessor.this.treeMaker.Ident(DictProcessor.this.names.fromString(TRACKER_CLASS))
                            , null
                    );

                    JCTree.JCMethodDecl setterMethod = setter(dictDecl, setter);
                    JCTree.JCMethodDecl getterMethod = getter(dictDecl, getter);

                    jcClassDecl.defs = jcClassDecl.defs
                            .append(dictDecl)
                            .append(setterMethod)
                            .append(getterMethod)
                    ;

                    Log.write("\t\tadd    " + clsName + "." + dictItemFieldName + " succ");
                    super.visitClassDef(jcClassDecl);
                }
            });
        }
    }

    /**
     * ?????? dictDecl ????????? setter??????
     *
     * @param dictDecl   ??????
     * @param setterName ?????????setter?????????
     * @return dictDecl?????????setter??????
     * @date 2022/6/23 22:51
     * @author chad
     * @since 1 by chad at 2022/6/23
     */
    private JCTree.JCMethodDecl setter(JCTree.JCVariableDecl dictDecl, String setterName) {
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
        //??????????????? this.name = name;
        statements.append(treeMaker.Exec(treeMaker.Assign(treeMaker.Select(treeMaker.Ident(names.fromString("this")),
                dictDecl.name), treeMaker.Ident(dictDecl.name))));
        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
        JCTree.JCVariableDecl paramVal = treeMaker.VarDef(
                treeMaker.Modifiers(Flags.PARAMETER), //????????????
                dictDecl.name,
                dictDecl.vartype,
                //???????????????
                null
        );
        //????????????
        return treeMaker.MethodDef(
                treeMaker.Modifiers(Flags.PUBLIC),
                DictProcessor.this.names.fromString(setterName),
                //????????????
                treeMaker.TypeIdent(TypeTag.VOID),
                //??????????????????
                List.nil(),
                //????????????
                List.of(paramVal),
                //????????????
                List.nil(),
                //?????????
                body,
                //????????????????????????interface????????????default???
                null
        );
    }

    /**
     * ?????? dictDecl ??? getter ??????
     *
     * @param dictDecl   ??????
     * @param getterName ??????????????????
     * @return getter ??????
     * @date 2022/6/23 22:44
     * @author chad
     * @since 1 by chad at 2022/6/23
     */
    private JCTree.JCMethodDecl getter(JCTree.JCVariableDecl dictDecl, String getterName) {
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
        //??????????????? return this.?????????
        statements.append(treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), dictDecl.getName())));
        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
        //????????????
        return treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC), DictProcessor.this.names.fromString(getterName)
                , dictDecl.vartype, List.nil(), List.nil(), List.nil(), body, null);
    }

    /**
     * ??? class ???????????? import ??????
     *
     * @param element class
     * @date 2022/5/19 12:19
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    private void addImportInfo(Element element) {
        TreePath treePath = javacTrees.getPath(element);
        Tree leaf = treePath.getLeaf();
        if (!(treePath.getCompilationUnit() instanceof JCTree.JCCompilationUnit) || !(leaf instanceof JCTree)) {
            return;
        }
        JCTree.JCCompilationUnit jccu = (JCTree.JCCompilationUnit) treePath.getCompilationUnit();

        for (JCTree jcTree : jccu.getImports()) {
            if (null == jcTree || !(jcTree instanceof JCTree.JCImport)) {
                continue;
            }
            JCTree.JCImport jcImport = (JCTree.JCImport) jcTree;
            if (null == jcImport.qualid || !(jcImport.qualid instanceof JCTree.JCFieldAccess)) {
                continue;
            }
            JCTree.JCFieldAccess jcFieldAccess = (JCTree.JCFieldAccess) jcImport.qualid;
            try {
                if (TRACKER_PACKAGE.equals(jcFieldAccess.selected.toString()) && TRACKER_CLASS.equals(jcFieldAccess.name.toString())) {
                    // ???????????? import??????????????????
                    return;
                }
            } catch (NullPointerException e) {
                Log.write(ExceptionUtil.stacktraceToString(e));
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
