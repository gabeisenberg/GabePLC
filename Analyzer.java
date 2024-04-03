package plc.project;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {
    public Scope scope;
    private Ast.Function function;
    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println",
                Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args ->
                        Environment.NIL);
    }
    public Scope getScope() {
        return scope;
    }
    @Override
    public Void visit(Ast.Source ast) { //likley need to fix further
        boolean hasMain = false;
        boolean returnTypeIsInteger = false;
        for (Ast.Function function : ast.getFunctions()) {
            // Check if there's a 'main' function with arity 0
            if ("main".equals(function.getName()) &&
                    function.getParameters().isEmpty()) {
                System.out.println("has a main with correct airty");
                hasMain = true;
                // Analyze the return type of the 'main' function
                for (Ast.Statement statement : function.getStatements()) {
                    if (statement instanceof Ast.Statement.Return) {
                        Ast.Expression expression = ((Ast.Statement.Return)
                                statement).getValue();

                        if (expression instanceof Ast.Expression.Literal) {
                            Object value = ((Ast.Expression.Literal)
                                    expression).getLiteral();
                            if (value instanceof Integer) {
                                System.out.println("reached value is instanceof BigInteger");
                                        returnTypeIsInteger = true;
                                break;
                            }
                        }

                    }
                }
                if (!returnTypeIsInteger) {
                    System.out.println("return type is not a integer");
                    throw new RuntimeException("The 'main' function must return an Integer type.");
                }
                break; // 'main' function found and analyzed, no need to continue
            }
        }
        if (!hasMain) {
            System.out.println("no main with correct airty");
            throw new RuntimeException("No 'main' function with arity 0 found.");
        }
        return null; // If all checks pass, return null
    }
    @Override
    public Void visit(Ast.Global ast) { //global done

        ast.getValue().ifPresent(this::visit);
        // Fetch the type of the global variable from the environment.
        Environment.Type globalType = Environment.getType(ast.getTypeName());

        if (ast.getValue().isPresent()) {
            Ast.Expression valueExpression = ast.getValue().get();
            // Note: Implementation of isAssignable method is required here.
            if (!isAssignable(valueExpression.getType(), globalType)) {
                throw new RuntimeException("The value of global '" + ast.getName()
                        + "' is not assignable to type '" + ast.getTypeName() + "'.");
            }
        }
        Environment.Variable globalVariable = scope.defineVariable(ast.getName(),
                ast.getName(), globalType, true, Environment.NIL);
        // Set the variable in the Ast.Global node for future reference.
        ast.setVariable(globalVariable);
        return null;
    }
    private boolean isAssignable(Environment.Type valueType, Environment.Type
            targetType) {
        return valueType.equals(targetType) ||
                valueType.equals(Environment.Type.ANY);
    }
    @Override
    public Void visit(Ast.Function ast) {
        //define function in scope
        Environment.Type t = null;
        //get return type
        Ast.Statement.Return r = null;
        Environment.Type returnType = null;
        for (Ast.Statement s : ast.getStatements()) {
            if (s instanceof Ast.Statement.Return) {
                r = (Ast.Statement.Return)s;
                visit(r);
                returnType = r.getValue().getType();
            }
        }
        //get variables and types
        List<Environment.Variable> params = new ArrayList<Environment.Variable>();
        List<Environment.Type> paramTypes = new ArrayList<Environment.Type>();
        for (int i = 0; i < ast.getParameters().size(); i++) {
            Environment.Variable v = scope.lookupVariable(ast.getParameters().get(i));
            params.add(v);
            paramTypes.add(v.getType());
        }
        if (returnType == null) {
            returnType = Environment.Type.NIL;
        }
        Environment.Function f = scope.defineFunction(ast.getName(), ast.getName(), paramTypes, string2type(ast.getReturnTypeName().get()), args -> Environment.NIL);
        ast.setFunction(f);
        Scope prev = scope;
        scope = new Scope(scope);
        //define variables
        for (int i = 0; i < params.size(); i++) {
            Environment.Variable p = params.get(i);
            Environment.Type pType = paramTypes.get(i);
            scope.defineVariable(p.getName(), p.getJvmName(), pType, true, Environment.NIL);
        }
        //visit each statement
        for (Ast.Statement s : ast.getStatements()) {
            visit(s);
        }
        scope = prev;
        return null;
    }

    public Environment.Type string2type(String s) {
        if (s.equals("Integer")) {
            return Environment.Type.INTEGER;
        }
        else if (s.equals("String")) {
            return Environment.Type.STRING;
        }
        else if (s.equals("Decimal")) {
            return Environment.Type.DECIMAL;
        }
        else if (s.equals("Character")) {
            return Environment.Type.CHARACTER;
        }
        else if (s.equals("Boolean")) {
            return Environment.Type.BOOLEAN;
        }
        return Environment.Type.NIL;
    }
    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (!(ast.getExpression() instanceof  Ast.Expression.Function)) {
            throw new RuntimeException("not a function for expression!");
        }
        visit(ast.getExpression());
        return null;
    }
    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException(); // TODO
    }
    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); // TODO
    }
    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); // TODO
    }
    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); // TODO
    }
    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); // TODO
    }
    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition()); //vising to analyze
        requireAssignable(Environment.Type.BOOLEAN,
                ast.getCondition().getType()); //after visitng, check if condition is while
        try{
            scope = new Scope(scope);
            for (Ast.Statement stmt: ast.getStatements()){
                visit(stmt);
            }
        }finally{
            scope = scope.getParent();
        }
        return null;
        //throw new UnsupportedOperationException(); // TODO
    }
    @Override
    public Void visit(Ast.Statement.Return ast) {
        Ast.Expression exp = ast.getValue();
        visit(exp);
        return null;
    }
    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (ast.getLiteral() instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if (ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
        }
        else if (ast.getLiteral() instanceof BigInteger) {
            BigInteger tempVal = (BigInteger)ast.getLiteral();
            if (tempVal.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ||
                    tempVal.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                throw new RuntimeException("Out of range!");
            }
            ast.setType(Environment.Type.INTEGER);
        }
        else if (ast.getLiteral() instanceof BigDecimal) {
            BigDecimal tempVal = (BigDecimal)ast.getLiteral();
            double doubleVal = tempVal.doubleValue();
            BigDecimal bigVal = BigDecimal.valueOf(doubleVal);
            if (!tempVal.equals(bigVal)) {
                throw new RuntimeException("Out of range!");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        else {
            ast.setType(Environment.Type.NIL);
        }
        return null;
    }
    @Override
    public Void visit(Ast.Expression.Group ast) {
        Ast.Expression exp = ast.getExpression();
        if (!(exp instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("Non binary expression!");
        }
        visit(ast.getExpression());
        Environment.Type t = ast.getExpression().getType();
        ast.setType(t);
        return null;
    }
    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String op = ast.getOperator();
        if (op.equals("&&") || op.equals("||")) {
            Ast.Expression.Literal lhs = (Ast.Expression.Literal)ast.getLeft();
            Ast.Expression.Literal rhs = (Ast.Expression.Literal)ast.getRight();
            if (!(lhs.getLiteral() instanceof Boolean && rhs.getLiteral()
                    instanceof Boolean)) {
                throw new RuntimeException("Wrong for && or ||");
            }
            visit(ast.getRight());
            visit(ast.getLeft());
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (op.equals("<") || op.equals(">") || op.equals("==") ||
                op.equals("!=")) {
            Ast.Expression.Literal lhs = (Ast.Expression.Literal)ast.getLeft();
            Ast.Expression.Literal rhs = (Ast.Expression.Literal)ast.getRight();
            if (lhs.getLiteral() instanceof Comparable && rhs.getLiteral()
                    instanceof Comparable) {
                Comparable lhsComp = (Comparable)lhs.getLiteral();
                Comparable rhsComp = (Comparable)rhs.getLiteral();
                try {
                    lhsComp.compareTo(rhsComp);
                }
                catch (Exception e) {
                    throw new RuntimeException("Wrong types to compare");
                }
                ast.setType(Environment.Type.BOOLEAN);
            }
            throw new RuntimeException("Wrong types to compare!");
        }
        else if (op.equals("+")) {
            Ast.Expression.Literal lhs = (Ast.Expression.Literal)ast.getLeft();
            Ast.Expression.Literal rhs = (Ast.Expression.Literal)ast.getRight();
            if (lhs.getLiteral() instanceof String || rhs.getLiteral() instanceof
                    String) {
                visit(ast.getLeft());
                visit(ast.getRight());
                ast.setType(Environment.Type.STRING);
            }
            else if (lhs.getLiteral() instanceof BigInteger && rhs.getLiteral()
                    instanceof BigInteger) {
                visit(ast.getLeft());
                visit(ast.getRight());
                ast.setType(Environment.Type.INTEGER);
            }
            else if (lhs.getLiteral() instanceof BigDecimal && rhs.getLiteral()
                    instanceof BigDecimal) {
                visit(ast.getLeft());
                visit(ast.getRight());
                ast.setType(Environment.Type.DECIMAL);
            }
            else {
                throw new RuntimeException("Wrong + typings");
            }
        }
        else if (op.equals("-") || op.equals("*") || op.equals("/")) {
            Ast.Expression.Literal lhs = (Ast.Expression.Literal)ast.getLeft();
            Ast.Expression.Literal rhs = (Ast.Expression.Literal)ast.getRight();
            if (lhs.getLiteral() instanceof BigInteger && rhs.getLiteral()
                    instanceof BigInteger) {
                visit(ast.getLeft());
                visit(ast.getRight());
                ast.setType(Environment.Type.INTEGER);
            }
            else if (lhs.getLiteral() instanceof BigDecimal && rhs.getLiteral()
                    instanceof BigDecimal) {
                visit(ast.getLeft());
                visit(ast.getRight());
                ast.setType(Environment.Type.DECIMAL);
            }
            else {
                throw new RuntimeException("Wrong -*/ typings");
            }
        }
        else if (op.equals("^")) {
            Ast.Expression.Literal lhs = (Ast.Expression.Literal)ast.getLeft();
            Ast.Expression.Literal rhs = (Ast.Expression.Literal)ast.getRight();
            if (lhs.getLiteral() instanceof BigInteger && rhs.getLiteral()
                    instanceof BigInteger) {
                visit(ast.getLeft());
                visit(ast.getRight());
                ast.setType(Environment.Type.INTEGER);
            }
            else {
                throw new RuntimeException("Wrong ^ typings");
            }
        }
        return null;
    }
    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException(); // TODO
    }
    @Override
    public Void visit(Ast.Expression.Function ast) {
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
        for (Ast.Expression e : ast.getArguments()) {
            visit(e);
        }
        return null;
    }
    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException(); // TODO
    }
    public static void requireAssignable(Environment.Type target, Environment.Type
            type) {
        throw new UnsupportedOperationException(); // TODO
    }
}