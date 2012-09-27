package lombok.javac.handlers;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import io.prombok.annotations.Packet;
import lombok.core.AST;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;

import java.util.regex.Pattern;

import static com.sun.tools.javac.tree.JCTree.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static org.apache.commons.lang3.StringUtils.*;

@MetaInfServices(JavacAnnotationHandler.class)
public class HandlePacket implements JavacAnnotationHandler<Packet> {

    @Override
    public boolean handle(AnnotationValues<Packet> annotation, JCTree.JCAnnotation ast, JavacNode annotationNode) {
        markAnnotationAsProcessed(annotationNode, Packet.class);
        JavacNode typeNode = annotationNode.up();
        return generatePacketMethods(typeNode);
    }

    public boolean generatePacketMethods(JavacNode typeNode) {
        boolean isClass = false;
        if (typeNode.get() instanceof JCTree.JCClassDecl) {
            long flags = ((JCTree.JCClassDecl) typeNode.get()).mods.flags;
            isClass = (flags & (Flags.INTERFACE | Flags.ANNOTATION | Flags.ENUM)) == 0;
        }
        if (!isClass) {
            return false;
        }

        ListBuffer<JavacNode> nodesForMarshalling = ListBuffer.lb();
        ListBuffer<JavacNode> nodesForUnmarshalling = ListBuffer.lb();
        for (JavacNode child : typeNode.down()) {
            if (child.getKind() != AST.Kind.FIELD) {
                continue;
            }
            JCVariableDecl fieldDecl = (JCVariableDecl) child.get();
            if ((fieldDecl.mods.flags & Flags.STATIC) != 0 ||
                    fieldDecl.name.toString().startsWith("$")) {
                // Skip static fields or that start with $
                continue;
            }
            List<JCAnnotation> fieldAnnotations = fieldDecl.mods.annotations;
            if (fieldAnnotations != null) {
                for (JCAnnotation annotation : fieldAnnotations) {
                    String name = annotation.annotationType.toString();
                    if (name.startsWith("In")) {
                        nodesForUnmarshalling.append(child);
                    }
                    if (name.startsWith("Out")) {
                        nodesForMarshalling.append(child);
                    }
                }
            }
        }
        if (!nodesForMarshalling.isEmpty() && methodExists("toByteBuf", typeNode) == MemberExistsResult.NOT_EXISTS) {
            generateMarshallingMethod(typeNode, nodesForMarshalling.toList());
        }
        if (!nodesForUnmarshalling.isEmpty() && methodExists("from", typeNode) == MemberExistsResult.NOT_EXISTS) {
            generateUnmarshallingMethod(typeNode, nodesForUnmarshalling.toList());
        }
        return true;
    }
    
    public JCStatement createInvokingStatement(TreeMaker maker, JavacNode typeNode, String variableNmae, String methodName, Name... args) {
        return maker.Exec(createInvokingMethod(maker, typeNode, variableNmae, methodName, args));
    }

    public JCStatement createInvokingStatement(TreeMaker maker, JavacNode typeNode, String variableNmae, String methodName, List<JCExpression> args) {
        return maker.Exec(createInvokingMethod(maker, typeNode, variableNmae, methodName, args));
    }

    public JCMethodInvocation createInvokingMethod(TreeMaker maker, JavacNode typeNode, String variableNmae, String methodName, Name... args) {
        ListBuffer<JCExpression> argsList = ListBuffer.lb();
        for (Name name : args) {
            argsList.append(maker.Ident(name));
        }
        return maker.Apply(List.<JCExpression>nil(), chainDots(maker, typeNode, variableNmae, methodName), argsList.toList());
    }

    public JCMethodInvocation createInvokingMethod(TreeMaker maker, JavacNode typeNode, String variableNmae, String methodName, List<JCExpression> args) {
        return maker.Apply(List.<JCExpression>nil(), chainDots(maker, typeNode, variableNmae, methodName), args);
    }
    
    public JCMethodInvocation createInvokingMethod(TreeMaker maker, JavacNode typeNode, List<JCExpression> args, String... variableAndMethodName) {
        return maker.Apply(List.<JCExpression>nil(), chainDots(maker, typeNode, variableAndMethodName), args);
    }
    
