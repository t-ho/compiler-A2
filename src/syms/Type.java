package syms;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import machine.StackMachine;
import source.ErrorHandler;
import source.Errors;
import source.Position;
import tree.ConstExp;
import tree.ExpNode;

/** This class provides the type structures defining the types
 * available in the language.
 * @version $Revision: 22 $  $Date: 2014-05-20 15:14:36 +1000 (Tue, 20 May 2014) $
 * It provides subclasses for each of the different kinds of "type",
 * e.g., scalar types, subranges, products of types, function types,
 * intersections of types, reference types, and the procedure types.
 * IdRefType is provided to allow type names to be used as types.
 * 
 * As well as the constructors for the types it provides a number of
 * access methods.
 * Each type provides a method for coercing an expression to the type.
 * Type also provides the special type ERROR_TYPE, 
 * which is used for handling type errors.
 */
public abstract class Type 
{
    /** All types require space to be allocated (may be 0) */
    protected int space;
    
    /** Track whether type has been resolved */
    protected boolean resolved;
    
    /** Name of type for error messages */
    protected String name;
    
    /** Error handler */
    protected Errors errors = ErrorHandler.getErrorHandler();
    
    /** Only subclasses provide public constructors. */
    protected Type( int n, boolean resolved ) {
        this.space = n;
        this.resolved = resolved;
    }
    protected Type( int n ) {
        this( n, false );
    }
    /** If something is of type ErrorType an error message for it will already
     * have been issued and hence to avoid generating spurious error messages 
     * ERROR_TYPE is compatible with everything.
     */
    public static final Type ERROR_TYPE = new Type( 0, true ) {
        
        @Override
        protected ExpNode coerce( ExpNode exp ) {
            return exp;
        }
        @Override
        public String toString() { 
            return "error_type";
        }
    };
    /** return the space required for an element of the type */
    public int getSpace() {
        assert resolved;
        return space;
    }
    public void setSpace( int space ) {
        this.space = space;
    }
    public String getName() {
        return name;
    }
    public void setName( String name ) {
        this.name = name;
    }
    @Override
    public String toString() {
        return name + " size " + space;
    }
    /** Resolve identifier references anywhere within type.
     * Default just sets resolved true; it needs to be overridden 
     * when appropriate.
     * @param pos - position for error messages (in overriding methods) 
     */
    public Type resolveType( Position pos ) {
        resolved = true;
        return this;
    }
    /** The coercion procedures will throw an IncompatibleTypes exception
     * if the expression being coerced can't be coerced to this type.
     */
    public static class IncompatibleTypes extends Exception {
        Position pos;
        
        /** Constructor.
         * @param msg error message to be reported
         * @param pos position of the expression for error reporting
         */
        public IncompatibleTypes( String msg, Position pos ) {
            super( msg );
            this.pos = pos;
        }
        public Position getPosition() {
            return pos;
        }
    }
    /** Coerce an expression to this type and report error if incompatible
     * @param exp is the expression to be coerced
     * @returns the coerced expression or ErrorNode on failure
     */
    public ExpNode coerceExp( ExpNode exp ) {
        /** Try coercing the expression. */
        try {
            return this.coerceToType( exp );
        } catch( IncompatibleTypes e ) {
            /** At this point the coercion has failed. */
            errors.error( e.getMessage(), e.getPosition() );
            return new ExpNode.ErrorNode( e.getPosition() ); 
        }
    }
    /** Coerce exp to this type or throw IncompatibleTypes exception if can't
     * @param exp expression to be coerced
     * @return coerced expression
     * @throws IncompatibleTypes if cannot coerce
     */
    public ExpNode coerceToType( ExpNode exp ) 
            throws IncompatibleTypes {
        ExpNode newExp = exp;
        /** Unless this type is a reference type, optionally dereference 
         * the expression to get its base type.
         */
        if( !(this instanceof ReferenceType) ) {
            newExp = optDereferenceExp( newExp );
        }
        /** If the type of the expression is this type or ERROR_TYPE, 
         * we are done.
         */
        Type fromType = newExp.getType();
        if( this.equals( fromType ) || fromType == ERROR_TYPE ) {
            return newExp;
        }
        /** Try coercing the expression. Dynamic dispatch on the desired
         * type is used to control the coercion process.
         */
        return this.coerce( newExp );
    }    
    /** Coerce an expression node passed as a parameter to be of this type.
     * This default version is just checking they are the same type. 
     * Subclasses of Type override this method.
     * @param exp expression to be coerced
     * @return resulting coerced expression node. 
     * @throws IncompatibleTypes exception if it can't coerce.
     */
    protected ExpNode coerce( ExpNode exp ) throws IncompatibleTypes {
        Type fromType = exp.getType();
        if( this.equals( fromType ) ) {
            return exp;
        }
        throw new IncompatibleTypes( 
            "cannot treat " + fromType.getName() + " as " + this.getName(),
            exp.getPosition() );
    }
    /** Type equality. Overridden for most subclasses.
     * @param other - type to be compared with this. */
    public boolean equals( Type other ) {
        return this == other;
    }
    /** If ScalarType then cast to ScalarType and return
     * else return null. Overridden in ScalarType. */
    public ScalarType getScalarType() {
        return null;
    }
    /** Scalar types are simple unstructured types that just have a range of
     * possible values. int and boolean are scalar types */
    public static class ScalarType extends Type {
        /** lower and upper bounds of scalar type */
        protected int lower, upper;
 
