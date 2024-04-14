package plc.project;

import java.io.PrintWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.math.BigDecimal;
public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(0);
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        newline(++indent);
        if (!ast.getGlobals().isEmpty()) {
            newline(indent);
        }
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        newline(0);
        for (Ast.Function function : ast.getFunctions()) {
            newline(indent);
            visit(function);
        }
        newline(0);
        print("}");
        return null;
    }


    @Override
    public Void visit(Ast.Global ast) {
        String type = Environment.getType(ast.getTypeName()).getJvmName();
        if (!ast.getMutable()) {
            print("final ");
        }
        if (ast.getTypeName().equals("Decimal")) {
            print(type + "[]");
        } else {
            print(type);
        }
        print(" " + ast.getName());

        ast.getValue().ifPresent(value -> {
            print(" = ");
            if (value instanceof Ast.Expression.PlcList) {
                print("{");
                Ast.Expression.PlcList list = (Ast.Expression.PlcList) value;
                for (int i = 0; i < list.getValues().size(); i++) {
                    visit(list.getValues().get(i));
                    if (i < list.getValues().size() - 1) {
                        print(", ");
                    }
                }
                print("}");
            } else {
                visit(value);
            }
        });
        print(";");
        return null;
    }


    @Override
    public Void visit(Ast.Function ast) {
        String returnType = ast.getReturnTypeName().map(Environment::getType).map(Environment.Type::getJvmName).orElse("void");
        print(returnType + " " + ast.getName() + "(");
        for (int i = 0; i < ast.getParameters().size(); i++) {
            if (i > 0) {
                print(", ");
            }
            print(Environment.getType(ast.getParameterTypeNames().get(i)).getJvmName() + " " + ast.getParameters().get(i));
        }
        print(") {");
        if (ast.getStatements().isEmpty()) {
            print(" }");
        } else {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));
                if (i != ast.getStatements().size() - 1) {
                    newline(indent);
                }
            }
            newline(--indent);
            print("}");
        }
        newline(0);
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String type = ast.getTypeName()
                .map(Environment::getType) // Convert the optional type name to Environment.Type if present
                .map(Environment.Type::getJvmName) // Convert Environment.Type to its JVM name
                .orElseGet(() -> { // If type name is not provided, infer from the expression if possible
                    return ast.getValue()
                            .map(value -> {
                                if (value instanceof Ast.Expression.Literal) {
                                    Object literal = ((Ast.Expression.Literal) value).getLiteral();
                                    if (literal instanceof BigInteger) {
                                        return "int"; // Adjust based on your environment settings and needs
                                    } else if (literal instanceof BigDecimal) {
                                        return "double"; // Adjust based on your environment settings and needs
                                    } else if (literal instanceof String) {
                                        return "String";
                                    } else {
                                        return "Object"; // Default case, can be adjusted as needed
                                    }
                                }
                                return "Object"; // Default if not a literal or other cases
                            })
                            .orElse("Object"); // Default if no value is present
                });

        print(type + " " + ast.getName());
        ast.getValue().ifPresent(value -> {
            print(" = ");
            visit(value);
        });
        print(";");
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");
        indent++;  // Increase indentation
        for (int i = 0; i < ast.getThenStatements().size(); i++) {
            newline(indent);  // Only add a newline before each statement
            visit(ast.getThenStatements().get(i));
        }
        indent--;  // Decrease indentation
        newline(indent);
        print("}");
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            indent++;  // Increase indentation
            for (int i = 0; i < ast.getElseStatements().size(); i++) {
                newline(indent);
                visit(ast.getElseStatements().get(i));
            }
            indent--;  // Decrease indentation
            newline(indent);
            print("}");
        }
        return null;
    }




    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (");
        visit(ast.getCondition());
        print(") {");
        newline(++indent);
        for (int i = 0; i < ast.getCases().size(); i++) {
            visit(ast.getCases().get(i));
        }
        --indent;
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if (ast.getValue().isPresent()) {
            print("case ", ast.getValue().get(), ":");
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));
                newline(indent);
            }
            print("break;");
            newline(--indent);
        }
        else {
            print("default:");
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");
        if (!ast.getStatements().isEmpty()) {
            for (int i = 0; i < ast.getStatements().size(); i++) {
                newline(++indent);
                visit(ast.getStatements().get(i));
            }
            newline(--indent);
            print("}");
        }
        else {
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() instanceof String) {
            print("\"", ast.getLiteral(), "\"");
        }
        else if (ast.getLiteral() instanceof Character) {
            print("'", ast.getLiteral(), "'");
        }
        else {
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        print(" ");
        print(ast.getOperator());
        print(" ");
        visit(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getVariable().getJvmName());  // Assuming getJvmName is always available
        ast.getOffset().ifPresent(offset -> {
            print("[");
            visit(offset);
            print("]");
        });
        return null;
    }


    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName());
        print("(");
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            if (i != ast.getArguments().size() - 1) {
                print(", ");
            }
        }
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");
        for (int i = 0; i < ast.getValues().size(); i++) {
            visit(ast.getValues().get(i));
            if (i != ast.getValues().size() - 1) {
                print(", ");
            }
        }
        print("}");
        return null;
    }

}