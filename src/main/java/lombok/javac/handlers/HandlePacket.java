package lombok.javac.handlers;

import com.sun.tools.javac.code.Flags;
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
import org.kohsuke.MetaInfServices;

import java.util.regex.Pattern;

import static com.sun.tools.javac.tree.JCTree.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static org.apache.commons.lang3.StringUtils.endsWithAny;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

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

        ListBuffer<JCStatement> tryStatements = ListBuffer.lb();
        for (JavacNode field : fields) {
            JCVariableDecl fieldDecl = (JCVariableDecl) field.get();
            String type = uncapitalize(fieldDecl.vartype.toString());
            JCStatement statement = null;

            if (endsWithAny(type, "int", "integer")) {
                statement = createInvokingStatement(maker, typeNode, "gbuf", "writeInt", fieldDecl.name);
            } else if (endsWithAny(type, "long")) {
                statement = createInvokingStatement(maker, typeNode, "gbuf", "writeLong", fieldDecl.name);
            } else if (endsWithAny(type, "byte")) {
                statement = createInvokingStatement(maker, typeNode, "gbuf", "writeByte", fieldDecl.name);
            } else if (endsWithAny(type, "boolean")) {
                statement = createInvokingStatement(maker, typeNode, "gbuf", "writeBoolean", fieldDecl.name);
            } else if (endsWithAny(type, "short")) {
                statement = createInvokingStatement(maker, typeNode, "gbuf", "writeShort", fieldDecl.name);
            } else if (endsWithAny(type, "char", "character")) {
                statement = createInvokingStatement(maker, typeNode, "gbuf", "writeChar", fieldDecl.name);
            } else if (endsWithAny(type, "float")) {
                statement = createInvokingStatement(maker, typeNode, "gbuf", "writeFloat", fieldDecl.name);
            } else if (endsWithAny(type, "double")) {
                statement = createInvokingStatement(maker, typeNode, "gbuf", "writeDouble", fieldDecl.name);
            } else if (endsWithAny(type, "string")) {
                statement = createInvokingStatement(maker, typeNode, "gbuf", "writeGeneric", List.<JCExpression>of(
                        maker.Ident(fieldDecl.name),
                        chainDots(maker, typeNode, "io", "prombok", "codec", "DefaultStringByteCodec", "class")
                ));
            } else {

            }
            if (statement != null) {
                statements.append(statement);
            }
        }

        JCReturn returnStatement = maker.Return(maker.Ident(typeNode.toName("gbuf")));
        statements.append(returnStatement);

        JCBlock body = maker.Block(0, statements.toList());

        JCMethodDecl methodDecl = maker.MethodDef(mods, typeNode.toName("toByteBuf"), returnType,
                List.<JCTypeParameter>nil(), List.<JCVariableDecl>nil(), List.<JCExpression>nil(), body, null);
        injectMethod(typeNode, methodDecl);
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
            String type = uncapitalize(fieldDecl.vartype.toString());
            JCStatement statement = null;
            if (endsWithAny(type, "int", "integer")) {
                statement = maker.Exec(maker.Assign(chainDots(maker, typeNode, "o", fieldDecl.name.toString()),
                        createInvokingMethod(maker, typeNode, "src", "readInt")));
            } else if (endsWithAny(type, "long")) {
                statement = maker.Exec(maker.Assign(chainDots(maker, typeNode, "o", fieldDecl.name.toString()),
                        createInvokingMethod(maker, typeNode, "src", "readLong")));
            } else if (endsWithAny(type, "byte")) {
                statement = maker.Exec(maker.Assign(chainDots(maker, typeNode, "o", fieldDecl.name.toString()),
                        createInvokingMethod(maker, typeNode, "src", "readByte")));
            } else if (endsWithAny(type, "boolean")) {
                statement = maker.Exec(maker.Assign(chainDots(maker, typeNode, "o", fieldDecl.name.toString()),
                        maker.Binary(EQ, createInvokingMethod(maker, typeNode, "b", "readBoolean"), maker.Literal(1))));
            } else if (endsWithAny(type, "short")) {
                statement = maker.Exec(maker.Assign(chainDots(maker, typeNode, "o", fieldDecl.name.toString()),
                        createInvokingMethod(maker, typeNode, "src", "readShort")));
            } else if (endsWithAny(type, "char", "character")) {
                statement = maker.Exec(maker.Assign(chainDots(maker, typeNode, "o", fieldDecl.name.toString()),
                        createInvokingMethod(maker, typeNode, "src", "readChar")));
            } else if (endsWithAny(type, "float")) {
                statement = maker.Exec(maker.Assign(chainDots(maker, typeNode, "o", fieldDecl.name.toString()),
                        createInvokingMethod(maker, typeNode, "src", "readFloat")));
            } else if (endsWithAny(type, "double")) {
                statement = maker.Exec(maker.Assign(chainDots(maker, typeNode, "o", fieldDecl.name.toString()),
                        createInvokingMethod(maker, typeNode, "src", "readDouble")));
            } else if (endsWithAny(type, "string")) {
                statement = maker.Exec(maker.Assign(chainDots(maker, typeNode, "o", fieldDecl.name.toString()),
                        createInvokingMethod(maker, typeNode, "src", "readGeneric", List.<JCExpression>of(
                        chainDots(maker, typeNode, "io", "prombok", "codec", "DefaultStringByteCodec", "class")))));
            } else {

            }
            if(statement != null) {
                tryStatements.append(statement);
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

}