        public ScalarType( int size, int lower, int upper ) {
            super( size, true );
            this.lower = lower;
            this.upper = upper;
        }
        public ScalarType( String name, int size, int lower, int upper ) {
            this( size, lower, upper );
            this.name = name;
        }
        /** Constructor when bounds evaluated later */
        public ScalarType( int size ) {
            super( size );
        }
        /** The least element of the type */
        public int getLower() {
            assert resolved;
            return lower;
        }
        /** The greatest element of the type */
        public int getUpper() {
            assert resolved;
            return upper;
        }
       @Override
        public ScalarType getScalarType() {
            return this;
        }
        /** Coerce expression to this Scalar type.
         * The objective is to create an expression of the this scalar type
         * from exp.
         * @param exp expression to be coerced
         * @throws IncompatibleTypes exception if it is not possible to coerce 
         *         exp to this scalar type
         */
        @Override
        protected ExpNode coerce( ExpNode exp ) throws IncompatibleTypes {
            Type fromType = exp.getType();
            if( fromType instanceof SubrangeType ) {
                /** This code implements Rule Widen subrange. 
                 * If the types don't match, the only other possible type
                 * for the expression which can be coerced to this scalar type 
                 * is a subrange type, provided its base type matches
                 * this type. If that is the case we insert a WidenSubrangeNode
                 * of this type with the expression as a subtree.
                 */
                Type baseType = ((SubrangeType)fromType).getBaseType();
                if( this.equals( baseType ) ) {
                    return new ExpNode.WidenSubrangeNode( exp.getPosition(), 
                            this, exp );
                }
            } 
            /** Otherwise we report the failure to coerce the expression via
             * an IncompatibleTypes exception.
             */
            throw new IncompatibleTypes( "can't coerce " + 
                    exp.getType().getName() + " to " + this.getName(), 
                    exp.getPosition() );
        }
    }
    /** If SubrangeType then cast to SubrangeType and return
     * else return null. Overridden in SubrangeType. */
    public SubrangeType getSubrangeType() {
        return null;
    }
    /** If this is a subrange type widen it to its base type.
     * Overridden in SubrangeType. */
    public Type optWidenSubrange() {
        return this;
    }
    /** Types defined as a subrange of a scalar type. */
    public static class SubrangeType extends ScalarType {
        /** The base type of the subrange type */
        private Type baseType;
        /** Constant expression trees for lower and upper bounds 
         * before evaluation */
        private ConstExp lowerExp, upperExp;

        public SubrangeType( ConstExp lowerExp, ConstExp upperExp ) {
            /** On a byte addressed machine, the size could be scaled to
             * just fit the subrange, e.g., a subrange of 0..255
             * might only require 1 byte.
             */
            super( StackMachine.SIZE_OF_INT );
            this.lowerExp = lowerExp;
            this.upperExp = upperExp;
        }
        public Type getBaseType() {
            return baseType;
        }
        @Override
        public SubrangeType getSubrangeType() {
            return this;
        }
        @Override
        public Type optWidenSubrange() {
            return baseType;
        }

