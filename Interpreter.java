package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {

        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        Environment.PlcObject value = ast.getValue().isPresent() ? visit(ast.getValue().get()) : Environment.NIL;
        scope.defineVariable(ast.getName(), ast.getMutable(), value);
        return Environment.NIL;
    }



    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope previous = scope;
            try {
                scope = new Scope(scope);
                for (int i = 0; i < ast.getParameters().size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), true, args.get(i));
                }
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
                return Environment.NIL;
            } catch (Return returnValue) {
                return returnValue.value;
            } finally {
                scope = previous;
            }
        });
        return Environment.NIL;
    }



    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        return visit(ast.getExpression());
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Environment.PlcObject value = ast.getValue().isPresent() ? visit(ast.getValue().get()) : Environment.NIL;
        scope.defineVariable(ast.getName(), true, value);
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        Environment.PlcObject value = visit(ast.getValue());

        // Check if the receiver is an Access expression and extract the variable name
        if (ast.getReceiver() instanceof Ast.Expression.Access) {
            Ast.Expression.Access access = (Ast.Expression.Access) ast.getReceiver();
            String variableName = access.getName();
            //Object temp = this.scope.lookupVariable("list").getValue(); //gets temp
            // Lookup the variable in the scope using the extracted name
            Environment.Variable variable = scope.lookupVariable(variableName);
            try { //check if list
                //get index to change
                Ast.Expression val = access.getOffset().get();
                Ast.Expression.Literal valTemp = (Ast.Expression.Literal)val;
                BigInteger tempIndex = (BigInteger)valTemp.getLiteral();
                int index = tempIndex.intValue();
                //get value to assign
                Object newVal = value.getValue();
                //get list to edit
                Environment.PlcObject t1 = variable.getValue();
                Object t2 = t1.getValue();
                List<Object> temp = (List<Object>) t2;
                //assign new list value
                temp.set(index, newVal);
            }
            catch (Exception e) {
                //must be a single variable
                variable.setValue(value);
            }
        } else {
            throw new RuntimeException("The receiver of an assignment must be a variable access.");
        }

        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        Ast.Expression.Literal temp = (Ast.Expression.Literal)ast.getCondition();
        if (temp.getLiteral() instanceof Boolean) {
            //evaluate
            Boolean cond = (Boolean)temp.getLiteral();
            List<Ast.Statement> valList = null;
            if (cond) {
                valList = ast.getThenStatements();
            }
            else {
                valList = ast.getElseStatements();
            }
            for (Ast.Statement i : valList) {
                visit(i);
            }
        }
        else {
            //error, do not proceed
            throw new RuntimeException("Not Boolean for If statement");
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {

        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {

        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        /*Environment.PlcObject temp = visit(ast.getValue());
        throw new Return(temp); //pass in environment plc value from visit*/
        throw new UnsupportedOperationException(); //TODO
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL; // Returns the NIL object for null literals
        }
        return Environment.create(ast.getLiteral());
    }



    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        //check if literal or binary
        Ast.Expression exp = ast.getExpression();
        return visit(exp);
        //throw new UnsupportedOperationException(); //TODO
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject left = null;
        Environment.PlcObject right = null;
        try {
            left = visit(ast.getLeft());
        }
        catch (Exception e) {
            left = null;
        }
        try {
            right = visit(ast.getRight());
        }
        catch (Exception e) {
            right = null;
        }
        switch (ast.getOperator()) {
            case "&&":
                if (left.getValue() instanceof Boolean && (Boolean)left.getValue() == true) {
                    if (right.getValue() instanceof Boolean && (Boolean)right.getValue() == true) {
                        return Environment.create(true);
                    }
                    else {
                        return Environment.create(false);
                    }
                }
                else {
                    return Environment.create(false);
                }
            case "||":
                if (left.getValue() instanceof Boolean && (Boolean)left.getValue() == true) {
                    return Environment.create(true);
                }
                else {
                    if (right.getValue() instanceof Boolean && (Boolean)right.getValue() == true) {
                        return Environment.create(false);
                    }
                    else {
                        return Environment.create(true);
                    }
                }
            case "<":
                //check if types are int or decimal?
                if (left.getValue() instanceof BigInteger) {
                    BigInteger lhsInt = (BigInteger)left.getValue();
                    BigInteger rhsInt = requireType(BigInteger.class, right);
                    int res = lhsInt.compareTo(rhsInt);
                    if (res < 0) {
                        return Environment.create(true);
                    }
                    else {
                        return Environment.create(false);
                    }
                }
                else if (left.getValue() instanceof BigDecimal) {
                    BigDecimal lhsInt = (BigDecimal)left.getValue();
                    BigDecimal rhsInt = requireType(BigDecimal.class, left);
                    int res = lhsInt.compareTo(rhsInt);
                    if (res < 0) {
                        return Environment.create(true);
                    }
                    else {
                        return Environment.create(false);
                    }
                }
                else {
                    throw new RuntimeException("Wrong < Type");
                }
            case ">": {
                if (left.getValue() instanceof BigInteger) {
                    BigInteger lhsInt = (BigInteger)left.getValue();
                    BigInteger rhsInt = requireType(BigInteger.class, right);
                    int res = lhsInt.compareTo(rhsInt);
                    if (res > 0) {
                        return Environment.create(true);
                    }
                    else {
                        return Environment.create(false);
                    }
                }
                else if (left.getValue() instanceof BigDecimal) {
                    BigDecimal lhsInt = (BigDecimal)left.getValue();
                    BigDecimal rhsInt = requireType(BigDecimal.class, left);
                    int res = lhsInt.compareTo(rhsInt);
                    if (res > 0) {
                        return Environment.create(true);
                    }
                    else {
                        return Environment.create(false);
                    }
                }
                else {
                    throw new RuntimeException("Wrong < Type");
                }
            }
            case "==":
                if (left.getValue().equals(right.getValue())) {
                    //do i need to check if classes are equal?
                    return Environment.create(true);
                }
                else {
                    return Environment.create(false);
                }
            case "!=":
                if (left.getClass().equals(right.getClass()) && left.getValue().equals(right.getValue())) {
                    return Environment.create(false);
                }
                else {
                    return Environment.create(true);
                }
            case "*":
                // Handle multiplication for BigInteger and BigDecimal
                if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) { //same type
                    return Environment.create(((BigInteger) left.getValue()).multiply((BigInteger) right.getValue()));
                }
                else if (left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) { //same type
                    return Environment.create(((BigDecimal) left.getValue()).multiply((BigDecimal) right.getValue()));
                }
                else {
                    throw new RuntimeException("Invalid types for * operator");
                }
            case "/":
                // Handle division for BigInteger and BigDecimal
                if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) { //running under the assumption integers/integers = integers
                    return Environment.create(((BigInteger) left.getValue()).divide((BigInteger) right.getValue()));
                }
                else if (left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) left.getValue()).divide((BigDecimal) right.getValue(), RoundingMode.HALF_EVEN));
                }
                else {
                    throw new RuntimeException("Invalid types for / operator");
                }
            case "^":
                if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                    BigInteger base = (BigInteger) left.getValue();
                    BigInteger exponent = (BigInteger) right.getValue();

                    // Convert the exponent to a long, cautiously.
                    long exponentAsLong;
                    try {
                        exponentAsLong = exponent.longValueExact();
                    } catch (ArithmeticException e) {
                        throw new RuntimeException("Exponent too large for exponentiation");
                    }

                    return Environment.create(expBigInteger(base, exponentAsLong));
                } else {
                    throw new RuntimeException("Invalid types for ^ operator: both base and exponent must be BigInteger");
                }
            case "+":
                // Check for string concatenation first
                if (left.getValue() instanceof String || right.getValue() instanceof String) { // if either left or right is a string, concatenate
                    return Environment.create(left.getValue().toString() + right.getValue().toString());
                }
                else if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) { //both are BigInteger
                    return Environment.create(((BigInteger) left.getValue()).add((BigInteger) right.getValue()));
                }
                else if (left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) { //both are BigDecimal
                    return Environment.create(((BigDecimal) left.getValue()).add((BigDecimal) right.getValue()));
                } else {
                    throw new RuntimeException("Invalid types for + operator");
                }
            case "-":
                // Handle subtraction for BigInteger and BigDecimal
                if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) { //if both bigInt, subtract
                    return Environment.create(((BigInteger) left.getValue()).subtract((BigInteger) right.getValue()));
                }
                else if (left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) left.getValue()).subtract((BigDecimal) right.getValue()));
                }
                else {
                    throw new RuntimeException("Invalid types for - operator");
                }
                // Add other operators here...
            default:
                throw new RuntimeException("Unsupported operator: " + ast.getOperator());
        }
    }

    private static BigInteger expBigInteger(BigInteger base, long exponent) { //DO the power calcuation by yourself
        if (exponent < 0) {
            throw new ArithmeticException("Negative exponent");
        }
        BigInteger result = BigInteger.ONE;
        while (exponent > 0) {
            if ((exponent & 1) == 1) {
                result = result.multiply(base);
            }
            base = base.multiply(base);
            exponent >>= 1;
        }
        return result;
    }
    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {

        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        // Look up the function in the current scope by its name and the number of arguments.
        Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());

        // Evaluate all argument expressions to get their values as PlcObjects.
        List<Environment.PlcObject> evaluatedArgs = ast.getArguments().stream()
                .map(this::visit) // Visit (evaluate) each argument expression.
                .collect(Collectors.toList());

        // Invoke the function with the evaluated arguments and return its result.
        return function.invoke(evaluatedArgs);
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> vals = ast.getValues();
        List<Object> resVals = new ArrayList<>();
        for (Ast.Expression item : vals) {
            Ast.Expression.Literal newItem = (Ast.Expression.Literal)item;
            resVals.add(visit(newItem).getValue());
        }
        return Environment.create(resVals);
    }


    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    public static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}