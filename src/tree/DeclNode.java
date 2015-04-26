package tree;

import java.util.LinkedList;
import java.util.List;

import syms.SymEntry;

/**
 * class DeclNode - Handles Declarations lists and procedures.
 * @version $Revision: 22 $  $Date: 2014-05-20 15:14:36 +1000 (Tue, 20 May 2014) $ 
 * DeclNode is an abstract class. 
 * The classes defined within DeclNode extend it.
 */
public abstract class DeclNode {
    
    /** Constructor */
    protected DeclNode() {
        super();
    }
    /** Simple visitor pattern implemented in subclasses */
    public abstract void accept( TreeVisitor visitor );
    /** Debugging output at level 0 */
    @Override
    public String toString() {
        return toString(0);
    }
    /** Debugging output of declarations */
    public abstract String toString( int level );
    /** Tree node representing a list of (procedure) declarations */
    public static class DeclListNode extends DeclNode {
        List<DeclNode> declarations;
        
        public DeclListNode() {
            declarations = new LinkedList<DeclNode>();
        }
        public List<DeclNode> getDeclarations() {
            return declarations;
        }
        public void addDeclaration( DeclNode declaration ) {
            declarations.add( declaration );
        }
        @Override
        public void accept(TreeVisitor visitor) {
            visitor.visitDeclListNode( this );
        }
        public String toString( int level ) {
            String s = "";
            for( DeclNode decl : declarations ) {
                s += Tree.newLine(level) + decl.toString(level);
            }
            return s;
        }
    }

    /** Tree node representing a single procedure. */
    public static class ProcedureNode extends DeclNode {
        private SymEntry.ProcedureEntry procEntry;
        private Tree.BlockNode block;

        public ProcedureNode( SymEntry.ProcedureEntry entry, 
                Tree.BlockNode block ) {
            this.procEntry = entry;
            this.block = block;
        }
        @Override
        public void accept( TreeVisitor visitor ) {
            visitor.visitProcedureNode( this );
        }
        public SymEntry.ProcedureEntry getProcEntry() {
            return procEntry;
        }
        public Tree.BlockNode getBlock() {
            return block;
        }
        public String toString( int level ) {
            return "PROCEDURE " + procEntry.getIdent() +
                " = " + block.toString( level+1 );
        }
    }
}