        /** Coerce expression to this subrange type
         * The objective is to create an expression of the this subrange type
         * from exp.
         * @param exp expression to be coerced
         * @throws IncompatibleTypes exception if it is not possible to coerce 
         *         exp to this subrange type
         */
        @Override
        protected ExpNode coerce( ExpNode exp ) throws IncompatibleTypes {
            /** This implements Rule Narrow subrange in the static semantics. 
             * If the types don't match, we can try coercing the expression
             * to the base type of this subrange, and then narrow that
             * to this type. If the coercion to the base type fails it will
             * generate an exception, which is allowed to pass up to the caller.
             */
            ExpNode coerceExp = getBaseType().coerceToType( exp );
            /** If we get here, coerceExp is of the same type as the base 
             * type of this subrange type. We just need to narrow it
             * down to this subrange. 
             */
            return new ExpNode.NarrowSubrangeNode( coerceExp.getPosition(), 
                        this, coerceExp );
        }
        /** Resolving a subrange type requires the lower and upper bound 
         * expressions to be evaluated.
         */
        @Override
        public Type resolveType( Position pos ) {
            if( !resolved ) {
                lower = lowerExp.getValue();
                upper = upperExp.getValue();
                if( upper < lower ) {
                    errors.error( "Upper bound of subrange less than lower bound", pos );
                }
                baseType = upperExp.getType();
                if( !upperExp.getType().equals(lowerExp.getType())) {
                    errors.error( "Types of bounds of subrange should match", pos );
                    baseType = ERROR_TYPE;
                }
                resolved = true;
            }
            return this;
        }
        /** A subrange type is equal to another subrange type only if they have
         * the same base type and lower and upper bounds.
         */
        @Override
        public boolean equals( Type other ) {
            if( other instanceof SubrangeType ) {
                SubrangeType otherSubrange = (SubrangeType)other;
                return baseType.equals( otherSubrange.getBaseType() ) &&
                        lower == otherSubrange.getLower() &&
                        upper == otherSubrange.getUpper();
            } else {
                return false;
            }
        }
        @Override
        public String toString() {
            return (baseType == null ? "<undefined>" : baseType.toString()) +
                "[" + lower + ".." + upper + "]";
        }   
    }

    /** Product types represent the product of a sequence of types */
    public static class ProductType extends Type {
        /** List of types in the product */
        private List<Type> types;
        
        private ProductType() {
            super( 0 );
            types = new LinkedList<Type>();
        }
        /** Constructor when list of types available */
        public ProductType( List<Type> types ) {
            super( 0 );
            this.types = types;
        }
        /** Constructor allowing individual types to be specified */
        public ProductType( Type... typeArray ) {
            this( Arrays.asList( typeArray ) );
        }
        /** The space required for a product of types is the sum of
         * the spaces required for each type in the product.
         */
        private int calcSpace( List<Type> types ) {
            int space = 0;
            for( Type t : types ) {
                space += t.getSpace();
            }
            return space;
        }
        public List<Type> getTypes() {
            return types;
        }
        /** Resolve identifier references anywhere within type */
        @Override
        public ProductType resolveType( Position pos ) {
            if( ! resolved ) {
                /* Build a list of resolved types */
                List<Type> resolvedTypes = new LinkedList<Type>();
                for( Type t : types ) {
                    resolvedTypes.add( t.resolveType( pos ) );
                }
                types = resolvedTypes;
                space = calcSpace( types );
                resolved = true;
            }
            return this;
        }
        /** Two product types are equal only if each element of the list
         * of types for one is equal to the corresponding element of the
         * list of types for the other.
         */
        @Override
        public boolean equals( Type other ) {
            if( other instanceof ProductType ) {
                List<Type> otherTypes = ((ProductType)other).getTypes();
                if( types.size() == otherTypes.size() ) {
                    Iterator<Type> iterateOther = otherTypes.iterator();
                    for( Type t : types ) {
                        Type otherType = iterateOther.next();
                        if( ! t.equals( otherType ) ) {
                            return false;
                        }
                    }
                    /* If we reach here then every type in the product has
                     * matched the corresponding type in the other product
                     */
                    return true;
                }
            }
            /* other is not a ProductType or has a different number of types */
            return false;
        }