    public JCTry createDefaultExceptionBlock(TreeMaker maker, JavacNode typeNode, List<JCStatement> tryStatements, List<JCStatement> catchStatements, List<JCStatement> finallyStatements) {
        return maker.Try(
                maker.Block(0, tryStatements),
                List.<JCCatch>of(maker.Catch(
                        maker.VarDef(maker.Modifiers(Flags.FINAL), typeNode.toName("e"), maker.Ident(typeNode.toName("Exception")), null),
                        maker.Block(0, catchStatements))),
                finallyStatements != null ? maker.Block(0, finallyStatements) : null);
    }

    public void generateMarshallingMethod(JavacNode typeNode, List<JavacNode> fields) {
        TreeMaker maker = typeNode.getTreeMaker();

        JCModifiers mods = maker.Modifiers(Flags.PUBLIC);
        JCExpression returnType = chainDots(maker, typeNode, "io", "netty", "buffer", "ByteBuf");

        ListBuffer<JCStatement> statements = ListBuffer.lb();

        JCExpression byteBufType = chainDots(maker, typeNode, "io", "netty", "buffer", "ByteBuf");
        JCVariableDecl byteBufDecl = maker.VarDef(maker.Modifiers(Flags.FINAL), typeNode.toName("bbuf"), byteBufType,
                maker.Apply(List.<JCExpression>nil(), chainDots(maker, typeNode, "io", "netty", "buffer", "Unpooled", "buffer"), List.<JCExpression>nil()));
        JCExpression genericBufType = chainDots(maker, typeNode, "io", "prombok", "buffer", "GenericByteBuf");
        JCVariableDecl genericBufDecl = maker.VarDef(maker.Modifiers(Flags.FINAL), typeNode.toName("gbuf"), genericBufType,
                maker.NewClass(null, List.<JCExpression>nil(), genericBufType, List.<JCExpression>of(maker.Ident(typeNode.toName("bbuf"))), null));
        statements.append(byteBufDecl);
        statements.append(genericBufDecl);

        for (JavacNode field : fields) {
            JCVariableDecl fieldDecl = (JCVariableDecl) field.get();
            String type = fieldDecl.vartype.toString();

            JCStatement statement = null;
            if (startsWith(type, "List<")) {
                String typeArgument = substringBetween(type, "<", ">");
                if (typeArgument != null && !typeArgument.contains(",")) {
                    JCStatement sizeStatement = createInvokingStatement(maker, typeNode, "gbuf", "writeInt",
                            List.<JCExpression>of(createInvokingMethod(maker, typeNode, fieldDecl.name.toString(), "size")));
                    statements.append(sizeStatement);

                    statement = maker.ForeachLoop(
                            createVarDef(typeNode, 0, "item", maker.Ident(typeNode.toName(typeArgument)), null),
                            maker.Ident(fieldDecl.name),
                            createWriteStatement(typeNode, maker, field, typeArgument, typeNode.toName("item")));
                }
            } else if (startsWith(type, "byte[]")) {
                JCStatement sizeStatement = createInvokingStatement(maker, typeNode, "gbuf", "writeInt",
                        List.<JCExpression>of(chainDots(maker, typeNode, fieldDecl.name.toString(), "length")));
                statements.append(sizeStatement);

                statement = createInvokingStatement(maker, typeNode, "gbuf", "writeBytes", fieldDecl.name);
            } else {
                statement = createWriteStatement(typeNode, maker, field, type, fieldDecl.getName());
            }

            if (statement != null) {
                JCExpression ifAnnotation = findAnnotationArgs(field, "If", "value");
                if (ifAnnotation != null) {
                    String expr = (String) ((JCLiteral) ifAnnotation).getValue();
                    statement = maker.If(maker.TypeCast(chainDots(maker,typeNode, "java", "lang", "Boolean"),
                            maker.Apply(List.<JCExpression>nil(),
                                    chainDots(maker, typeNode, "org", "mvel2", "MVEL", "eval"),
                                    List.<JCExpression>of(maker.Literal(expr), maker.Ident(typeNode.toName("this"))))),
                            statement,null);
                }
                statements.append(statement);

                injectAnnotationNoArgs(field, "lombok", "Setter");
            }
        }

        JCReturn returnStatement = maker.Return(maker.Ident(typeNode.toName("gbuf")));
        statements.append(returnStatement);

        JCBlock body = maker.Block(0, statements.toList());

        JCMethodDecl methodDecl = maker.MethodDef(mods, typeNode.toName("toByteBuf"), returnType,
                List.<JCTypeParameter>nil(), List.<JCVariableDecl>nil(), List.<JCExpression>nil(), body, null);
        injectMethod(typeNode, methodDecl);
    }

