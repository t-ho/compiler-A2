package tree;

import source.Position;
import syms.Scope;
import syms.SymbolTable;
/** 
 * class Tree - Abstract syntax tree nodes and support functions.
 * @version $Revision: 22 $  $Date: 2014-05-20 15:14:36 +1000 (Tue, 20 May 2014) $
 * Uses visitor pattern in order to separate the static semantic checks
 * and code generation from tree building. 
 * The accept method for each type of tree node, calls the corresponding
 * visit method of the tree visitor. 
 */
public abstract class Tree {
    /** Position in the input source program */
    private Position pos;
    /** Constructor */
    protected Tree( Position pos ) {
        this.pos = pos;
    }
    public Position getPosition() {
        return pos;
    }
    /** Debugging output */
    public abstract String toString( int level );
    /** Debugging output at level 0 */
    public String toString() {
        return toString(0);
    }
    /** Tree node representing the main program. */
    public static class ProgramNode extends Tree {
        private SymbolTable baseSymbolTable;
        private BlockNode mainProc;

        public ProgramNode( Position pos, SymbolTable baseSyms, BlockNode mainProc ) {
            super( pos );
            this.baseSymbolTable = baseSyms;
            this.mainProc = mainProc;
        }
        public void accept( TreeVisitor visitor ) {
            visitor.visitProgramNode( this );
        }
        public SymbolTable getBaseSymbolTable() {
            return baseSymbolTable;
        }
        public BlockNode getBlock() {
            return mainProc;
        }
        @Override
        public String toString( int level ) {
            return mainProc.toString(level);
        }
    }

    /** Node representing a Block consisting of declarations and
     * body of a procedure, function, or the main program. */
    public static class BlockNode extends Tree {
        protected DeclNode.DeclListNode procedures;
        protected StatementNode body;
        protected Scope blockLocals;

        /** Constructor for a block within a procedure */
        public BlockNode( Position pos, DeclNode.DeclListNode procedures, 
                StatementNode body) {
            super( pos );
            this.procedures = procedures;
            this.body = body;
        }
        public void accept( TreeVisitor visitor ) {
            visitor.visitBlockNode( this );
        }
        public DeclNode.DeclListNode getProcedures() {
            return procedures;
        }
        public StatementNode getBody() {
            return body;
        }
        public Scope getBlockLocals() {
            return blockLocals;
        }
        public void setBlockLocals( Scope blockLocals ) {
            this.blockLocals = blockLocals;
        }
        @Override
        public String toString( int level ) {
            return getProcedures().toString(level+1) + 
                    newLine(level) + "BEGIN" + 
                    newLine(level+1) + body.toString(level+1) +
                    newLine(level) + "END";
        }
    }
    /** Returns a string with a newline followed by spaces of length 2n. */
    public static String newLine( int n ) {
       String ind = "\n";
       while( n > 0) {
           ind += "  ";
           n--;
       }
       return ind;
    }

}