        /** Coerce expression to this product type.
         * @param exp should be an ArgumentsNode with a list of 
         *     expressions of the same length as this product type 
         * @throws IncompatibleTypes exception if it is not possible to coerce
         *         exp to this product type
         */
        @Override
        protected ExpNode.ArgumentsNode coerce( ExpNode exp ) 
                throws IncompatibleTypes {
            /** If exp is not an ArgumentsNode consisting of a list of 
             * expressions of the same length as this product type, 
             * then exp can't be coerced to this product type and 
             * we raise an exception.
             */
            if( exp instanceof ExpNode.ArgumentsNode) {
                ExpNode.ArgumentsNode args = (ExpNode.ArgumentsNode)exp;
                if( this.getTypes().size() == args.getArgs().size() ) {
                    /** If exp is an ArgumentNode of the same size as this
                     * product type, we coerce each expression in the list
                     * of arguments, to the corresponding type in the product,
                     * accumulating a new (coerced) list of expressions as we
                     * go. If any of the argument expressions can't be 
                     * coerced, an exception will be raised, which we allow
                     * the caller to handle because the failure to coerce any
                     * expression in the list of arguments, corresponds to a
                     * failure to coerce the whole arguments node.
                     */
                    ListIterator<ExpNode> iterateArgs = 
                        args.getArgs().listIterator();
                    List<ExpNode> newArgs = new LinkedList<ExpNode>();
                    for( Type t : this.getTypes() ) {
                        ExpNode subExp = iterateArgs.next();
                        /** Type incompatibilities detected in the
                         * coercion will generate an exception,
                         * which we allow to pass back up to the next level
                         */
                        newArgs.add( t.coerceToType( subExp ) );
                    }
                    /** If we get here, all expressions in the list have been
                     * successfully coerced to the corresponding type in the 
                     * product, and the coerced list of expressions newArgs 
                     * will be of type toProductType. We return an 
                     * ArgumentsNode of this product type, with newArgs as 
                     * its list of expressions.
                     */
                    return new ExpNode.ArgumentsNode( this, newArgs );
                } else {
                    throw new IncompatibleTypes( 
                        "length mismatch in coercion to ProductType", 
                        exp.getPosition() );
                }
            } else {
                throw new IncompatibleTypes( 
                    "Arguments node expected for coercion to ProductType",
                    exp.getPosition() );
            }
        }
        @Override
        public String toString() {
            String result = "(";
            String sep = "";
            for( Type t: types ) {
                result += sep + t;
                sep = "*";
            }
            return result + ")";
        }
    }
    /** Function types represent a function from an argument type
     * to a result type.
     */
    public static class FunctionType extends Type {
        /** Type of the argument to the function */
        private Type argType;
        /** Type of the result of the function */
        private Type resultType;
        
        public FunctionType( Type arg, Type result ) {
            super( 0 );
            this.argType = arg;
            this.resultType = result;
        }
        public Type getArgType() {
            return argType;
        }
        public Type getResultType() {
            return resultType;
        }
        /** Resolve identifier references anywhere within type */
        @Override
        public FunctionType resolveType( Position pos ) {
            if( ! resolved ) {
                argType = argType.resolveType( pos );
                resultType = resultType.resolveType( pos );
                resolved = true;
            }
            return this;
        }
        /** Two function types are equal only if their argument and result
         * types are equal.
         */
        @Override
        public boolean equals( Type other ) {
            if( other instanceof FunctionType ) {
                FunctionType otherFunction = (FunctionType)other;
                return getArgType().equals(otherFunction.getArgType()) &&
                    getResultType().equals(otherFunction.getResultType());
            }
            return false;
        }
        @Override
        public String toString() {
            return "(" + argType + "->" + resultType + ")";
        }
    }
    /** Intersection types represent the intersection of a set of types.
     * They can be used as the types of overloaded operators. 
     * For example "=" has two types to allow two integers to be compared
     * and two booleans to be compared. */
    public static class IntersectionType extends Type {
        /** List of possible types */
        private List<Type> types;
        