    private JCStatement createWriteStatement(JavacNode typeNode, TreeMaker maker, JavacNode field, String type, Name name) {
        type = uncapitalize(type);
        JCStatement statement = null;
        JCExpression writer = findAnnotationArgs(field, "Out", "writer");
        if (writer != null) {
            statement = createInvokingStatement(maker, typeNode, "gbuf", "writeGeneric", List.<JCExpression>of(
                    maker.Ident(name), writer));
        } else if (endsWithAny(type, "int", "integer")) {
            statement = createInvokingStatement(maker, typeNode, "gbuf", "writeInt", name);
        } else if (endsWithAny(type, "long")) {
            statement = createInvokingStatement(maker, typeNode, "gbuf", "writeLong", name);
        } else if (endsWithAny(type, "byte")) {
            statement = createInvokingStatement(maker, typeNode, "gbuf", "writeByte", name);
        } else if (endsWithAny(type, "boolean")) {
            statement = createInvokingStatement(maker, typeNode, "gbuf", "writeBoolean", name);
        } else if (endsWithAny(type, "short")) {
            statement = createInvokingStatement(maker, typeNode, "gbuf", "writeShort", name);
        } else if (endsWithAny(type, "char", "character")) {
            statement = createInvokingStatement(maker, typeNode, "gbuf", "writeChar", name);
        } else if (endsWithAny(type, "float")) {
            statement = createInvokingStatement(maker, typeNode, "gbuf", "writeFloat", name);
        } else if (endsWithAny(type, "double")) {
            statement = createInvokingStatement(maker, typeNode, "gbuf", "writeDouble", name);
        } else if (endsWithAny(type, "string")) {
            statement = createInvokingStatement(maker, typeNode, "gbuf", "writeGeneric", List.<JCExpression>of(
                    maker.Ident(name),
                    chainDots(maker, typeNode, "io", "prombok", "codec", "DefaultStringByteCodec", "class")
            ));
        } else {
            statement = createInvokingStatement(maker, typeNode, "gbuf", "writeBytes", List.<JCExpression>of(
                    createInvokingMethod(maker, typeNode, name.toString(), "toByteBuf")
            ));
        }
        return statement;
    }

