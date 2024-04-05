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
        else if (s.equals("Nil")) {
            return Environment.Type.NIL;
        }
        throw new RuntimeException("wrong type!");
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
        if (!ast.getTypeName().isPresent()) {
            if (!ast.getValue().isPresent()) {
                throw new RuntimeException("no type/name present for declaration!");
            }
            else {
                visit(ast.getValue().get());
                //check that type is valid
                if (ast.getValue().get().getType() instanceof Environment.Type) {
                    Environment.Variable v = scope.defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType(), true, Environment.NIL);
                    ast.setVariable(v);
                    return null;
                }
                else {
                    throw new RuntimeException("cant read type for declaration");
                }
            }
        }
        Environment.Variable v = scope.defineVariable(ast.getName(), ast.getName(), string2type(ast.getTypeName().get()), true, Environment.NIL);
        ast.setVariable(v);
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        // Ensure the receiver is an access expression
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("The receiver of the assignment must be an access expression.");
        }

        // Visit the receiver to resolve its variable (and thus, type)
        visit(ast.getReceiver());
        // Visit the value to ensure it's analyzed
        visit(ast.getValue());

        // Get the types
        Environment.Type receiverType = ast.getReceiver().getType();
        Environment.Type valueType = ast.getValue().getType();

        // Check if the value is assignable to the receiver
        if (!isAssignable2(valueType, receiverType)) {
            throw new RuntimeException("The value is not assignable to the receiver. Expected type " + receiverType + ", but got " + valueType + ".");
        }

        return null;
    }

    /**
     * Checks if one type is assignable to another. This method assumes that you have an isAssignable method similar to the one you mentioned before.
     */
    private boolean isAssignable2(Environment.Type valueType, Environment.Type targetType) {
        return valueType.equals(targetType) || valueType.equals(Environment.Type.ANY);
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("Not Boolean condition for if!");
        }
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("Then statements empty for if!");
        }
        for (Ast.Statement s : ast.getThenStatements()) {
            Scope prev = scope;
            scope = new Scope(scope);
            visit(s);
        }
        for (Ast.Statement s : ast.getElseStatements()) {
            Scope prev = scope;
            scope = new Scope(scope);
            visit(s);
        }
        return null;
    }
    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());
        Environment.Type conditionType = ast.getCondition().getType();

        boolean defaultCaseFound = false;
        for (Ast.Statement.Case caseStmt : ast.getCases()) {
            // Check for default case (last case with no value)
            if (defaultCaseFound) {
                throw new RuntimeException("DEFAULT case must be the last case.");
            }

            if (!caseStmt.getValue().isPresent()) {
                defaultCaseFound = true;
            } else {
                // Ensure case value type matches condition type
                visit(caseStmt.getValue().get());
                if (!caseStmt.getValue().get().getType().equals(conditionType)) {
                    throw new RuntimeException("Case value type does not match condition type.");
                }
            }

            // Visit the case
            visit(caseStmt);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        // Create a new scope for the case
        Scope originalScope = scope;
        scope = new Scope(scope);

        try {
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
            }
        } finally {
            // Restore the original scope
            scope = originalScope;
        }

        return null;
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
        // Check if there's an offset (for array or list access)
        ast.getOffset().ifPresent(offset -> {
            visit(offset); // Ensure the offset expression is analyzed
            if (!offset.getType().equals(Environment.Type.INTEGER)) {
                throw new RuntimeException("Offset type must be Integer for accessing elements.");
            }
        });

        // Look up the variable by name in the current scope
        Environment.Variable variable = scope.lookupVariable(ast.getName());
        if (variable == null) {
            throw new RuntimeException("Variable '" + ast.getName() + "' not defined.");
        }

        // Set the variable (and implicitly its type) on the AST node
        ast.setVariable(variable);

        return null;
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
        Environment.Type listType = ast.getType();
        for (Ast.Expression e : ast.getValues()) {
            visit(e);
            if (!e.getType().equals(listType)) {
                throw new RuntimeException("Wrong item type for list!");
            }
        }
        return null;
    }
    public static void requireAssignable(Environment.Type target, Environment.Type
            type) {
        Boolean f1 = true;
        if (target.equals(Environment.Type.ANY)) {
            f1 = true;
        }
        else if (target.equals(Environment.Type.COMPARABLE)) {
            if (type.equals(Environment.Type.INTEGER) || type.equals(Environment.Type.CHARACTER)
                    || type.equals(Environment.Type.DECIMAL) || type.equals(Environment.Type.STRING)) {
                f1 = true;
            }
            else {
                f1 = false;
            }
        }
        else if (!target.equals(type)) {
            f1 = false;
        }
        if (!f1) {
            throw new RuntimeException("wrong type");
        }
    }
}