        /** @param typeArray - list of types in the intersection
         * @requires the types in typeArray are distinct */
        public IntersectionType( Type... typeArray ) {
            super( 0 );
            types = new ArrayList<Type>();
            for( Type t : typeArray ) {
                types.add( t );
            }
        }
        public List<Type> getTypes() {
            return types;
        }
        /** Add a type to the list of types, but if it is a IntersectionType
         * flatten it and add each type in the intersection. 
         */
        public void addType( Type t ) {
            if( t instanceof IntersectionType ) {
                types.addAll( ((IntersectionType)t).getTypes() );
            } else {
                types.add( t );
            }
        }
        /** Resolve identifier references anywhere within type */
        @Override
        public IntersectionType resolveType( Position pos ) {
            if( !resolved ) {
                /* Build a list of resolved types */
                List<Type> resolvedTypes = new LinkedList<Type>();
                for( Type t : types ) {
                    resolvedTypes.add( t.resolveType( pos ) );
                }
                types = resolvedTypes;
                resolved = true;
            }
            return this;
        }
        /* Two intersection types are equal if they contain the same sets of
         * types.
         * @param other - type to be compared with this
         * @requires the lists in each intersection type have distinct elements
         */
        @Override
        public boolean equals( Type other ) {
            if( other instanceof IntersectionType ) {
                List<Type> otherTypes = ((IntersectionType)other).getTypes();
                if( types.size() == otherTypes.size() ) {
                    for( Type t : types ) {
                        if( ! otherTypes.contains( t ) ) {
                            return false;
                        }
                    }
                    /** If we reach here then all types in this intersection
                     * are also contained in the other intersection, and hence
                     * the two intersections are equivalent.
                     */
                    return true;
                }
            }
            /* other is not an IntersectionType or 
             * has a different number of types */
            return false;
        }
        /** An ExpNode can be coerced to a IntersectionType if it can be
         * coerced to one of the types of the intersection.
         * @throws IncompatibleTypes exception if it is not possible to 
         *         coerce exp to any type within the intersection
         */
        @Override
        protected ExpNode coerce( ExpNode exp ) throws IncompatibleTypes {
            /** We iterate through all the types in the intersection, trying 
             * to coerce the exp to each, until one succeeds and we return
             * that coerced expression. If a coercion to a type in the 
             * intersection fails it will throw an exception, which is caught.
             * Once caught we ignore the exception, and allow the for loop to
             * try the next type in the intersection.
             */
            for( Type toType : this.getTypes() ) {
                try {
                    return toType.coerceToType( exp );
                } catch( IncompatibleTypes ex ) {
                    // allow "for" loop to try the next alternative 
                }
            }
            /** If we get here, we were unable to to coerce exp to any one of
             * the types in the intersection, and hence we can't coerce exp to
             * the intersection type.
             */
            throw new IncompatibleTypes( "none of types match",
                    exp.getPosition() );
        }
        @Override
        public String toString() {
            String s = "(";
            String sep = "";
            for( Type t : types ) {
                s += sep + t;
                sep = " & ";
            }
            return s + ")";
        }
    }
    /** Type for a procedure. */
    public static class ProcedureType extends Type {
        
        public ProcedureType() {
            // size of type allows for the procedure to be a parameter
            super(2*StackMachine.SIZE_OF_ADDRESS); 
        }
        @Override
        public ProcedureType resolveType( Position pos ) {
            resolved = true;
            return this;
        }
        @Override
        public String toString() {
            String s = "PROCEDURE"; 
            return s;
        }
    }

    /** Type for a type identifier. Used until the type identifier can
     * be resolved.
     */
    public static class IdRefType extends Type {
        /** Name of the referenced type */
        private String name;
        /** Symbol table scope at the point of definition of the type
         * reference. Used when resolving the reference. */
        private Scope scope;
        /** Position of use of type identifier */
        Position pos;
        /** Resolved real type, or ERROR_TYPE if can't be resolved. */
        private Type realType;
        /** Status of resolution of reference. */
        private enum Status{ Unresolved, Resolving, Resolved }
        private Status status;
        