    public void generateUnmarshallingMethod(JavacNode typeNode, List<JavacNode> fields) {
        TreeMaker maker = typeNode.getTreeMaker();
        JCModifiers mods = maker.Modifiers(Flags.PUBLIC | Flags.STATIC);
        JCExpression returnType = maker.Ident(((JCClassDecl) typeNode.get()).name);

        ListBuffer<JCStatement> statements = ListBuffer.lb();

        JCVariableDecl objectDecl = maker.VarDef(maker.Modifiers(Flags.FINAL), typeNode.toName("o"), returnType,
                maker.NewClass(null, List.<JCExpression>nil(), returnType, List.<JCExpression>nil(), null));
        statements.append(objectDecl);

        ListBuffer<JCStatement> tryStatements = ListBuffer.lb();

        JCExpression bufferType = chainDots(maker, typeNode, "io", "prombok", "buffer", "GenericByteBuf");
        JCVariableDecl bufferDecl = maker.VarDef(maker.Modifiers(Flags.FINAL), typeNode.toName("src"),
                bufferType,
                maker.NewClass(null, List.<JCExpression>nil(), bufferType, List.<JCExpression>of(maker.Ident(typeNode.toName("buf"))), null));
        tryStatements.append(bufferDecl);

        for (JavacNode field : fields) {
            JCVariableDecl fieldDecl = (JCVariableDecl) field.get();
            String type = fieldDecl.vartype.toString();
            String fieldName = fieldDecl.name.toString();
            JCStatement statement = null;

            if (startsWith(type, "List<")) {
                String typeArgument = substringBetween(type, "<", ">");
                if (typeArgument != null && !typeArgument.contains(",")) {
                    JCVariableDecl countStatement = createVarDef(typeNode, Flags.FINAL, fieldName + "Count",
                            maker.TypeIdent(TypeTags.INT), createReadExpression(typeNode, maker, field, "int"));
                    tryStatements.append(countStatement);

                    JCExpressionStatement listConstructStatement = maker.Exec(maker.Assign(
                            chainDots(maker, typeNode, "o", fieldDecl.name.toString()),
                            createNewClass(typeNode, chainDots(maker, typeNode, "java", "util", "ArrayList"),
                                    maker.Ident(countStatement.getName()))));
                    tryStatements.append(listConstructStatement);

                    statement = createInclForLoop(typeNode, maker.Ident(countStatement.getName()),
                            maker.Exec(createInvokingMethod(maker, typeNode,
                                    List.<JCExpression>of(createReadExpression(typeNode, maker, field, typeArgument)),
                                    "o", fieldDecl.name.toString(), "add")));
                }
            } else if (startsWith(type, "byte[]")) {
                JCVariableDecl sizeStatement = createVarDef(typeNode, Flags.FINAL, fieldName + "Size",
                        maker.TypeIdent(TypeTags.INT), createReadExpression(typeNode, maker, field, "int"));
                tryStatements.append(sizeStatement);

                statement = maker.Exec(maker.Assign(
                        chainDots(maker, typeNode, "o", fieldDecl.name.toString()),
                        maker.Apply(List.<JCExpression>nil(),
                                maker.Select(createInvokingMethod(maker, typeNode, "src", "readBytes", sizeStatement.name),
                                        typeNode.toName("array")),
                                List.<JCExpression>nil())));
            } else {
                statement = maker.Exec(maker.Assign(chainDots(maker, typeNode, "o", fieldDecl.name.toString()),
                        createReadExpression(typeNode, maker, field, type)));
            }

            if(statement != null) {
                JCExpression ifAnnotation = findAnnotationArgs(field, "If", "value");
                if (ifAnnotation != null) {
                    String expr = (String) ((JCLiteral) ifAnnotation).getValue();
                    statement = maker.If(maker.TypeCast(chainDots(maker,typeNode, "java", "lang", "Boolean"),
                            maker.Apply(List.<JCExpression>nil(),
                                    chainDots(maker, typeNode, "org", "mvel2", "MVEL", "eval"),
                                    List.<JCExpression>of(maker.Literal(expr), maker.Ident(typeNode.toName("o"))))),
                            statement,null);
                }
                tryStatements.append(statement);

                injectAnnotationNoArgs(field, "lombok", "Getter");
            }
        }

        JCTry tryCatchBlock = createDefaultExceptionBlock(maker, typeNode, tryStatements.toList(),
                List.<JCStatement>of(createInvokingStatement(maker, typeNode, "e", "printStackTrace")),
                null);
        statements.append(tryCatchBlock);

        JCReturn returnStatement = maker.Return(maker.Ident(typeNode.toName("o")));
        statements.append(returnStatement);

        JCBlock body = maker.Block(0, statements.toList());

        JCExpression byteBufferType = chainDots(maker, typeNode, "io", "netty", "buffer", "ByteBuf");
        JCVariableDecl param = maker.VarDef(maker.Modifiers(Flags.FINAL), typeNode.toName("buf"),
                byteBufferType, null);
        JCMethodDecl methodDecl = maker.MethodDef(mods, typeNode.toName("from"), returnType,
                List.<JCTypeParameter>nil(), List.<JCVariableDecl>of(param), List.<JCExpression>nil(), body, null);

        injectMethod(typeNode, methodDecl);
    }

