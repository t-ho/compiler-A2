package syms;

import machine.StackMachine;
import source.Position;
import syms.Type.FunctionType;
import syms.Type.PointerType;
import syms.Type.ProductType;
import syms.Type.ScalarType;

public class Predefined {
    /** Predefined integer type. */
    public static ScalarType INTEGER_TYPE;
    /** Predefined boolean type. */
    public static ScalarType BOOLEAN_TYPE;
    /** Predefined type of a nil pointer */
    public static PointerType NIL_TYPE;
    public static ProductType NIL_TYPE_PAIR;
    public static FunctionType NIL_RELATIONAL_TYPE;
    public static Type.ProductType PAIR_INTEGER_TYPE;   
    public static Type.ProductType PAIR_BOOLEAN_TYPE;           
    public static Type.FunctionType ARITHMETIC_BINARY;
    public static Type.FunctionType INT_RELATIONAL_TYPE;    
    public static Type.FunctionType LOGICAL_BINARY;
    public static Type.FunctionType ARITHMETIC_UNARY;   
    public static Type.FunctionType LOGICAL_UNARY;
    
    public static void addPredefinedEntries( SymbolTable symtab ) {
        // Define types needed for predefined entries
        /** Predefined integer type. */
        INTEGER_TYPE = new ScalarType( "int", StackMachine.SIZE_OF_INT, 
                        Integer.MIN_VALUE, Integer.MAX_VALUE ) {
                    @Override
                    public String toString() { 
                        return name; 
                    }
                };
        /** Predefined boolean type. */
        BOOLEAN_TYPE = new ScalarType( "boolean", StackMachine.SIZE_OF_BOOLEAN, 
                    StackMachine.FALSE_VALUE, StackMachine.TRUE_VALUE ) {
                @Override
                public String toString() { 
                    return name; 
                }
            };
          /** Predefined type of a nil pointer */
        NIL_TYPE = new PointerType( Type.ERROR_TYPE ) {
                    @Override
                    public String toString() {
                        return name;
                    }
                    {
                    name = "nil_type";
                    }
            };
        NIL_TYPE_PAIR = new ProductType( NIL_TYPE, NIL_TYPE);
        NIL_RELATIONAL_TYPE = new FunctionType( NIL_TYPE_PAIR, BOOLEAN_TYPE );
        PAIR_INTEGER_TYPE = new ProductType( INTEGER_TYPE, INTEGER_TYPE );                  
        PAIR_BOOLEAN_TYPE = new ProductType( BOOLEAN_TYPE, BOOLEAN_TYPE );
        ARITHMETIC_BINARY = new FunctionType( PAIR_INTEGER_TYPE, INTEGER_TYPE );
        INT_RELATIONAL_TYPE = new FunctionType(PAIR_INTEGER_TYPE, BOOLEAN_TYPE);
        LOGICAL_BINARY = new FunctionType( PAIR_BOOLEAN_TYPE, BOOLEAN_TYPE );
        ARITHMETIC_UNARY = new FunctionType( INTEGER_TYPE, INTEGER_TYPE );
        LOGICAL_UNARY = new FunctionType( BOOLEAN_TYPE, BOOLEAN_TYPE );
        // Add predefined symbols to predefined scope
        symtab.addType( "int", Position.NO_POSITION, INTEGER_TYPE );
        symtab.addType( "boolean", Position.NO_POSITION, BOOLEAN_TYPE );
        symtab.addConstant("false", Position.NO_POSITION, BOOLEAN_TYPE, 
                StackMachine.FALSE_VALUE );
        symtab.addConstant("true", Position.NO_POSITION, BOOLEAN_TYPE, 
                StackMachine.TRUE_VALUE );
        symtab.addConstant("nil", Position.NO_POSITION, NIL_TYPE, 
                StackMachine.NULL_ADDR );
        symtab.addOperator("_=_", Position.NO_POSITION, NIL_RELATIONAL_TYPE );
        symtab.addOperator("_!=_", Position.NO_POSITION, NIL_RELATIONAL_TYPE );
        symtab.addOperator("-_", Position.NO_POSITION, ARITHMETIC_UNARY );
        symtab.addOperator("_+_", Position.NO_POSITION, ARITHMETIC_BINARY );
        symtab.addOperator("_-_", Position.NO_POSITION, ARITHMETIC_BINARY );
        symtab.addOperator("_*_", Position.NO_POSITION, ARITHMETIC_BINARY );
        symtab.addOperator("_/_", Position.NO_POSITION, ARITHMETIC_BINARY );
        symtab.addOperator("_=_", Position.NO_POSITION, INT_RELATIONAL_TYPE );
        symtab.addOperator("_=_", Position.NO_POSITION, LOGICAL_BINARY );
        symtab.addOperator("_!=_", Position.NO_POSITION, INT_RELATIONAL_TYPE );
        symtab.addOperator("_!=_", Position.NO_POSITION, LOGICAL_BINARY );
        symtab.addOperator("_>_", Position.NO_POSITION, INT_RELATIONAL_TYPE);
        symtab.addOperator("_<_", Position.NO_POSITION, INT_RELATIONAL_TYPE);
        symtab.addOperator("_>=_", Position.NO_POSITION, INT_RELATIONAL_TYPE);
        symtab.addOperator("_<=_", Position.NO_POSITION, INT_RELATIONAL_TYPE);
    }
}