        public IdRefType( String name, Scope scope, Position pos ) {
            super( 0 );
            this.name = name;
            this.scope = scope;
            this.pos = pos;
            this.status = Status.Unresolved;
        }
        public String getName() {
            return name;
        }
        /** Resolve the type identifier and return the real type. */
        @Override
        public Type resolveType( Position usePos ) {
            // System.out.println( "Resolving " + name );
            switch( status ) {
            case Unresolved:
                status = Status.Resolving;
                realType = ERROR_TYPE;
                SymEntry entry = scope.lookup( name );
                if( entry != null && entry instanceof SymEntry.TypeEntry ) {
                    /* resolve identifiers in the referenced type */
                    entry.resolve();
                    /* if status of this entry has resolved then there was a
                     * circular reference and we leave the realType as 
                     * ERROR_TYPE to avoid other parts of the compiler getting
                     * into infinite loops chasing types.
                     */
                    if( status == Status.Resolving ) {
                        realType = entry.getType();
                    }
                    assert realType != null;
                } else {
                    errors.error( "Undefined type: " + name, pos );
                }
                status = Status.Resolved;
                break;
            case Resolving:
                errors.error( name + " is circularly defined", pos );
                /* Will resolve to ERROR_TYPE */
                status = Status.Resolved;
                break;
            case Resolved:
                /* Already resolved */
                break;
            }
            return realType;
        }
        @Override
        public String toString() {
            return name;
        }
    }
    /** AddressType is the common part of ReferenceType (and PointerType) */ 
    public static class AddressType extends Type {
        /** Type of addressed object */
        protected Type baseType;
        
        public AddressType( Type baseType ) {
            super( StackMachine.SIZE_OF_ADDRESS );
            this.baseType = baseType;
        }
        public Type getBaseType() {
            return baseType;
        }
        @Override
        public AddressType resolveType( Position pos ) {
            if( !resolved ) {
                baseType = baseType.resolveType( pos );
                resolved = true;
            }
            return this;
        }
        @Override
        public String toString() {
            return super.toString();
        }
    }
    /** This method implements Rule Dereference in the static semantics if
     * applicable, otherwise it leaves the expression unchanged. 
     * Optionally dereference a Reference type expression to get its base type
     * If exp is type ReferenceType(T) for some base type T,
     * a new DereferenceNode of type T is created with exp as a subtree
     * and returned, otherwise exp is returned unchanged.
     */
    public static ExpNode optDereferenceExp( ExpNode exp ) {
        Type fromType = exp.getType();
        if( fromType instanceof ReferenceType ) {
            /* Dereference of fromType is not optional here */
            return 
                new ExpNode.DereferenceNode( fromType.optDereferenceType(), exp );
        } else {
            return exp;
        }
    }
    /** If this type is a reference type return its base type
     * otherwise just return this.
     * Default return this - overridden in ReferenceType.
     */
    public Type optDereferenceType() {
        return this;
    }
    /** Type used for variables in order to distinguish a variable
     * of type ref(int), say, from its value which is of type int.
     */
    public static class ReferenceType extends AddressType {
        
        public ReferenceType( Type baseType ) {
            super( baseType );
        }
        /** If this type is a reference type return its base type
         * otherwise just return this.
         * As this subclass is ReferenceType, must return its base type.
         */
        @Override
        public Type optDereferenceType() {
            return getBaseType();
        }
        /** Two reference types are equal only if their base types are equal */
        @Override
        public boolean equals( Type other ) {
            return other instanceof ReferenceType &&
                ((ReferenceType)other).getBaseType().equals(
                        this.getBaseType() );
        }
        @Override
        public String toString() {
            return "ref(" + baseType + ")";
        }
    }
    
    /** Get pointer type by optionally dereferencing this type
     * if necessary.
     * @return pointer type or null if not pointer type
     */
    public PointerType getPointerType() {
        /* Retrieve base type if a reference type */
        Type thisType = this.optDereferenceType();
        if( thisType instanceof PointerType ) {
            return (PointerType)thisType;
        }
        return null;
    }
    public static class PointerType extends AddressType {
                
