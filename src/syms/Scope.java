package syms;

import java.util.Collection;
import java.util.TreeMap;
import java.util.SortedMap;

import machine.StackMachine;
import syms.SymEntry;
import syms.SymEntry.ProcedureEntry;
import tree.Tree;

/** A Scope represents a static scope for a procedure, main program or 
 * the predefined scope. 
 * @version $Revision: 22 $  $Date: 2014-05-20 15:14:36 +1000 (Tue, 20 May 2014) $
 * It provides operations to add and look up identifiers. 
 * Searching for an identifier in a scope starts at the current scope, 
 * but then if it is not found, the search proceeds to the next outer 
 * (parent) scope, and so on. 
 */
public class Scope {
    /** Parent Scope */
    private Scope parent;
    /** Static level of this scope */
    private int level;
    /** Symbol table entry for the procedure that owns this scope */
    private ProcedureEntry procEntry;
    /** Symbol table entries. A SortedMap is used to avoid issues with 
     * hashing functions working differently on different implementations. 
     * This only affects minor things like the order of dumping symbol 
     * tables in trace backs. A Map/HashMap would still give a valid 
     * implementation. */
    private SortedMap<String, SymEntry> entries;
    /** space allocated for local variables within this scope */
    private int variableSpace;

    /** This constructs a single scope within a symbol table
     * that is linked to the parent scope, which may be null to
     * indicate that there is no parent. 
     * @param parent scope
     * @param level of nesting of scope 
     */
    public Scope( Scope parent, int level, ProcedureEntry procEntry ) {
        this.parent = parent;
        this.level = level;
        this.procEntry = procEntry;
        /* Initially empty */
        this.entries = new TreeMap<String, SymEntry>();
        variableSpace = 0;
    }
    public Scope getParent() {
        return parent;
    }
    public int getLevel() {
        return level;
    }
    public ProcedureEntry getProcEntry() {
        return procEntry;
    }
    /** @return the set of entries in this scope */
    public Collection<SymEntry> getEntries() {
        return entries.values();
    }
    /** Lookup id starting in the current scope and 
     * thence in the parent scope and so on.
     * @param id Identifier to search for.
     * @return symbol table entry for the id, or null if not found.
     */
    public SymEntry lookup( String id ) {
        if( entries.containsKey( id ) ) {
            return entries.get( id );
        }
        if( parent != null ) {
            return parent.lookup( id );
        }
        return null;
    }
    /** Add an entry to the scope unless an entry for the same name exists.
     * @param entry to be added
     * @return the entry added or null is it already exited in this scope. 
     */
    public SymEntry addEntry( SymEntry entry ) {
        if( entries.containsKey( entry.getIdent() ) ) {
            return null;
        } else {
            entries.put( entry.getIdent(), entry );
            return entry;
        }
    }
    /** @return the amount of space allocated to local variables
     * within the current scope. */
    public int getVariableSpace() {
            return variableSpace;
    }
    /** Allocate space for a local variable.
     * @param size is the amount of space required for the variable.
     * @return address (offset) of allocated space */
    public int allocVariableSpace( int size ) {
            int base = variableSpace;
            variableSpace += size;
            return StackMachine.LOCALS_BASE + base;
    }

    /* Dump contents of this scope */
    @Override
    public String toString() {
        String s = "Level " + level + " " + procEntry.getIdent();
        for( SymEntry entry : entries.values() ) {
            s += Tree.newLine(level) + entry;
        }
        return s;
    }
}