    private JCExpression createReadExpression(JavacNode typeNode, TreeMaker maker, JavacNode field, String type) {
        type = uncapitalize(type);
        JCExpression methodInvocation = null;
        JCExpression reader = findAnnotationArgs(field, "In", "reader");
        if (reader != null) {
            methodInvocation = createInvokingMethod(maker, typeNode, "src", "readGeneric", List.<JCExpression>of(
                    reader));
        } else if (endsWithAny(type, "int", "integer")) {
            methodInvocation = createInvokingMethod(maker, typeNode, "src", "readInt");
        } else if (endsWithAny(type, "long")) {
            methodInvocation = createInvokingMethod(maker, typeNode, "src", "readLong");
        } else if (endsWithAny(type, "byte")) {
            methodInvocation = createInvokingMethod(maker, typeNode, "src", "readByte");
        } else if (endsWithAny(type, "boolean")) {
            methodInvocation = maker.Binary(EQ, createInvokingMethod(maker, typeNode, "b", "readBoolean"), maker.Literal(1));
        } else if (endsWithAny(type, "short")) {
            methodInvocation = createInvokingMethod(maker, typeNode, "src", "readShort");
        } else if (endsWithAny(type, "char", "character")) {
            methodInvocation = createInvokingMethod(maker, typeNode, "src", "readChar");
        } else if (endsWithAny(type, "float")) {
            methodInvocation = createInvokingMethod(maker, typeNode, "src", "readFloat");
        } else if (endsWithAny(type, "double")) {
            methodInvocation = createInvokingMethod(maker, typeNode, "src", "readDouble");
        } else if (endsWithAny(type, "string")) {
            methodInvocation = createInvokingMethod(maker, typeNode, "src", "readGeneric", List.<JCExpression>of(
                    chainDots(maker, typeNode, "io", "prombok", "codec", "DefaultStringByteCodec", "class")));
        } else {
            methodInvocation = createInvokingMethod(maker, typeNode, capitalize(type), "from", typeNode.toName("src"));
        }
        return methodInvocation;
    }

    public JCExpression findAnnotationArgs(JavacNode fieldNode, String annotationName, String argName) {
        for (JCAnnotation annotation : findAnnotations(fieldNode, Pattern.compile(annotationName))) {
            for (JCExpression arg : annotation.getArguments()) {
                if (arg instanceof JCAssign) {
                    if (StringUtils.equals(((JCAssign) arg).lhs.toString(), argName)) {
                        return ((JCAssign) arg).rhs;
                    }
                }
            }
        }
        return null;
    }

    public JCVariableDecl createVarDef(JavacNode typeNode, long modsFlag, String name, JCExpression vartype, JCExpression init) {
        TreeMaker maker = typeNode.getTreeMaker();
        return maker.VarDef(maker.Modifiers(modsFlag), typeNode.toName(name), vartype, init);
    }

    public JCNewClass createNewClass(JavacNode typeNode, JCExpression clazz, JCExpression... args) {
        TreeMaker maker = typeNode.getTreeMaker();
        ListBuffer<JCExpression> lb = ListBuffer.lb();
        for (JCExpression arg : args) {
            lb.append(arg);
        }
        return maker.NewClass(null, List.<JCExpression>nil(), clazz, lb.toList(), null);
    }

    public JCForLoop createInclForLoop(JavacNode typeNode, JCExpression conditionLt, JCStatement body) {
        TreeMaker maker = typeNode.getTreeMaker();
        return maker.ForLoop(List.<JCStatement>of(createVarDef(typeNode, 0, "i", maker.TypeIdent(TypeTags.INT), maker.Literal(0))),
                maker.Binary(LT, maker.Ident(typeNode.toName("i")), conditionLt),
                List.<JCExpressionStatement>of(maker.Exec(maker.Unary(POSTINC, maker.Ident(typeNode.toName("i"))))),
                body);
    }
    
    public void injectAnnotationNoArgs(JavacNode node, String... name) {
        TreeMaker maker = node.getTreeMaker();
        JCAnnotation annotation = maker.Annotation(chainDots(maker, node, name), List.<JCExpression>nil());
        if (node.get() instanceof JCClassDecl) {
            ((JCClassDecl) node.get()).mods.annotations = ((JCClassDecl) node.get()).mods.annotations.append(annotation);
        } else if (node.get() instanceof JCVariableDecl) {
            ((JCVariableDecl) node.get()).mods.annotations = ((JCVariableDecl) node.get()).mods.annotations.append(annotation);
        }
        node.add(annotation, AST.Kind.ANNOTATION).recursiveSetHandled();
    }

}