        public PointerType( Type baseType ) {
            super( baseType ); // A pointer is an address
            resolved = baseType.resolved;
        }
        /** To resolve a pointer type we just resolve its base type
         * but to avoid getting into an infinite loop we mark the 
         * pointer type as resolved first.
         */
        @Override
        public PointerType resolveType( Position pos ) {
            if( !resolved ) {
                /** As a pointer type may validly circularly reference itself
                 * we set the pointer type to resolved before resolving its 
                 * base type.
                 */
                resolved = true;
                /** Base type must be a type identifier reference.
                 * Resolving the base type is deferred until it is needed
                 * and resolved in getBaseType */
                if( !(baseType instanceof IdRefType) ) {
                    errors.fatal( "Base type of pointer type is not a type identifier", 
                            Position.NO_POSITION );
                }
            }
            return this;
        }
        @Override
        public Type getBaseType() {
            if( !baseType.resolved ) {
                baseType = baseType.resolveType(Position.NO_POSITION);
            }
            return baseType;
        }
        /** Coerce expression to this PointerType
         * The objective is to create an expression of this pointer type
         * from exp.
         * @param exp expression to be coerced
         * @throws IncompatibleTypes exception if it is not possible to coerce 
         *         exp to this pointer Type
         */
        @Override
        public ExpNode coerce( ExpNode exp ) throws IncompatibleTypes {
            Type fromType = exp.getType();
            if( fromType.equals( Predefined.NIL_TYPE ) ) {
                return exp;
            }
            throw new IncompatibleTypes( 
                    "cannot treat " + exp.getType() + " as " + this,
                    exp.getPosition() );
        }
        /** Two pointer types are equal only if their base types are equal. */
        @Override
        public boolean equals( Type other ) {
            return other instanceof PointerType &&
                ((PointerType)other).getBaseType().equals(
                        this.getBaseType() );
        }
        @Override
        public String toString() {
            return "^" + baseType.getName();
        }
    }
    public RecordType getRecordType() {
        Type thisType = this.optDereferenceType();
        if( thisType instanceof RecordType ) {
            return (RecordType)thisType;
        }
        return null;
    }
    /** Type representing a record. */
    public static class RecordType extends Type {
        private Position pos;
        private List<Field> fieldList; /* In order declared */
        private SortedMap<String,Field> fields;
        
        public RecordType() {
            super( 0 );           // Type size will need to be resolved later
            this.pos = Position.NO_POSITION;
            this.fieldList = new ArrayList<Field>();
            this.fields = new TreeMap<String,Field>();
            resolved = false;
        }
        /** When retrieving the space required for a record.
         * @return the number of words required to store a single element
         *         of this type.
         */
        @Override
        public int getSpace() {
            assert resolved;
            return space;
        }
        public Position getPosition() {
            return pos;
        }
        public void setPosition( Position pos ) {
            this.pos = pos;
        }
        public List<Field> getFieldList() {
            assert resolved;
            return fieldList;
        }
        /** @param id - name of the field
         * @return true iff id is a field of the record.
         */
        public boolean containsField( String id ) {
            return fields.containsKey( id );
        }
        /** Add a field to a record. */
        public void add( Field f ) {
            fieldList.add( f );
            fields.put( f.getId(), f );
        }
        /** Lookup a field
         * @requires resolved
         * @param id name of field
         * @return the field's type, or ERROR_TYPE if not a field
         */
        public Type getFieldType( String id ) {
            assert resolved;
            Field field = fields.get( id );
            if( field == null ) {
                return ERROR_TYPE;
            }
            return field.getType();
        }
        /** Get the location of a field in a record.
         * @param id - the name of the field
         * @requires resolved && fields.containsKey(id)
         * @return the offset of the field from the start of the record
         */
        public int getOffset( String id ) {
            assert resolved && fields.containsKey( id );
            Field field = fields.get( id );
            return field.getOffset();
        }
        /** Allocate the offsets of the fields and 
         * calculate the size of the record 
         */
        @Override
        public Type resolveType( Position pos ) {
            if( !resolved ) {
                int offset = 0;
                /* Allocate fields in the order declared */
                for( Field field : fieldList ) {
                    field.setOffset( offset );
                    Type fieldType = field.resolveType();
                    offset += fieldType.getSpace();
                }
                space = offset;
                resolved = true;
            }
            return this;
        }
        @Override
        public String toString() {
            String result = "RECORD ";
            String sep = "";
            for( Field field : fields.values() ) {
                result += sep + field.toString();
                sep = "; ";
            }
            return result + " END";
        }
    }
    /** A single field in a record */
    public static class Field {
        private Position pos;
        private String id;
        private Type type;
        private Type declaredType;
        private int offset;
        
        public Field( Position pos, String id, Type t ) {
            this.pos = pos;
            this.id = id;
            this.type = t;
            this.declaredType = t;
        }
        public Position getPosn() {
            return pos;
        }
        public String getId() {
            return id;
        }
        public Type resolveType() {
            type = type.resolveType( pos );
            return type;
        }
        public Type getType() {
            return type;
        }
        public int getOffset() {
            return offset;
        }
        public void setOffset( int offset ) {
            this.offset = offset;
        }
        @Override
        public String toString() {
            return id + ":" + declaredType;
        }
    }
}
